package nc.coconut.elasticsearch.repository;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TheEntity {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TheEntity theEntity = (TheEntity) o;

        return id != null ? id.equals(theEntity.id) : theEntity.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
