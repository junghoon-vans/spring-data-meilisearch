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
package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class SimilarQuery {

	private String documentId;
	private String embedder;
	@Nullable private String[] attributesToRetrieve;
	@Nullable private Integer offset;
	@Nullable private Integer limit;
	@Nullable private String filter;
	@Nullable private Boolean showRankingScore;
	@Nullable private Boolean showRankingScoreDetails;
	@Nullable private Double rankingScoreThreshold;
	@Nullable private Boolean retrieveVectors;

	public SimilarQuery(String documentId, String embedder) {
		this(builder().withDocumentId(documentId).withEmbedder(embedder));
	}

	public SimilarQuery(SimilarQueryBuilder builder) {
		String documentId = builder.getDocumentId();
		String embedder = builder.getEmbedder();
		Assert.notNull(documentId, "documentId must not be null");
		Assert.notNull(embedder, "embedder must not be null");

		this.documentId = documentId;
		this.embedder = embedder;
		this.attributesToRetrieve = builder.getAttributesToRetrieve();
		this.offset = builder.getOffset();
		this.limit = builder.getLimit();
		this.filter = builder.getFilter();
		this.showRankingScore = builder.getShowRankingScore();
		this.showRankingScoreDetails = builder.getShowRankingScoreDetails();
		this.rankingScoreThreshold = builder.getRankingScoreThreshold();
		this.retrieveVectors = builder.getRetrieveVectors();
	}

	public static SimilarQueryBuilder builder() {
		return new SimilarQueryBuilder();
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		Assert.notNull(documentId, "documentId must not be null");
		this.documentId = documentId;
	}

	public String getEmbedder() {
		return embedder;
	}

	public void setEmbedder(String embedder) {
		Assert.notNull(embedder, "embedder must not be null");
		this.embedder = embedder;
	}

	@Nullable
	public String[] getAttributesToRetrieve() {
		return attributesToRetrieve;
	}

	public void setAttributesToRetrieve(@Nullable String[] attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
	}

	@Nullable
	public Integer getOffset() {
		return offset;
	}

	public void setOffset(@Nullable Integer offset) {
		this.offset = offset;
	}

	@Nullable
	public Integer getLimit() {
		return limit;
	}

	public void setLimit(@Nullable Integer limit) {
		this.limit = limit;
	}

	@Nullable
	public String getFilter() {
		return filter;
	}

	public void setFilter(@Nullable String filter) {
		this.filter = filter;
	}

	@Nullable
	public Boolean getShowRankingScore() {
		return showRankingScore;
	}

	public void setShowRankingScore(@Nullable Boolean showRankingScore) {
		this.showRankingScore = showRankingScore;
	}

	@Nullable
	public Boolean getShowRankingScoreDetails() {
		return showRankingScoreDetails;
	}

	public void setShowRankingScoreDetails(@Nullable Boolean showRankingScoreDetails) {
		this.showRankingScoreDetails = showRankingScoreDetails;
	}

	@Nullable
	public Double getRankingScoreThreshold() {
		return rankingScoreThreshold;
	}

	public void setRankingScoreThreshold(@Nullable Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
	}

	@Nullable
	public Boolean getRetrieveVectors() {
		return retrieveVectors;
	}

	public void setRetrieveVectors(@Nullable Boolean retrieveVectors) {
		this.retrieveVectors = retrieveVectors;
	}
}
