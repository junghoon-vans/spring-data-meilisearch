/*
 * Copyright 2023-2025 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.core.mapping;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Faceting;
import io.vanslog.spring.data.meilisearch.annotations.LocalizedAttribute;
import io.vanslog.spring.data.meilisearch.annotations.MinWordSizeForTypos;
import io.vanslog.spring.data.meilisearch.annotations.Pagination;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.annotations.Synonym;
import io.vanslog.spring.data.meilisearch.annotations.TypoTolerance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentEntity}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentEntityUnitTests {

	// region entities
	@Document(indexUid = "")
	static class EntityWithEmptyIndexUid {}

	static class EntityWithoutExplicitIndexUid {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Document(indexUid = "custom-index")
	static class EntityWithExplicitIndexUid {
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	@Document(indexUid = "products")
	@Setting( //
			searchableAttributes = { "description", "brand", "color" }, //
			displayedAttributes = { "description", "brand", "color", "productId" }, //
			sortableAttributes = { "productId" }, //
			rankingRules = { "typo", "words", "proximity", "attribute", "sort", "exactness" }, //
			distinctAttribute = "productId", //
			filterableAttributes = { "brand", "color", "price" }, //
			synonyms = { //
					@Synonym(word = "phone", synonyms = { "mobile", "cellphone" }), //
					@Synonym(word = "laptop", synonyms = { "notebook" }) //
			}, //
			dictionary = { "netflix", "spotify" }, //
			stopWords = { "a", "an", "the" }, //
			separatorTokens = { "-", "_", "@" }, //
			nonSeparatorTokens = { ".", "#" }, //
			proximityPrecision = "byWord", //
			searchCutoffMs = 50, //
			localizedAttributes = { //
					@LocalizedAttribute( //
							attributePatterns = { "description", "brand", "color" }, //
							locales = { "en", "fr" } //
					) //
			})
	static class EntityWithoutComplexSettings {
		@Id private String id;
		private String description;
		private String brand;
		private String color;
		private String productId;
		private double price;
	}

	@Document(indexUid = "products")
	@TypoTolerance( //
			enabled = true, minWordSizeForTypos = @MinWordSizeForTypos(oneTypo = 5, twoTypos = 9), //
			disableOnWords = { "skype", "zoom" }, //
			disableOnAttributes = { "serial_number" } //
	) //
	@Faceting(maxValuesPerFacet = 200) //
	@Pagination(maxTotalHits = 2000) //
	static class EntityWithComplexSettings {
		@Id private String id;
		private String description;
		private String brand;
		private String color;
		private String productId;
		private double price;
	}

	@Nested
	@DisplayName("index uid")
	class IndexUidTests {
		@Test
		void shouldThrowExceptionWhenIndexUidIsBlank() {
			TypeInformation<EntityWithEmptyIndexUid> entityTypeInfo = TypeInformation.of(EntityWithEmptyIndexUid.class);
			assertThatThrownBy(() -> new SimpleMeilisearchPersistentEntity<>(entityTypeInfo))
					.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void shouldUseClassNameAsIndexUidWhenNotSpecified() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithoutExplicitIndexUid> entity = //
					new SimpleMeilisearchPersistentEntity<>(TypeInformation.of(EntityWithoutExplicitIndexUid.class));

			// when
			String indexUid = entity.getIndexUid();

			// then
			assertThat(indexUid).isEqualTo("EntityWithoutExplicitIndexUid");
		}

		@Test
		void shouldUseSpecifiedIndexUid() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithExplicitIndexUid> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithExplicitIndexUid.class));

			// when
			String indexUid = entity.getIndexUid();

			// then
			assertThat(indexUid).isEqualTo("custom-index");
		}
	}

	@Nested
	@DisplayName("index settings")
	class SettingsTests {

		@Test
		void shouldReturnSimpleSettings() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithoutComplexSettings> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithoutComplexSettings.class));

			// when
			var settings = entity.getDefaultSettings();
			var typoTolerance = settings.getTypoTolerance();
			var faceting = settings.getFaceting();
			var pagination = settings.getPagination();

			// then
			assertThat(settings).isNotNull();

			// Check basic settings
			assertThat(settings.getSearchableAttributes()).containsExactly("description", "brand", "color");
			assertThat(settings.getDisplayedAttributes()).containsExactly("description", "brand", "color", "productId");
			assertThat(settings.getSortableAttributes()).containsExactly("productId");
			assertThat(settings.getRankingRules()).containsExactly("typo", "words", "proximity", "attribute", "sort",
					"exactness");
			assertThat(settings.getDistinctAttribute()).isEqualTo("productId");
			assertThat(settings.getFilterableAttributes()).containsExactly("brand", "color", "price");

			// Check synonyms
			assertThat(settings.getSynonyms()).hasSize(2);
			assertThat(settings.getSynonyms().get("phone")).containsExactly("mobile", "cellphone");
			assertThat(settings.getSynonyms().get("laptop")).containsExactly("notebook");

			// Check dictionary and stop words
			assertThat(settings.getDictionary()).containsExactly("netflix", "spotify");
			assertThat(settings.getStopWords()).containsExactly("a", "an", "the");

			// Check tokenization settings
			assertThat(settings.getSeparatorTokens()).containsExactly("-", "_", "@");
			assertThat(settings.getNonSeparatorTokens()).containsExactly(".", "#");

			// Check performance settings
			assertThat(settings.getProximityPrecision()).isEqualTo("byWord");
			assertThat(settings.getSearchCutoffMs()).isEqualTo(50);

			// Check localized attributes
			var localizedAttributes = settings.getLocalizedAttributes();
			assertThat(localizedAttributes).hasSize(1);
			assertThat(localizedAttributes[0].getAttributePatterns()).containsExactly("description", "brand", "color");
			assertThat(localizedAttributes[0].getLocales()).containsExactly("en", "fr");

			// Check complex settings
			assertThat(typoTolerance).isNull();
			assertThat(faceting).isNull();
			assertThat(pagination).isNull();
		}

		@Test
		void shouldReturnComplexSettings() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithComplexSettings> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithComplexSettings.class));

			// when
			var settings = entity.getDefaultSettings();
			var typoTolerance = settings.getTypoTolerance();
			var faceting = settings.getFaceting();
			var pagination = settings.getPagination();

			// then
			assertThat(settings).isNotNull();

			// Check typo tolerance settings
			assertThat(typoTolerance).isNotNull();
			assertThat(typoTolerance.isEnabled()).isTrue();
			assertThat(typoTolerance.getMinWordSizeForTypos()).containsEntry("oneTypo", 5);
			assertThat(typoTolerance.getMinWordSizeForTypos()).containsEntry("twoTypos", 9);
			assertThat(typoTolerance.getDisableOnWords()).containsExactly("skype", "zoom");
			assertThat(typoTolerance.getDisableOnAttributes()).containsExactly("serial_number");

			// Check faceting settings
			assertThat(faceting).isNotNull();
			assertThat(faceting.getMaxValuesPerFacet()).isEqualTo(200);

			// Check pagination settings
			assertThat(pagination).isNotNull();
			assertThat(pagination.getMaxTotalHits()).isEqualTo(2000);
		}
	}
	// endregion
}
