package nc.rubiks.core.search.elasticsearch.listener;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticsearchEntityListenerTest {

    @Mock
    private ElasticsearchRepository<Currency, String> elasticsearchRepository;

    @Test
    public void testInit_InitializeMap() {

        // Given
        List<ElasticsearchRepository> repositories = new ArrayList<>();
        repositories.add(new ElasticsearchRepositoryGeneric(Long.class));
        repositories.add(new ElasticsearchRepositoryGeneric(Currency.class));

        // When
        ElasticsearchEntityListener listener = new ElasticsearchEntityListener();
        listener.init(repositories);

        // Then
        assertThat(ElasticsearchEntityListener.elasticsearchRepositoriesMap).containsKeys(Long.class, Currency.class);
        assertThat(ElasticsearchEntityListener.elasticsearchRepositoriesMap).containsValues(repositories.get(0), repositories.get(1));
    }

    @Test
    public void test_postPersist_noRepo_doNothing() {

        // Given
        ElasticsearchRepository mockESRepo = mock(ElasticsearchRepository.class);
        when(mockESRepo.getIndexedClass()).thenReturn(Currency.class);

        ElasticsearchEntityListener listener = new ElasticsearchEntityListener();
        ElasticsearchEntityListener.elasticsearchRepositoriesMap.put(Currency.class, mockESRepo);

        // When
        listener.postPersist(new Long(1));

        // Then
        verify(mockESRepo, never()).save(any());
    }

    @Test
    public void test_postPersist_ESRepo_saveEntityInRepo() {

        // Given
        ElasticsearchRepository mockESRepo = mock(ElasticsearchRepository.class);
        when(mockESRepo.getIndexedClass()).thenReturn(Currency.class);

        Currency cur = Currency.getInstance("EUR");

        ElasticsearchEntityListener listener = new ElasticsearchEntityListener();
        ElasticsearchEntityListener.elasticsearchRepositoriesMap.put(Currency.class, mockESRepo);

        // When
        listener.postPersist(cur);

        // Then
        verify(mockESRepo, times(1)).save(cur);
    }

    @Test
    public void test_postRemove_noRepo_doNothing() {

        // Given
        when(elasticsearchRepository.getIndexedClass()).thenReturn(Currency.class);

        ElasticsearchEntityListener listener = new ElasticsearchEntityListener();
        ElasticsearchEntityListener.elasticsearchRepositoriesMap.put(Currency.class, elasticsearchRepository);

        Double d = new Double("2");

        // When
        listener.postRemove(d);

        // Then
        verify(elasticsearchRepository, never()).delete(any(Currency.class));
    }

    @Test
    public void test_postRemove_ESRepo_deleteEntityInRepo() {

        // Given
        when(elasticsearchRepository.getIndexedClass()).thenReturn(Currency.class);

        Currency cur = Currency.getInstance("EUR");

        ElasticsearchEntityListener listener = new ElasticsearchEntityListener();
        ElasticsearchEntityListener.elasticsearchRepositoriesMap.put(Currency.class, elasticsearchRepository);

        // When
        listener.postRemove(cur);

        // Then
        verify(elasticsearchRepository, times(1)).delete(cur);
    }
}
