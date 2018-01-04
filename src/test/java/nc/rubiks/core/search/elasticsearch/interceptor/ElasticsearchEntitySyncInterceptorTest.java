package nc.rubiks.core.search.elasticsearch.interceptor;

import nc.rubiks.core.search.elasticsearch.config.RubiksElasticsearchProperties;
import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncActionEnum;
import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import nc.rubiks.core.search.elasticsearch.service.impl.TheEntity;
import nc.rubiks.core.search.elasticsearch.service.impl.TheEntityWithDto;
import nc.rubiks.core.search.elasticsearch.service.impl.TheEntityWithNamedQuery;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ElasticsearchEntitySyncInterceptorTest {

    private RubiksElasticsearchProperties rubiksElasticsearchProperties;

    @Before
    public void setUp() {
        this.rubiksElasticsearchProperties = new RubiksElasticsearchProperties();
        rubiksElasticsearchProperties.setScanBasePackage("nc.rubiks");
    }

    @Test
    public void test_init_scanForClassesWithAnnotations() throws ClassNotFoundException, IllegalAccessException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();

        // When
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        // Then
        assertThat((Map<Class, List<Field>>) FieldUtils.readStaticField(ElasticsearchEntitySyncInterceptor.class, "syncedTypes", true))
            .containsKeys(TheEntity.class, TheEntityWithDto.class, TheEntityWithNamedQuery.class, TheChildEntity.class, TheChildEntity2.class, TheEntityWithChildren.class);
        assertThat((ElasticsearchSyncService)FieldUtils.readStaticField(ElasticsearchEntitySyncInterceptor.class, "elasticsearchSyncService", true))
            .isEqualTo(elasticsearchSyncService);
    }

    @Test
    public void test_onSave_withSyncedEntity_addAction() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntity theEntity = new TheEntity();
        theEntity.setId(456l);

        // When
        interceptor.onSave(theEntity, 456l, null, null, null);

        // Then
        verify(elasticsearchSyncService, times(1)).addAction(TheEntity.class, 456l, ElasticsearchSyncActionEnum.CREATE);
    }

    @Test
    public void test_onSave_withNonSyncedEntity_doNothing() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntityNotSynced theEntity = new TheEntityNotSynced();
        theEntity.setId(456l);

        // When
        interceptor.onSave(theEntity, 456l, null, null, null);

        // Then
        verify(elasticsearchSyncService, never()).addAction(any(Class.class), any(Long.class), any(ElasticsearchSyncActionEnum.class));
    }

    @Test
    public void test_onFlushDirty_withSyncedEntity_addAction() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntity theEntity = new TheEntity();
        theEntity.setId(456l);

        // When
        interceptor.onFlushDirty(theEntity, 456l, null, null, null, null);

        // Then
        verify(elasticsearchSyncService, times(1)).addAction(TheEntity.class, 456l, ElasticsearchSyncActionEnum.UPDATE);
    }

    @Test
    public void test_onFlushDirty_withNonSyncedEntity_doNothing() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntityNotSynced theEntity = new TheEntityNotSynced();
        theEntity.setId(456l);

        // When
        interceptor.onFlushDirty(theEntity, 456l, null, null, null, null);

        // Then
        verify(elasticsearchSyncService, never()).addAction(any(Class.class), any(Long.class), any(ElasticsearchSyncActionEnum.class));
    }

    @Test
    public void test_onDelete_withSyncedEntity_addAction() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntity theEntity = new TheEntity();
        theEntity.setId(456l);

        // When
        interceptor.onDelete(theEntity, 456l, null, null, null);

        // Then
        verify(elasticsearchSyncService, times(1)).addAction(TheEntity.class, 456l, ElasticsearchSyncActionEnum.DELETE);
    }

    @Test
    public void test_onDelete_withNonSyncedEntity_doNothing() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntityNotSynced theEntity = new TheEntityNotSynced();
        theEntity.setId(456l);

        // When
        interceptor.onDelete(theEntity, 456l, null, null, null);

        // Then
        verify(elasticsearchSyncService, never()).addAction(any(Class.class), any(Long.class), any(ElasticsearchSyncActionEnum.class));
    }

    @Test
    public void test_onSave_withChildEntityToTrigger_create2TwoElasticsearchSyncAction() throws ClassNotFoundException {

        // Given
        ElasticsearchSyncService elasticsearchSyncService = mock(ElasticsearchSyncService.class);
        ElasticsearchEntitySyncInterceptor interceptor = new ElasticsearchEntitySyncInterceptor();
        interceptor.init(elasticsearchSyncService, rubiksElasticsearchProperties);

        TheEntityWithChildren theEntity = new TheEntityWithChildren();
        theEntity.setId(546l);
        TheChildEntity theChildEntity = new TheChildEntity();
        theChildEntity.setId(547l);
        theEntity.setTheChildEntity(theChildEntity);
        TheChildEntity2 theChildEntity21 = new TheChildEntity2();
        theChildEntity21.setId(548l);
        TheChildEntity2 theChildEntity22 = new TheChildEntity2();
        theChildEntity22.setId(549l);
        theEntity.setTheChildEntity2Set(new HashSet<>());
        theEntity.getTheChildEntity2Set().add(theChildEntity21);
        theEntity.getTheChildEntity2Set().add(theChildEntity22);

        // When
        interceptor.onSave(theEntity, 546l, null, null, null);

        // Then
        verify(elasticsearchSyncService, times(1)).addAction(eq(TheEntityWithChildren.class), eq(546l), eq(ElasticsearchSyncActionEnum.CREATE));
        verify(elasticsearchSyncService, times(1)).addAction(eq(TheChildEntity.class), eq(547l), eq(ElasticsearchSyncActionEnum.UPDATE));
        verify(elasticsearchSyncService, times(1)).addAction(eq(TheChildEntity2.class), eq(548l), eq(ElasticsearchSyncActionEnum.UPDATE));
        verify(elasticsearchSyncService, times(1)).addAction(eq(TheChildEntity2.class), eq(549l), eq(ElasticsearchSyncActionEnum.UPDATE));
    }
}
