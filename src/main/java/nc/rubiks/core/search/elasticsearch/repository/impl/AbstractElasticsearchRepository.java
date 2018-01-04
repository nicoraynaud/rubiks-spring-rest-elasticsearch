package nc.rubiks.core.search.elasticsearch.repository.impl;

import nc.rubiks.core.search.elasticsearch.RubiksElasticsearchException;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;
import nc.rubiks.core.search.elasticsearch.model.Element;
import nc.rubiks.core.search.elasticsearch.model.Result;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nicoraynaud on 28/04/2017.
 *
 * AbstractElasticsearchRepository is the abstract repository for any Object persisted in Elasticsearch.
 * It relies on the underlying ElasticSearchTemplate and its "test mode".
 *
 * This class provides basic CRUD for both documents and indices.
 *
 * @author nicoraynaud
 */
public abstract class AbstractElasticsearchRepository<T, ID extends Serializable> implements ElasticsearchRepository<T, ID> {

    private final Logger log = LoggerFactory.getLogger(AbstractElasticsearchRepository.class);

    protected final String indexName;
    protected DocumentMapper documentMapper;
    private Class<T> indexedClass;
    private Class annotatedClass;
    private RestHighLevelClient highLevelClient;
    private ElasticSearchTemplate elasticSearchTemplate;

    /**
     * Build the ElasticsearchRepository for a given type and type id's type
     * @param highLevelClient the highLevel ES Client
     * @param documentMapper the documentMapper used for (de)serializing objects (from) to JSON
     * @param elasticSearchTemplate the base elasticsearchTemplate used for low level ES operations
     * @param indexedClass the class that is indexed in Elasticsearch (the document type defining the structure to be indexed)
     */
    public AbstractElasticsearchRepository(RestHighLevelClient highLevelClient,
                                           DocumentMapper documentMapper,
                                           ElasticSearchTemplate elasticSearchTemplate,
                                           Class<T> indexedClass) {
        this.highLevelClient = highLevelClient;
        this.documentMapper = documentMapper;
        this.elasticSearchTemplate = elasticSearchTemplate;
        this.indexedClass = indexedClass;
        this.indexName = getIndexName();
    }

    /**
     * Build the ElasticsearchRepository for a given type and type id's type
     * @param highLevelClient the highLevel ES Client
     * @param documentMapper the documentMapper used for (de)serializing objects (from) to JSON
     * @param elasticSearchTemplate the base elasticsearchTemplate used for low level ES operations
     * @param indexedClass the class that is indexed in Elasticsearch (what the mapper will use to convert)
     * @param annotatedClass the actual class that is indexed in ES (when used, this means that the @ElasticsearchDocument annotation
     *                       is set on a different class than the indexedClass : when we sync an entity but index a DTO instead).
     */
    public AbstractElasticsearchRepository(RestHighLevelClient highLevelClient,
                                           DocumentMapper documentMapper,
                                           ElasticSearchTemplate elasticSearchTemplate,
                                           Class<T> indexedClass,
                                           Class annotatedClass) {
        this.highLevelClient = highLevelClient;
        this.documentMapper = documentMapper;
        this.elasticSearchTemplate = elasticSearchTemplate;
        this.indexedClass = indexedClass;
        this.annotatedClass = annotatedClass;
        this.indexName = getIndexName();
    }

    @Override
    public Page<T> search(Pageable pageable, String query) {
        return search(pageable, QueryBuilders.queryStringQuery(query).analyzeWildcard(true));
    }

    @Override
    public Page<T> search(Pageable pageable, QueryBuilder query) {

        if (!indexExists()) {
            return new PageImpl<>(Collections.emptyList());
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.from(pageable.getOffset());
        sourceBuilder.size(pageable.getPageSize());
        sourceBuilder.query(query);

        if (pageable.getSort() != null) {
            for (Sort.Order order : pageable.getSort()) {
                sourceBuilder.sort(order.getProperty(), SortOrder.fromString(order.getDirection().name()));
            }
        }

        SearchResponse response = search(sourceBuilder);

        Page<T> result = new PageImpl<>(Arrays.stream(response.getHits().getHits()).map(h -> {
            try {
                return documentMapper.mapToObject(h.getSourceAsMap(), getIndexedClass());
            } catch (IOException ex) {
                log.error("Error occured during parsing of searchComplex results", ex);
                throw new RubiksElasticsearchException("Unable to parse result from ES : ", ex);
            }
        }).collect(Collectors.toList()),
            pageable,
            response.getHits().totalHits);

        return result;
    }

    @Override
    public SearchResponse search(Pageable pageable, QueryBuilder query, AggregationBuilder aggregation) {
        return search(pageable, query, Collections.singleton(aggregation));
    }

    @Override
    public SearchResponse search(Pageable pageable, QueryBuilder query, Collection<AggregationBuilder> aggregations) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.from(pageable.getOffset());
        sourceBuilder.size(pageable.getPageSize());
        sourceBuilder.query(query);

        if (aggregations != null) {
            aggregations.forEach(sourceBuilder::aggregation);
        }

       return search(sourceBuilder);
    }

