package io.vanslog.spring.data.meilisearch.core.support;

import java.io.IOException;

/**
 * Interface to map entity to JSON and vice versa.
 *
 * @author Junghoon Ban
 * @since 1.0.0
 */
public interface EntityMapper {

    /**
     * Writes given object into JSON string.
     * @param object object to be written
     * @return JSON string
     */
    String toJson(Object object) throws IOException;

    /**
     * Reads object from given JSON string.
     * @param json JSON string
     * @param clazz type of object
     * @return object
     * @param <T> type of object
     */
    <T> T fromJson(String json, Class<T> clazz) throws IOException;
}
