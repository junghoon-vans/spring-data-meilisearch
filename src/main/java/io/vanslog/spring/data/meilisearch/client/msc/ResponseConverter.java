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
package io.vanslog.spring.data.meilisearch.client.msc;

import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchHitsImpl;
import io.vanslog.spring.data.meilisearch.core.federation.FederationResponse;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.Searchable;

/**
 * Class to convert Meilisearch classes into Spring Data Meilisearch responses.
 */
public class ResponseConverter {

	private final ObjectMapper objectMapper;

	public ResponseConverter() {
		this.objectMapper = new ObjectMapper();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> mapHitList(Searchable searchable, Class<?> clazz) {
		return (List<T>) searchable.getHits().stream() //
				.map(hit -> objectMapper.convertValue(hit, clazz)) //
				.map(content -> new SearchHit<>(content, searchable)) //
				.toList();
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> mapHitList(FacetSearchable searchable, Class<?> clazz) {
		return (List<T>) searchable.getFacetHits().stream() //
				.map(hit -> objectMapper.convertValue(hit, clazz)) //
				.map(content -> new SearchHit<>(content, searchable)) //
				.toList();
	}

	public <T> SearchHits<T> mapHits(Searchable searchable, Class<T> clazz) {
		List<SearchHit<T>> searchHits = this.mapHitList(searchable, clazz);
		Duration executionDuration = Duration.ofMillis(searchable.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapHits(FacetSearchable searchable, Class<T> clazz) {
		List<SearchHit<T>> searchHits = this.mapHitList(searchable, clazz);
		Duration executionDuration = Duration.ofMillis(searchable.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapResult(MultiSearchResult result, Class<T> clazz) {
		List<? extends SearchHit<T>> searchHits = result.getHits().stream() //
				.map(hit -> {
					FederationResponse federation = objectMapper.convertValue(hit.get("_federation"), FederationResponse.class);
					hit.remove("_federation");
					return new SearchHit<>(objectMapper.convertValue(hit, clazz), result, federation);
				}).toList();

		Duration executionDuration = Duration.ofMillis(result.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapResults(Results<MultiSearchResult> results, Class<T> clazz) {
		MultiSearchResult[] multiSearchResults = results.getResults();
		List<SearchHit<T>> searchHits = Arrays.stream(multiSearchResults) //
				.flatMap(result -> result.getHits().stream() //
						.map(hit -> objectMapper.convertValue(hit, clazz)) //
						.map(content -> new SearchHit<>(content, result)))
				.toList();

		int maxProcessingTime = Arrays.stream(multiSearchResults) //
				.mapToInt(MultiSearchResult::getProcessingTimeMs).max().orElse(0);
		Duration executionDuration = Duration.ofMillis(maxProcessingTime);

		return new SearchHitsImpl<>(executionDuration, searchHits);
	}
}
