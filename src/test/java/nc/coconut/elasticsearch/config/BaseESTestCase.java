package nc.coconut.elasticsearch.config;

import nc.coconut.elasticsearch.repository.ElasticSearchTemplate;
import org.elasticsearch.client.RestClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by 2617ray on 03/05/2017.
 */
public abstract class BaseESTestCase {

    protected static RestClient client;
    protected static ElasticSearchTemplate template;

    @BeforeClass
    public static void beforeClass() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setClusterNodes("http://localhost:9200");

        // Always set test mode to true
        properties.setTestMode(true);

        TestElasticsearchConfiguration conf = new TestElasticsearchConfiguration(properties);
        template = conf.buildElasticSearchTemplate();
        client = conf.buildRestClient(template);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if (template != null) template.deleteAllIndices(client);
        if (client != null) client.close();
    }

    private static class TestElasticsearchConfiguration extends BaseElasticsearchConfiguration {
        private TestElasticsearchConfiguration(ElasticsearchProperties elasticsearchProperties) {
            super(elasticsearchProperties);
        }
    }
}
