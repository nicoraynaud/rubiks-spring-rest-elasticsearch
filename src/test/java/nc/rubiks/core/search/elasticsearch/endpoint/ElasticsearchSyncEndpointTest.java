package nc.rubiks.core.search.elasticsearch.endpoint;

import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(JUnit4.class)
public class ElasticsearchSyncEndpointTest {

    @Test
    public void test_reset_callService() {

        // Given
        ElasticsearchSyncService service = mock(ElasticsearchSyncService.class);
        ElasticsearchSyncEndpoint endpoint = new ElasticsearchSyncEndpoint(service);

        // When
        ElasticsearchSyncReset result = endpoint.invoke();

        // Then
        verify(service, times(1)).reset();
        assertThat(endpoint.getId()).isEqualTo("elasticsearch_sync_reset");
        assertThat(endpoint.isSensitive()).isTrue();
    }
}
