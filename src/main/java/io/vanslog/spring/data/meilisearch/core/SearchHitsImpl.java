/*
 * Copyright 2025-2026 the original author or authors.
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
import java.util.Collections;
import java.util.List;

import org.springframework.data.util.Lazy;

public class SearchHitsImpl<T> implements SearchHits<T> {

	private final Duration executionDuration;
	private final List<? extends SearchHit<T>> searchHits;
	private final long totalHits;
	private final TotalHitsRelation totalHitsRelation;
	private final Lazy<List<SearchHit<T>>> unmodifiableSearchHits;

	public SearchHitsImpl(Duration executionDuration, List<? extends SearchHit<T>> searchHits) {
		this(executionDuration, searchHits, searchHits.size(), TotalHitsRelation.OFF);
	}

	public SearchHitsImpl(Duration executionDuration, List<? extends SearchHit<T>> searchHits, long totalHits,
			TotalHitsRelation totalHitsRelation) {
		this.executionDuration = executionDuration;
		this.searchHits = searchHits;
		this.totalHits = totalHits;
		this.totalHitsRelation = totalHitsRelation;
		this.unmodifiableSearchHits = Lazy.of(() -> Collections.unmodifiableList(searchHits));
	}

	@Override
	public Duration getExecutionDuration() {
		return executionDuration;
	}

	@Override
	public SearchHit<T> getSearchHit(int index) {
		return searchHits.get(index);
	}

	@Override
	public List<SearchHit<T>> getSearchHits() {
		return unmodifiableSearchHits.get();
	}

	@Override
	public long getTotalHits() {
		return totalHits;
	}

	@Override
	public TotalHitsRelation getTotalHitsRelation() {
		return totalHitsRelation;
	}

	@Override
	public String toString() {
		return "SearchHits{" + //
				"executionDuration=" + executionDuration + //
				", searchHits={" + searchHits.size() + " elements}" + //
				", totalHits=" + totalHits + //
				", totalHitsRelation=" + totalHitsRelation + //
				'}';
	}
}
