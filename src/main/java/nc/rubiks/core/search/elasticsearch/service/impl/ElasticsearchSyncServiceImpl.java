package nc.rubiks.core.search.elasticsearch.service.impl;

import nc.rubiks.core.search.elasticsearch.RubiksElasticsearchException;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncAction;
import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncActionEnum;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchSyncActionRepository;
import nc.rubiks.core.search.elasticsearch.service.ElasticsearchSyncService;
import nc.rubiks.core.search.elasticsearch.service.EntityToElasticsearchDocumentConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for :
 * - Adding ESSyncAction items to the database (addAction)
 *   Whenever an object that is annotated @ElasticsearchDocument is modified in an Hibernate session,
 *   a new ElasticsearchSyncAction record is created to reflect this change in the linked Elasticsearch document.
 *
 * - Processing the ESSyncAction items previously stored (sync)
 *   On a regular basis, the sync job is ran through the EsSyncJob class. This job takes all the records
 *   stored in DB ordered by CreatedDate ascending (older first) and execute the related Action (CREATE/UPDATE/DELETE)
 *   in the Elasticsearch index.
 *
 * @author nicoraynaud
 */
public class ElasticsearchSyncServiceImpl implements ElasticsearchSyncService {

    private final Logger log = LoggerFactory.getLogger(ElasticsearchSyncServiceImpl.class);

    private final ElasticsearchSyncActionRepository elasticsearchSyncActionRepository;

    private final Map<Class, ElasticsearchRepository> elasticsearchRepositoriesMap;

    private final Map<Class, EntityToElasticsearchDocumentConverter> elasticsearchDtoConvertersMap;

    private final EntityManager entityManager;

    private final int nbTryouts;

    public ElasticsearchSyncServiceImpl(ElasticsearchSyncActionRepository elasticsearchSyncActionRepository,
                                        List<ElasticsearchRepository> elasticsearchRepositories,
                                        List<EntityToElasticsearchDocumentConverter> elasticsearchConverters,
                                        EntityManager entityManager,
                                        int nbTryouts) {
        log.debug("Initializing ElasticsearchSyncService...");
        this.elasticsearchSyncActionRepository = elasticsearchSyncActionRepository;
        elasticsearchRepositoriesMap = new HashMap<>();
        elasticsearchRepositories.forEach(er -> elasticsearchRepositoriesMap.put(er.getIndexedClass(), er));
        elasticsearchDtoConvertersMap = new HashMap<>();
        elasticsearchConverters.forEach(ec -> elasticsearchDtoConvertersMap.put(ec.getEntityType(), ec));
        this.entityManager = entityManager;
        this.nbTryouts = nbTryouts;
    }

    @Override
    public void addAction(Class clazz, Serializable id, ElasticsearchSyncActionEnum action) {
        if (log.isDebugEnabled()) {
            log.debug("Adding action to Sync ES for class [{}], id [{}] and action [{}]", clazz, id, action);
        }

        ElasticsearchSyncAction elasticsearchSyncAction = new ElasticsearchSyncAction();
        elasticsearchSyncAction.setObjType(clazz.getCanonicalName());
        elasticsearchSyncAction.setObjId(id.toString());
        elasticsearchSyncAction.setAction(action);

        elasticsearchSyncActionRepository.save(elasticsearchSyncAction);
    }

    @Override
    public void sync() {
        log.debug("Syncing ES and database...");

        for (ElasticsearchSyncAction esa : elasticsearchSyncActionRepository.findAllOrderByCreatedDateAsc(this.nbTryouts)) {
            try {
                log.debug("Syncing : {}", esa);
                Class clazz = Class.forName(esa.getObjType());
                Class targetType = clazz;

                // Fetch the annotation and, if exists, the associated DTO type
                ElasticsearchDocument elasticsearchDocumentAnnotation = (ElasticsearchDocument) clazz.getAnnotation(ElasticsearchDocument.class);
                if (elasticsearchDocumentAnnotation.documentType() != void.class) {
                    log.debug("Syncing {} type as {}", clazz.getSimpleName(), targetType.getSimpleName());
                    targetType = elasticsearchDocumentAnnotation.documentType();
                }

                // Case when we don't have the matching ElasticsearchRepository instance to synchronize the object : we skip it
                if (!elasticsearchRepositoriesMap.containsKey(targetType)) {
                    throw new RubiksElasticsearchException(
                        String.format("Unable to find the ElasticsearchRepository<%s>, the object [%s] will not be synchronized.", targetType, esa));
                }

                ElasticsearchRepository esr = elasticsearchRepositoriesMap.get(targetType);

                // Otherwise, based on the action, we call the right ElasticsearchRepository method
                switch (esa.getAction()) {
                    case CREATE:
                    case UPDATE:
                        Object document = fetchObjectToSync(clazz, esa.getObjId(), elasticsearchDocumentAnnotation.namedQuery(), targetType);
                        log.debug("Saving object in ES...");
                        esr.save(document);
                        break;
                    case DELETE:
                        log.debug("Deleting object from ES...");
                        esr.delete(Long.parseLong(esa.getObjId()));
                        break;
                }

                // Delete the Action after processing it
                log.debug("Deleting ElasticsearchSyncAction...");
                elasticsearchSyncActionRepository.delete(esa);

            } catch (Exception ex) {
                esa.setNbTryouts(esa.getNbTryouts() + 1);
                elasticsearchSyncActionRepository.save(esa);
                log.error("Unable to sync object [{}] : {}", esa, ex);
            }
        }
    }

    /**
     * Method that returns the object to synchronize in Elasticsearch based on the
     * properties saved in the ElasticsearchSyncAction object.
     *
     * This method will check whether there is a documentType, and if yes, search for a corresponding
     * EntityToElasticsearchDocumentConverter implementation to convert the source object to this DTO
     *
     * If no documentType provided, method will try to use the provided namedQuery to get the entity.
     * At last, it will use the default entityManager method to fetch the entity to return
     *
     * @param clazz The class of the object to fetch
     * @param id The Id of the object to fetch
     * @param documentType The targetType of the object to return
     * @return The object to store in Elasticsearch
     */
    @SuppressWarnings("unchecked")
    private Object fetchObjectToSync(Class clazz, String id, String namedQuery, Class documentType) {
        log.debug("Fetching object to synchronize from database with class [{}] and id [{}]...", clazz, id);

        // In case there is a target DTO type, fetch the corresponding converter and use it
        if (!documentType.equals(clazz)) {
            if (!elasticsearchDtoConvertersMap.containsKey(clazz)) {
                throw new RubiksElasticsearchException(
                    String.format("Unable to find the EntityToElasticsearchDocumentConverter<%s, %s>, the object will not be synchronized.", clazz.getSimpleName(), documentType.getSimpleName()));
            }
            return elasticsearchDtoConvertersMap.get(clazz).convert(id);
        } else {
            // Otherwise, simply fetch the record from the database using the entityManager
            // Note that only Long ids are currently supported
            // If a namedQuery is provided, use it
            if (StringUtils.isNotEmpty(namedQuery)) {
                return entityManager
                    .createNamedQuery(namedQuery, clazz)
                    .setParameter("id", Long.parseLong(id))
                    .getSingleResult();
            } else {
                // Otherwise, use the default entityManager method to fetch the entity
                return entityManager
                    .find(clazz, Long.parseLong(id));
            }
        }
    }

    @Override
    public void reset() {
        entityManager.createNamedQuery("resetTryouts").executeUpdate();
    }
}
