package nc.coconut.elasticsearch.config;

import nc.coconut.elasticsearch.repository.ElasticSearchTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Base Elasticsearch configuration class.
 *
 * Should be inherited to leverage automation of config setup
 */
public abstract class BaseElasticsearchConfiguration {

    private final Logger log = LoggerFactory.getLogger(BaseElasticsearchConfiguration.class);

    protected ElasticsearchProperties elasticsearchProperties;
    private ElasticSearchTemplate elasticSearchTemplate;

    public BaseElasticsearchConfiguration(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Bean
    public ElasticSearchTemplate buildElasticSearchTemplate() {
        elasticSearchTemplate = new ElasticSearchTemplate(elasticsearchProperties.getContext(), elasticsearchProperties.getTestMode());
        return elasticSearchTemplate;
    }

    protected RestClient buildRestClient(ElasticSearchTemplate template, Class... entities) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        RestClient client;
        HttpHost[] hosts = new HttpHost[elasticsearchProperties.getClusterNodes().size()];
        int i = 0;

        for (Pair<String, Integer> nodes : elasticsearchProperties.getClusterNodes()) {

            String host;
            String scheme;

            if (StringUtils.indexOfIgnoreCase(nodes.getLeft(), "https://") != -1) {
                host = StringUtils.substring(
                    nodes.getLeft(),
                    "https://".length());
                scheme = "https";

            } else if (StringUtils.indexOfIgnoreCase(nodes.getLeft(), "http://") != -1) {
                host = StringUtils.substring(
                    nodes.getLeft(),
                    "http://".length());
                scheme = "http";
            } else {
                throw new ElasticsearchConfigurationException("Configuration URL must be prefixed with protocol : e.g. http://localhost:9200");
            }

            hosts[i] = new HttpHost(host, nodes.getRight(), scheme);
            i++;
        }

        // If we have authentication, setup it
        if (StringUtils.isNotEmpty(elasticsearchProperties.getUsername())) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));

            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (certificate, authType) -> true).build();

            client = RestClient.builder(hosts)
                .setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setSSLContext(sslContext))
                .build();
        }
        // Otherwise, create a normal client without authentication
        else {
            SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (certificate, authType) -> true).build();

            client = RestClient.builder(hosts)
                .setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                        .setSSLContext(sslContext))
                .build();
        }

        // Add to entities, the entities to setup the index for.
        // if no specific settings, you do not have to initialize them.
        // Only setup for specific indexing needs
        if (entities != null) {
            Arrays.stream(entities).forEach(c -> InitIndexUtil.initIndices(client, template, c));
        }

        return client;
    }

    /**
     * Method responsible for cleaning ES cluster from test runs
     *
     * This will be called by Spring context after before a context is destroyed
     * It will only clean when the test-mode is enabled
     */
    @PreDestroy
    private void preDestroy() {
        if (elasticsearchProperties.getTestMode()) {
            try {
                RestClient client = buildRestClient(this.elasticSearchTemplate, (Class) null);
                this.elasticSearchTemplate.deleteAllIndices(client);
                client.close();
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException ex) {
                log.error("Error while deleting all indexes on PreDestroy event", ex);
            }
        }
    }

}
