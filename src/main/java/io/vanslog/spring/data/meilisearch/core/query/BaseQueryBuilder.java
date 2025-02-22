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
package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import com.meilisearch.sdk.model.MatchingStrategy;

public abstract class BaseQueryBuilder<Q extends BaseQuery, SELF extends BaseQueryBuilder<Q, SELF>> {

	@Nullable protected String q;
	@Nullable protected Sort sort;
	@Nullable protected Pageable pageable;
	@Nullable protected String[] attributesToRetrieve;
	@Nullable protected String[] attributesToCrop;
	@Nullable protected Integer cropLength;
	@Nullable protected String cropMarker;
	@Nullable protected String highlightPreTag;
	@Nullable protected String highlightPostTag;
	@Nullable protected MatchingStrategy matchingStrategy;
	@Nullable protected String[] attributesToHighlight;
	@Nullable protected String[] attributesToSearchOn;
	@Nullable protected String[] filter;
	@Nullable protected String[][] filterArray;
	protected boolean showMatchesPosition = false;
	@Nullable protected String[] facets;
	protected boolean showRankingScore = false;
	protected boolean showRankingScoreDetails = false;
	@Nullable protected Double rankingScoreThreshold;
	@Nullable protected String[] locales;
	@Nullable protected String distinct;

	public BaseQueryBuilder() {}

	public SELF withQ(String q) {
		this.q = q;
		return self();
	}

	public SELF withSort(Sort sort) {
		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}
		return self();
	}

	public SELF withPageable(Pageable pageable) {
		this.pageable = pageable;
		return self();
	}

	public SELF withAttributesToRetrieve(String... attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
		return self();
	}

	public SELF withAttributesToCrop(String... attributesToCrop) {
		this.attributesToCrop = attributesToCrop;
		return self();
	}

	public SELF withCropLength(Integer cropLength) {
		this.cropLength = cropLength;
		return self();
	}

	public SELF withCropMarker(String cropMarker) {
		this.cropMarker = cropMarker;
		return self();
	}

	public SELF withHighlightPreTag(String highlightPreTag) {
		this.highlightPreTag = highlightPreTag;
		return self();
	}

	public SELF withHighlightPostTag(String highlightPostTag) {
		this.highlightPostTag = highlightPostTag;
		return self();
	}

	public SELF withMatchingStrategy(MatchingStrategy matchingStrategy) {
		this.matchingStrategy = matchingStrategy;
		return self();
	}

	public SELF withAttributesToHighlight(String... attributesToHighlight) {
		this.attributesToHighlight = attributesToHighlight;
		return self();
	}

	public SELF withAttributesToSearchOn(String... attributesToSearchOn) {
		this.attributesToSearchOn = attributesToSearchOn;
		return self();
	}

	public SELF withFilter(String... filter) {
		this.filter = filter;
		return self();
	}

	public SELF withFilterArray(String[][] filterArray) {
		this.filterArray = filterArray;
		return self();
	}

	public SELF withShowMatchesPosition(boolean showMatchesPosition) {
		this.showMatchesPosition = showMatchesPosition;
		return self();
	}

	public SELF withFacets(String... facets) {
		this.facets = facets;
		return self();
	}

	public SELF withShowRankingScore(boolean showRankingScore) {
		this.showRankingScore = showRankingScore;
		return self();
	}

	public SELF withShowRankingScoreDetails(boolean showRankingScoreDetails) {
		this.showRankingScoreDetails = showRankingScoreDetails;
		return self();
	}

	public SELF withRankingScoreThreshold(Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
		return self();
	}

	public SELF withLocales(String... locales) {
		this.locales = locales;
		return self();
	}

	public SELF withDistinct(String distinct) {
		this.distinct = distinct;
		return self();
	}

	@Nullable
	public String getQ() {
		return q;
	}

	@Nullable
	public Sort getSort() {
		return sort;
	}

	@Nullable
	public Pageable getPageable() {
		return pageable;
	}

	@Nullable
	public String[] getAttributesToRetrieve() {
		return attributesToRetrieve;
	}

	@Nullable
	public String[] getAttributesToCrop() {
		return attributesToCrop;
	}

	@Nullable
	public Integer getCropLength() {
		return cropLength;
	}

	@Nullable
	public String getCropMarker() {
		return cropMarker;
	}

	@Nullable
	public String getHighlightPreTag() {
		return highlightPreTag;
	}

	@Nullable
	public String getHighlightPostTag() {
		return highlightPostTag;
	}

	@Nullable
	public MatchingStrategy getMatchingStrategy() {
		return matchingStrategy;
	}

	@Nullable
	public String[] getAttributesToHighlight() {
		return attributesToHighlight;
	}

	@Nullable
	public String[] getAttributesToSearchOn() {
		return attributesToSearchOn;
	}

	@Nullable
	public String[] getFilter() {
		return filter;
	}

	@Nullable
	public String[][] getFilterArray() {
		return filterArray;
	}

	public boolean getShowMatchesPosition() {
		return showMatchesPosition;
	}

	@Nullable
	public String[] getFacets() {
		return facets;
	}

	public boolean getShowRankingScore() {
		return showRankingScore;
	}

	public boolean getShowRankingScoreDetails() {
		return showRankingScoreDetails;
	}

	@Nullable
	public Double getRankingScoreThreshold() {
		return rankingScoreThreshold;
	}

	@Nullable
	public String[] getLocales() {
		return locales;
	}

	@Nullable
	public String getDistinct() {
		return distinct;
	}

	public abstract Q build();

	private SELF self() {
		// noinspection unchecked
		return (SELF) this;
	}
}
