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

public class SimilarQueryBuilder {

	@Nullable private String documentId;
	@Nullable private String embedder;
	@Nullable private String[] attributesToRetrieve;
	@Nullable private Integer offset;
	@Nullable private Integer limit;
	@Nullable private String filter;
	@Nullable private Boolean showRankingScore;
	@Nullable private Boolean showRankingScoreDetails;
	@Nullable private Double rankingScoreThreshold;
	@Nullable private Boolean retrieveVectors;

	public SimilarQueryBuilder() {}

	public SimilarQuery build() {
		return new SimilarQuery(this);
	}

	public SimilarQueryBuilder withDocumentId(String documentId) {
		this.documentId = documentId;
		return this;
	}

	public SimilarQueryBuilder withEmbedder(String embedder) {
		this.embedder = embedder;
		return this;
	}

	public SimilarQueryBuilder withAttributesToRetrieve(@Nullable String[] attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
		return this;
	}

	public SimilarQueryBuilder withOffset(@Nullable Integer offset) {
		this.offset = offset;
		return this;
	}

	public SimilarQueryBuilder withLimit(@Nullable Integer limit) {
		this.limit = limit;
		return this;
	}

	public SimilarQueryBuilder withFilter(@Nullable String filter) {
		this.filter = filter;
		return this;
	}

	public SimilarQueryBuilder withShowRankingScore(@Nullable Boolean showRankingScore) {
		this.showRankingScore = showRankingScore;
		return this;
	}

	public SimilarQueryBuilder withShowRankingScoreDetails(@Nullable Boolean showRankingScoreDetails) {
		this.showRankingScoreDetails = showRankingScoreDetails;
		return this;
	}

	public SimilarQueryBuilder withRankingScoreThreshold(@Nullable Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
		return this;
	}

	public SimilarQueryBuilder withRetrieveVectors(@Nullable Boolean retrieveVectors) {
		this.retrieveVectors = retrieveVectors;
		return this;
	}

	@Nullable
	public String getDocumentId() {
		return documentId;
	}

	@Nullable
	public String getEmbedder() {
		return embedder;
	}

	@Nullable
	public String[] getAttributesToRetrieve() {
		return attributesToRetrieve;
	}

	@Nullable
	public Integer getOffset() {
		return offset;
	}

	@Nullable
	public Integer getLimit() {
		return limit;
	}

	@Nullable
	public String getFilter() {
		return filter;
	}

	@Nullable
	public Boolean getShowRankingScore() {
		return showRankingScore;
	}

	@Nullable
	public Boolean getShowRankingScoreDetails() {
		return showRankingScoreDetails;
	}

	@Nullable
	public Double getRankingScoreThreshold() {
		return rankingScoreThreshold;
	}

	@Nullable
	public Boolean getRetrieveVectors() {
		return retrieveVectors;
	}
}
