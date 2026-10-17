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

import io.vanslog.spring.data.meilisearch.client.MeilisearchClient.MultiSearchResultMetadata;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient.MultiSearchResultWithMetadata;
import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHitMetadata;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchHitsImpl;
import io.vanslog.spring.data.meilisearch.core.SearchResponseMetadata;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.document.Document;
import io.vanslog.spring.data.meilisearch.core.federation.FederationResponse;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.SearchResult;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.SimilarDocumentsResults;

/**
 * Class to convert Meilisearch classes into Spring Data Meilisearch responses.
 */
public class ResponseConverter {

	private static final TypeReference<Map<String, Object>> METADATA_MAP_TYPE = new TypeReference<>() {};

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
				.map(hit -> mapSearchHit(hit, searchable, clazz)) //
				.toList();
	}

	private <T> SearchHit<T> mapSearchHit(Map<String, Object> hit, Searchable searchable, Class<T> clazz) {
		return new SearchHit<>(readHit(hit, clazz), searchable.getProcessingTimeMs(), searchable.getQuery(),
				searchable.getFacetStats(), searchable.getFacetDistribution(), null, readHitMetadata(hit));
	}

	public <T> List<SearchHit<T>> mapHitList(FacetSearchable searchable, Class<T> clazz) {
		return searchable.getFacetHits().stream() //
				.map(hit -> new SearchHit<>(readHit(hit, clazz), searchable)) //
				.toList();
	}

	public <T> SearchHits<T> mapHits(Searchable searchable, Class<T> clazz) {
		List<SearchHit<T>> searchHits = this.mapHitList(searchable, clazz);
		Duration executionDuration = Duration.ofMillis(searchable.getProcessingTimeMs());
		SearchResponseMetadata responseMetadata = mapResponseMetadata(searchable, searchHits.size());
		List<SearchResponseMetadata> responseMetadataList = List.of(responseMetadata);

		if (searchable instanceof MeilisearchSearchResult result && result.hasTotalHits()) {
			return new SearchHitsImpl<>(executionDuration, searchHits, result.getTotalHits(), TotalHitsRelation.EQUAL_TO,
					responseMetadataList);
		}

		if (searchable instanceof SearchResultPaginated result) {
			return new SearchHitsImpl<>(executionDuration, searchHits, result.getTotalHits(), TotalHitsRelation.EQUAL_TO,
					responseMetadataList);
		}

		return new SearchHitsImpl<>(executionDuration, searchHits, searchHits.size(), TotalHitsRelation.OFF,
				responseMetadataList);
	}

	private SearchResponseMetadata mapResponseMetadata(Searchable searchable, int hitCount) {

		Long totalHits = null;
		Long estimatedTotalHits = null;
		Integer page = null;
		Integer hitsPerPage = null;
		Integer totalPages = null;
		Integer offset = null;
		Integer limit = null;

		if (searchable instanceof MeilisearchSearchResult result) {
			totalHits = result.hasTotalHits() ? (long) result.getTotalHits() : null;
			Integer estimated = result.getEstimatedTotalHits();
			estimatedTotalHits = estimated == null ? null : estimated.longValue();
			page = result.getPage();
			hitsPerPage = result.getHitsPerPage();
			totalPages = result.getTotalPages();
			offset = result.getOffset();
			limit = result.getLimit();
		} else if (searchable instanceof SearchResultPaginated result) {
			totalHits = (long) result.getTotalHits();
			page = result.getPage();
			hitsPerPage = result.getHitsPerPage();
			totalPages = result.getTotalPages();
		} else if (searchable instanceof SearchResult result) {
			estimatedTotalHits = (long) result.getEstimatedTotalHits();
			offset = result.getOffset();
			limit = result.getLimit();
		}

		return new SearchResponseMetadata(0, 0, hitCount, searchable.getProcessingTimeMs(), searchable.getQuery(), null,
				totalHits, estimatedTotalHits, page, hitsPerPage, totalPages, offset, limit,
				metadataMap(searchable.getFacetDistribution()), metadataMap(searchable.getFacetStats()), null);
	}

	@Nullable
	private Map<String, Object> metadataMap(@Nullable Object source) {
		return source == null ? null : objectMapper.convertValue(source, METADATA_MAP_TYPE);
	}

	private SearchResponseMetadata mapResponseMetadata(MultiSearchResult result, int queryIndex, int firstHitIndex,
			int hitCount) {

		MultiSearchResultMetadata capturedMetadata = result instanceof MultiSearchResultWithMetadata enriched
				? enriched.getResponseMetadata()
				: null;
		Integer processingTimeMs = capturedMetadata != null ? capturedMetadata.processingTimeMs()
				: result.getProcessingTimeMs();
		Long estimatedTotalHits;
		if (capturedMetadata != null) {
			estimatedTotalHits = capturedMetadata.estimatedTotalHits();
		} else {
			estimatedTotalHits = result.getEstimatedTotalHits() > 0 ? (long) result.getEstimatedTotalHits() : null;
		}

		return new SearchResponseMetadata(queryIndex, firstHitIndex, hitCount, processingTimeMs,
				capturedMetadata != null ? capturedMetadata.query() : result.getQuery(),
				capturedMetadata != null ? capturedMetadata.indexUid() : result.getIndexUid(),
				capturedMetadata != null ? capturedMetadata.totalHits() : null, estimatedTotalHits,
				capturedMetadata != null ? capturedMetadata.page() : null,
				capturedMetadata != null ? capturedMetadata.hitsPerPage() : null,
				capturedMetadata != null ? capturedMetadata.totalPages() : null,
				capturedMetadata != null ? capturedMetadata.offset() : null,
				capturedMetadata != null ? capturedMetadata.limit() : null, metadataMap(result.getFacetDistribution()),
				metadataMap(result.getFacetStats()), metadataMap(result.getFacetsByIndex()));
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
					FederationResponse federation = objectMapper.convertValue(source.remove("_federation"),
							FederationResponse.class);
					return new SearchHit<>(readHit(source, clazz), result.getProcessingTimeMs(), result.getQuery(),
							result.getFacetStats(), result.getFacetDistribution(), federation, readHitMetadata(source));
				}).toList();

		Duration executionDuration = Duration.ofMillis(result.getProcessingTimeMs());
		SearchResponseMetadata responseMetadata = new SearchResponseMetadata(0, 0, searchHits.size(),
				result.getProcessingTimeMs(), result.getQuery(), result.getIndexUid(), null, null, null, null, null, null, null,
				metadataMap(result.getFacetDistribution()), metadataMap(result.getFacetStats()),
				metadataMap(result.getFacetsByIndex()));

		return new SearchHitsImpl<>(executionDuration, searchHits, searchHits.size(), TotalHitsRelation.OFF,
				List.of(responseMetadata));
	}

	public <T> SearchHits<T> mapResult(SimilarDocumentsResults result, Class<T> clazz) {
		List<SearchHit<T>> searchHits = result.getHits().stream() //
				.map(hit -> new SearchHit<>(readHit(hit, clazz), result.getProcessingTimeMs(), "", null, null, null,
						readHitMetadata(hit))) //
				.toList();

		Duration executionDuration = Duration.ofMillis(result.getProcessingTimeMs());
		return new SearchHitsImpl<>(executionDuration, searchHits);
	}

	public <T> SearchHits<T> mapResults(Results<MultiSearchResult> results, Class<T> clazz) {
		MultiSearchResult[] multiSearchResults = results.getResults();
		List<SearchHit<T>> searchHits = new ArrayList<>();
		List<SearchResponseMetadata> responseMetadata = new ArrayList<>(multiSearchResults.length);

		int firstHitIndex = 0;
		int maxProcessingTime = 0;
		for (int queryIndex = 0; queryIndex < multiSearchResults.length; queryIndex++) {
			MultiSearchResult result = multiSearchResults[queryIndex];
			for (Map<String, Object> hit : result.getHits()) {
				searchHits.add(mapSearchHit(hit, result, clazz));
			}

			int hitCount = searchHits.size() - firstHitIndex;
			SearchResponseMetadata metadata = mapResponseMetadata(result, queryIndex, firstHitIndex, hitCount);
			responseMetadata.add(metadata);
			maxProcessingTime = Math.max(maxProcessingTime,
					metadata.getProcessingTimeMs() != null ? metadata.getProcessingTimeMs() : 0);
			firstHitIndex += hitCount;
		}

		return new SearchHitsImpl<>(Duration.ofMillis(maxProcessingTime), searchHits, searchHits.size(),
				TotalHitsRelation.OFF, responseMetadata);
	}

	private <T> T readHit(Map<String, Object> hit, Class<T> clazz) {

		if (FacetHit.class.equals(clazz)) {
			return objectMapper.convertValue(hit, clazz);
		}

		return meilisearchConverter.read(clazz, toDocument(hit));
	}

	@Nullable
	private static SearchHitMetadata readHitMetadata(Map<String, Object> source) {

		Map<String, Object> formattedFields = readMap(source.get("_formatted"));
		Map<String, Object> matchesPosition = readMap(source.get("_matchesPosition"));
		Object rankingScoreValue = source.get("_rankingScore");
		Double rankingScore = rankingScoreValue instanceof Number number ? number.doubleValue() : null;
		Map<String, Object> rankingScoreDetails = readMap(source.get("_rankingScoreDetails"));
		if (formattedFields == null && matchesPosition == null && rankingScore == null && rankingScoreDetails == null) {
			return null;
		}
		return new SearchHitMetadata(formattedFields, matchesPosition, rankingScore, rankingScoreDetails);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static Map<String, Object> readMap(@Nullable Object value) {

		if (!(value instanceof Map<?, ?> source)) {
			return null;
		}

		// These maps come from JSON object fields, whose keys are strings; the metadata constructor snapshots them.
		return (Map<String, Object>) source;
	}

	private static Document toDocument(Map<String, Object> hit) {

		Document document = Document.create();
		document.putAll(hit);
		return document;
	}
}
