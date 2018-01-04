package nc.rubiks.core.search.elasticsearch.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Object being stored in DB representing an action to perform
 * in order to synchronize a database record with an Elasticsearch document record
 *
 * @author nicoraynaud
 */
@Entity
@NamedQuery(name = "resetTryouts", query = "UPDATE ElasticsearchSyncAction esa SET esa.nbTryouts = 0")
public class ElasticsearchSyncAction {

    @Id
    @GeneratedValue
    private UUID id;

    private String objType;

    private String objId;

    @Enumerated(EnumType.STRING)
    private ElasticsearchSyncActionEnum action;

    private LocalDateTime createdDate = LocalDateTime.now();

    private int nbTryouts;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getObjType() {
        return objType;
    }

    public void setObjType(String objType) {
        this.objType = objType;
    }

    public String getObjId() {
        return objId;
    }

    public void setObjId(String objId) {
        this.objId = objId;
    }

    public ElasticsearchSyncActionEnum getAction() {
        return action;
    }

    public void setAction(ElasticsearchSyncActionEnum action) {
        this.action = action;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public int getNbTryouts() {
        return nbTryouts;
    }

    public void setNbTryouts(int nbTryouts) {
        this.nbTryouts = nbTryouts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElasticsearchSyncAction that = (ElasticsearchSyncAction) o;

        if (nbTryouts != that.nbTryouts) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (objType != null ? !objType.equals(that.objType) : that.objType != null) return false;
        if (objId != null ? !objId.equals(that.objId) : that.objId != null) return false;
        if (action != that.action) return false;
        return createdDate != null ? createdDate.equals(that.createdDate) : that.createdDate == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (objType != null ? objType.hashCode() : 0);
        result = 31 * result + (objId != null ? objId.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (createdDate != null ? createdDate.hashCode() : 0);
        result = 31 * result + nbTryouts;
        return result;
    }

    @Override
    public String toString() {
        return "ElasticsearchSyncAction{" +
            "id=" + id +
            ", objType='" + objType + '\'' +
            ", objId='" + objId + '\'' +
            ", action=" + action +
            ", createdDate=" + createdDate +
            ", nbTryouts=" + nbTryouts +
            '}';
    }
}
