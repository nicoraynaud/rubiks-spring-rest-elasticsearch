package nc.rubiks.core.search.elasticsearch.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking Entities that are stored in Elasticsearch
 *
 * @author nicoraynaud
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ElasticsearchDocument {

    /**
     * The name of the index to create if different from the object simple class name.
     * If specified, the configuration files must be named after this index name :
     * - indexname.mapping.json
     * - indexname.setting.json
     *
     * @return The indexName
     */
    String indexName() default "";

    /**
     * The relating DTO type to use as a document for ES syncing when not the target type of the annotation
     * If not provided or empty, the lib will index the annotated class directly
     *
     * @return the type representing the document in ES
     */
    Class documentType() default void.class;

    /**
     * The namedQuery to use when fetching the record from DB while indexing in ES.
     * Note that the query must use only one parameter of name ":id".
     * If not provided or empty, a standard entityManager.find(Clazz, id) query will be used.
     *
     * @return The namedQuery name
     */
    String namedQuery() default "";

    /**
     * Whether or not this entity is automatically synced by the library
     * whenever it is modified in an hibernate session.
     *
     * @return true if synced, false otherwise
     */
    boolean synced() default false;
}
