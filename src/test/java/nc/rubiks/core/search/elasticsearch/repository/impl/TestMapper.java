package nc.rubiks.core.search.elasticsearch.repository.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nc.rubiks.core.search.elasticsearch.mapper.DocumentMapper;

import java.io.IOException;
import java.util.Map;

/**
 * Created by 2617ray on 03/05/2017.
 */
public class TestMapper implements DocumentMapper {
    private ObjectMapper objectMapper;

    public TestMapper() {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Override
    public String mapToString(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    @Override
    public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
        return objectMapper.readValue(source, clazz);
    }

    @Override
    public <T> T mapToObject(Map source, Class<T> clazz) throws IOException {
        return objectMapper.convertValue(source, clazz);
    }

    @Override
    public JsonNode readTree(String inputSource) throws IOException {
        return objectMapper.readTree(inputSource);
    }
}
