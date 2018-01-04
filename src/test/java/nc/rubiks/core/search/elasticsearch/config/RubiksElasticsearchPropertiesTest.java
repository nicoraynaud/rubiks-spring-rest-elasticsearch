package nc.rubiks.core.search.elasticsearch.config;

import nc.rubiks.core.search.elasticsearch.repository.impl.CountObject;
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
public class RubiksElasticsearchPropertiesTest {


    @Test
    public void test_setClusterNodesNoData_returnEmpty() {
        // Given
        String clusterNodes = "";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void test_setClusterNodes2_parseThem() {
        // Given
        String clusterNodes = "https://ww.myeld.com:8932,http://elk.intranet.rubiks:80";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result.get(0)).isEqualByComparingTo(Pair.of("https://ww.myeld.com", 8932));
        assertThat(result.get(1)).isEqualByComparingTo(Pair.of("http://elk.intranet.rubiks", 80));
    }

    @Test
    public void test_setClusterNodes1_parseThem() {
        // Given
        String clusterNodes = "https://ww.myeld.com:8932";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();

        // Then
        assertThat(result.get(0)).isEqualByComparingTo(Pair.of("https://ww.myeld.com", 8932));
    }

    @Test(expected = RubiksElasticsearchConfigurationException.class)
    public void test_setClusterNodesInvalid_throwException() {
        // Given
        String clusterNodes = "ww.myeld.com:,elk.intranet.rubiks";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setClusterNodes(clusterNodes);

        // When
        List<Pair<String, Integer>> result = properties.getClusterNodes();
    }

    @Test
    public void test_setIndexedClasses_parseThem() {
        // Given
        String indexedObjects = "java.lang.Object,nc.rubiks.core.search.elasticsearch.repository.impl.CountObject";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setIndexedObjects(indexedObjects);

        // When
        List<Class> result = properties.getIndexedObjects();

        // Then
        assertThat(result).containsExactly(Object.class, CountObject.class);
    }

    @Test(expected = RubiksElasticsearchConfigurationException.class)
    public void test_setWrongIndexedClasses_throwException() {
        // Given
        String indexedObjects = "java.lang.Objet,";
        RubiksElasticsearchProperties properties = new RubiksElasticsearchProperties();
        properties.setIndexedObjects(indexedObjects);

        // When
        List<Class> result = properties.getIndexedObjects();
    }
}
