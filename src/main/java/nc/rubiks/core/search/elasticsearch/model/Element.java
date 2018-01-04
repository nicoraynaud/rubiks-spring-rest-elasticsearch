package nc.rubiks.core.search.elasticsearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Element represent a document as a result of a search
 * @param <T> The domain object being queried (results will be serialized to this type)
 */
public class Element<T> {

    private String index;
    private String type;
    private Float score;
    private String id;
    private T source;
    private Map<String, Object> elementAsMap;

    /**
     * @return The ES index name containing the document
     */
    @JsonProperty("_index")
    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * @return The ES type inside the index containing the document
     */
    @JsonProperty("_type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return The score of this document regarding the search
     */
    @JsonProperty("_score")
    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    /**
     * @return The ES id of the document
     */
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The ES document serialized in the domain object type
     */
    public T getSource() {
        return source;
    }

    public void setSource(T element) {
        this.source = element;
    }

    /**
     * @return The raw ES document as a Map object
     */
    @JsonProperty("_source")
    public Map<String, Object> getElementAsMap() {
        return elementAsMap;
    }

    public void setElementAsMap(Map<String, Object> elementAsMap) {
        this.elementAsMap = elementAsMap;
    }

    @Override
    public String toString() {
        return "Element{" +
            "index='" + index + '\'' +
            ", type='" + type + '\'' +
            ", score=" + score +
            ", id='" + id + '\'' +
            ", source=" + source +
            '}';
    }
}