    /**
     * Low level hook of ES advanced search
     * This method makes sure the correct index is used for querying
     * @param sourceBuilder The Source builder object to query the index with (see https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-search.html#_using_the_searchsourcebuilder)
     * @return The raw searchResponse
     */
    protected SearchResponse search(SearchSourceBuilder sourceBuilder) {

        SearchRequest searchRequest = new SearchRequest(elasticSearchTemplate.getRootIndexName(indexName));
        searchRequest.types(indexName);
        searchRequest.source(sourceBuilder);
        return search(searchRequest);
    }

    /**
     * Lowest level hook of ES advanced search
     * <b>To Use with extreme caution</b> as parameters used here are not at all configured in this method : indexName, type, etc.
     * Parameters must therefore be manually initialized <b>before</b>, using ElasticsearchTemplate features.
     * @param searchRequest the SearchRequest to execute
     * @return the ES SearchResponse
     */
    private SearchResponse search(SearchRequest searchRequest) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("ES Query : {}", searchRequest);
            }
            SearchResponse response = highLevelClient.search(searchRequest);
            if (log.isTraceEnabled()) {
                log.trace("ES Response : {}", response);
            }
            return response;
        } catch (IOException ex) {
            log.error("Error occured during execution of Search request from document {} with content [{}] : {}", indexName, searchRequest, ex);
            throw new RubiksElasticsearchException("An error occured during execution of Search request", ex);
        }
    }

    @Override
    public Result<T> searchComplex(Pageable pageable, String jsonQuery) {

        Validate.notNull(pageable);
        Validate.notNull(jsonQuery);

        Response searchResponse = null;

        try (NStringEntity nStringEntity = new NStringEntity(jsonQuery, ContentType.APPLICATION_JSON)) {
            Map<String, String> params = new HashMap<>();
            params.put("from", String.valueOf(pageable.getOffset()));
            params.put("size", String.valueOf(pageable.getPageSize()));

            searchResponse = highLevelClient.getLowLevelClient().performRequest(
                HttpPost.METHOD_NAME,
                '/' + elasticSearchTemplate.getRootIndexName(indexName) + '/' + indexName + "/_search",
                params,
                nStringEntity);

            Result<T> results = parseResults(searchResponse, pageable);

            return results;

        } catch (Exception ex) {
            log.error("Error occured during searchComplex of document {} with query [{}] : {}", indexName, jsonQuery, ex);
        } finally {
            if (searchResponse != null) EntityUtils.consumeQuietly(searchResponse.getEntity());
        }
        return new Result<>();

    }

    /**
     * Private method responsible for parsing search results into a list of entities and a total
     * @param searchResponse the SearchResponse to parse
     * @return a paris of list of results and the total number of results
     * @throws IOException When reading the response fails
     */
    @SuppressWarnings("unchecked")
    private Result<T> parseResults(Response searchResponse, Pageable pageable) throws IOException {

        Result<T> result = documentMapper.mapToObject(EntityUtils.toString(searchResponse.getEntity()), Result.class);
        result.getHits().getRawResults().forEach(r -> {
            try {
                r.setSource(documentMapper.mapToObject(r.getElementAsMap(), getIndexedClass()));
            } catch (IOException ex) {
                log.error("Error occured during parsing of searchComplex results", ex);
            }
        });

        result.getHits().setResults(
            new PageImpl<T>(
                result.getHits().getRawResults().stream().map(Element::getSource).collect(Collectors.toList()),
            pageable,
            result.getHits().getTotal()));

        return result;
    }

    @Override
    public T findOne(ID id) {

        Validate.notNull(id);

        GetResponse getResponse = null;
        try {
            GetRequest get = new GetRequest().index(elasticSearchTemplate.getRootIndexName(indexName)).type(indexName).id(id.toString());
            getResponse = highLevelClient.get(get);
            if (!getResponse.isExists()) {
                log.error("Document {} with id [{}] does not exists", indexName, id);
                return null;
            }
            return documentMapper.mapToObject(getResponse.getSource(), getIndexedClass());
        } catch (Exception ex) {
            log.error("Error occured during fetching of document {} with id [{}] : {}", indexName, id, ex);
        }
        return null;
    }

    @Override
    public boolean exists(ID id) {
        return findOne(id) != null;
    }

    @Override
    public long count() {
        if (!indexExists()) {
            return 0;
        }
        Response countResponse = null;

        try {
            countResponse = highLevelClient.getLowLevelClient().performRequest(
                HttpGet.METHOD_NAME,
                '/' + elasticSearchTemplate.getRootIndexName(indexName) + '/' + indexName + "/_count",
                Collections.emptyMap());
            CountObject result = documentMapper.mapToObject(EntityUtils.toString(countResponse.getEntity()), CountObject.class);
            return result.getCount();
        } catch (Exception ex) {
            log.error("Error occured during counting of document {} : {}", indexName, ex);
        } finally {
            if (countResponse != null) EntityUtils.consumeQuietly(countResponse.getEntity());
        }

        return 0;
    }

    @Override
    public <S extends T> S save(S entity) {
        Validate.notNull(entity);

        try {
            String id = getIndexedClass().getMethod("getId").invoke(entity).toString();
            IndexRequest indexRequest = new IndexRequest(elasticSearchTemplate.getRootIndexName(indexName), indexName, id);
            String json = documentMapper.mapToString(entity);
            indexRequest.source(json, XContentType.JSON);
            indexRequest.setRefreshPolicy(elasticSearchTemplate.getRefreshPolicy());
            highLevelClient.index(indexRequest);
        } catch (Exception ex) {
            log.error("Error occured during indexing of document {} : {}", indexName, ex);
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

        log.debug("Deleting record for document {} and id [{}]", indexName, id);
        if (!indexExists()) {
            return;
        }
        try {
            DeleteRequest deleteRequest = new DeleteRequest(elasticSearchTemplate.getRootIndexName(indexName), indexName, id.toString());
            deleteRequest.setRefreshPolicy(elasticSearchTemplate.getRefreshPolicy());
            highLevelClient.delete(deleteRequest);
        } catch (Exception ex) {
            log.error("Error occured during deleting of document {} with id [{}] : {}", indexName, id, ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(T entity) {
        Validate.notNull(entity);

        log.debug("Deleting record for document {} and entity [{}]", indexName, entity);
        try {
            Object id = getIndexedClass().getMethod("getId").invoke(entity);
            delete((ID) id);
        } catch (Exception ex) {
            log.error("Error occured during deletion of document {} with entity [{}] : {}", indexName, entity, ex);
        }
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        entities.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        log.debug("Deleting all records for document {}", indexName);

        if (!indexExists()) {
            return;
        }

        try {
            highLevelClient.getLowLevelClient().performRequest(
                HttpPost.METHOD_NAME,
                '/' + elasticSearchTemplate.getRootIndexName(indexName) + '/' + indexName + "/_delete_by_query",
                elasticSearchTemplate.getParams(),
                new NStringEntity("{\n" +
                    "  \"query\": {\n" +
                    "    \"match_all\": {}\n" +
                    "  }\n" +
                    "}", ContentType.APPLICATION_JSON));
        } catch (Exception ex) {
            log.error("Error occured during deletion of all records for index {} : {}", indexName, ex);
        }
    }

    @Override
    public Class<T> getIndexedClass() {
        return indexedClass;
    }

    protected boolean indexExists() {
        return elasticSearchTemplate.indexExists(highLevelClient.getLowLevelClient(), indexName, indexName);
    }

    /**
     * The indexName is calculated based on the following rule :
     * - When specified in the "indexName" property of the ElasticsearchDocument annotation, use it
     * - When not specified and there is a annotatedClass property, use the annotatedClass simpleName
     * - Otherwise, use the indexedClass simpleName
     * @return the IndexName to use
     */
    private String getIndexName() {
        ElasticsearchDocument annotation = getIndexedClass().getAnnotation(ElasticsearchDocument.class);
        String specifiedIndexName = annotation != null ? annotation.indexName() : null;
        return StringUtils.lowerCase(
            StringUtils.isNotBlank(specifiedIndexName) ?
                specifiedIndexName :
                (annotatedClass == null ? getIndexedClass().getSimpleName() : annotatedClass.getSimpleName()));
    }
}

