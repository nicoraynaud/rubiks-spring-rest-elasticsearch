package nc.rubiks.core.search.elasticsearch.repository.impl;

import nc.rubiks.core.search.elasticsearch.config.BaseESTestCase;
import nc.rubiks.core.search.elasticsearch.model.Result;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by 2617ray on 03/05/2017.
 */
@RunWith(JUnit4.class)
public class AbstractElasticsearchRepositoryTest extends BaseESTestCase {

    private TestElasticsearchRepository testElasticsearchRepository;

    @Before
    public void before() {
        template.deleteIndex(highLevelClient.getLowLevelClient(), "theentity");
        testElasticsearchRepository = new TestElasticsearchRepository(highLevelClient, template, new TestMapper());
    }

    private void indexEntity(Long id, String prop) {
        testElasticsearchRepository.save(new TheEntity().id(id).prop(prop));
    }

    @Test
    public void getIndexName_whenNoDTO_returnSimpleClassName() {

        // Given
        TestElasticsearchRepository testElasticsearchRepository = new TestElasticsearchRepository(highLevelClient, template, new TestMapper());

        // When & Then
        assertThat(testElasticsearchRepository.indexName).isEqualTo("theentity");
    }

    @Test
    public void getIndexName_whenDTO_returnSimpleClassNameOfEntityClass() {

        // Given
        TestElasticsearchDTORepository testElasticsearchDTORepository = new TestElasticsearchDTORepository(highLevelClient, template, new TestMapper());

        // When & Then
        assertThat(testElasticsearchRepository.indexName).isEqualTo("theentity");
    }

    @Test
    public void getIndexName_whenIndexNameInAnnotation_returnLowercaseIndexName() {

        // Given
        TestAnnotatedElasticsearchRepository testAnnotatedElasticsearchRepository = new TestAnnotatedElasticsearchRepository(highLevelClient, template, new TestMapper());

        // When & Then
        assertThat(testAnnotatedElasticsearchRepository.indexName).isEqualTo("kikouu");
    }

    @Test
    public void test_save() {
        // Given
        TheEntity e = new TheEntity();
        e.setId(5556l);

        // When
        testElasticsearchRepository.save(e);

        // Then
        assertThat(testElasticsearchRepository.findOne(5556l).getId()).isEqualTo(5556l);
    }

    @Test
    public void test_save_multipleentities() {
        // Given
        TheEntity e = new TheEntity();
        e.setId(5558l);
        TheEntity e2 = new TheEntity();
        e2.setId(6669l);

        // When
        testElasticsearchRepository.save(Arrays.asList(e, e2));

        // Then
        assertThat(testElasticsearchRepository.findOne(5558l).getId()).isEqualTo(5558l);
        assertThat(testElasticsearchRepository.findOne(6669l).getId()).isEqualTo(6669l);
    }

    @Test
    public void test_save_exception_doNothing() {
        // Given
        TheEntity e = new TheEntity();
        e.setId(5558l);

        TestElasticsearchRepository repository = new TestElasticsearchRepository(highLevelClient,null, new TestMapper());

        // When
        TheEntity result = repository.save(e);

        // Then
        assertThat(result).isEqualTo(e);
    }


    @Test
    public void test_findOne_exception_returnNull() {
        // Given
        TestElasticsearchRepository repository = new TestElasticsearchRepository(null, null, null);

        // When
        TheEntity e = repository.findOne(7777l);

        // Then
        assertThat(e).isNull();
    }

    @Test
    public void test_findOne() {
        // Given
        indexEntity(7777l, null);

        // When
        TheEntity e = testElasticsearchRepository.findOne(7777l);

        // Then
        assertThat(e.getId()).isEqualTo(7777l);
    }

    @Test
    public void test_exists_true() {
        // Given
        indexEntity(7778l, null);

        // When
        boolean exists = testElasticsearchRepository.exists(7778l);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    public void test_exists_false() {
        // Given

        // When
        boolean exists = testElasticsearchRepository.exists(7779l);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    public void searchQueryString_return_1() {

        // Given
        indexEntity(1111l, "value1");
        indexEntity(1112l, "value2");

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search(new PageRequest(0, 10), "prop:value1");

        // Then
        assertThat(results).extracting("id").contains(1111l);
    }

    @Test
    public void searchQueryString_postfilter_return_1() {

        // Given
        indexEntity(1111l, "value");
        indexEntity(1112l, "value");

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search(new PageRequest(0, 10), "prop:value AND id:1111");

        // Then
        assertThat(results).extracting("id").contains(1111l);
    }

    @Test
    public void searchQueryString_return_0() {

        // Given
        indexEntity(8889l, null);
        indexEntity(8888l, null);

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search(new PageRequest(0, 10), "id:101010");

        // Then
        assertThat(results).hasSize(0);
    }

    @Test
    public void searchQueryString_paginated_return_page() {

        // Given
        indexEntity(1001l, "element");
        indexEntity(1002l, "element");
        indexEntity(1003l, "element");
        indexEntity(1004l, "element");

        // When
        Page<TheEntity> results = testElasticsearchRepository.search(new PageRequest(1, 2), "prop:element");

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.getNumber()).isEqualTo(1);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getTotalElements()).isEqualTo(4);
    }

