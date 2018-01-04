package nc.rubiks.core.search.elasticsearch.service;

import nc.rubiks.core.search.elasticsearch.entity.ElasticsearchSyncActionEnum;

import java.io.Serializable;

/**
 * Service responsible for :
 * - Adding ESSyncAction items to the database (addAction)
 * - Processing the ESSyncAction items previously stored (sync)
 *   This method is called on a regular basis from the @Scheduled Job EsSyncJob
 *
 * @author nicoraynaud
 */
public interface ElasticsearchSyncService {

    /**
     * Adds a new sync action to the database for later synchronisation
     * @param clazz The entity class of the object to sync
     * @param id The entity's Id of the object to sync
     * @param action The action to perform (CREATE/UPDATE/DELETE)
     */
    void addAction(Class clazz, Serializable id, ElasticsearchSyncActionEnum action);

    /**
     * Entry point called by the EsSyncJob to process all database's stored ElasticsearchSyncAction
     */
    void sync();

    /**
     * Reset all ElasticsearchSyncAction that have reached their maximum number
     * of tryouts to 0 so that the next Synchronisation job will try them again.
     */
    void reset();
}
