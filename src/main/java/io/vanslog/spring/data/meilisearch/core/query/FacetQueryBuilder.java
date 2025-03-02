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

public class FacetQueryBuilder {

    @Nullable protected String facetName;
    @Nullable protected String facetQuery;
    @Nullable protected String q;
    @Nullable protected String[] filter;
    @Nullable protected String[][] filterArray;
    @Nullable protected MatchingStrategy matchingStrategy;
    @Nullable protected String[] attributesToSearchOn;

    public FacetQueryBuilder() {}

    public FacetQueryBuilder withFacetName(String facetName) {
        this.facetName = facetName;
        return this;
    }

    public FacetQueryBuilder withFacetQuery(String facetQuery) {
        this.facetQuery = facetQuery;
        return this;
    }

    public FacetQueryBuilder withQ(String q) {
        this.q = q;
        return this;
    }

    public FacetQueryBuilder withFilter(String[] filter) {
        this.filter = filter;
        return this;
    }

    public FacetQueryBuilder withFilterArray(String[][] filterArray) {
        this.filterArray = filterArray;
        return this;
    }

    public FacetQueryBuilder withMatchingStrategy(MatchingStrategy matchingStrategy) {
        this.matchingStrategy = matchingStrategy;
        return this;
    }

    public FacetQueryBuilder withAttributesToSearchOn(String[] attributesToSearchOn) {
        this.attributesToSearchOn = attributesToSearchOn;
        return this;
    }

    @Nullable
    public String getFacetName() {
        return facetName;
    }

    @Nullable
    public String getFacetQuery() {
        return facetQuery;
    }

    @Nullable
    public String getQ() {
        return q;
    }

    @Nullable
    public String[] getFilter() {
        return filter;
    }

    @Nullable
    public String[][] getFilterArray() {
        return filterArray;
    }

    @Nullable
    public MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }

    @Nullable
    public String[] getAttributesToSearchOn() {
        return attributesToSearchOn;
    }

	public FacetQuery build() {
		return new FacetQuery(this);
	}
}
