package nc.rubiks.core.search.elasticsearch.config;

import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicoraynaud on 02/05/2017.
 */
public final class InitIndexUtil {

    private static final Logger log = LoggerFactory.getLogger(InitIndexUtil.class);

    private InitIndexUtil() {

    }

    /**
     * Indexes a given entity based on the configuration files
     * specified in the resources folder
     * @param elasticSearchTemplate the ES REST template client
     * @param entityClass the entity class to create the index for
     */
    static void initIndices(RestClient client, ElasticSearchTemplate elasticSearchTemplate, Class entityClass) {

        ElasticsearchDocument annotation = (ElasticsearchDocument) entityClass.getAnnotation(ElasticsearchDocument.class);
        String specifiedIndexName = annotation != null ? annotation.indexName() : null;

        String indexName =
            StringUtils.isBlank(specifiedIndexName) ? StringUtils.lowerCase(entityClass.getSimpleName()) : StringUtils.lowerCase(specifiedIndexName);

        log.info("Entity [{}] will be indexed under document [{}]", entityClass, indexName);

        if (elasticSearchTemplate.indexExists(client, indexName, indexName)) {
            log.info("Index for {} already exists, skipping configuration", indexName);
            return;
        }

        log.info("Index for [{}] does not yet exist, configuring it...", indexName);

        try {
            // Create index
            ResourcePatternResolver settingPatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] settingLocation = settingPatternResolver.getResources("classpath*:**/config/elasticsearch/" + indexName + ".setting.json");
            InputStream settingResource =  null;
            if (settingLocation.length > 0) {
                settingResource = settingLocation[0].getInputStream();
                log.info("setting.json file found for resource : {}", settingLocation[0].getURL());
            } else {
                log.info("No setting.json file found for configuration.");
            }
            if (settingResource != null) {
                String setting = IOUtils.toString(settingResource, StandardCharsets.UTF_8);
                elasticSearchTemplate.createIndex(client, indexName, indexName, setting);
            }

            // Add mapping to Index
            ResourcePatternResolver mappingPatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] mappingLocation = mappingPatternResolver.getResources("classpath*:**/config/elasticsearch/" + indexName + ".mapping.json");
            InputStream mappingResource =  null;
            if (mappingLocation.length > 0) {
                mappingResource = mappingLocation[0].getInputStream();
                log.info("mapping.json file found for resource : {}", mappingLocation[0].getURL());
            } else {
                log.info("No mapping.json file found for configuration.");
            }
            if (mappingResource != null) {
                String mapping = IOUtils.toString(mappingResource, StandardCharsets.UTF_8);

                // Create default index if none existing
                if (settingResource == null) {
                    elasticSearchTemplate.createIndex(client, indexName, indexName, "");
                }

                elasticSearchTemplate.putMapping(client, indexName, indexName, mapping);
            }

        } catch (Exception ex) {
            log.error("Unable to setup elasticsearch configuration for index {} : {}", entityClass, ex);
        }

        log.info("Entity [{}] successfully configured under document [{}]", entityClass, indexName);
    }

    /**
     * Scans the provided package for objects mapped with @ElasticsearchDocument annotation
     * All these objects are subject to be indexed in an ES cluster.
     * @param basePackage the base package to start scanning from
     * @return The list of classes to be indexed
     */
    public static List<Class> findElasticsearchDocumentEntities(String basePackage) {

        List<Class> classes = new ArrayList<>();

        // Scan for entities with Annotation @ElasticsearchDocument to automatically
        // initialize indexes if the specific settings (conf and mapping) are provided.
        // If none is provided, it will be automatically created at the first save() of a document
        try {
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(ElasticsearchDocument.class));
            for (BeanDefinition bd : scanner.findCandidateComponents(StringUtils.isBlank(basePackage) ? "nc.rubiks" : basePackage)) {
                    classes.add(Class.forName(bd.getBeanClassName()));
            }
        } catch (ClassNotFoundException e) {
            throw new RubiksElasticsearchConfigurationException("An error occured while scanning for Elasticsearch Indexed entities", e);
        }

        return classes;
    }
}
