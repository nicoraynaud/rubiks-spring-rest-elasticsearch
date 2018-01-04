package nc.rubiks.core.search.elasticsearch.job;

import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ElasticsearchSyncJobTest {

    @Test
    public void test_process_callService() {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);

        ElasticsearchSyncJob job = new ElasticsearchSyncJob(elasticsearchSyncService);

        // When
        job.runProcurationExpirationJob();

        // Then
        verify(elasticsearchSyncService, times(1)).sync();
    }
}
