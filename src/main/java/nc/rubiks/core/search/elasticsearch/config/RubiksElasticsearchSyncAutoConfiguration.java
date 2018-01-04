package nc.rubiks.core.search.elasticsearch.config;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchSyncActionRepository;
import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import nc.rubiks.core.search.elasticsearch.service.EntityToElasticsearchDocumentConverter;
import nc.rubiks.core.search.elasticsearch.service.impl.ElasticsearchSyncServiceImpl;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.ScheduledLockConfiguration;
import net.javacrumbs.shedlock.spring.ScheduledLockConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Configuration
@ComponentScan({
    "nc.rubiks.core.search.elasticsearch.job",
    "nc.rubiks.core.search.elasticsearch.interceptor",
    "nc.rubiks.core.search.elasticsearch.endpoint",
    "nc.rubiks.core.search.elasticsearch.listener" })
@EnableJpaRepositories("nc.rubiks.core.search.elasticsearch.repository")
@EntityScan("nc.rubiks.core.search.elasticsearch.entity")
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "rubiks.elasticsearch.sync", name = "enabled", havingValue = "true")
public class RubiksElasticsearchSyncAutoConfiguration {

    private static final int SHEDLOCK_POOL_SIZE = 10;
    private static final int SHEDLOCK_DEFAULT_LOCK_TIME_SECONDS = 30;

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchSyncService getEsSyncService(ElasticsearchSyncActionRepository elasticsearchSyncActionRepository,
                                                     List<ElasticsearchRepository> elasticsearchRepositories,
                                                     Optional<List<EntityToElasticsearchDocumentConverter>> elasticsearchConverters,
                                                     EntityManager entityManager,
                                                     @Value("${rubiks.elasticsearch.sync.nb-retry:3}")
                                                     int nbTryouts) {
        return new ElasticsearchSyncServiceImpl(
            elasticsearchSyncActionRepository,
            elasticsearchRepositories,
            elasticsearchConverters.orElse(Collections.emptyList()),
            entityManager, nbTryouts);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduledLockConfiguration taskScheduler(LockProvider lockProvider) {
        return ScheduledLockConfigurationBuilder
            .withLockProvider(lockProvider)
            .withPoolSize(SHEDLOCK_POOL_SIZE)
            .withDefaultLockAtMostFor(Duration.ofSeconds(SHEDLOCK_DEFAULT_LOCK_TIME_SECONDS))
            .build();
    }
}
