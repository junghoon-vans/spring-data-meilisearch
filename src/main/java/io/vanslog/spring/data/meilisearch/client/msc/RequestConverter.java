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

import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.IndexQuery;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import com.meilisearch.sdk.FacetSearchRequest;
import com.meilisearch.sdk.IndexSearchRequest;
import com.meilisearch.sdk.MultiSearchRequest;
import com.meilisearch.sdk.SearchRequest;

/**
 * Class to convert Spring Data Meilisearch classes into Meilisearch requests.
 */
public class RequestConverter {

	public SearchRequest searchRequest(BaseQuery query) {
		Pageable pageable = query.getPageable();

		return SearchRequest.builder() //
				.q(query.getQ()) //
				.page(pageable.getPageNumber() + 1) //
				.hitsPerPage(pageable.getPageSize()) //
				.sort(convertSortToSortOptions(query.getSort())) //
				.attributesToRetrieve(query.getAttributesToRetrieve()) //
				.attributesToCrop(query.getAttributesToCrop()) //
				.cropLength(query.getCropLength()) //
				.cropMarker(query.getCropMarker()) //
				.highlightPreTag(query.getHighlightPreTag()) //
				.highlightPostTag(query.getHighlightPostTag()) //
				.matchingStrategy(query.getMatchingStrategy()) //
				.attributesToHighlight(query.getAttributesToHighlight()) //
				.attributesToSearchOn(query.getAttributesToSearchOn()) //
				.filter(query.getFilter()) //
				.filterArray(query.getFilterArray()) //
				.showMatchesPosition(query.isShowMatchesPosition()) //
				.facets(query.getFacets()) //
				.showRankingScore(query.isShowRankingScore()) //
				.showRankingScoreDetails(query.isShowRankingScoreDetails()) //
				.rankingScoreThreshold(query.getRankingScoreThreshold()) //
				.locales(query.getLocales()) //
				.distinct(query.getDistinct()) //
				.build();
	}

	public IndexSearchRequest indexSearchRequest(BaseQuery query, String indexUid) {
		IndexSearchRequest.IndexSearchRequestBuilder builder = IndexSearchRequest.builder();
		Pageable pageable = query.getPageable();

		builder.q(query.getQ()) //
				.page(pageable.getPageNumber() + 1) //
				.hitsPerPage(pageable.getPageSize()) //
				.sort(convertSortToSortOptions(query.getSort())) //
				.attributesToRetrieve(query.getAttributesToRetrieve()) //
				.attributesToCrop(query.getAttributesToCrop()) //
				.cropLength(query.getCropLength()) //
				.cropMarker(query.getCropMarker()) //
				.highlightPreTag(query.getHighlightPreTag()) //
				.highlightPostTag(query.getHighlightPostTag()) //
				.matchingStrategy(query.getMatchingStrategy()) //
				.attributesToHighlight(query.getAttributesToHighlight()) //
				.attributesToSearchOn(query.getAttributesToSearchOn()) //
				.filter(query.getFilter()) //
				.filterArray(query.getFilterArray()) //
				.showMatchesPosition(query.isShowMatchesPosition()) //
				.facets(query.getFacets()) //
				.showRankingScore(query.isShowRankingScore()) //
				.showRankingScoreDetails(query.isShowRankingScoreDetails()) //
				.rankingScoreThreshold(query.getRankingScoreThreshold()) //
				.locales(query.getLocales()) //
				.distinct(query.getDistinct());

		if (query instanceof IndexQuery indexQuery) {
			builder.indexUid(indexQuery.getIndexUid() != null ? indexQuery.getIndexUid() : indexUid) //
					.federationOptions(indexQuery.getFederationOptions());
		} else {
			builder.indexUid(indexUid);
		}

		return builder.build();
	}

	public MultiSearchRequest multiSearchRequest(List<? extends BaseQuery> queries, String indexUid) {
		MultiSearchRequest request = new MultiSearchRequest();
		queries.forEach(query -> request.addQuery(indexSearchRequest(query, indexUid)));
		return request;
	}

	public FacetSearchRequest searchRequest(FacetQuery query) {
		return FacetSearchRequest.builder() //
				.facetName(query.getFacetName()) //
				.facetQuery(query.getFacetQuery()) //
				.q(query.getQ()) //
				.filter(query.getFilter()) //
				.filterArray(query.getFilterArray()) //
				.matchingStrategy(query.getMatchingStrategy()).attributesToSearchOn(query.getAttributesToSearchOn()) //
				.build();
	}

	@Nullable
	private String[] convertSortToSortOptions(@Nullable Sort sort) {
		if (sort == null) {
			return null;
		}

		return sort.stream().map(order -> order.getProperty() + ":" + (order.isAscending() ? "asc" : "desc"))
				.toArray(String[]::new);
	}
}
