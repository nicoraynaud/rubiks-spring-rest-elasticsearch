package nc.rubiks.core.search.elasticsearch.repository;

import nc.rubiks.core.search.elasticsearch.model.Result;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by nicoraynaud on 28/04/2017.
 */
public interface ElasticsearchRepository<T, ID extends Serializable> {

    /**
     * Searches for records of an entity in elasticsearch using pagination
     * @param pageable the pagination information to request
     * @param query the string query to use (see https://www.elastic.co/guide/en/elasticsearch/reference/5.6/query-dsl-query-string-query.html)
     * @return the paginated list of results
     */
    Page<T> search(Pageable pageable, String query);

    /**
     * Searches for records of an entity in elasticsearch using pagination
     * @param pageable the pagination information to request
     * @param query the QueryBuilder query to use (see https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-query-builders.html)
     * @return the paginated list of results
     */
    Page<T> search(Pageable pageable, QueryBuilder query);

    /**
     * Searches and aggregates the documents according to the given queries and aggregations
     * Note that this is the raw result from the REST ES client lib : entities are not deserialized
     * @param pageable The page information of the search
     * @param query The search query to use
     * @param aggregation The aggregation to use
     * @return The result of the query
     */
    SearchResponse search(Pageable pageable, QueryBuilder query, AggregationBuilder aggregation);

    /**
     * Searches and aggregates the documents according to the given queries and aggregations
     * Note that this is the raw result from the REST ES client lib : entities are not deserialized
     * @param pageable The page information of the search
     * @param query The search query to use
     * @param aggregations The aggregations to use
     * @return The result of the query
     */
    SearchResponse search(Pageable pageable, QueryBuilder query, Collection<AggregationBuilder> aggregations);

    /**
     * Searches for records of an entity in elasticsearch using pagination, filters and aggregations
     * @param pageable the pagination information to request
     * @param jsonQuery the Query as Json (see https://www.elastic.co/guide/en/elasticsearch/reference/5.6/query-dsl.html)
     * @return the Result object with hits and aggregations
     * @deprecated This helper is here to keep retro compatibility with search based on the low level rest API. It will be removed in the next release.
     */
    @Deprecated
    Result<T> searchComplex(Pageable pageable, String jsonQuery);

    /**
     * Retrieves an entity by its id.
     *
     * @param id must not be {@literal null}.
     * @return the entity with the given id or {@literal null} if none found
     * @throws IllegalArgumentException if {@code id} is {@literal null}
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
     * Returns the number of entities available.
     *
     * @return the number of entities
     */
    long count();

    /**
     * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param entity The entity to save
     * @param <S> The entity that inherits T
     * @return the saved entity
     */
    <S extends T> S save(S entity);

    /**
     * Saves all given entities.
     *
     * @param entities The list of entities to save
     * @param <S> The entity that inherits T
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
     * @param entity The entity to delete
     * @throws IllegalArgumentException in case the given entity is {@literal null}.
     */
    void delete(T entity);

    /**
     * Deletes the given entities.
     *
     * @param entities The list of entities to delete
     * @throws IllegalArgumentException in case the given {@link Iterable} is {@literal null}.
     */
    void delete(Iterable<? extends T> entities);

    /**
     * Deletes all entities managed by the repository.
     */
    void deleteAll();

    /**
     * The Document type indexed by this repository
     * @return the class of the document
     */
    Class<T> getIndexedClass();

}
