package nc.rubiks.core.search.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hits represent the results of the query (the items matching the search)
 * @param <T> The domain object being queried (results will be serialized to this type)
 */
public class Hits<T> {

    private long total = 0;
    private Float maxScore = 0F;
    private List<Element<T>> rawResults = new ArrayList<>();
    private Page<T> results = new PageImpl<>(Collections.emptyList(), null, 0);

    /**
     * @return The total number of results (and not on this single page)
     */
    @JsonProperty("total")
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * @return The maximum score reached by the results
     */
    @JsonProperty("max_score")
    public Float getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Float maxScore) {
        this.maxScore = maxScore;
    }

    /**
     * @return The ES raw results of the query (with their ES properties)
     */
    @JsonProperty("hits")
    public List<Element<T>> getRawResults() {
        return rawResults;
    }

    public void setRawResults(List<Element<T>> results) {
        this.rawResults = results;
    }

    /**
     * @return The list of results as a Page
     */
    public Page<T> getResults() {
        return results;
    }

    public void setResults(Page<T> results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "Hits{" +
            "total=" + total +
            ", maxScore=" + maxScore +
            ", rawResults=" + rawResults +
            '}';
    }
}
