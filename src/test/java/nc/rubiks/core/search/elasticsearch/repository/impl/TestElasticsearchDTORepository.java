package nc.rubiks.core.search.elasticsearch.repository.impl;

import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TestElasticsearchDTORepository extends AbstractElasticsearchRepository<TheDto, Long> implements ElasticsearchRepository<TheDto, Long> {

    public TestElasticsearchDTORepository(RestHighLevelClient highLevelClient, ElasticSearchTemplate elasticSearchTemplate, DocumentMapper documentMapper) {
        super(highLevelClient, documentMapper, elasticSearchTemplate, TheDto.class, TheEntity.class);
    }

}
