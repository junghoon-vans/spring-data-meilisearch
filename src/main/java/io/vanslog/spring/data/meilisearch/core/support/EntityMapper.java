package io.vanslog.spring.data.meilisearch.core.support;

import java.io.IOException;

/**
 * Interface to map entity to JSON and vice versa.
 *
 * @since 1.0.0
 * @author Junghoon Ban
 */
public interface EntityMapper {

	public String toJson(Object object) throws IOException;

	public <T> T fromJson(String json, Class<T> clazz) throws IOException;
}
