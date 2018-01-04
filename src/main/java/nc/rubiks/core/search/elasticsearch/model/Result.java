package nc.rubiks.core.search.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * The Result of a Complex ES query
 * This object contains both the query results and the aggregations results
 * Hits : Query results contains IDs, source objects and total items
 * Aggregations : results contain a map of all aggregations which keys represent the names of the aggs queries
 * @param <T> The domain object being queried (results will be serialized to this type)
 */
public class Result<T> {

    private Hits<T> hits = new Hits<>();

    private Map<String, Object> aggregations = new HashMap<>();

    /**
     * @return The list of results for the ES query
     */
    @JsonProperty("hits")
    public Hits<T> getHits() {
        return hits;
    }

    public void setHits(Hits<T> hits) {
        this.hits = hits;
    }

    /**
     * @return The list of aggregations for the ES query
     */
    @JsonProperty("aggregations")
    public Map<String, Object> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<String, Object> aggregations) {
        this.aggregations = aggregations;
    }
    @Override
    public String toString() {
        return "Result{" +
            "hits=" + hits +
            ", aggregations=" + aggregations +
            '}';
    }
}
