package nc.rubiks.core.search.elasticsearch.entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class ElasticsearchSyncActionTest {

    @Test
    public void test_equals() {

        // Given
        ElasticsearchSyncAction es1 = new ElasticsearchSyncAction();
        es1.setId(UUID.randomUUID());
        es1.setNbTryouts(1);
        es1.setObjId("456");
        es1.setObjType("objType");
        es1.setAction(ElasticsearchSyncActionEnum.CREATE);

        ElasticsearchSyncAction es2 = new ElasticsearchSyncAction();
        es2.setId(es1.getId());
        es2.setNbTryouts(1);
        es2.setObjId("456");
        es2.setObjType("objType");
        es2.setAction(ElasticsearchSyncActionEnum.CREATE);
        es2.setCreatedDate(es1.getCreatedDate());

        // When & Then
        assertThat(es1.equals(es2)).isTrue();
    }

    @Test
    public void test_hashCode() {

        // Given
        ElasticsearchSyncAction es1 = new ElasticsearchSyncAction();
        es1.setId(UUID.randomUUID());
        es1.setNbTryouts(1);
        es1.setObjId("456");
        es1.setObjType("objType");
        es1.setAction(ElasticsearchSyncActionEnum.CREATE);

        ElasticsearchSyncAction es2 = new ElasticsearchSyncAction();
        es2.setId(es1.getId());
        es2.setNbTryouts(1);
        es2.setObjId("456");
        es2.setObjType("objType");
        es2.setAction(ElasticsearchSyncActionEnum.CREATE);
        es2.setCreatedDate(es1.getCreatedDate());

        // When & Then
        assertThat(es1.hashCode()).isEqualTo(es2.hashCode());
    }
}
