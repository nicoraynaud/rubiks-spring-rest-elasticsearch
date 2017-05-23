package nc.coconut.elasticsearch.config;

import nc.coconut.elasticsearch.repository.ElasticSearchTemplate;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Utils class to scan resources for indice configuration and mapping
 * given a Class in parameter.
 */
public class InitIndexUtil {

    private static final Logger log = LoggerFactory.getLogger(InitIndexUtil.class);

    private InitIndexUtil() {

    }

    /**
     * Indexes a given entity based on the configuration files
     * specified in the resources folder
     * @param elasticSearchTemplate the ES REST template client
     * @param entityClass the entity class to create the index for
     */
    public static void initIndices(RestClient client, ElasticSearchTemplate elasticSearchTemplate, Class entityClass) {

        String entityName = entityClass.getSimpleName().toLowerCase();

        log.info("Entity [{}] will be indexed under document [{}]", entityClass, entityName);

        if (elasticSearchTemplate.indiceExists(client, entityName, entityName)) {
            log.info("Index for {} already exists, skipping configuration", entityName);
            return;
        }

        log.info("Index for [{}] does not yet exist, configuring it...", entityName);

        try {
            // Create index
            ResourcePatternResolver settingPatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] settingLocation = settingPatternResolver.getResources("classpath*:**/config/elasticsearch/" + entityName + ".setting.json");
            InputStream settingResource =  null;
            if (settingLocation.length > 0) {
                settingResource = settingLocation[0].getInputStream();
                log.info("setting.json file found for resource : {}", settingLocation[0].getURL());
            } else {
                log.info("No setting.json file found for configuration.");
            }
            if (settingResource != null) {
                String setting = new Scanner(settingResource).useDelimiter("\\A").next();
                elasticSearchTemplate.createIndice(client, entityName, entityName, setting);
            }

            // Add mapping to Index
            ResourcePatternResolver mappingPatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] mappingLocation = mappingPatternResolver.getResources("classpath*:**/config/elasticsearch/" + entityName + ".mapping.json");
            InputStream mappingResource =  null;
            if (mappingLocation.length > 0) {
                mappingResource = mappingLocation[0].getInputStream();
                log.info("mapping.json file found for resource : {}", mappingLocation[0].getURL());
            } else {
                log.info("No mapping.json file found for configuration.");
            }
            if (mappingResource != null) {
                String mapping = new Scanner(mappingResource).useDelimiter("\\A").next();

                // Create default index if none existing
                if (settingResource == null) {
                    elasticSearchTemplate.createIndice(client, entityName, entityName, "");
                }

                elasticSearchTemplate.putMapping(client, entityName, entityName, mapping);
            }

        } catch (Exception ex) {
            log.error("Unable to setup elasticsearch configuration for index {} : {}", entityClass, ex);
        }

        log.info("Entity [{}] successfully configured under document [{}]", entityClass, entityName);
    }
}
