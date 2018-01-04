package nc.rubiks.core.search.elasticsearch.config;

/**
 * Created by nicoraynaud on 03/05/2017.
 */
public class RubiksElasticsearchConfigurationException extends RuntimeException {

    public RubiksElasticsearchConfigurationException(String message) {
        super(message);
    }

    public RubiksElasticsearchConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
