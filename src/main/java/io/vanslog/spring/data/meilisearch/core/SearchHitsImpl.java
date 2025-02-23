package io.vanslog.spring.data.meilisearch.core;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.data.util.Lazy;

public class SearchHitsImpl<T> implements SearchHits<T> {

	private final Duration executionDuration;
	private final List<? extends SearchHit<T>> searchHits;
	private final Lazy<List<SearchHit<T>>> unmodifiableSearchHits;

	public SearchHitsImpl(Duration executionDuration, List<? extends SearchHit<T>> searchHits) {
		this.executionDuration = executionDuration;
		this.searchHits = searchHits;
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
	public String toString() {
		return "SearchHits{" + //
				"executionDuration=" + executionDuration + //
				", searchHits={" + searchHits.size() + " elements}" + //
				'}';
	}
}
