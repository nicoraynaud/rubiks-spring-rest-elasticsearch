package nc.rubiks.core.search.elasticsearch.endpoint;

public class ElasticsearchSyncReset {

    private String message = "Synchronization tryouts has been reset for all expired records in ElasticsearchSyncAction table";

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
