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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meilisearch.sdk.model.IndexesQuery;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TypoTolerance;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexQuery;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderDistributionSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.FacetingSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.LocalizedAttributeSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.PaginationSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.TypoToleranceSettings;
import org.springframework.util.Assert;

/**
 * Converts Spring Data Meilisearch index lifecycle requests into SDK requests.
 *
 * @author Junghoon Ban
 */
class IndexRequestConverter {

	IndexesQuery indexesQuery(MeilisearchIndexQuery query) {

		return new IndexesQuery() //
				.setOffset(query.getOffset()) //
				.setLimit(query.getLimit());
	}

	Settings toSettings(MeilisearchIndexSettings source) {

		Settings settings = new Settings();

		if (source.getSearchableAttributes() != null) {
			settings.setSearchableAttributes(toArray(source.getSearchableAttributes()));
		}
		if (source.getDisplayedAttributes() != null) {
			settings.setDisplayedAttributes(toArray(source.getDisplayedAttributes()));
		}
		if (source.getSortableAttributes() != null) {
			settings.setSortableAttributes(toArray(source.getSortableAttributes()));
		}
		if (source.getRankingRules() != null) {
			settings.setRankingRules(toArray(source.getRankingRules()));
		}
		if (source.getDistinctAttribute() != null) {
			settings.setDistinctAttribute(source.getDistinctAttribute());
		}
		if (source.getFilterableAttributes() != null) {
			settings.setFilterableAttributes(toArray(source.getFilterableAttributes()));
		}
		if (source.getSynonyms() != null) {
			settings.setSynonyms(toSynonymMap(source.getSynonyms()));
		}
		if (source.getStopWords() != null) {
			settings.setStopWords(toArray(source.getStopWords()));
		}
		if (source.getDictionary() != null) {
			settings.setDictionary(toArray(source.getDictionary()));
		}
		if (source.getPagination() != null) {
			settings.setPagination(toPagination(source.getPagination()));
		}
		if (source.getFaceting() != null) {
			settings.setFaceting(toFaceting(source.getFaceting()));
		}
		if (source.getTypoTolerance() != null) {
			settings.setTypoTolerance(toTypoTolerance(source.getTypoTolerance()));
		}
		if (source.getProximityPrecision() != null) {
			settings.setProximityPrecision(source.getProximityPrecision());
		}
		if (source.getSearchCutoffMs() != null) {
			settings.setSearchCutoffMs(source.getSearchCutoffMs());
		}
		if (source.getSeparatorTokens() != null) {
			settings.setSeparatorTokens(toArray(source.getSeparatorTokens()));
		}
		if (source.getNonSeparatorTokens() != null) {
			settings.setNonSeparatorTokens(toArray(source.getNonSeparatorTokens()));
		}
		if (source.getLocalizedAttributes() != null) {
			settings.setLocalizedAttributes(toLocalizedAttributes(source.getLocalizedAttributes()));
		}
		if (source.getEmbedders() != null) {
			settings.setEmbedders(toEmbedders(source.getEmbedders()));
		}

		return settings;
	}

	private static String[] toArray(List<String> source) {
		return source.toArray(String[]::new);
	}

	private static HashMap<String, String[]> toSynonymMap(Map<String, List<String>> source) {

		HashMap<String, String[]> synonyms = new HashMap<>();
		source.forEach((word, values) -> synonyms.put(word, toArray(values)));
		return synonyms;
	}

	private static com.meilisearch.sdk.model.Pagination toPagination(PaginationSettings source) {

		com.meilisearch.sdk.model.Pagination pagination = new com.meilisearch.sdk.model.Pagination();
		pagination.setMaxTotalHits(source.getMaxTotalHits());
		return pagination;
	}

	private static com.meilisearch.sdk.model.Faceting toFaceting(FacetingSettings source) {

		com.meilisearch.sdk.model.Faceting faceting = new com.meilisearch.sdk.model.Faceting();
		faceting.setMaxValuesPerFacet(source.getMaxValuesPerFacet());
		return faceting;
	}

