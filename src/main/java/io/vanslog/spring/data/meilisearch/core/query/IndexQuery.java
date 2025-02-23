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

public class IndexQuery extends BaseQuery {

	@Nullable protected String indexUid;
	@Nullable protected FederationOptions federationOptions;

	public IndexQuery(String q) {
		super(q);
	}

	public IndexQuery(IndexQueryBuilder builder) {
		super(builder);
		this.indexUid = builder.getIndexUid();
		this.federationOptions = builder.getFederationOptions();
	}

	public static IndexQueryBuilder builder() {
		return new IndexQueryBuilder();
	}

	@Nullable
	public String getIndexUid() {
		return indexUid;
	}

	public void setIndexUid(@Nullable String indexUid) {
		this.indexUid = indexUid;
	}

	@Nullable
	public FederationOptions getFederationOptions() {
		return federationOptions;
	}

	public void setFederationOptions(@Nullable FederationOptions federationOptions) {
		this.federationOptions = federationOptions;
	}
}
