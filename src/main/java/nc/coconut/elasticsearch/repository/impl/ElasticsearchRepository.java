package nc.coconut.elasticsearch.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.Serializable;

/**
 * Interface for all Elasticsearch repositories
 *
 * @param <T> The entity Type
 * @param <ID> The entity's ID type
 */
public interface ElasticsearchRepository<T, ID extends Serializable> {

    /**
     * Searches for records of an entity in elasticsearch
     * @param query the string queries to use (see https://www.elastic.co/guide/en/elasticsearch/reference/5.4/search-uri-request.html)
     * @return the whole list of results
     */
    Iterable<T> search(String... query);

    /**
     * Searches for records of an entity in elasticsearch using pagination
     * @param pageable the pagination information to request
     * @param query the string queries to use (see https://www.elastic.co/guide/en/elasticsearch/reference/5.4/search-uri-request.html)
     * @return the paginated list of results
     */
    Page<T> search(Pageable pageable, String... query);

    /**
     * Searches for records of an entity in elasticsearch using pagination
     * @param pageable the pagination information to request
     * @param jsonQuery the Query as Json (see https://www.elastic.co/guide/en/elasticsearch/reference/5.4/query-dsl.html)
     * @return the paginated list of results
     */
    Page<T> searchComplex(Pageable pageable, String jsonQuery);

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal null} if none found
     */
    T findOne(ID id);

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id must not be {@literal null}.
     * @return true if an entity with the given id exists, {@literal false} otherwise
     * @throws IllegalArgumentException if {@code id} is {@literal null}
     */
    boolean exists(ID id);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    Iterable<T> findAll();

    /**
     * Returns all instances of the type with the given IDs.
     *
     * @param ids the list of ids of documents to retrieve
     * @return the list of entities corresponding to the list of ids
     */
    Iterable<T> findAll(Iterable<ID> ids);

    /**
     * Returns the number of entities available.
     *
     * @return the number of entities
     */
    long count();

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param entity the entity to index
     * @param <S> The entity's type
     * @return the saved entity
     */
    <S extends T> S save(S entity);

    /**
     * Saves all given entities.
     *
     * @param entities the list of entities to index
     * @param <S> The entity's type
     * @return the saved entities
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    <S extends T> Iterable<S> save(Iterable<S> entities);

    /**
     * Deletes the entity with the given id.
     *
     * @param id must not be {@literal null}.
     * @throws IllegalArgumentException in case the given {@code id} is {@literal null}
     */
    void delete(ID id);

    /**
     * Deletes a given entity.
     *
     * @param entity the entity to delete from the index
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    void delete(T entity);

    /**
     * Deletes the given entities.
     *
     * @param entities the list of entities to delete from the index
     * @throws IllegalArgumentException in case the given {@link Iterable} is {@literal null}.
     */
    void delete(Iterable<? extends T> entities);

    /**
     * Deletes all entities managed by the repository.
     */
    void deleteAll();

    /**
     * The entity class the repository is used for
     * @return the class type of the entity
     */
    Class<T> getEntityClass();

}
