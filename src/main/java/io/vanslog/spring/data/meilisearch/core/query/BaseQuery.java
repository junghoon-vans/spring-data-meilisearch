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

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public abstract class BaseQuery implements Query {

	static int DEFAULT_PAGE_SIZE = 10;
	static Pageable DEFAULT_PAGE = PageRequest.of(0, DEFAULT_PAGE_SIZE);

	@Nullable protected String q;
	@Nullable protected Sort sort;
	protected Pageable pageable = DEFAULT_PAGE;
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

	public BaseQuery() {}

	public BaseQuery(String q) {
		this.q = q;
	}

	public <Q extends BaseQuery, B extends BaseQueryBuilder<Q, B>> BaseQuery(BaseQueryBuilder<Q, B> builder) {
		this.q = builder.getQ();
		this.sort = builder.getSort();
		setPageable(builder.getPageable() != null ? builder.getPageable() : DEFAULT_PAGE);
		this.attributesToRetrieve = builder.getAttributesToRetrieve();
		this.attributesToCrop = builder.getAttributesToCrop();
		this.cropLength = builder.getCropLength();
		this.cropMarker = builder.getCropMarker();
		this.highlightPreTag = builder.getHighlightPreTag();
		this.highlightPostTag = builder.getHighlightPostTag();
		this.matchingStrategy = builder.getMatchingStrategy();
		this.attributesToHighlight = builder.getAttributesToHighlight();
		this.attributesToSearchOn = builder.getAttributesToSearchOn();
		this.filter = builder.getFilter();
		this.filterArray = builder.getFilterArray();
		this.showMatchesPosition = builder.getShowMatchesPosition();
		this.facets = builder.getFacets();
		this.showRankingScore = builder.getShowRankingScore();
		this.showRankingScoreDetails = builder.getShowRankingScoreDetails();
		this.rankingScoreThreshold = builder.getRankingScoreThreshold();
		this.locales = builder.getLocales();
		this.distinct = builder.getDistinct();
	}

	@Nullable
	public Sort getSort() {
		return this.sort;
	}

	public void setSort(@Nullable Sort sort) {
		this.sort = sort;
	}

	public Pageable getPageable() {
		return this.pageable;
	}

	@SuppressWarnings("unchecked")
	public final <T extends Query> T setPageable(Pageable pageable) {
		Assert.notNull(pageable, "Pageable must not be null!");
		this.pageable = pageable;
		return this.addSort(pageable.getSort());
	}

	@SuppressWarnings("unchecked")
	public final <T extends Query> T addSort(@Nullable Sort sort) {
		if (sort == null) {
			return (T) this;
		}

		if (this.sort == null) {
			this.sort = sort;
		} else {
			this.sort = this.sort.and(sort);
		}

		return (T) this;
	}

	@Override
	@Nullable
	public String getQ() {
		return q;
	}

	@Override
	public void setQ(@Nullable String q) {
		this.q = q;
	}

	@Nullable
	public String[] getAttributesToRetrieve() {
		return attributesToRetrieve;
	}

	public void setAttributesToRetrieve(String[] attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
	}

	@Nullable
	public String[] getAttributesToCrop() {
		return attributesToCrop;
	}

	public void setAttributesToCrop(String[] attributesToCrop) {
		this.attributesToCrop = attributesToCrop;
	}

	@Nullable
	public Integer getCropLength() {
		return cropLength;
	}

	public void setCropLength(Integer cropLength) {
		this.cropLength = cropLength;
	}

	@Nullable
	public String getCropMarker() {
		return cropMarker;
	}

	public void setCropMarker(String cropMarker) {
		this.cropMarker = cropMarker;
	}

	@Nullable
	public String getHighlightPreTag() {
		return highlightPreTag;
	}

	public void setHighlightPreTag(String highlightPreTag) {
		this.highlightPreTag = highlightPreTag;
	}

	@Nullable
	public String getHighlightPostTag() {
		return highlightPostTag;
	}

	public void setHighlightPostTag(String highlightPostTag) {
		this.highlightPostTag = highlightPostTag;
	}

	@Override
	@Nullable
	public MatchingStrategy getMatchingStrategy() {
		return matchingStrategy;
	}

	@Override
	public void setMatchingStrategy(MatchingStrategy matchingStrategy) {
		this.matchingStrategy = matchingStrategy;
	}

	@Nullable
	public String[] getAttributesToHighlight() {
		return attributesToHighlight;
	}

	public void setAttributesToHighlight(String[] attributesToHighlight) {
		this.attributesToHighlight = attributesToHighlight;
	}

	@Override
	@Nullable
	public String[] getAttributesToSearchOn() {
		return attributesToSearchOn;
	}

	@Override
	public void setAttributesToSearchOn(String[] attributesToSearchOn) {
		this.attributesToSearchOn = attributesToSearchOn;
	}

	@Override
	@Nullable
	public String[] getFilter() {
		return filter;
	}

	@Override
	public void setFilter(String[] filter) {
		this.filter = filter;
	}

	@Override
	@Nullable
	public String[][] getFilterArray() {
		return filterArray;
	}

	@Override
	public void setFilterArray(String[][] filterArray) {
		this.filterArray = filterArray;
	}

	public boolean isShowMatchesPosition() {
		return showMatchesPosition;
	}

	public void setShowMatchesPosition(boolean showMatchesPosition) {
		this.showMatchesPosition = showMatchesPosition;
	}

	@Nullable
	public String[] getFacets() {
		return facets;
	}

	public void setFacets(String[] facets) {
		this.facets = facets;
	}

	public boolean isShowRankingScore() {
		return showRankingScore;
	}

	public void setShowRankingScore(boolean showRankingScore) {
		this.showRankingScore = showRankingScore;
	}

	public boolean isShowRankingScoreDetails() {
		return showRankingScoreDetails;
	}

	public void setShowRankingScoreDetails(boolean showRankingScoreDetails) {
		this.showRankingScoreDetails = showRankingScoreDetails;
	}

	@Nullable
	public Double getRankingScoreThreshold() {
		return rankingScoreThreshold;
	}

	public void setRankingScoreThreshold(Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
	}

	@Nullable
	public String[] getLocales() {
		return locales;
	}

	public void setLocales(String[] locales) {
		this.locales = locales;
	}

	@Nullable
	public String getDistinct() {
		return distinct;
	}

	public void setDistinct(String distinct) {
		this.distinct = distinct;
	}
}
