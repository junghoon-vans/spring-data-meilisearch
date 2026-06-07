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

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetRating;
import com.meilisearch.sdk.model.Searchable;

/**
 * Raw search response adapter used after reading JSON responses from Meilisearch.
 *
 * @author Junghoon Ban
 */
final class MeilisearchSearchResult implements Searchable {

	private final ArrayList<HashMap<String, Object>> hits;
	@Nullable private final Object facetDistribution;
	@Nullable private final HashMap<String, FacetRating> facetStats;
	private final int processingTimeMs;
	private final String query;
	@Nullable private final Integer totalHits;

	private MeilisearchSearchResult(ArrayList<HashMap<String, Object>> hits, @Nullable Object facetDistribution,
			@Nullable HashMap<String, FacetRating> facetStats, int processingTimeMs, String query,
			@Nullable Integer totalHits) {

		this.hits = hits;
		this.facetDistribution = facetDistribution;
		this.facetStats = facetStats;
		this.processingTimeMs = processingTimeMs;
		this.query = query;
		this.totalHits = totalHits;
	}

	static MeilisearchSearchResult from(JsonNode source, ObjectMapper objectMapper) {

		ArrayList<HashMap<String, Object>> hits = readHits(source, objectMapper);
		Object facetDistribution = source.has("facetDistribution")
				? objectMapper.convertValue(source.get("facetDistribution"), Object.class) : null;
		HashMap<String, FacetRating> facetStats = readFacetStats(source, objectMapper);
		Integer totalHits = source.has("totalHits") ? source.get("totalHits").asInt() : null;

		return new MeilisearchSearchResult(hits, facetDistribution, facetStats,
				source.path("processingTimeMs").asInt(), source.path("query").asText(), totalHits);
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<HashMap<String, Object>> readHits(JsonNode source, ObjectMapper objectMapper) {
		return objectMapper.convertValue(source.path("hits"), ArrayList.class);
	}

	@Nullable
	private static HashMap<String, FacetRating> readFacetStats(JsonNode source, ObjectMapper objectMapper) {

		if (!source.has("facetStats")) {
			return null;
		}

		JavaType facetStatsType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class,
				FacetRating.class);
		return objectMapper.convertValue(source.get("facetStats"), facetStatsType);
	}

	@Override
	public ArrayList<HashMap<String, Object>> getHits() {
		return hits;
	}

	@Override
	@Nullable
	public Object getFacetDistribution() {
		return facetDistribution;
	}

	@Override
	@Nullable
	public HashMap<String, FacetRating> getFacetStats() {
		return facetStats;
	}

	@Override
	public int getProcessingTimeMs() {
		return processingTimeMs;
	}

	@Override
	public String getQuery() {
		return query;
	}

	boolean hasTotalHits() {
		return totalHits != null;
	}

	int getTotalHits() {
		return totalHits != null ? totalHits : hits.size();
	}
}
