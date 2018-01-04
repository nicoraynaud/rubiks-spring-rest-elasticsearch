package nc.rubiks.core.search.elasticsearch.repository.impl;

import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TestAnnotatedElasticsearchRepository extends AbstractElasticsearchRepository<TheAnnotatedEntity, Long> implements ElasticsearchRepository<TheAnnotatedEntity, Long> {

    public TestAnnotatedElasticsearchRepository(RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
        super(highLevelClient, documentMapper, elasticSearchTemplate, TheAnnotatedEntity.class);
    }

}
