package nc.coconut.elasticsearch.mapper;


import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Mapper interface to implement to (de)serialize entities (from)to ES
 */
public interface EntityMapper {

    String mapToString(Object object) throws IOException;

    <T> T mapToObject(String source, Class<T> clazz) throws IOException;

    JsonNode readTree(String inputSource) throws IOException;
}
