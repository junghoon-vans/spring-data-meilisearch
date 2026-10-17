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
package io.vanslog.spring.data.meilisearch.core;

import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * Optional presentation and ranking data returned for an individual search hit.
 *
 * @author Junghoon Ban
 */
public final class SearchHitMetadata {

	@Nullable private final Map<String, Object> formattedFields;
	@Nullable private final Map<String, Object> matchesPosition;
	@Nullable private final Double rankingScore;
	@Nullable private final Map<String, Object> rankingScoreDetails;

	public SearchHitMetadata(@Nullable Map<String, Object> formattedFields, @Nullable Map<String, Object> matchesPosition,
			@Nullable Double rankingScore, @Nullable Map<String, Object> rankingScoreDetails) {

		this.formattedFields = immutableCopy(formattedFields);
		this.matchesPosition = immutableCopy(matchesPosition);
		this.rankingScore = rankingScore;
		this.rankingScoreDetails = immutableCopy(rankingScoreDetails);
	}

	/**
	 * @return formatted hit fields, or {@literal null} when not requested or returned
	 */
	@Nullable
	public Map<String, Object> getFormattedFields() {
		return formattedFields;
	}

	/**
	 * @return match positions by field, or {@literal null} when not requested or returned
	 */
	@Nullable
	public Map<String, Object> getMatchesPosition() {
		return matchesPosition;
	}

	/**
	 * @return the ranking score, or {@literal null} when not requested or returned
	 */
	@Nullable
	public Double getRankingScore() {
		return rankingScore;
	}

	/**
	 * @return ranking score details, or {@literal null} when not requested or returned
	 */
	@Nullable
	public Map<String, Object> getRankingScoreDetails() {
		return rankingScoreDetails;
	}

	@Nullable
	private static Map<String, Object> immutableCopy(@Nullable Map<String, Object> source) {
		return source == null ? null : SearchMetadataSupport.immutableMap(source);
	}
}
