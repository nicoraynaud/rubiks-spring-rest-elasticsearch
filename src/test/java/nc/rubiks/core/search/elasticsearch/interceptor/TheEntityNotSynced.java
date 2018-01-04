package nc.rubiks.core.search.elasticsearch.interceptor;

import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;

import java.util.Objects;

/**
 * Created by 2617ray on 03/05/2017.
 */
@ElasticsearchDocument
public class TheEntityNotSynced {

    private Long id;
    private String prop;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TheEntityNotSynced id(Long id) {
        this.id = id;
        return this;
    }

    public String getProp() {
        return prop;
    }

    public void setProp(String prop) {
        this.prop = prop;
    }

    public TheEntityNotSynced prop(String prop) {
        this.prop = prop;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TheEntityNotSynced theEntity = (TheEntityNotSynced) o;
        return Objects.equals(id, theEntity.id) &&
            Objects.equals(prop, theEntity.prop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, prop);
    }
}
