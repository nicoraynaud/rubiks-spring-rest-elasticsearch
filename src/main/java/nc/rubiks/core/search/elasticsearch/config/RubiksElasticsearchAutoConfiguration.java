package nc.rubiks.core.search.elasticsearch.config;

import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

/**
 * Created by nicoraynaud on 03/05/2017.
 */
@Configuration
@EnableConfigurationProperties({RubiksElasticsearchProperties.class})
public class RubiksElasticsearchAutoConfiguration {

    private final Logger log = LoggerFactory.getLogger(RubiksElasticsearchAutoConfiguration.class);

    private RubiksElasticsearchProperties rubiksElasticsearchProperties;
    private ElasticSearchTemplate elasticSearchTemplate;
    private RestClient restClient;

    public RubiksElasticsearchAutoConfiguration(RubiksElasticsearchProperties rubiksElasticsearchProperties) {
        this.rubiksElasticsearchProperties = rubiksElasticsearchProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticSearchTemplate buildElasticSearchTemplate() {
        elasticSearchTemplate = new ElasticSearchTemplate(rubiksElasticsearchProperties.getContext(), rubiksElasticsearchProperties.getTestMode());
        return elasticSearchTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RestHighLevelClient buildRestClient(ElasticSearchTemplate template) {

        try {
            RestClientBuilder restClientBuilder;
            HttpHost[] hosts = new HttpHost[rubiksElasticsearchProperties.getClusterNodes().size()];
            int i = 0;

            for (Pair<String, Integer> nodes : rubiksElasticsearchProperties.getClusterNodes()) {

                String host = StringUtils.substringAfter(nodes.getLeft(), "://");
                String scheme = StringUtils.substringBefore(nodes.getLeft(), "://");

                if (!scheme.equals("http") && !scheme.equals("https")) {
                    throw new RubiksElasticsearchConfigurationException("Configuration URL must be prefixed with protocol : e.g. http://localhost:9200");
                }

                hosts[i] = new HttpHost(host, nodes.getRight(), scheme);
                i++;
            }

            // If we have authentication, setup it
            if (StringUtils.isNotEmpty(rubiksElasticsearchProperties.getUsername())) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(rubiksElasticsearchProperties.getUsername(), rubiksElasticsearchProperties.getPassword()));

                SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

                restClientBuilder = RestClient.builder(hosts)
                    .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLContext(sslContext));
            } else {
                // Otherwise, create a normal client without authentication
                SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

                restClientBuilder = RestClient.builder(hosts)
                    .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder
                            .setSSLContext(sslContext));
            }

            this.restClient = restClientBuilder.build();
            RestHighLevelClient highLevelClient = new RestHighLevelClient(restClientBuilder);

            // Both
            // - search for classes in the configuration
            // - scan for objects with Annotation @ElasticsearchDocument
            // to automatically initialize indexes if the specific settings (conf and mapping) are provided.
            // If none is provided, it will be automatically created at the first save() of a document
            Stream.concat(rubiksElasticsearchProperties.getIndexedObjects().stream(),
                InitIndexUtil.findElasticsearchDocumentEntities(this.rubiksElasticsearchProperties.getScanBasePackage()).stream())
                .forEach(clazz -> {
                    log.debug("Adding ES Sync for type [{}]", clazz.getSimpleName());
                    InitIndexUtil.initIndices(highLevelClient.getLowLevelClient(), template, clazz);
                });

            return highLevelClient;
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
            throw new RubiksElasticsearchConfigurationException("Unable to configure RubiksElasticsearch library RestClient", ex);
        }
    }

    /**
     * Méthod responsible for cleaning ES cluster from test runs
     *
     * This will be called by Spring context after before a context is destroyed
     * It will only clean when the test-mode is enabled
     */
    @PreDestroy
    void preDestroy() {
        if (rubiksElasticsearchProperties.getTestMode()) {
            try {
                this.elasticSearchTemplate.deleteAllIndices(this.restClient);
                this.restClient.close();
            } catch (Exception ex) {
                log.error("Error while deleting all indexes on PreDestroy event", ex);
            }
        }
    }

}
