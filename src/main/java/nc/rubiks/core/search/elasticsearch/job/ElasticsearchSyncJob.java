package nc.rubiks.core.search.elasticsearch.job;

import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author nicoraynaud
 */
@Component
@ConditionalOnProperty(prefix = "rubiks.elasticsearch.sync", name = "enabled", havingValue = "true")
public class ElasticsearchSyncJob {

    private final Logger log = LoggerFactory.getLogger(ElasticsearchSyncJob.class);

    private final ElasticsearchSyncService elasticsearchSyncService;

    public ElasticsearchSyncJob(ElasticsearchSyncService elasticsearchSyncService) {
        this.elasticsearchSyncService = elasticsearchSyncService;
    }

    /**
     * This job runs the sync() service method
     *
     * It is scheduled to run every n seconds
     * The lock is configured to be maintained for 1 minute at least (this lock is released when
     * the job is done or if this cluster-node dies, after 1 minute max)
     */
    @Scheduled(fixedDelayString = "${rubiks.elasticsearch.sync.rate-milliseconds}")
    @SchedulerLock(name = "runEsSyncJob", lockAtMostFor = 1 * 60 * 1000)
    public void runProcurationExpirationJob() {
        log.info("Starting [runEsSyncJob]");
        elasticsearchSyncService.sync();
        log.info("Job [runEsSyncJob] done");
    }
}