    @Test
    public void searchQueryString_sorted_return_sorted_page() {

        // Given
        indexEntity(1001l, "zelement");
        indexEntity(1002l, "element1");
        indexEntity(1003l, "element2");
        indexEntity(1004l, "element1");

        // When
        Sort sort = new Sort(Sort.Direction.ASC, "prop.keyword").and(new Sort(Sort.Direction.DESC, "id"));
        PageRequest pr = new PageRequest(0, 4, sort);
        Page<TheEntity> results = testElasticsearchRepository.search(pr, "prop:*");

        // Then
        assertThat(results).hasSize(4);
        assertThat(results.getNumber()).isEqualTo(0);
        assertThat(results.getTotalPages()).isEqualTo(1);
        assertThat(results.getTotalElements()).isEqualTo(4);

        assertThat(results.getContent()).extracting("id").containsExactly(1004l, 1002l, 1003l, 1001l);
    }

    @Test
    public void searchQueryString_exception_returnEmpty() {

        // Given

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search(new PageRequest(0, 10), "prop:value1");

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    public void count_return4() {

        // Given
        indexEntity(1001l, "element");
        indexEntity(1002l, "element");
        indexEntity(1003l, "element");
        indexEntity(1004l, "element");

        // When
        long count = testElasticsearchRepository.count();

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    public void count_indexNotFound_return0() {

        // Given

        // When
        long count = testElasticsearchRepository.count();

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void delete_existing_deleteIt() {

        // Given
        indexEntity(1001l, "element");
        assertThat(testElasticsearchRepository.exists(1001l)).isTrue();

        TheEntity ent = new TheEntity();
        ent.setId(1001l);

        // When
        testElasticsearchRepository.delete(ent);

        // Then
        assertThat(testElasticsearchRepository.exists(1001l)).isFalse();
    }

    @Test
    public void delete_indexNotExisting_return() {

        // Given

        // When
        testElasticsearchRepository.delete(1001l);

        // Then
        assertThat(testElasticsearchRepository.exists(1001l)).isFalse();
    }

    @Test
    public void delete_objects_deleteById() {

        // Given
        indexEntity(1001l , "element");
        indexEntity(1002l , "element");
        indexEntity(1003l , "element");
        assertThat(testElasticsearchRepository.exists(1001l)).isTrue();
        assertThat(testElasticsearchRepository.exists(1002l)).isTrue();
        assertThat(testElasticsearchRepository.exists(1003l)).isTrue();

        TheEntity ent = new TheEntity();
        ent.setId(1001l);
        TheEntity ent2 = new TheEntity();
        ent2.setId(1002l);

        // When
        testElasticsearchRepository.delete(Arrays.asList(ent, ent2));

        // Then
        assertThat(testElasticsearchRepository.exists(1001l)).isFalse();
        assertThat(testElasticsearchRepository.exists(1002l)).isFalse();
        assertThat(testElasticsearchRepository.exists(1003l)).isTrue();
    }

    @Test
    public void deleteAll_deleteAllItems() {

        // Given
        indexEntity(1001l, "element");
        indexEntity(1002l, "element");
        indexEntity(1003l, "element");
        indexEntity(1004l, "element");
        assertThat(testElasticsearchRepository.count()).isEqualTo(4);

        // When
        testElasticsearchRepository.deleteAll();

        // Then
        assertThat(testElasticsearchRepository.count()).isEqualTo(0);
    }

    @Test
    public void deleteAll_indexNotExisting_return() {

        // Given

        // When
        testElasticsearchRepository.deleteAll();

        // Then
        assertThat(testElasticsearchRepository.count()).isEqualTo(0);
    }

    @Test
    public void searchWithJsonQuery() {

        // Given
        indexEntity(1001l, "element");
        indexEntity(1002l, "element");
        indexEntity(1111l, "element");

        String jsonQuery = "{\n" +
            "    \"query\" : {\n" +
            "        \"term\" : { \"id\" : \"1002\" }\n" +
            "    }\n" +
            "}";

        // When
        Result<TheEntity> result = testElasticsearchRepository.searchComplex(new PageRequest(0,5), jsonQuery);

        // Then
        assertThat(result.getHits().getResults().getTotalPages()).isEqualTo(1);
        assertThat(result.getHits().getResults().getTotalElements()).isEqualTo(1);
        assertThat(result.getHits().getResults().getContent()).hasSize(1);
        assertThat(result.getHits().getResults().getContent().get(0).getId()).isEqualTo(1002);
    }

    @Test
    public void searchComplex_Exception_ReturnEmptyList() {

        // Given
        String query = "{}";

        // When
        Result<TheEntity> result = testElasticsearchRepository.searchComplex(new PageRequest(0,5), query);

        // Then
        assertThat(result.getHits().getResults().getSize()).isEqualTo(0);
        assertThat(result.getHits().getResults().getTotalElements()).isEqualTo(0);
        assertThat(result.getHits().getResults().getContent()).isEmpty();
    }

    @Test
    public void searchComplex_WithFaceting_ReturnResultsAndFacets() {

        // Given
        indexEntity(1001l, "statut1");
        indexEntity(1002l, "statut1");
        indexEntity(1111l, "statut2");

        String jsonQuery = "{\n" +
            "    \"query\" : {\n" +
            "        \"match_all\" : {}\n" +
            "    },\n" +
            "    \"aggs\": {\n" +
            "        \"by_prop\": {\n" +
            "            \"terms\": { \"field\": \"prop.keyword\" }\n" +
            "        }\n" +
            "    }" +
            "}";

        // When
        Result<TheEntity> result = testElasticsearchRepository.searchComplex(new PageRequest(0,5), jsonQuery);

        // Then
        assertThat(result.getHits().getTotal()).isEqualTo(3);
        assertThat(result.getHits().getResults()).hasSize(3);
        assertThat(result.getHits().getResults()).extracting("id").containsExactlyInAnyOrder(1001L, 1002L, 1111L);
        assertThat(result.getAggregations()).hasSize(1);
        assertThat((Map) result.getAggregations().get("by_prop")).hasSize(3);
        assertThat((List) ((Map) result.getAggregations().get("by_prop")).get("buckets")).hasSize(2);
        assertThat(((Map) ((List) ((Map) result.getAggregations().get("by_prop")).get("buckets")).get(0)).get("key")).isEqualTo("statut1");
        assertThat(((Map) ((List) ((Map) result.getAggregations().get("by_prop")).get("buckets")).get(0)).get("doc_count")).isEqualTo(2);
        assertThat(((Map) ((List) ((Map) result.getAggregations().get("by_prop")).get("buckets")).get(1)).get("key")).isEqualTo("statut2");
        assertThat(((Map) ((List) ((Map) result.getAggregations().get("by_prop")).get("buckets")).get(1)).get("doc_count")).isEqualTo(1);
    }

    @Test
    public void search_WithQueryAndAggregation_ReturnResponseWithBoth() {

        // Given
        indexEntity(1001l, "statut1");
        indexEntity(1002l, "statut1");
        indexEntity(1111l, "statut2");
        indexEntity(1113l, "statut2");
        indexEntity(1114l, "statut2");
        indexEntity(1115l, "statut3");
        indexEntity(1116l, "statut4");

        QueryBuilder query = QueryBuilders.queryStringQuery("-statut4");

        AggregationBuilder aggregations = AggregationBuilders.terms("by_prop").field("prop.keyword");

        // When
        SearchResponse response = testElasticsearchRepository.search(new PageRequest(0, 5), query, aggregations);

        // Then
        assertThat(response.getHits().totalHits).isEqualTo(6);
        assertThat(response.getHits().getHits()).hasSize(5);
        assertThat(response.getAggregations().asList()).hasSize(1);
        assertThat(response.getAggregations().get("by_prop").getName()).isEqualTo("by_prop");
        assertThat(response.getAggregations().get("by_prop").getType()).isEqualTo("sterms");
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets()).hasSize(3);
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(0).getKey()).isEqualTo("statut2");
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(0).getDocCount()).isEqualTo(3);
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(1).getKey()).isEqualTo("statut1");
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(1).getDocCount()).isEqualTo(2);
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(2).getKey()).isEqualTo("statut3");
        assertThat(((Terms) response.getAggregations().get("by_prop")).getBuckets().get(2).getDocCount()).isEqualTo(1);

    }
}
