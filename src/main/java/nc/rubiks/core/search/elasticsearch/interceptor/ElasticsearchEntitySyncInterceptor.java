package nc.rubiks.core.search.elasticsearch.interceptor;

import nc.rubiks.core.search.elasticsearch.RubiksElasticsearchException;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchTriggerSync;
import nc.rubiks.core.search.elasticsearch.config.InitIndexUtil;
import nc.rubiks.core.search.elasticsearch.config.RubiksElasticsearchProperties;
import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncActionEnum;
import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This interceptor is responsible for creating an ElasticsearchSyncAction record for any
 * created/updated/deleted entity that has the @ElasticsearchDocument annotation.
 *
 * In order to work, it needs to be plugged in using the application.yml configuration of the project with the following setting :
 * <code>
 *  spring.jpa.properties:
 *      hibernate.session_factory.interceptor: ElasticsearchEntitySyncInterceptor
 * </code>
 * @author nicoraynaud
 */
@Component
@ConditionalOnProperty(prefix = "rubiks.elasticsearch.sync", name = "enabled", havingValue = "true")
public final class ElasticsearchEntitySyncInterceptor extends EmptyInterceptor {

    private final transient Logger log = LoggerFactory.getLogger(ElasticsearchEntitySyncInterceptor.class);

    private static ElasticsearchSyncService elasticsearchSyncService;

    private static Map<Class, List<Field>> syncedTypes;

    private static final String NOT_CONFIGURED_PROPERLY_MESSAGE = "You must enable rubiks.elasticsearch.sync feature (set it to true) in order to use this Interceptor.";

    @Autowired
    public synchronized void init(ElasticsearchSyncService elasticsearchSyncService, RubiksElasticsearchProperties rubiksElasticsearchProperties) {

        log.info("Initializing ElasticsearchEntitySyncInterceptor...");

        ElasticsearchEntitySyncInterceptor.elasticsearchSyncService = elasticsearchSyncService;

        // Scan classes having the @ElasticsearchDocument annotation with synced = true
        // Search for @ElasticsearchTriggerSync in order to map what fields need to trigger additional SyncActions
        syncedTypes = new HashMap<>();
        InitIndexUtil.findElasticsearchDocumentEntities(rubiksElasticsearchProperties.getScanBasePackage()).stream()
            .filter(clazz -> ((ElasticsearchDocument) clazz.getAnnotation(ElasticsearchDocument.class)).synced())
            .forEach(clazz -> syncedTypes.put(clazz, FieldUtils.getFieldsListWithAnnotation(clazz, ElasticsearchTriggerSync.class)));
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {

        Validate.notNull(elasticsearchSyncService, NOT_CONFIGURED_PROPERLY_MESSAGE);

        if (!syncedTypes.containsKey(entity.getClass())) {
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Trigger ES Create for object [{}] with id [{}]", entity.getClass(), id);
        }
        elasticsearchSyncService.addAction(entity.getClass(), id, ElasticsearchSyncActionEnum.CREATE);
        createAssociatedActions(entity);

        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {

        Validate.notNull(elasticsearchSyncService, NOT_CONFIGURED_PROPERLY_MESSAGE);

        if (!syncedTypes.containsKey(entity.getClass())) {
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Trigger ES Update for object [{}] with id [{}]", entity.getClass(), id);
        }
        elasticsearchSyncService.addAction(entity.getClass(), id, ElasticsearchSyncActionEnum.UPDATE);
        createAssociatedActions(entity);

        return false;
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {

        Validate.notNull(elasticsearchSyncService, NOT_CONFIGURED_PROPERLY_MESSAGE);

        if (!syncedTypes.containsKey(entity.getClass())) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Trigger ES Delete for object [{}] with id [{}]", entity.getClass(), id);
        }
        elasticsearchSyncService.addAction(entity.getClass(), id, ElasticsearchSyncActionEnum.DELETE);
        createAssociatedActions(entity);
    }

    /**
     * Scan the entity for @ElasticsearchTriggerSync annotation
     * If any, add an additional Sync action for this field
     * @param entity The entity being synchronized
     */
    private void createAssociatedActions(Object entity) {

        if (syncedTypes.get(entity.getClass()).isEmpty()) {
            return;
        }

        try {
            for (Field field : syncedTypes.get(entity.getClass())) {
                field.setAccessible(true);
                Object fieldValue = field.get(entity);
                // In case of a collection type, cast it and iterate over values
                if (fieldValue instanceof Collection<?>) {
                    for (Object value : (Collection) fieldValue) {
                        Object idObj = value.getClass().getMethod("getId").invoke(value);
                        if (idObj != null) {
                            elasticsearchSyncService.addAction(value.getClass(), (Serializable) idObj, ElasticsearchSyncActionEnum.UPDATE);
                        }
                    }
                } else {
                    Object idObj = fieldValue.getClass().getMethod("getId").invoke(fieldValue);
                    if (idObj != null) {
                        elasticsearchSyncService.addAction(fieldValue.getClass(), (Serializable) idObj, ElasticsearchSyncActionEnum.UPDATE);
                    }
                }
            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RubiksElasticsearchException(
                String.format("Unable to trigger an additional Sync action for entity [%s]", entity.getClass().getSimpleName()), e);
        }
    }

}
