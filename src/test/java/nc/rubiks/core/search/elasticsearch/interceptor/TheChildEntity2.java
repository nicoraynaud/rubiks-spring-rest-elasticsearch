package nc.rubiks.core.search.elasticsearch.interceptor;

import nc.rubiks.core.search.elasticsearch.annotation.ElasticsearchDocument;

@ElasticsearchDocument(synced = true)
public class TheChildEntity2 {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
