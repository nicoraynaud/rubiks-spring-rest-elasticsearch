package nc.coconut.elasticsearch.config;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class ElasticsearchConfigurationException extends RuntimeException {

    public ElasticsearchConfigurationException() {
    }

    public ElasticsearchConfigurationException(String message) {
        super(message);
    }

    public ElasticsearchConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ElasticsearchConfigurationException(Throwable cause) {
        super(cause);
    }

    public ElasticsearchConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
