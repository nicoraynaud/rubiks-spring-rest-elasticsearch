package nc.rubiks.core.search.elasticsearch.listener;

import nc.rubiks.core.search.elasticsearch.model.Result;
import nc.rubiks.core.search.elasticsearch.repository.ElasticsearchRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Collection;

public class ElasticsearchRepositoryGeneric implements ElasticsearchRepository {

    private Class type;

    public ElasticsearchRepositoryGeneric(Class type) {
        this.type = type;
    }

    @Override
    public Page search(Pageable pageable, String query) {
        return null;
    }

    @Override
    public Page search(Pageable pageable, QueryBuilder query) {
        return null;
    }

    @Override
    public SearchResponse search(Pageable pageable, QueryBuilder query, AggregationBuilder aggregation) {
        return null;
    }

    @Override
    public SearchResponse search(Pageable pageable, QueryBuilder query, Collection aggregations) {
        return null;
    }

    @Override
    public Result searchComplex(Pageable pageable, String jsonQuery) {
        return null;
    }

    @Override
    public Object findOne(Serializable serializable) {
        return null;
    }

    @Override
    public boolean exists(Serializable serializable) {
        return false;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void delete(Serializable serializable) {

    }

    @Override
    public void delete(Object entity) {

    }

    @Override
    public void delete(Iterable entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public Class getIndexedClass() {
        return type;
    }

    @Override
    public Iterable save(Iterable entities) {
        return null;
    }

    @Override
    public Object save(Object entity) {
        return null;
    }
}
