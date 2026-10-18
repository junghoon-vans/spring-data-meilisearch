/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.client.msc;

import java.util.List;

import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;

/**
 * SDK results container retaining response metadata separately from the configured codec's result objects.
 *
 * @author Junghoon Ban
 */
final class MultiSearchResultsWithMetadata extends Results<MultiSearchResult> {

	private final List<MultiSearchResultMetadata> responseMetadata;

	MultiSearchResultsWithMetadata(Results<MultiSearchResult> decoded, List<MultiSearchResultMetadata> responseMetadata) {
		this.results = decoded.getResults();
		this.limit = decoded.getLimit();
		this.offset = decoded.getOffset();
		this.total = decoded.getTotal();
		this.responseMetadata = responseMetadata;
	}

	MultiSearchResultMetadata getResponseMetadata(int queryIndex) {
		return responseMetadata.get(queryIndex);
	}
}
