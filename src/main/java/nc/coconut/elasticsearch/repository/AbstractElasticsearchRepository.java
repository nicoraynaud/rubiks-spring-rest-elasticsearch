package nc.coconut.elasticsearch.repository;

import com.fasterxml.jackson.databind.JsonNode;
import nc.coconut.elasticsearch.mapper.EntityMapper;
import nc.coconut.elasticsearch.repository.impl.ElasticsearchRepository;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Base Elasticsearch repository class.
 *
 * Should be inherited in order to leverage generic CRUD over entities in a ES cluster indice.
 */
public class AbstractElasticsearchRepository<T, ID extends Serializable> implements ElasticsearchRepository<T, ID> {

    private final Logger log = LoggerFactory.getLogger(AbstractElasticsearchRepository.class);

    private final String rootIndexName;
    private EntityMapper entityMapper;
    private Class<T> entityClass;
    private RestClient client;
    private ElasticSearchTemplate elasticSearchTemplate;

    protected AbstractElasticsearchRepository(RestClient client, ElasticSearchTemplate elasticSearchTemplate, EntityMapper entityMapper, Class<T> entityClass) {
        this.client = client;
        this.elasticSearchTemplate = elasticSearchTemplate;
        this.entityMapper = entityMapper;
        this.entityClass = entityClass;
        this.rootIndexName = getIndexName();
    }

    @Override
    public Iterable<T> search(String... query) {
        return performSearch(null, query).getRight();
    }

    @Override
    public Page<T> search(Pageable pageable, String... query) {
        Pair<Long, List<T>> result = performSearch(pageable, query);
        return new PageImpl<>(result.getRight(), pageable, result.getLeft());
    }

    protected Pair<Long, List<T>> performSearch(Pageable pageable, String... query) {

        Validate.notEmpty(query);

        Response searchResponse = null;
        try {
            Map<String, String> params = new HashMap<>();
            Arrays.stream(query).forEach(q -> params.put("q", q));

            if (pageable != null) {
                params.put("from", String.valueOf(pageable.getOffset()));
                params.put("size", String.valueOf(pageable.getPageSize()));
            }

            searchResponse = client.performRequest(
                "GET",
                elasticSearchTemplate.getRootIndiceName(rootIndexName) + '/' + getIndexName() + "/_search",
                params);

            JsonNode root = entityMapper.readTree(EntityUtils.toString(searchResponse.getEntity()));
            long total = root.path("hits").path("total").asLong();

            List<T> resultList = new ArrayList<>();
            root.path("hits").path("hits").iterator().forEachRemaining(
                hit -> {
                    try {
                        resultList.add(entityMapper.mapToObject(entityMapper.mapToString(hit.path("_source")), getEntityClass()));
                    } catch (IOException ex) {
                        log.error("Error occured during parsing of searchComplex results", ex);
                    }
                });

            return Pair.of(total, resultList);

        } catch (Exception ex) {
            log.error("Error occured during searchComplex of document {} with query [{}] : {}", getIndexName(), query, ex);
        } finally {
            if (searchResponse != null) EntityUtils.consumeQuietly(searchResponse.getEntity());
        }

        return Pair.of(0l, Collections.emptyList());
    }

