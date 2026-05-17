/*
 * Copyright 2026 the original author or authors.
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

import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchHitsImpl;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.document.MeilisearchDocument;
import io.vanslog.spring.data.meilisearch.core.federation.FederationResponse;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.SimilarDocumentsResults;

/**
 * Class to convert Meilisearch classes into Spring Data Meilisearch responses.
 */
public class ResponseConverter {

	private final MeilisearchConverter meilisearchConverter;
	private final ObjectMapper objectMapper;

	public ResponseConverter() {
		this(new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext()), new ObjectMapper());
	}

	public ResponseConverter(MeilisearchConverter meilisearchConverter) {
		this(meilisearchConverter, new ObjectMapper());
	}

	public ResponseConverter(MeilisearchConverter meilisearchConverter, ObjectMapper objectMapper) {

		Assert.notNull(meilisearchConverter, "MeilisearchConverter must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");

		this.meilisearchConverter = meilisearchConverter;
		this.objectMapper = objectMapper;
	}

	public <T> List<SearchHit<T>> mapHitList(Searchable searchable, Class<T> clazz) {
		return searchable.getHits().stream() //
				.map(hit -> new SearchHit<>(readHit(hit, clazz), searchable)) //
				.toList();
	}

	public <T> List<SearchHit<T>> mapHitList(FacetSearchable searchable, Class<T> clazz) {
		return searchable.getFacetHits().stream() //
				.map(hit -> new SearchHit<>(readHit(hit, clazz), searchable)) //
				.toList();
	}

	public <T> SearchHits<T> mapHits(Searchable searchable, Class<T> clazz) {
		List<SearchHit<T>> searchHits = this.mapHitList(searchable, clazz);
		Duration executionDuration = Duration.ofMillis(searchable.getProcessingTimeMs());
		if (searchable instanceof MeilisearchSearchResult) {
			MeilisearchSearchResult result = (MeilisearchSearchResult) searchable;
			if (result.hasTotalHits()) {
				return new SearchHitsImpl<>(executionDuration, searchHits, result.getTotalHits(),
						TotalHitsRelation.EQUAL_TO);
			}
		}

		if (searchable instanceof SearchResultPaginated) {
			SearchResultPaginated result = (SearchResultPaginated) searchable;
			return new SearchHitsImpl<>(executionDuration, searchHits, result.getTotalHits(),
					TotalHitsRelation.EQUAL_TO);
		}
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
					Map<String, Object> source = new LinkedHashMap<>(hit);
					FederationResponse federation = objectMapper.convertValue(source.remove("_federation"), FederationResponse.class);
					return new SearchHit<>(readHit(source, clazz), result, federation);
				}).toList();

		Duration executionDuration = Duration.ofMillis(result.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapResult(SimilarDocumentsResults result, Class<T> clazz) {
		List<SearchHit<T>> searchHits = result.getHits().stream() //
				.map(hit -> new SearchHit<>(readHit(hit, clazz), result)) //
				.toList();

		Duration executionDuration = Duration.ofMillis(result.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapResults(Results<MultiSearchResult> results, Class<T> clazz) {
		MultiSearchResult[] multiSearchResults = results.getResults();
		List<SearchHit<T>> searchHits = Arrays.stream(multiSearchResults) //
				.flatMap(result -> result.getHits().stream() //
						.map(hit -> new SearchHit<>(readHit(hit, clazz), result)))
				.toList();

		int maxProcessingTime = Arrays.stream(multiSearchResults) //
				.mapToInt(MultiSearchResult::getProcessingTimeMs).max().orElse(0);
		Duration executionDuration = Duration.ofMillis(maxProcessingTime);

		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	private <T> T readHit(Map<String, Object> hit, Class<T> clazz) {

		if (FacetHit.class.equals(clazz)) {
			return objectMapper.convertValue(hit, clazz);
		}

		return meilisearchConverter.read(clazz, toDocument(hit));
	}

	private static MeilisearchDocument toDocument(Map<String, Object> hit) {

		MeilisearchDocument document = MeilisearchDocument.create();
		document.putAll(hit);
		return document;
	}
}