	private static TypoTolerance toTypoTolerance(TypoToleranceSettings source) {

		TypoTolerance typoTolerance = new TypoTolerance();
		typoTolerance.setEnabled(source.isEnabled());
		HashMap<String, Integer> minWordSizeForTypos = new HashMap<>();
		if (source.getOneTypo() != null) {
			minWordSizeForTypos.put("oneTypo", source.getOneTypo());
		}
		if (source.getTwoTypos() != null) {
			minWordSizeForTypos.put("twoTypos", source.getTwoTypos());
		}
		if (!minWordSizeForTypos.isEmpty()) {
			typoTolerance.setMinWordSizeForTypos(minWordSizeForTypos);
		}
		if (source.getDisableOnWords() != null) {
			typoTolerance.setDisableOnWords(toArray(source.getDisableOnWords()));
		}
		if (source.getDisableOnAttributes() != null) {
			typoTolerance.setDisableOnAttributes(toArray(source.getDisableOnAttributes()));
		}
		return typoTolerance;
	}

	private static com.meilisearch.sdk.model.LocalizedAttribute[] toLocalizedAttributes(
			List<LocalizedAttributeSettings> source) {

		return source.stream().map(it -> new com.meilisearch.sdk.model.LocalizedAttribute(
				toArray(it.getAttributePatterns()), toArray(it.getLocales())))
				.toArray(com.meilisearch.sdk.model.LocalizedAttribute[]::new);
	}

	private static HashMap<String, com.meilisearch.sdk.model.Embedder> toEmbedders(
			Map<String, EmbedderSettings> source) {

		HashMap<String, com.meilisearch.sdk.model.Embedder> embedders = new HashMap<>();
		source.forEach((name, embedder) -> {
			Assert.hasText(name, "Embedder name must not be blank");
			embedders.put(name, toEmbedder(embedder));
		});
		return embedders;
	}

	private static com.meilisearch.sdk.model.Embedder toEmbedder(EmbedderSettings source) {

		com.meilisearch.sdk.model.Embedder embedder = new com.meilisearch.sdk.model.Embedder();
		if (source.getSource() != null) {
			embedder.setSource(com.meilisearch.sdk.model.EmbedderSource.valueOf(source.getSource().name()));
		}
		if (source.getApiKey() != null) {
			embedder.setApiKey(source.getApiKey());
		}
		if (source.getModel() != null) {
			embedder.setModel(source.getModel());
		}
		if (source.getDocumentTemplate() != null) {
			embedder.setDocumentTemplate(source.getDocumentTemplate());
		}
		if (source.getDimensions() != null) {
			embedder.setDimensions(source.getDimensions());
		}
		if (source.getDistribution() != null) {
			embedder.setDistribution(toEmbedderDistribution(source.getDistribution()));
		}
		if (source.getRequest() != null) {
			embedder.setRequest(source.getRequest());
		}
		if (source.getResponse() != null) {
			embedder.setResponse(source.getResponse());
		}
		if (source.getDocumentTemplateMaxBytes() != null) {
			embedder.setDocumentTemplateMaxBytes(source.getDocumentTemplateMaxBytes());
		}
		if (source.getRevision() != null) {
			embedder.setRevision(source.getRevision());
		}
		if (source.getHeaders() != null) {
			embedder.setHeaders(source.getHeaders());
		}
		if (source.getBinaryQuantized() != null) {
			embedder.setBinaryQuantized(source.getBinaryQuantized());
		}
		if (source.getUrl() != null) {
			embedder.setUrl(source.getUrl());
		}
		if (source.getInputField() != null) {
			embedder.setInputField(toArray(source.getInputField()));
		}
		if (source.getInputType() != null) {
			embedder.setInputType(com.meilisearch.sdk.model.EmbedderInputType.valueOf(source.getInputType().name()));
		}
		if (source.getQuery() != null) {
			embedder.setQuery(source.getQuery());
		}
		return embedder;
	}

	private static com.meilisearch.sdk.model.EmbedderDistribution toEmbedderDistribution(
			EmbedderDistributionSettings source) {

		Assert.isTrue(source.getMean() != null && source.getSigma() != null,
				"Embedder distribution mean and sigma must be configured together");
		return com.meilisearch.sdk.model.EmbedderDistribution.custom(source.getMean(), source.getSigma());
	}

}
