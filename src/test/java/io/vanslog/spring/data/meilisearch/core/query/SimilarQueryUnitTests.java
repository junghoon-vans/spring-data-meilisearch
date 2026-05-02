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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimilarQuery}.
 *
 * @author Junghoon Ban
 */
class SimilarQueryUnitTests {

	@Test
	void shouldRequireDocumentId() {
		assertThatThrownBy(() -> SimilarQuery.builder().withEmbedder("manual").build()) //
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRequireEmbedder() {
		assertThatThrownBy(() -> SimilarQuery.builder().withDocumentId("143").build()) //
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldLeaveOptionalValuesUnsetByDefault() {
		SimilarQuery query = new SimilarQuery("143", "manual");

		assertThat(query.getDocumentId()).isEqualTo("143");
		assertThat(query.getEmbedder()).isEqualTo("manual");
		assertThat(query.getAttributesToRetrieve()).isNull();
		assertThat(query.getOffset()).isNull();
		assertThat(query.getLimit()).isNull();
		assertThat(query.getFilter()).isNull();
		assertThat(query.getShowRankingScore()).isNull();
		assertThat(query.getShowRankingScoreDetails()).isNull();
		assertThat(query.getRankingScoreThreshold()).isNull();
		assertThat(query.getRetrieveVectors()).isNull();
	}

	@Test
	void shouldBuildQueryWithAllSupportedOptions() {
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

		assertThat(query.getDocumentId()).isEqualTo("143");
		assertThat(query.getEmbedder()).isEqualTo("manual");
		assertThat(query.getAttributesToRetrieve()).containsExactly("title", "description");
		assertThat(query.getOffset()).isEqualTo(10);
		assertThat(query.getLimit()).isEqualTo(20);
		assertThat(query.getFilter()).isEqualTo("genres = Action");
		assertThat(query.getShowRankingScore()).isTrue();
		assertThat(query.getShowRankingScoreDetails()).isTrue();
		assertThat(query.getRankingScoreThreshold()).isEqualTo(0.5);
		assertThat(query.getRetrieveVectors()).isTrue();
	}
}
