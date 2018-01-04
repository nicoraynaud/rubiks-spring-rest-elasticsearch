package nc.rubiks.core.search.elasticsearch.endpoint;

import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConfigurationProperties(prefix = "endpoints.elasticsearch_sync_reset")
@ConditionalOnProperty(prefix = "rubiks.elasticsearch.sync", name = "enabled", havingValue = "true")
public class ElasticsearchSyncEndpoint extends AbstractEndpoint<ElasticsearchSyncReset> {

    private final Logger log = LoggerFactory.getLogger(ElasticsearchSyncEndpoint.class);

    private final ElasticsearchSyncService elasticsearchSyncService;

    public ElasticsearchSyncEndpoint(ElasticsearchSyncService elasticsearchSyncService) {
        super("elasticsearch_sync_reset", true);
        this.elasticsearchSyncService = elasticsearchSyncService;
    }

    @Override
    @Transactional
    public ElasticsearchSyncReset invoke() {
        log.info("Reseting all ElasticsearchSyncAction nbTryouts to 0...");
        this.elasticsearchSyncService.reset();
        log.info("Reset done.");
        return new ElasticsearchSyncReset();
    }
}
