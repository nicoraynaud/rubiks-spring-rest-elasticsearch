package nc.rubiks.core.search.elasticsearch.repository.impl;

import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TestElasticsearchRepository extends AbstractElasticsearchRepository<TheEntity, Long> implements ElasticsearchRepository<TheEntity, Long> {

    public TestElasticsearchRepository(RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
        super(highLevelClient, documentMapper, elasticSearchTemplate, TheEntity.class);
    }

}
