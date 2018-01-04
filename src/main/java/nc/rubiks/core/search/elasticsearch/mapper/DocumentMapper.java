package nc.rubiks.core.search.elasticsearch.mapper;


import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Map;

/**
 * Created by nicoraynaud on 28/04/2017.
 */
public interface DocumentMapper {

    String mapToString(Object object) throws IOException;

    <T> T mapToObject(String source, Class<T> clazz) throws IOException;

    <T> T mapToObject(Map source, Class<T> clazz) throws IOException;

    JsonNode readTree(String inputSource) throws IOException;
}
