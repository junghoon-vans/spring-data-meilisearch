/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.core;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.util.Streamable;

public interface SearchHits<T> extends Streamable<SearchHit<T>> {

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
