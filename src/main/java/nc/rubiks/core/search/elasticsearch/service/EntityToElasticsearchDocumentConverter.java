package nc.rubiks.core.search.elasticsearch.service;

/**
 * Interface to implement in order to provide ElasticsearchSyncService with an implementation
 * of how to convert an entity into a document that will be indexed in Elasticsearch.
 *
 * @param <T> The entity type
 * @param <D> The document type
 */
public interface EntityToElasticsearchDocumentConverter<T, D> {

    /**
     * @return The entity Type to convert from
     */
    Class<T> getEntityType();

    /**
     * @return The Document type to convert to
     */
    Class<D> getDocumentType();

    /**
     * Methods that returns a Document representation from an entity ID
     * Note that, when called by the ElasticsearchSyncService, the ID is always a string.
     * It is your responsibility to cast the ID into the actual entity ID type.
     * @param id The entity ID in database
     * @return The Document to index in Elasticsearch
     */
    D convert(String id);
}