    @Override
    public Page<T> searchComplex(Pageable pageable, String jsonQuery) {

        Validate.notNull(pageable);
        Validate.notNull(jsonQuery);

        Response searchResponse = null;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("from", String.valueOf(pageable.getOffset()));
            params.put("size", String.valueOf(pageable.getPageSize()));

            searchResponse = client.performRequest(
                "POST",
                elasticSearchTemplate.getRootIndiceName(rootIndexName) + '/' + getIndexName() + "/_search",
                params,
                new NStringEntity(jsonQuery, ContentType.APPLICATION_JSON));

            JsonNode root = entityMapper.readTree(EntityUtils.toString(searchResponse.getEntity()));
            long total = root.path("hits").path("total").asLong();

            List<T> resultList = new ArrayList<>();
            root.path("hits").path("hits").iterator().forEachRemaining(
                hit -> {
                    try {
                        resultList.add(entityMapper.mapToObject(entityMapper.mapToString(hit.path("_source")), getEntityClass()));
                    } catch (IOException ex) {
                        log.error("Error occured during parsing of searchComplex results", ex);
                    }
                });

            return new PageImpl<>(resultList, pageable, total);

        } catch (Exception ex) {
            log.error("Error occured during searchComplex of document {} with query [{}] : {}", getIndexName(), jsonQuery, ex);
        } finally {
            if (searchResponse != null) EntityUtils.consumeQuietly(searchResponse.getEntity());
        }
        return new PageImpl<T>(Collections.emptyList(), pageable, 0);

    }

    @Override
    public T findOne(ID id) {

        Validate.notNull(id);

        Response getResponse = null;
        try {
            getResponse = client.performRequest(
                "GET",
                elasticSearchTemplate.getRootIndiceName(rootIndexName) + '/' + getIndexName() + '/' + id,
                Collections.emptyMap());

            JsonNode root = entityMapper.readTree(EntityUtils.toString(getResponse.getEntity()));
            return entityMapper.mapToObject(entityMapper.mapToString(root.path("_source")), getEntityClass());
        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() == 404) {
                return null;
            }
            log.error("Error occured during fetching of document {} with id [{}] : {}", getIndexName(), id, ex);
        } catch (Exception ex) {
            log.error("Error occured during fetching of document {} with id [{}] : {}", getIndexName(), id, ex);
        } finally {
            if (getResponse != null) EntityUtils.consumeQuietly(getResponse.getEntity());
        }
        return null;
    }

    @Override
    public boolean exists(ID id) {
        return findOne(id) != null;
    }

    @Override
    public Iterable<T> findAll() {

        log.debug("Searching for all records of document {}", getIndexName());

        return this.search("*:*");
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        throw new NotImplementedException("This feature is not yet implemented");
    }

    @Override
    public long count() {
        if (!indexExists()) {
            return 0;
        }
        Response countResponse = null;

        try {
            countResponse = client.performRequest(
                "GET",
                elasticSearchTemplate.getRootIndiceName(rootIndexName) + '/' + getIndexName() + "/_count",
                Collections.emptyMap());
            CountObject result = entityMapper.mapToObject(EntityUtils.toString(countResponse.getEntity()), CountObject.class);
            return result.getCount();
        } catch (Exception ex) {
            log.error("Error occured during counting of document {} : {}", getIndexName(), ex);
        } finally {
            if (countResponse != null) EntityUtils.consumeQuietly(countResponse.getEntity());
        }

        return 0;
    }

    @Override
    public <S extends T> S save(S entity) {
        Validate.notNull(entity);

        try {
            String json = entityMapper.mapToString(entity);
            String id = getEntityClass().getMethod("getId").invoke(entity).toString();
            elasticSearchTemplate.index(client, rootIndexName, getIndexName(), id, json);
        } catch (Exception ex) {
            log.error("Error occured during indexing of document {} : {}", getIndexName(), ex);
        }

        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        Validate.notNull(entities);
        entities.forEach(this::save);
        return entities;
    }

    @Override
    public void delete(ID id) {
        Validate.notNull(id);

        log.debug("Deleting record for document {} and id [{}]", getIndexName(), id);
        if (!indexExists()) {
            return;
        }
        try {
            client.performRequest(
                "DELETE",
                elasticSearchTemplate.getRootIndiceName(rootIndexName) + '/' + getIndexName() + '/' + id,
                elasticSearchTemplate.getParams());
        } catch (Exception ex) {
            log.error("Error occured during deleting of document {} with id [{}] : {}", getIndexName(), id, ex);
        }
    }

    @Override
    public void delete(T entity) {
        Validate.notNull(entity);

        log.debug("Deleting record for document {} and entity [{}]", getIndexName(), entity);
        try {
            ID id = (ID) getEntityClass().getMethod("getId").invoke(entity);
            delete(id);
        } catch (Exception ex) {
            log.error("Error occured during deletion of document {} with entity [{}] : {}", getIndexName(), entity, ex);
        }
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        log.debug("Deleting all records for document {}", getIndexName());

        if (!indexExists()) {
            return;
        }

        findAll().forEach(this::delete);
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    private boolean indexExists() {
        return elasticSearchTemplate.indiceExists(client, rootIndexName, getIndexName());
    }

    protected String getIndexName() {
        return getEntityClass().getSimpleName().toLowerCase();
    }
}

