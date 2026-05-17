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

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.model.Settings;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderInputType;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSource;

/**
 * Unit tests for {@link IndexSettingsResponseConverter}.
 *
 * @author Junghoon Ban
 */
class IndexSettingsResponseConverterUnitTests {

	private final IndexSettingsResponseConverter converter = new IndexSettingsResponseConverter();

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
