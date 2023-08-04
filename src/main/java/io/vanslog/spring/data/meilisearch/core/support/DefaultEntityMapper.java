package io.vanslog.spring.data.meilisearch.core.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Default implementation of {@link EntityMapper}
 * using Jackson's {@link ObjectMapper}.
 *
 * @author Junghoon Ban
 * @see EntityMapper
 * @see ObjectMapper
 */
public class DefaultEntityMapper implements EntityMapper {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new {@link DefaultEntityMapper} with default {@link ObjectMapper}.
     */
    public DefaultEntityMapper() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    @Override
    public String toJson(Object object) throws IOException {
        return objectMapper.writeValueAsString(object);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }
}
