package nc.coconut.elasticsearch.config;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by 2617ray on 03/05/2017.
 */
@RunWith(JUnit4.class)
public class ElasticsearchPropertiesTest {


    @Test
    public void test_setClusterNodesNoData_returnEmpty() {
        // Given
        String clusterNodes = "";
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void test_setClusterNodes2_parseThem() {
        // Given
        String clusterNodes = "https://ww.myeld.com:8932,http://elk.intranet.world:80";
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result.get(0)).isEqualByComparingTo(Pair.of("https://ww.myeld.com", 8932));
        assertThat(result.get(1)).isEqualByComparingTo(Pair.of("http://elk.intranet.world", 80));
    }

    @Test
    public void test_setClusterNodes1_parseThem() {
        // Given
        String clusterNodes = "https://ww.myeld.com:8932";
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result.get(0)).isEqualByComparingTo(Pair.of("https://ww.myeld.com", 8932));
    }

    @Test(expected = ElasticsearchConfigurationException.class)
    public void test_setClusterNodesInvalid_throwException() {
        // Given
        String clusterNodes = "ww.myeld.com:,elk.intranet.world";
        ElasticsearchProperties properties = new ElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();
    }
}
