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
package io.vanslog.spring.data.meilisearch.core;

import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Metadata for one Meilisearch search response, including its position in a flattened multi-search result.
 *
 * @author Junghoon Ban
 */
public final class SearchResponseMetadata {

	private final int queryIndex;
	private final int firstHitIndex;
	private final int hitCount;
	@Nullable private final Integer processingTimeMs;
	@Nullable private final String query;
	@Nullable private final String indexUid;
	@Nullable private final Long totalHits;
	@Nullable private final Long estimatedTotalHits;
	@Nullable private final Integer page;
	@Nullable private final Integer hitsPerPage;
	@Nullable private final Integer totalPages;
	@Nullable private final Integer offset;
	@Nullable private final Integer limit;
	@Nullable private final Map<String, Object> facetDistribution;
	@Nullable private final Map<String, Object> facetStats;
	@Nullable private final Map<String, Object> facetsByIndex;

	public SearchResponseMetadata(int queryIndex, int firstHitIndex, int hitCount, @Nullable Integer processingTimeMs,
			@Nullable String query, @Nullable String indexUid, @Nullable Long totalHits, @Nullable Long estimatedTotalHits,
			@Nullable Integer page, @Nullable Integer hitsPerPage, @Nullable Integer totalPages, @Nullable Integer offset,
			@Nullable Integer limit, @Nullable Map<String, Object> facetDistribution,
			@Nullable Map<String, Object> facetStats, @Nullable Map<String, Object> facetsByIndex) {

		this.queryIndex = queryIndex;
		this.firstHitIndex = firstHitIndex;
		this.hitCount = hitCount;
		this.processingTimeMs = processingTimeMs;
		this.query = query;
		this.indexUid = indexUid;
		this.totalHits = totalHits;
		this.estimatedTotalHits = estimatedTotalHits;
		this.page = page;
		this.hitsPerPage = hitsPerPage;
		this.totalPages = totalPages;
		this.offset = offset;
		this.limit = limit;
		this.facetDistribution = immutableCopy(facetDistribution);
		this.facetStats = immutableCopy(facetStats);
		this.facetsByIndex = immutableCopy(facetsByIndex);
	}

	/**
	 * @return the zero-based response position in the request (zero for single search and federation)
	 */
	public int getQueryIndex() {
		return queryIndex;
	}

	/**
	 * @return the zero-based position of this response's first hit in {@link SearchHits#getSearchHits()}
	 */
	public int getFirstHitIndex() {
		return firstHitIndex;
	}

	/**
	 * @return the number of returned hits associated with this response, including zero
	 */
	public int getHitCount() {
		return hitCount;
	}

	/**
	 * @return the server-reported processing time, or {@literal null} when it was not captured
	 */
	@Nullable
	public Integer getProcessingTimeMs() {
		return processingTimeMs;
	}

	/**
	 * @return the query text (or facet query for a facet-search response), when present
	 */
	@Nullable
	public String getQuery() {
		return query;
	}

	/**
	 * @return the response's index uid, when present
	 */
	@Nullable
	public String getIndexUid() {
		return indexUid;
	}

	/**
	 * @return an exact total reported by this response, or {@literal null} when absent
	 */
	@Nullable
	public Long getTotalHits() {
		return totalHits;
	}

	/**
	 * @return an estimated total reported by this response, or {@literal null} when absent
	 */
	@Nullable
	public Long getEstimatedTotalHits() {
		return estimatedTotalHits;
	}

	/**
	 * @return the total-hit relation for this response, distinct from the aggregate {@link SearchHits} relation
	 */
	public SearchResponseTotalHitsRelation getTotalHitsRelation() {
		if (totalHits != null) {
			return SearchResponseTotalHitsRelation.EQUAL_TO;
		}
		if (estimatedTotalHits != null) {
			return SearchResponseTotalHitsRelation.ESTIMATED;
		}
		return SearchResponseTotalHitsRelation.OFF;
	}

	/**
	 * @return the server-reported page number, or {@literal null} when absent
	 */
	@Nullable
	public Integer getPage() {
		return page;
	}

	/**
	 * @return the server-reported page size, or {@literal null} when absent
	 */
	@Nullable
	public Integer getHitsPerPage() {
		return hitsPerPage;
	}

	/**
	 * @return the server-reported total page count, or {@literal null} when absent
	 */
	@Nullable
	public Integer getTotalPages() {
		return totalPages;
	}

	/**
	 * @return the server-reported offset, or {@literal null} when absent
	 */
	@Nullable
	public Integer getOffset() {
		return offset;
	}

	/**
	 * @return the server-reported result limit, or {@literal null} when absent
	 */
	@Nullable
	public Integer getLimit() {
		return limit;
	}

	/**
	 * @return the response-level facet distribution, or {@literal null} when absent
	 */
	@Nullable
	public Map<String, Object> getFacetDistribution() {
		return facetDistribution;
	}

	/**
	 * @return the response-level facet statistics, or {@literal null} when absent
	 */
	@Nullable
	public Map<String, Object> getFacetStats() {
		return facetStats;
	}

	/**
	 * @return per-index facet distribution/statistics, or {@literal null} when absent
	 */
	@Nullable
	public Map<String, Object> getFacetsByIndex() {
		return facetsByIndex;
	}

	@Nullable
	private static Map<String, Object> immutableCopy(@Nullable Map<String, Object> source) {
		return source == null ? null : SearchMetadataSupport.immutableMap(source);
	}
}
