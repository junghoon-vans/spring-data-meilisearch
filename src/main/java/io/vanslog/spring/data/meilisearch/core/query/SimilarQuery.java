package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class SimilarQuery {

	@Nullable private String documentId;
	@Nullable private String embedder;
	@Nullable private String[] attributesToRetrieve;
	private int offset;
	private int limit;
	@Nullable private String filter;
	private boolean showRankingScore;
	private boolean showRankingScoreDetails;
	@Nullable private Double rankingScoreThreshold;
	private boolean retrieveVectors;

	public SimilarQuery(String documentId, String embedder) {
		this(builder().withDocumentId(documentId).withEmbedder(embedder));
	}

	public SimilarQuery(SimilarQueryBuilder builder) {
		Assert.notNull(builder.getDocumentId(), "documentId must not be null");
		Assert.notNull(builder.getEmbedder(), "embedder must not be null");

		this.documentId = builder.getDocumentId();
		this.embedder = builder.getEmbedder();
		this.attributesToRetrieve = builder.getAttributesToRetrieve();
		this.offset = builder.getOffset();
		this.limit = builder.getLimit();
		this.filter = builder.getFilter();
		this.showRankingScore = builder.isShowRankingScore();
		this.showRankingScoreDetails = builder.isShowRankingScoreDetails();
		this.rankingScoreThreshold = builder.getRankingScoreThreshold();
		this.retrieveVectors = builder.isRetrieveVectors();
	}

	public static SimilarQueryBuilder builder() {
		return new SimilarQueryBuilder();
	}

	@Nullable
	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(@Nullable String documentId) {
		this.documentId = documentId;
	}

	@Nullable
	public String getEmbedder() {
		return embedder;
	}

	public void setEmbedder(@Nullable String embedder) {
		this.embedder = embedder;
	}

	@Nullable
	public String[] getAttributesToRetrieve() {
		return attributesToRetrieve;
	}

	public void setAttributesToRetrieve(@Nullable String[] attributesToRetrieve) {
		this.attributesToRetrieve = attributesToRetrieve;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Nullable
	public String getFilter() {
		return filter;
	}

	public void setFilter(@Nullable String filter) {
		this.filter = filter;
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

	public void setRankingScoreThreshold(@Nullable Double rankingScoreThreshold) {
		this.rankingScoreThreshold = rankingScoreThreshold;
	}

	public boolean isRetrieveVectors() {
		return retrieveVectors;
	}

	public void setRetrieveVectors(boolean retrieveVectors) {
		this.retrieveVectors = retrieveVectors;
	}
}
