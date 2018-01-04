package nc.rubiks.core.search.elasticsearch.util;

import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ElasticsearchReindexUtilTest {

    private JpaRepository<EntityToReindex, Long> repository;

    private ElasticsearchRepository<EntityToReindex, Long> searchRepository;

    @Before
    public void setUp() {
        repository = mock(JpaRepository.class);
        searchRepository = mock(ElasticsearchRepository.class);
    }

    @Test
    public void test_reIndex_noData_doNothing() throws ExecutionException, InterruptedException {

        // Given
        Page<EntityToReindex> results = new PageImpl<>(Arrays.asList(), new PageRequest(0, 50), 0);
        when(repository.findAll(any(Pageable.class))).thenReturn(results);

        // When
        Future<Boolean> future = ElasticsearchReindexUtil.reIndex(searchRepository, repository);
        future.get();

        // Then
        verify(searchRepository, never()).save(any(EntityToReindex.class));
    }

    @Test
    public void test_reIndex_2pages_reIndex77items() throws ExecutionException, InterruptedException {

        // Given
        List<EntityToReindex> list1 = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            list1.add(new EntityToReindex());
        }

        List<EntityToReindex> list2 = new ArrayList<>();
        for (int i = 0; i < 27; i++) {
            list2.add(new EntityToReindex());
        }

        Page<EntityToReindex> results1 = new PageImpl<>(list1, new PageRequest(0, 50), 77);
        Page<EntityToReindex> results2 = new PageImpl<>(list2, new PageRequest(1, 50), 77);
        when(repository.findAll(any(Pageable.class))).thenReturn(results1).thenReturn(results2);

        // When
        Future<Boolean> future = ElasticsearchReindexUtil.reIndex(searchRepository, repository);
        future.get();

        // Then
        verify(searchRepository, times(77)).save(any(EntityToReindex.class));
    }
}
