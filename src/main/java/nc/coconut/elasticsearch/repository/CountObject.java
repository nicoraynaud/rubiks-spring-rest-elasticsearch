package nc.coconut.elasticsearch.repository;

/**
 * Object returned by ES when querying for a count of items in an Indice
 */
public class CountObject {

    private long count;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
