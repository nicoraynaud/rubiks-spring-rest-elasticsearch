package nc.rubiks.core.search.elasticsearch.interceptor;

import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;
import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchTriggerSync;

import java.util.Set;

@ElasticsearchDocument(synced = true)
public class TheEntityWithChildren {

    private long id;

    @ElasticsearchTriggerSync
    private TheChildEntity theChildEntity;

    @ElasticsearchTriggerSync
    private Set<TheChildEntity2> theChildEntity2Set;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public TheChildEntity getTheChildEntity() {
        return theChildEntity;
    }

    public void setTheChildEntity(TheChildEntity theChildEntity) {
        this.theChildEntity = theChildEntity;
    }

    public Set<TheChildEntity2> getTheChildEntity2Set() {
        return theChildEntity2Set;
    }

    public void setTheChildEntity2Set(Set<TheChildEntity2> theChildEntity2Set) {
        this.theChildEntity2Set = theChildEntity2Set;
    }
}
