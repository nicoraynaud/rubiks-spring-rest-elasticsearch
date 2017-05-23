package nc.coconut.elasticsearch.repository;

import nc.coconut.elasticsearch.mapper.EntityMapper;
import nc.coconut.elasticsearch.repository.impl.ElasticsearchRepository;
import org.elasticsearch.client.RestClient;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TestElasticsearchRepository extends AbstractElasticsearchRepository<TheEntity, Long> implements ElasticsearchRepository<TheEntity, Long> {

    public TestElasticsearchRepository(RestClient client, ElasticSearchTemplate elasticSearchTemplate, EntityMapper entityMapper) {
        super(client, elasticSearchTemplate, entityMapper, TheEntity.class);
    }

}
