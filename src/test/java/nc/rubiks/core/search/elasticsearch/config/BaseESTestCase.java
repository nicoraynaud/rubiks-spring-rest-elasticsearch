package nc.rubiks.core.search.elasticsearch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by 2617ray on 03/05/2017.
 */
public abstract class BaseESTestCase {

    protected static RestHighLevelClient highLevelClient;
    protected static ElasticSearchTemplate template;

    @BeforeClass
    public static void beforeClass() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map confYml = mapper.readValue(BaseESTestCase.class.getClassLoader().getResourceAsStream("config/application.yml"), Map.class);

        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(((Map) ((Map)confYml.get("rubiks")).get("elasticsearch")).get("cluster-nodes").toString());
        if (((Map)((Map)confYml.get("rubiks")).get("elasticsearch")).get("username") != null) {
            properties.setUsername(((Map) ((Map) confYml.get("rubiks")).get("elasticsearch")).get("username").toString());
        }
        if (((Map)((Map)confYml.get("rubiks")).get("elasticsearch")).get("password") != null) {
            properties.setPassword(((Map) ((Map) confYml.get("rubiks")).get("elasticsearch")).get("password").toString());
        }

        // Always set test mode to true
        properties.setTestMode(true);

        TestRubiksElasticsearchAutoConfiguration conf = new TestRubiksElasticsearchAutoConfiguration(properties);
        template = conf.buildElasticSearchTemplate();
        highLevelClient = conf.buildRestClient(template);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if (template != null && highLevelClient != null) template.deleteAllIndices(highLevelClient.getLowLevelClient());
        if (highLevelClient != null) highLevelClient.close();
    }

    private static class TestRubiksElasticsearchAutoConfiguration extends RubiksElasticsearchAutoConfiguration {
        private TestRubiksElasticsearchAutoConfiguration(RubiksElasticsearchProperties rubiksElasticsearchProperties) {
            super(rubiksElasticsearchProperties);
        }
    }
}
