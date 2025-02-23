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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class FacetQuery implements Query {

	@Nullable protected String facetName;
	@Nullable protected String facetQuery;
	@Nullable protected String q;
	@Nullable protected String[] filter;
	@Nullable protected String[][] filterArray;
	@Nullable protected MatchingStrategy matchingStrategy;
	@Nullable protected String[] attributesToSearchOn;

	public FacetQuery(String facetName) {
		Assert.notNull(facetName, "facetName must not be null");
		this.facetName = facetName;
	}

	public FacetQuery(String facetName, String q) {
		Assert.notNull(facetName, "facetName must not be null");
		this.facetName = facetName;
		this.q = q;
	}

	public FacetQuery(FacetQueryBuilder builder) {
		Assert.notNull(builder.getFacetName(), "facetName must not be null");
		this.facetName = builder.getFacetName();
		this.facetQuery = builder.getFacetQuery();
		this.q = builder.getQ();
		this.filter = builder.getFilter();
		this.filterArray = builder.getFilterArray();
		this.matchingStrategy = builder.getMatchingStrategy();
		this.attributesToSearchOn = builder.getAttributesToSearchOn();
	}

	public static FacetQueryBuilder builder() {
		return new FacetQueryBuilder();
	}

	@Nullable
	public String getFacetName() {
		return facetName;
	}

	public void setFacetName(@Nullable String facetName) {
		this.facetName = facetName;
	}

	@Nullable
	public String getFacetQuery() {
		return facetQuery;
	}

	public void setFacetQuery(@Nullable String facetQuery) {
		this.facetQuery = facetQuery;
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

	@Override
	@Nullable
	public String[] getFilter() {
		return filter;
	}

	@Override
	public void setFilter(@Nullable String[] filter) {
		this.filter = filter;
	}

	@Override
	@Nullable
	public String[][] getFilterArray() {
		return filterArray;
	}

	@Override
	public void setFilterArray(@Nullable String[][] filterArray) {
		this.filterArray = filterArray;
	}

	@Override
	@Nullable
	public MatchingStrategy getMatchingStrategy() {
		return matchingStrategy;
	}

	@Override
	public void setMatchingStrategy(@Nullable MatchingStrategy matchingStrategy) {
		this.matchingStrategy = matchingStrategy;
	}

	@Override
	@Nullable
	public String[] getAttributesToSearchOn() {
		return attributesToSearchOn;
	}

	@Override
	public void setAttributesToSearchOn(@Nullable String[] attributesToSearchOn) {
		this.attributesToSearchOn = attributesToSearchOn;
	}
}
