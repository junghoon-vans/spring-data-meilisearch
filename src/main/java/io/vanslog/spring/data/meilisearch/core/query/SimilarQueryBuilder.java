package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;

public class SimilarQueryBuilder {

	@Nullable private String documentId;
	@Nullable private String embedder;
	@Nullable private String[] attributesToRetrieve;
	private int offset;
	private int limit;
	@Nullable private String filter;
	private boolean showRankingScore = false;
	private boolean showRankingScoreDetails = false;
	@Nullable private Double rankingScoreThreshold;
	private boolean retrieveVectors = false;

	public SimilarQueryBuilder() {}

	public SimilarQuery build() {
		return new SimilarQuery(this);
	}

	public SimilarQueryBuilder withDocumentId(@Nullable String documentId) {
		this.documentId = documentId;
		return this;
	}

	public SimilarQueryBuilder withEmbedder(@Nullable String embedder) {
		this.embedder = embedder;
		return this;
	}

	public SimilarQueryBuilder withAttributesToRetrieve(@Nullable String[] attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
		return this;
	}

	public SimilarQueryBuilder withOffset(int offset) {
		this.offset = offset;
		return this;
	}

	public SimilarQueryBuilder withLimit(int limit) {
		this.limit = limit;
		return this;
	}

	public SimilarQueryBuilder withFilter(@Nullable String filter) {
		this.filter = filter;
		return this;
	}

	public SimilarQueryBuilder withShowRankingScore(boolean showRankingScore) {
		this.showRankingScore = showRankingScore;
		return this;
	}

	public SimilarQueryBuilder withShowRankingScoreDetails(boolean showRankingScoreDetails) {
		this.showRankingScoreDetails = showRankingScoreDetails;
		return this;
	}

	public SimilarQueryBuilder withRankingScoreThreshold(@Nullable Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
		return this;
	}

	public SimilarQueryBuilder withRetrieveVectors(boolean retrieveVectors) {
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

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
	}

	@Nullable
	public String getFilter() {
		return filter;
	}

	public boolean isShowRankingScore() {
		return showRankingScore;
	}

	public boolean isShowRankingScoreDetails() {
		return showRankingScoreDetails;
	}

	@Nullable
	public Double getRankingScoreThreshold() {
		return rankingScoreThreshold;
	}

	public boolean isRetrieveVectors() {
		return retrieveVectors;
	}
}
