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
package io.vanslog.spring.data.meilisearch.core;

import com.meilisearch.sdk.model.FacetRating;
import java.util.HashMap;
import org.springframework.lang.Nullable;

public class SearchHit<T> {

	private final T content;
	private final int processingTimeMs;
	private final String query;
	@Nullable private final HashMap<String, FacetRating> facetStats;
	@Nullable private final Object facetDistribution;

	public SearchHit(T content, int processingTimeMs, String query, @Nullable HashMap<String, FacetRating> facetStats,
			@Nullable Object facetDistribution) {
		this.content = content;
		this.processingTimeMs = processingTimeMs;
		this.query = query;
		this.facetStats = facetStats;
		this.facetDistribution = facetDistribution;
	}

	public SearchHit(T content, int processingTimeMs, String query) {
		this(content, processingTimeMs, query, null, null);
	}

	public T getContent() {
		return content;
	}

	public int getProcessingTimeMs() {
		return processingTimeMs;
	}

	public String getQuery() {
		return query;
	}

	@Nullable
	public HashMap<String, FacetRating> getFacetStats() {
		return facetStats;
	}

	@Nullable
	public Object getFacetDistribution() {
		return facetDistribution;
	}
}
