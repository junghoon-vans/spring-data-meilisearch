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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.model.Settings;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderDistributionSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderInputType;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSource;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.FacetingSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.LocalizedAttributeSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.PaginationSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.TypoToleranceSettings;

/**
 * Unit tests for {@link IndexRequestConverter}.
 *
 * @author Junghoon Ban
 */
class IndexRequestConverterUnitTests {

	private final IndexRequestConverter converter = new IndexRequestConverter();

	@Test
	void shouldConvertIndexSettingsToSdkSettings() {

		MeilisearchIndexSettings settings = MeilisearchIndexSettings.builder() //
				.withSearchableAttributes(List.of("title", "description")) //
				.withDisplayedAttributes(List.of("id", "title")) //
				.withSortableAttributes(List.of("title")) //
				.withRankingRules(List.of("words", "typo")) //
				.withDistinctAttribute("title") //
				.withFilterableAttributes(List.of("genres")) //
				.withSynonyms(Map.of("hero", List.of("superhero"))) //
				.withStopWords(List.of("the")) //
				.withDictionary(List.of("meilisearch")) //
				.withPagination(new PaginationSettings(1500)) //
				.withFaceting(new FacetingSettings(75)) //
				.withTypoTolerance(new TypoToleranceSettings(true, 5, 9, List.of("skype"),
						List.of("serial_number"))) //
				.withProximityPrecision("byWord") //
				.withSearchCutoffMs(50) //
				.withSeparatorTokens(List.of("-")) //
				.withNonSeparatorTokens(List.of(".")) //
				.withLocalizedAttributes(List.of(new LocalizedAttributeSettings(List.of("title_*"), List.of("eng")))) //
				.withEmbedders(Map.of("default", EmbedderSettings.builder() //
						.withSource(EmbedderSource.REST) //
						.withUrl("https://example.com/embed") //
						.withInputField(List.of("title")) //
						.withInputType(EmbedderInputType.TEXT_ARRAY) //
						.withDistribution(new EmbedderDistributionSettings(0.5, 0.25)) //
						.withRequest(Map.of("text", "{{text}}")) //
						.withResponse(Map.of("embedding", "$.embedding")) //
						.withHeaders(Map.of("Authorization", "Bearer token")) //
						.withBinaryQuantized(true) //
						.build())) //
				.build();

		Settings sdkSettings = converter.toSettings(settings);

		assertThat(sdkSettings.getSearchableAttributes()).containsExactly("title", "description");
		assertThat(sdkSettings.getDisplayedAttributes()).containsExactly("id", "title");
		assertThat(sdkSettings.getSortableAttributes()).containsExactly("title");
		assertThat(sdkSettings.getRankingRules()).containsExactly("words", "typo");
		assertThat(sdkSettings.getDistinctAttribute()).isEqualTo("title");
		assertThat(sdkSettings.getFilterableAttributes()).containsExactly("genres");
		assertThat(sdkSettings.getSynonyms()).containsKey("hero");
		assertThat(sdkSettings.getSynonyms().get("hero")).containsExactly("superhero");
		assertThat(sdkSettings.getStopWords()).containsExactly("the");
		assertThat(sdkSettings.getDictionary()).containsExactly("meilisearch");
		assertThat(sdkSettings.getPagination().getMaxTotalHits()).isEqualTo(1500);
		assertThat(sdkSettings.getFaceting().getMaxValuesPerFacet()).isEqualTo(75);
		assertThat(sdkSettings.getTypoTolerance().isEnabled()).isTrue();
		assertThat(sdkSettings.getTypoTolerance().getMinWordSizeForTypos()).containsEntry("oneTypo", 5)
				.containsEntry("twoTypos", 9);
		assertThat(sdkSettings.getTypoTolerance().getDisableOnWords()).containsExactly("skype");
		assertThat(sdkSettings.getTypoTolerance().getDisableOnAttributes()).containsExactly("serial_number");
		assertThat(sdkSettings.getProximityPrecision()).isEqualTo("byWord");
		assertThat(sdkSettings.getSearchCutoffMs()).isEqualTo(50);
		assertThat(sdkSettings.getSeparatorTokens()).containsExactly("-");
		assertThat(sdkSettings.getNonSeparatorTokens()).containsExactly(".");
		assertThat(sdkSettings.getLocalizedAttributes()[0].getAttributePatterns()).containsExactly("title_*");
		assertThat(sdkSettings.getLocalizedAttributes()[0].getLocales()).containsExactly("eng");
		assertThat(sdkSettings.getEmbedders().get("default").getSource())
				.isEqualTo(com.meilisearch.sdk.model.EmbedderSource.REST);
		assertThat(sdkSettings.getEmbedders().get("default").getUrl()).isEqualTo("https://example.com/embed");
		assertThat(sdkSettings.getEmbedders().get("default").getInputField()).containsExactly("title");
		assertThat(sdkSettings.getEmbedders().get("default").getInputType())
				.isEqualTo(com.meilisearch.sdk.model.EmbedderInputType.TEXT_ARRAY);
		assertThat(sdkSettings.getEmbedders().get("default").getDistribution().getMean()).isEqualTo(0.5);
		assertThat(sdkSettings.getEmbedders().get("default").getDistribution().getSigma()).isEqualTo(0.25);
		assertThat(sdkSettings.getEmbedders().get("default").getRequest()).containsEntry("text", "{{text}}");
		assertThat(sdkSettings.getEmbedders().get("default").getResponse()).containsEntry("embedding", "$.embedding");
		assertThat(sdkSettings.getEmbedders().get("default").getHeaders())
				.containsEntry("Authorization", "Bearer token");
		assertThat(sdkSettings.getEmbedders().get("default").getBinaryQuantized()).isTrue();
	}

