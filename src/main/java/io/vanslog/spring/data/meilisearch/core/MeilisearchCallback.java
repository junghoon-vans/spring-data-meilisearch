package io.vanslog.spring.data.meilisearch.core;

import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;

/**
 * Callback interface for low level operations executed against a Meilisearch environment.
 * @param <T> return type
 */
@FunctionalInterface
public interface MeilisearchCallback<T> {

	T doWithIndex(Index index) throws MeilisearchException;
}
