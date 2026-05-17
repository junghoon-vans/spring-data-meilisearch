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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TypoTolerance;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderDistributionSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderInputType;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.EmbedderSource;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.FacetingSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.LocalizedAttributeSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.PaginationSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.TypoToleranceSettings;
import org.springframework.lang.Nullable;

/**
 * Converts SDK index settings responses into Spring Data Meilisearch settings values.
 *
 * @author Junghoon Ban
 */
class IndexSettingsResponseConverter {

	MeilisearchIndexSettings fromSettings(Settings source) {

		MeilisearchIndexSettings.Builder builder = MeilisearchIndexSettings.builder();

		if (source.getSearchableAttributes() != null) {
			builder.withSearchableAttributes(toList(source.getSearchableAttributes()));
		}
		if (source.getDisplayedAttributes() != null) {
			builder.withDisplayedAttributes(toList(source.getDisplayedAttributes()));
		}
		if (source.getSortableAttributes() != null) {
			builder.withSortableAttributes(toList(source.getSortableAttributes()));
		}
		if (source.getRankingRules() != null) {
			builder.withRankingRules(toList(source.getRankingRules()));
		}
		builder.withDistinctAttribute(source.getDistinctAttribute());
		if (source.getFilterableAttributes() != null) {
			builder.withFilterableAttributes(toList(source.getFilterableAttributes()));
		}
		if (source.getSynonyms() != null) {
			builder.withSynonyms(toSynonyms(source.getSynonyms()));
		}
		if (source.getStopWords() != null) {
			builder.withStopWords(toList(source.getStopWords()));
		}
		if (source.getDictionary() != null) {
			builder.withDictionary(toList(source.getDictionary()));
		}
		if (source.getPagination() != null) {
			builder.withPagination(new PaginationSettings(source.getPagination().getMaxTotalHits()));
		}
		if (source.getFaceting() != null) {
			builder.withFaceting(new FacetingSettings(source.getFaceting().getMaxValuesPerFacet()));
		}
		if (source.getTypoTolerance() != null) {
			builder.withTypoTolerance(toTypoToleranceSettings(source.getTypoTolerance()));
		}
		builder.withProximityPrecision(source.getProximityPrecision());
		builder.withSearchCutoffMs(source.getSearchCutoffMs());
		if (source.getSeparatorTokens() != null) {
			builder.withSeparatorTokens(toList(source.getSeparatorTokens()));
		}
		if (source.getNonSeparatorTokens() != null) {
			builder.withNonSeparatorTokens(toList(source.getNonSeparatorTokens()));
		}
		if (source.getLocalizedAttributes() != null) {
			builder.withLocalizedAttributes(toLocalizedAttributeSettings(source.getLocalizedAttributes()));
		}
		if (source.getEmbedders() != null) {
			builder.withEmbedders(toEmbedderSettings(source.getEmbedders()));
		}

		return builder.build();
	}

	private static List<String> toList(String[] source) {
		return Arrays.asList(source);
	}

	private static Map<String, List<String>> toSynonyms(Map<String, String[]> source) {

		Map<String, List<String>> synonyms = new LinkedHashMap<>();
		source.forEach((word, values) -> synonyms.put(word, toList(values)));
		return synonyms;
	}

	private static TypoToleranceSettings toTypoToleranceSettings(TypoTolerance source) {

		Integer oneTypo = getTypoSize(source, "oneTypo");
		Integer twoTypos = getTypoSize(source, "twoTypos");
		List<String> disableOnWords = source.getDisableOnWords() != null ? toList(source.getDisableOnWords()) : null;
		List<String> disableOnAttributes = source.getDisableOnAttributes() != null ? toList(source.getDisableOnAttributes())
				: null;

		return new TypoToleranceSettings(source.isEnabled(), oneTypo, twoTypos, disableOnWords, disableOnAttributes);
	}

	@Nullable
	private static Integer getTypoSize(TypoTolerance source, String key) {
		return source.getMinWordSizeForTypos() != null ? source.getMinWordSizeForTypos().get(key) : null;
	}

	private static List<LocalizedAttributeSettings> toLocalizedAttributeSettings(
			com.meilisearch.sdk.model.LocalizedAttribute[] source) {

		return Arrays.stream(source)
				.map(it -> new LocalizedAttributeSettings(toList(it.getAttributePatterns()), toList(it.getLocales())))
				.toList();
	}

	private static Map<String, EmbedderSettings> toEmbedderSettings(
			Map<String, com.meilisearch.sdk.model.Embedder> source) {

		Map<String, EmbedderSettings> embedders = new LinkedHashMap<>();
		source.forEach((name, embedder) -> embedders.put(name, toEmbedderSettings(embedder)));
		return embedders;
	}

	private static EmbedderSettings toEmbedderSettings(com.meilisearch.sdk.model.Embedder source) {

		EmbedderSettings.EmbedderBuilder builder = EmbedderSettings.builder();
		if (source.getSource() != null) {
			builder.withSource(EmbedderSource.valueOf(source.getSource().name()));
		}
		builder.withApiKey(source.getApiKey());
		builder.withModel(source.getModel());
		builder.withDocumentTemplate(source.getDocumentTemplate());
		builder.withDimensions(source.getDimensions());
		if (source.getDistribution() != null) {
			builder.withDistribution(new EmbedderDistributionSettings(source.getDistribution().getMean(),
					source.getDistribution().getSigma()));
		}
		if (source.getRequest() != null) {
			builder.withRequest(source.getRequest());
		}
		if (source.getResponse() != null) {
			builder.withResponse(source.getResponse());
		}
		builder.withDocumentTemplateMaxBytes(source.getDocumentTemplateMaxBytes());
		builder.withRevision(source.getRevision());
		if (source.getHeaders() != null) {
			builder.withHeaders(source.getHeaders());
		}
		builder.withBinaryQuantized(source.getBinaryQuantized());
		builder.withUrl(source.getUrl());
		if (source.getInputField() != null) {
			builder.withInputField(toList(source.getInputField()));
		}
		if (source.getInputType() != null) {
			builder.withInputType(EmbedderInputType.valueOf(source.getInputType().name()));
		}
		builder.withQuery(source.getQuery());
		return builder.build();
	}
}
