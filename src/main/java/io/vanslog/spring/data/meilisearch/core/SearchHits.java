package io.vanslog.spring.data.meilisearch.core;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import org.springframework.data.util.Streamable;

public interface SearchHits<T> extends Streamable<SearchHit<T>> {

	/**
	 * @return the number of total hits.
	 */
	long getTotalHits();

	/**
	 * @return whether the {@link SearchHits} has search hits.
	 */
	default boolean hasSearchHits() {
		return !getSearchHits().isEmpty();
	}

	/**
	 * @return the execution duration it took to complete the request
	 */
	Duration getExecutionDuration();

	/**
	 * @param index position in List.
	 * @return the {@link SearchHit} at position {index}
	 * @throws IndexOutOfBoundsException on invalid index
	 */
	SearchHit<T> getSearchHit(int index);

	/**
	 * @return the contained {@link SearchHit}s.
	 */
	List<SearchHit<T>> getSearchHits();

	default Iterator<SearchHit<T>> iterator() {
		return getSearchHits().iterator();
	}
}
