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
package io.vanslog.spring.data.meilisearch.client.msc;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetSearchable;

/**
 * Raw facet search response adapter used after reading JSON responses from Meilisearch.
 *
 * @author Junghoon Ban
 */
final class MeilisearchFacetSearchResult implements FacetSearchable {

	private final ArrayList<HashMap<String, Object>> facetHits;
	private final int processingTimeMs;
	private final String facetQuery;

	private MeilisearchFacetSearchResult(ArrayList<HashMap<String, Object>> facetHits, int processingTimeMs,
			String facetQuery) {

		this.facetHits = facetHits;
		this.processingTimeMs = processingTimeMs;
		this.facetQuery = facetQuery;
	}

	static MeilisearchFacetSearchResult from(JsonNode source, ObjectMapper objectMapper) {
		return new MeilisearchFacetSearchResult(readFacetHits(source, objectMapper),
				source.path("processingTimeMs").asInt(), source.path("facetQuery").asText());
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<HashMap<String, Object>> readFacetHits(JsonNode source, ObjectMapper objectMapper) {
		return objectMapper.convertValue(source.path("facetHits"), ArrayList.class);
	}

	@Override
	public ArrayList<HashMap<String, Object>> getFacetHits() {
		return facetHits;
	}

	@Override
	public int getProcessingTimeMs() {
		return processingTimeMs;
	}

	@Override
	public String getFacetQuery() {
		return facetQuery;
	}
}
