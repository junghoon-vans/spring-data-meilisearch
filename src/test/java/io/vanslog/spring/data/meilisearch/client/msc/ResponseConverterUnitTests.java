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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.SearchResult;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.SimilarDocumentsResults;

import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;

/**
 * Unit tests for {@link ResponseConverter}.
 *
 * @author Junghoon Ban
 */
class ResponseConverterUnitTests {

	private final ResponseConverter converter = new ResponseConverter();

	@Test
	void shouldConvertPaginatedTotalHitsMetadataToSearchHits() {
		SearchResultPaginated result = new SearchResultPaginated();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hit("143", "Escape Room"));
		hits.add(hit("144", "Carol"));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "totalHits", 10);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getTotalHits()).isEqualTo(10);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.EQUAL_TO);
	}

	@Test
	void shouldKeepTotalHitsOffForEstimatedSearchResults() {
		SearchResult result = new SearchResult();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hit("143", "Escape Room"));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "estimatedTotalHits", 15);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	@Test
	void shouldConvertSimilarDocumentsResultsToSearchHits() {
		SimilarDocumentsResults result = new SimilarDocumentsResults();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hit("143", "Escape Room"));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "estimatedTotalHits", 15);

		SearchHits<SimilarMovie> searchHits = converter.mapResult(result, SimilarMovie.class);

		assertThat(searchHits.getExecutionDuration().toMillis()).isEqualTo(25);
		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
		SearchHit<SimilarMovie> searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getProcessingTimeMs()).isEqualTo(25);
		assertThat(searchHit.getQuery()).isEmpty();
		assertThat(searchHit.getContent().getId()).isEqualTo("143");
		assertThat(searchHit.getContent().getTitle()).isEqualTo("Escape Room");
	}

	@Test
	void shouldKeepTotalHitsOffForFlattenedMultiSearchResults() {
		MultiSearchResult result1 = new MultiSearchResult();
		ReflectionTestUtils.setField(result1, "hits", new ArrayList<>(List.of(hit("143", "Escape Room"))));
		ReflectionTestUtils.setField(result1, "processingTimeMs", 25);

		MultiSearchResult result2 = new MultiSearchResult();
		ReflectionTestUtils.setField(result2, "hits", new ArrayList<>(List.of(hit("144", "Carol"))));
		ReflectionTestUtils.setField(result2, "processingTimeMs", 15);

		Results<MultiSearchResult> results = new Results<>();
		ReflectionTestUtils.setField(results, "results", new MultiSearchResult[] { result1, result2 });

		SearchHits<SimilarMovie> searchHits = converter.mapResults(results, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	@Test
	void shouldKeepTotalHitsOffForFacetSearchResults() {
		FacetSearchable result = new FacetSearchable() {
			@Override
			public ArrayList<HashMap<String, Object>> getFacetHits() {
				return new ArrayList<>(List.of(hit("143", "Escape Room")));
			}

			@Override
			public int getProcessingTimeMs() {
				return 25;
			}

			@Override
			public String getFacetQuery() {
				return "Escape";
			}
		};

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	private static HashMap<String, Object> hit(String id, String title) {
		HashMap<String, Object> hit = new HashMap<>();
		hit.put("id", id);
		hit.put("title", title);
		return hit;
	}

	static class SimilarMovie {

		private String id;
		private String title;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