	@Test
	void shouldConvertSdkSettingsToIndexSettings() {

		Settings sdkSettings = new Settings();
		sdkSettings.setSearchableAttributes(new String[] { "title", "description" });
		sdkSettings.setDisplayedAttributes(new String[] { "id", "title" });
		sdkSettings.setFilterableAttributes(new String[] { "genres" });
		HashMap<String, String[]> synonyms = new HashMap<>();
		synonyms.put("hero", new String[] { "superhero" });
		sdkSettings.setSynonyms(synonyms);
		sdkSettings.setPagination(new com.meilisearch.sdk.model.Pagination(1500));
		com.meilisearch.sdk.model.Faceting faceting = new com.meilisearch.sdk.model.Faceting();
		faceting.setMaxValuesPerFacet(75);
		sdkSettings.setFaceting(faceting);
		com.meilisearch.sdk.model.TypoTolerance typoTolerance = new com.meilisearch.sdk.model.TypoTolerance();
		typoTolerance.setEnabled(true);
		HashMap<String, Integer> minWordSizeForTypos = new HashMap<>();
		minWordSizeForTypos.put("oneTypo", 5);
		minWordSizeForTypos.put("twoTypos", 9);
		typoTolerance.setMinWordSizeForTypos(minWordSizeForTypos);
		sdkSettings.setTypoTolerance(typoTolerance);
		sdkSettings.setLocalizedAttributes(new com.meilisearch.sdk.model.LocalizedAttribute[] {
				new com.meilisearch.sdk.model.LocalizedAttribute(new String[] { "title_*" }, new String[] { "eng" }) });
		HashMap<String, com.meilisearch.sdk.model.Embedder> embedders = new HashMap<>();
		embedders.put("default", new com.meilisearch.sdk.model.Embedder()
				.setSource(com.meilisearch.sdk.model.EmbedderSource.REST).setUrl("https://example.com/embed")
				.setInputType(com.meilisearch.sdk.model.EmbedderInputType.TEXT_ARRAY));
		sdkSettings.setEmbedders(embedders);

		MeilisearchIndexSettings settings = converter.fromSettings(sdkSettings);

		assertThat(settings.getSearchableAttributes()).containsExactly("title", "description");
		assertThat(settings.getDisplayedAttributes()).containsExactly("id", "title");
		assertThat(settings.getFilterableAttributes()).containsExactly("genres");
		assertThat(settings.getSynonyms()).containsEntry("hero", List.of("superhero"));
		assertThat(settings.getPagination().getMaxTotalHits()).isEqualTo(1500);
		assertThat(settings.getFaceting().getMaxValuesPerFacet()).isEqualTo(75);
		assertThat(settings.getTypoTolerance().getOneTypo()).isEqualTo(5);
		assertThat(settings.getTypoTolerance().getTwoTypos()).isEqualTo(9);
		assertThat(settings.getLocalizedAttributes().get(0).getAttributePatterns()).containsExactly("title_*");
		assertThat(settings.getLocalizedAttributes().get(0).getLocales()).containsExactly("eng");
		assertThat(settings.getEmbedders().get("default").getSource()).isEqualTo(EmbedderSource.REST);
		assertThat(settings.getEmbedders().get("default").getUrl()).isEqualTo("https://example.com/embed");
		assertThat(settings.getEmbedders().get("default").getInputType()).isEqualTo(EmbedderInputType.TEXT_ARRAY);
	}
}
