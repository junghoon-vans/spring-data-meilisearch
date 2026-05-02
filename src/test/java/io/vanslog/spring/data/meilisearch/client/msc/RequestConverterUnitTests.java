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
package io.vanslog.spring.data.meilisearch.client.msc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.SimilarDocumentRequest;

import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;

/**
 * Unit tests for {@link RequestConverter}.
 *
 * @author Junghoon Ban
 */
class RequestConverterUnitTests {

	private final RequestConverter converter = new RequestConverter();

	@Test
	void shouldConvertMinimalSimilarQueryWithoutDefaultingOptionalValues() {
		SimilarQuery query = new SimilarQuery("143", "manual");

		SimilarDocumentRequest request = converter.similarSearchRequest(query);

		assertThat(request.getId()).isEqualTo("143");
		assertThat(request.getEmbedder()).isEqualTo("manual");
		assertThat(request.getAttributesToRetrieve()).isNull();
		assertThat(request.getOffset()).isNull();
		assertThat(request.getLimit()).isNull();
		assertThat(request.getFilter()).isNull();
		assertThat(request.getShowRankingScore()).isNull();
		assertThat(request.getShowRankingScoreDetails()).isNull();
		assertThat(request.getRankingScoreThreshold()).isNull();
		assertThat(request.getRetrieveVectors()).isNull();
	}

	@Test
	void shouldConvertAllSimilarQueryOptions() {
		SimilarQuery query = SimilarQuery.builder() //
				.withDocumentId("143") //
				.withEmbedder("manual") //
				.withAttributesToRetrieve(new String[] { "title", "description" }) //
				.withOffset(10) //
				.withLimit(20) //
				.withFilter("genres = Action") //
				.withShowRankingScore(true) //
				.withShowRankingScoreDetails(true) //
				.withRankingScoreThreshold(0.5) //
				.withRetrieveVectors(true) //
				.build();

		SimilarDocumentRequest request = converter.similarSearchRequest(query);

		assertThat(request.getId()).isEqualTo("143");
		assertThat(request.getEmbedder()).isEqualTo("manual");
		assertThat(request.getAttributesToRetrieve()).containsExactly("title", "description");
		assertThat(request.getOffset()).isEqualTo(10);
		assertThat(request.getLimit()).isEqualTo(20);
		assertThat(request.getFilter()).isEqualTo("genres = Action");
		assertThat(request.getShowRankingScore()).isTrue();
		assertThat(request.getShowRankingScoreDetails()).isTrue();
		assertThat(request.getRankingScoreThreshold()).isEqualTo(0.5);
		assertThat(request.getRetrieveVectors()).isTrue();
	}
}
