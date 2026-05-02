/*
 * Copyright 2023-2026 the original author or authors.
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
import io.vanslog.spring.data.meilisearch.annotations.Embedder;
import io.vanslog.spring.data.meilisearch.annotations.EmbedderParameter;
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

	@Document(indexUid = "movies")
	@Setting(embedders = { //
			@Embedder( //
					name = "openai", //
					source = Embedder.Source.OPEN_AI, //
					apiKey = "sk-test", //
					model = "text-embedding-3-small", //
					documentTemplate = "A movie titled {{doc.title}}", //
					dimensions = 1536, //
					documentTemplateMaxBytes = 400, //
					binaryQuantized = Embedder.TriState.TRUE //
			), //
			@Embedder( //
					name = "rest", //
					source = Embedder.Source.REST, //
					url = "https://example.com/embed", //
					inputField = { "title", "description" }, //
					inputType = Embedder.InputType.TEXT_ARRAY, //
					distributionMean = 0.5, //
					distributionSigma = 0.25, //
					request = { //
							@EmbedderParameter(name = "text", value = "{{text}}") //
					}, //
					response = { //
							@EmbedderParameter(name = "embedding", value = "$.embedding") //
					}, //
					headers = { //
							@EmbedderParameter(name = "Authorization", value = "Bearer token") //
					}, //
					query = "{{q}}" //
			), //
			@Embedder( //
					name = "minimal", //
					binaryQuantized = Embedder.TriState.FALSE //
			) //
	})
	static class EntityWithEmbeddersSettings {
		@Id private String id;
		private String title;
		private String description;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = @Embedder(name = ""))
	static class EntityWithBlankEmbedderName {
		@Id private String id;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = { @Embedder(name = "default"), @Embedder(name = "default") })
	static class EntityWithDuplicateEmbedderNames {
		@Id private String id;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = @Embedder(name = "default", distributionMean = 0.5))
	static class EntityWithPartialEmbedderDistribution {
		@Id private String id;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = @Embedder( //
			name = "default", //
			apiKey = " ", //
			model = " ", //
			documentTemplate = " ", //
			revision = " ", //
			url = " ", //
			query = " " //
	))
	static class EntityWithBlankEmbedderValues {
		@Id private String id;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = @Embedder(name = "default", request = @EmbedderParameter(name = "", value = "{{text}}")))
	static class EntityWithBlankEmbedderParameterName {
		@Id private String id;
	}

	@Document(indexUid = "movies")
	@Setting(embedders = @Embedder(name = "default", headers = { //
			@EmbedderParameter(name = "Authorization", value = "Bearer token"), //
			@EmbedderParameter(name = "Authorization", value = "Bearer other") //
	}))
	static class EntityWithDuplicateEmbedderHeaderNames {
		@Id private String id;
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

		@Test
		void shouldReturnEmbeddersSettings() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithEmbeddersSettings> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithEmbeddersSettings.class));

			// when
			var settings = entity.getDefaultSettings();
			var embedders = settings.getEmbedders();
			var openAi = embedders.get("openai");
			var rest = embedders.get("rest");
			var minimal = embedders.get("minimal");

			// then
			assertThat(embedders).hasSize(3);

			assertThat(openAi.getSource()).isEqualTo(com.meilisearch.sdk.model.EmbedderSource.OPEN_AI);
			assertThat(openAi.getApiKey()).isEqualTo("sk-test");
			assertThat(openAi.getModel()).isEqualTo("text-embedding-3-small");
			assertThat(openAi.getDocumentTemplate()).isEqualTo("A movie titled {{doc.title}}");
			assertThat(openAi.getDimensions()).isEqualTo(1536);
			assertThat(openAi.getDocumentTemplateMaxBytes()).isEqualTo(400);
			assertThat(openAi.getBinaryQuantized()).isTrue();

			assertThat(rest.getSource()).isEqualTo(com.meilisearch.sdk.model.EmbedderSource.REST);
			assertThat(rest.getUrl()).isEqualTo("https://example.com/embed");
			assertThat(rest.getInputField()).containsExactly("title", "description");
			assertThat(rest.getInputType()).isEqualTo(com.meilisearch.sdk.model.EmbedderInputType.TEXT_ARRAY);
			assertThat(rest.getDistribution().getMean()).isEqualTo(0.5);
			assertThat(rest.getDistribution().getSigma()).isEqualTo(0.25);
			assertThat(rest.getRequest()).containsEntry("text", "{{text}}");
			assertThat(rest.getResponse()).containsEntry("embedding", "$.embedding");
			assertThat(rest.getHeaders()).containsEntry("Authorization", "Bearer token");
			assertThat(rest.getQuery()).isEqualTo("{{q}}");

			assertThat(minimal.getSource()).isNull();
			assertThat(minimal.getApiKey()).isNull();
			assertThat(minimal.getModel()).isNull();
			assertThat(minimal.getDocumentTemplate()).isNull();
			assertThat(minimal.getDimensions()).isNull();
			assertThat(minimal.getDistribution()).isNull();
			assertThat(minimal.getRequest()).isNull();
			assertThat(minimal.getResponse()).isNull();
			assertThat(minimal.getDocumentTemplateMaxBytes()).isNull();
			assertThat(minimal.getRevision()).isNull();
			assertThat(minimal.getHeaders()).isNull();
			assertThat(minimal.getBinaryQuantized()).isFalse();
			assertThat(minimal.getUrl()).isNull();
			assertThat(minimal.getInputField()).isNull();
			assertThat(minimal.getInputType()).isNull();
			assertThat(minimal.getQuery()).isNull();
		}

		@Test
		void shouldIgnoreBlankEmbedderValues() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithBlankEmbedderValues> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithBlankEmbedderValues.class));

			// when
			var embedder = entity.getDefaultSettings().getEmbedders().get("default");

			// then
			assertThat(embedder.getApiKey()).isNull();
			assertThat(embedder.getModel()).isNull();
			assertThat(embedder.getDocumentTemplate()).isNull();
			assertThat(embedder.getRevision()).isNull();
			assertThat(embedder.getUrl()).isNull();
			assertThat(embedder.getQuery()).isNull();
		}

		@Test
		void shouldRejectBlankEmbedderName() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithBlankEmbedderName> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithBlankEmbedderName.class));

			// when / then
			assertThatThrownBy(entity::getDefaultSettings).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Embedder name must not be blank");
		}

		@Test
		void shouldRejectDuplicateEmbedderNames() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithDuplicateEmbedderNames> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithDuplicateEmbedderNames.class));

			// when / then
			assertThatThrownBy(entity::getDefaultSettings).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Embedder name must be unique");
		}

		@Test
		void shouldRejectPartialEmbedderDistribution() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithPartialEmbedderDistribution> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithPartialEmbedderDistribution.class));

			// when / then
			assertThatThrownBy(entity::getDefaultSettings).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("distributionMean and distributionSigma must be configured together");
		}

		@Test
		void shouldRejectBlankEmbedderParameterName() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithBlankEmbedderParameterName> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithBlankEmbedderParameterName.class));

			// when / then
			assertThatThrownBy(entity::getDefaultSettings).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Embedder parameter name must not be blank");
		}

		@Test
		void shouldRejectDuplicateEmbedderParameterNames() {
			// given
			SimpleMeilisearchPersistentEntity<EntityWithDuplicateEmbedderHeaderNames> entity = new SimpleMeilisearchPersistentEntity<>(
					TypeInformation.of(EntityWithDuplicateEmbedderHeaderNames.class));

			// when / then
			assertThatThrownBy(entity::getDefaultSettings).isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Embedder parameter name must be unique");
		}
	}
	// endregion
}
