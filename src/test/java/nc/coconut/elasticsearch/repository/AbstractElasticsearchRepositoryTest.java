package nc.coconut.elasticsearch.repository;

import nc.coconut.elasticsearch.config.BaseESTestCase;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by 2617ray on 03/05/2017.
 */
@RunWith(JUnit4.class)
public class AbstractElasticsearchRepositoryTest extends BaseESTestCase {

    private TestElasticsearchRepository testElasticsearchRepository;

    @Before
    public void before() {
        template.deleteIndice(client, "theentity");
        testElasticsearchRepository = new TestElasticsearchRepository(client, template, new TestMapper());
    }

    private void indexEntity(String id, String prop) {
        template.index(client, "theentity", "theentity", id,
            "{ \"id\": " + id + ", \"prop\": \"" + prop + "\" }");
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
    public void test_findOne() {
        // Given
        indexEntity("7777", null);

        // When
        TheEntity e = testElasticsearchRepository.findOne(7777l);

        // Then
        assertThat(e.getId()).isEqualTo(7777l);
    }

    @Test
    public void test_exists_true() {
        // Given
        indexEntity("7778", null);

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
        indexEntity("1111", "value1");
        indexEntity("1112", "value2");

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search("prop:value1");

        // Then
        Assertions.assertThat(results).extracting("id").contains(1111l);
    }

    @Test
    public void searchQueryString_postfilter_return_1() {

        // Given
        indexEntity("1111", "value");
        indexEntity("1112", "value");

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search("prop:value", "id:1111");

        // Then
        Assertions.assertThat(results).extracting("id").contains(1111l);
    }

    @Test
    public void searchQueryString_return_0() {

        // Given
        indexEntity("8889", null);
        indexEntity("8888", null);

        // When
        Iterable<TheEntity> results = testElasticsearchRepository.search("id:101010");

        // Then
        Assertions.assertThat(results).hasSize(0);
    }

    @Test
    public void searchQueryString_paginated_return_page() {

        // Given
        indexEntity("1001", "element");
        indexEntity("1002", "element");
        indexEntity("1003", "element");
        indexEntity("1004", "element");

        // When
        Page<TheEntity> results = testElasticsearchRepository.search(new PageRequest(1, 2), "prop:element");

        // Then
        Assertions.assertThat(results).hasSize(2);
        assertThat(results.getNumber()).isEqualTo(1);
        assertThat(results.getTotalPages()).isEqualTo(2);
        assertThat(results.getTotalElements()).isEqualTo(4);
    }

    @Test
    public void count_return4() {

        // Given
        indexEntity("1001", "element");
        indexEntity("1002", "element");
        indexEntity("1003", "element");
        indexEntity("1004", "element");

        // When
        long count = testElasticsearchRepository.count();

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    public void delete_existing_deleteIt() {

        // Given
        indexEntity("1001", "element");
        assertThat(testElasticsearchRepository.exists(1001l)).isTrue();

        TheEntity ent = new TheEntity();
        ent.setId(1001l);

        // When
        testElasticsearchRepository.delete(ent);

        // Then
        assertThat(testElasticsearchRepository.exists(1001l)).isFalse();
    }

    @Test
    public void deleteAll_deleteAllItems() {

        // Given
        indexEntity("1001", "element");
        indexEntity("1002", "element");
        indexEntity("1003", "element");
        indexEntity("1004", "element");
        assertThat(testElasticsearchRepository.count()).isEqualTo(4);

        // When
        testElasticsearchRepository.deleteAll();

        // Then
        assertThat(testElasticsearchRepository.count()).isEqualTo(0);
    }

    @Test
    public void searchWithJsonQuery() {

        // Given
        indexEntity("1001", "element");
        indexEntity("1002", "element");
        indexEntity("1111", "element");

        String jsonQuery = "{\n" +
            "    \"query\" : {\n" +
            "        \"term\" : { \"id\" : \"1002\" }\n" +
            "    }\n" +
            "}";

        // When
        Page<TheEntity> result = testElasticsearchRepository.searchComplex(new PageRequest(0,5), jsonQuery);

        // Then
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1002);
    }
}
