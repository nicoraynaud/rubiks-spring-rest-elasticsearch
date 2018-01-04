package nc.rubiks.core.search.elasticsearch;

public class RubiksElasticsearchException extends RuntimeException {

    public RubiksElasticsearchException(String message) {
        super(message);
    }

    public RubiksElasticsearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
