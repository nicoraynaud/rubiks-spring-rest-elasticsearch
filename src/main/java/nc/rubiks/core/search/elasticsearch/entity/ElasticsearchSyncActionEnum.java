package nc.rubiks.core.search.elasticsearch.entity;

/**
 * The different Elasticsearch action to perform for a given object
 *
 * @author nicoraynaud
 */
public enum ElasticsearchSyncActionEnum {

    /**
     * Used to create a new document in Elasticsearch
     */
    CREATE,

    /**
     * Used to udpate an existing document in Elasticsearch
     */
    UPDATE,

    /**
     * Used to delete a document in Elasticsearch
     */
    DELETE
}
