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

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.meilisearch.sdk.model.SimilarDocumentsResults;

import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;

/**
 * Unit tests for {@link ResponseConverter}.
 *
 * @author Junghoon Ban
 */
class ResponseConverterUnitTests {

	private final ResponseConverter converter = new ResponseConverter();

	@Test
	void shouldConvertSimilarDocumentsResultsToSearchHits() {
		SimilarDocumentsResults result = new SimilarDocumentsResults();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		HashMap<String, Object> hit = new HashMap<>();
		hit.put("id", "143");
		hit.put("title", "Escape Room");
		hits.add(hit);

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);

		SearchHits<SimilarMovie> searchHits = converter.mapResult(result, SimilarMovie.class);

		assertThat(searchHits.getExecutionDuration().toMillis()).isEqualTo(25);
		assertThat(searchHits.getSearchHits()).hasSize(1);
		SearchHit<SimilarMovie> searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getProcessingTimeMs()).isEqualTo(25);
		assertThat(searchHit.getQuery()).isEmpty();
		assertThat(searchHit.getContent().getId()).isEqualTo("143");
		assertThat(searchHit.getContent().getTitle()).isEqualTo("Escape Room");
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
