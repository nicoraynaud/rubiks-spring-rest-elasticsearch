package nc.rubiks.core.search.elasticsearch.service.impl;

import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncAction;
import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncActionEnum;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchSyncActionRepository;
import nc.rubiks.core.search.elasticsearch.service.EntityToElasticsearchDocumentConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ElasticsearchSyncServiceImplTest {

    private ElasticsearchSyncActionRepository elasticsearchSyncActionRepository;
    private ElasticsearchRepository<TheEntity, Long> elasticsearchTheEntityRepository;
    private ElasticsearchRepository<TheEntityDto, Long> elasticsearchTheEntityDtoRepository;
    private ElasticsearchRepository<TheEntityWithNamedQuery, Long> elasticsearchTheEntityWithNamedQueryRepository;
    private EntityManager entityManager;

    @Before
    public void setUp() {

        elasticsearchSyncActionRepository = mock(ElasticsearchSyncActionRepository.class);
        elasticsearchTheEntityRepository = mock(ElasticsearchRepository.class);
        when(elasticsearchTheEntityRepository.getIndexedClass()).thenReturn(TheEntity.class);
        elasticsearchTheEntityDtoRepository = mock(ElasticsearchRepository.class);
        when(elasticsearchTheEntityDtoRepository.getIndexedClass()).thenReturn(TheEntityDto.class);
        elasticsearchTheEntityWithNamedQueryRepository = mock(ElasticsearchRepository.class);
        when(elasticsearchTheEntityWithNamedQueryRepository.getIndexedClass()).thenReturn(TheEntityWithNamedQuery.class);
        entityManager = mock(EntityManager.class);
    }

    @Test
    public void test_addAction_saveActionInRepo() {

        // Given
        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityRepository, elasticsearchTheEntityDtoRepository),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.addAction(TheEntity.class, 564l, ElasticsearchSyncActionEnum.CREATE);

        // Then
        ArgumentCaptor<ElasticsearchSyncAction> elasticsearchSyncActionArgumentCaptor = ArgumentCaptor.forClass(ElasticsearchSyncAction.class);
        verify(elasticsearchSyncActionRepository, times(1)).save(elasticsearchSyncActionArgumentCaptor.capture());

        assertThat(elasticsearchSyncActionArgumentCaptor.getValue().getObjId()).isEqualTo("564");
        assertThat(elasticsearchSyncActionArgumentCaptor.getValue().getObjType()).isEqualTo("nc.rubiks.core.search.elasticsearch.service.impl.TheEntity");
        assertThat(elasticsearchSyncActionArgumentCaptor.getValue().getAction()).isEqualTo(ElasticsearchSyncActionEnum.CREATE);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_CREATE() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.CREATE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntity");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        TheEntity theEntity = new TheEntity();
        theEntity.setId(564l);
        when(entityManager.find(TheEntity.class, 564l)).thenReturn(theEntity);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityRepository, elasticsearchTheEntityDtoRepository),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        ArgumentCaptor<TheEntity> captor = ArgumentCaptor.forClass(TheEntity.class);
        verify(elasticsearchTheEntityRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(theEntity);
        verify(elasticsearchSyncActionRepository, times(1)).delete(action);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_DELETE() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.DELETE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntity");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityRepository, elasticsearchTheEntityDtoRepository),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        verify(entityManager, never()).find(TheEntity.class, 564l);
        verify(elasticsearchTheEntityRepository, times(1)).delete(564l);
        verify(elasticsearchSyncActionRepository, times(1)).delete(action);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_NoElasticsearchRepository_doNothing() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.UPDATE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntity");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        TheEntity theEntity = new TheEntity();
        theEntity.setId(564l);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            new ArrayList<>(),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        verify(entityManager, never()).find(TheEntity.class, 564l);
        verify(elasticsearchSyncActionRepository, never()).delete(action);
        ArgumentCaptor<ElasticsearchSyncAction> captor = ArgumentCaptor.forClass(ElasticsearchSyncAction.class);
        verify(elasticsearchSyncActionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getNbTryouts()).isEqualTo(1);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_DocumentTypeDifferent_NoConverter_doNothing() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.UPDATE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntityWithDto");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        TheEntity theEntity = new TheEntity();
        theEntity.setId(564l);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityDtoRepository),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        verify(entityManager, never()).find(TheEntity.class, 564l);
        verify(elasticsearchSyncActionRepository, never()).delete(action);
        ArgumentCaptor<ElasticsearchSyncAction> captor = ArgumentCaptor.forClass(ElasticsearchSyncAction.class);
        verify(elasticsearchSyncActionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getNbTryouts()).isEqualTo(1);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_DocumentTypeDifferent_ConverterExists_callItAndCallElasticsearchRepo() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.UPDATE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntityWithDto");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        TheEntity theEntity = new TheEntity();
        theEntity.setId(564l);

        EntityToElasticsearchDocumentConverter<TheEntityWithDto, TheEntityDto> converter = mock(EntityToElasticsearchDocumentConverter.class);
        TheEntityDto dto = new TheEntityDto();
        when(converter.getEntityType()).thenReturn(TheEntityWithDto.class);
        when(converter.convert("564")).thenReturn(dto);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityDtoRepository),
            Arrays.asList(converter),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        verify(entityManager, never()).find(TheEntity.class, 564l);
        verify(converter, times(1)).convert("564");
        verify(elasticsearchTheEntityDtoRepository, times(1)).save(dto);
        verify(elasticsearchSyncActionRepository, times(1)).delete(action);
    }

    @Test
    public void test_sync_performESSyncFromRepoItems_withNamedQuery_UseNamedQueryToFetchItem() {

        // Given
        ElasticsearchSyncAction action = new ElasticsearchSyncAction();
        action.setAction(ElasticsearchSyncActionEnum.CREATE);
        action.setObjId("564");
        action.setObjType("nc.rubiks.core.search.elasticsearch.service.impl.TheEntityWithNamedQuery");
        when(elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(3)).thenReturn(Arrays.asList(action));

        TheEntityWithNamedQuery theEntity = new TheEntityWithNamedQuery();
        theEntity.setId(564l);
        TypedQuery<TheEntityWithNamedQuery> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(eq("theNamedQuery"), eq(TheEntityWithNamedQuery.class))).thenReturn(query);
        when(query.setParameter("id", 564l)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(theEntity);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            Arrays.asList(elasticsearchTheEntityWithNamedQueryRepository),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.sync();

        // Then
        verify(entityManager, never()).find(TheEntity.class, 564l);
        ArgumentCaptor<TheEntityWithNamedQuery> captor = ArgumentCaptor.forClass(TheEntityWithNamedQuery.class);
        verify(elasticsearchTheEntityWithNamedQueryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isEqualTo(theEntity);
        verify(elasticsearchSyncActionRepository, times(1)).delete(action);
    }

    @Test
    public void test_reset_callNamedQuery() {

        // Given
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNamedQuery("resetTryouts")).thenReturn(query);

        ElasticsearchSyncServiceImpl elasticsearchSyncService = new ElasticsearchSyncServiceImpl(elasticsearchSyncActionRepository,
            new ArrayList<>(),
            new ArrayList<>(),
            entityManager,
            3);

        // When
        elasticsearchSyncService.reset();

        // Then
        verify(query, times(1)).executeUpdate();
    }
}
