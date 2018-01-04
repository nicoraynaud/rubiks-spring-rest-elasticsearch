package nc.rubiks.core.search.elasticsearch.config;

import nc.rubiks.core.search.elasticsearch.repository.impl.ElasticSearchTemplate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class BaseRubiksElasticsearchAutoConfigurationTest {

    private ElasticSearchTemplate elasticSearchTemplate;

    @Before
    public void setUp() {
        elasticSearchTemplate = mock(ElasticSearchTemplate.class);
    }

    @Test
    public void test_preDestroy_callDeleteIndices() throws IllegalAccessException {

        // Given
        String clusterNodes = "https://ww.myeld.com:8932,http://elk.intranet.rubiks:80";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);
        properties.setTestMode(true);

        RubiksElasticsearchAutoConfiguration testRubiksElasticsearchConfiguration = new RubiksElasticsearchAutoConfiguration(properties);
        FieldUtils.writeField(testRubiksElasticsearchConfiguration, "elasticSearchTemplate", elasticSearchTemplate, true);

        // When
        testRubiksElasticsearchConfiguration.preDestroy();

        // Then
        verify(elasticSearchTemplate, times(1)).deleteAllIndices(any(RestClient.class));

    }

    @Test
    public void test_preDestroy_nonTestMode_doNothing() throws IllegalAccessException {

        // Given
        String clusterNodes = "http://localhost:9200";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);
        properties.setTestMode(false);

        RubiksElasticsearchAutoConfiguration testRubiksElasticsearchConfiguration = new RubiksElasticsearchAutoConfiguration(properties);
        FieldUtils.writeField(testRubiksElasticsearchConfiguration, "elasticSearchTemplate", elasticSearchTemplate, true);

        // When
        testRubiksElasticsearchConfiguration.preDestroy();

        // Then
        verify(elasticSearchTemplate, never()).deleteAllIndices(any(RestClient.class));

    }

}
