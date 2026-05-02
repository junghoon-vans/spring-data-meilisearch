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

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.SimilarDocumentRequest;

import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;

/**
 * Unit tests for {@link RequestConverter}.
 *
 * @author Junghoon Ban
 */
class RequestConverterUnitTests {

	private static final String[] RETRIEVED_ATTRIBUTES = { "title", "description" };

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
		SimilarQuery query = new SimilarQuery("143", "manual");
		query.setAttributesToRetrieve(RETRIEVED_ATTRIBUTES);
		query.setOffset(10);
		query.setLimit(20);
		query.setFilter("genres = Action");
		query.setShowRankingScore(true);
		query.setShowRankingScoreDetails(true);
		query.setRankingScoreThreshold(0.5);
		query.setRetrieveVectors(true);

		SimilarDocumentRequest request = converter.similarSearchRequest(query);

		assertThat(request) //
				.returns("143", SimilarDocumentRequest::getId) //
				.returns("manual", SimilarDocumentRequest::getEmbedder) //
				.returns(10, SimilarDocumentRequest::getOffset) //
				.returns(20, SimilarDocumentRequest::getLimit) //
				.returns("genres = Action", SimilarDocumentRequest::getFilter) //
				.returns(true, SimilarDocumentRequest::getShowRankingScore) //
				.returns(true, SimilarDocumentRequest::getShowRankingScoreDetails) //
				.returns(0.5, SimilarDocumentRequest::getRankingScoreThreshold) //
				.returns(true, SimilarDocumentRequest::getRetrieveVectors);
		assertThat(request.getAttributesToRetrieve()).containsExactly(RETRIEVED_ATTRIBUTES);
	}
}
