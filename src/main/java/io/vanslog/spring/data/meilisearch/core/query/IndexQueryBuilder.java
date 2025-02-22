package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;

public class IndexQueryBuilder extends BaseQueryBuilder<IndexQuery, IndexQueryBuilder> {

	@Nullable protected String indexUid;
	@Nullable protected FederationOptions federationOptions;

	public IndexQueryBuilder withIndexUid(String indexUid) {
		this.indexUid = indexUid;
		return this;
	}

	public IndexQueryBuilder withFederationOptions(FederationOptions federationOptions) {
		this.federationOptions = federationOptions;
		return this;
	}

	@Nullable
	public String getIndexUid() {
		return indexUid;
	}

	@Nullable
	public FederationOptions getFederationOptions() {
		return federationOptions;
	}

	@Override
	public IndexQuery build() {
		return new IndexQuery(this);
	}
}
