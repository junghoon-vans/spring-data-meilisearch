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
package io.vanslog.spring.data.meilisearch.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Runtime settings for a Meilisearch index.
 *
 * @author Junghoon Ban
 */
public class MeilisearchIndexSettings {

	@Nullable private final List<String> searchableAttributes;
	@Nullable private final List<String> displayedAttributes;
	@Nullable private final List<String> sortableAttributes;
	@Nullable private final List<String> rankingRules;
	@Nullable private final String distinctAttribute;
	@Nullable private final List<String> filterableAttributes;
	@Nullable private final Map<String, List<String>> synonyms;
	@Nullable private final List<String> stopWords;
	@Nullable private final List<String> dictionary;
	@Nullable private final PaginationSettings pagination;
	@Nullable private final FacetingSettings faceting;
	@Nullable private final TypoToleranceSettings typoTolerance;
	@Nullable private final String proximityPrecision;
	@Nullable private final Integer searchCutoffMs;
	@Nullable private final List<String> separatorTokens;
	@Nullable private final List<String> nonSeparatorTokens;
	@Nullable private final List<LocalizedAttributeSettings> localizedAttributes;
	@Nullable private final Map<String, EmbedderSettings> embedders;

	private MeilisearchIndexSettings(Builder builder) {

		this.searchableAttributes = copyList(builder.searchableAttributes);
		this.displayedAttributes = copyList(builder.displayedAttributes);
		this.sortableAttributes = copyList(builder.sortableAttributes);
		this.rankingRules = copyList(builder.rankingRules);
		this.distinctAttribute = builder.distinctAttribute;
		this.filterableAttributes = copyList(builder.filterableAttributes);
		this.synonyms = copyListMap(builder.synonyms);
		this.stopWords = copyList(builder.stopWords);
		this.dictionary = copyList(builder.dictionary);
		this.pagination = builder.pagination;
		this.faceting = builder.faceting;
		this.typoTolerance = builder.typoTolerance;
		this.proximityPrecision = builder.proximityPrecision;
		this.searchCutoffMs = builder.searchCutoffMs;
		this.separatorTokens = copyList(builder.separatorTokens);
		this.nonSeparatorTokens = copyList(builder.nonSeparatorTokens);
		this.localizedAttributes = copyList(builder.localizedAttributes);
		this.embedders = copyMap(builder.embedders);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Nullable
	public List<String> getSearchableAttributes() {
		return searchableAttributes;
	}

	@Nullable
	public List<String> getDisplayedAttributes() {
		return displayedAttributes;
	}

	@Nullable
	public List<String> getSortableAttributes() {
		return sortableAttributes;
	}

	@Nullable
	public List<String> getRankingRules() {
		return rankingRules;
	}

	@Nullable
	public String getDistinctAttribute() {
		return distinctAttribute;
	}

	@Nullable
	public List<String> getFilterableAttributes() {
		return filterableAttributes;
	}

	@Nullable
	public Map<String, List<String>> getSynonyms() {
		return synonyms;
	}

	@Nullable
	public List<String> getStopWords() {
		return stopWords;
	}

	@Nullable
	public List<String> getDictionary() {
		return dictionary;
	}

	@Nullable
	public PaginationSettings getPagination() {
		return pagination;
	}

	@Nullable
	public FacetingSettings getFaceting() {
		return faceting;
	}

	@Nullable
	public TypoToleranceSettings getTypoTolerance() {
		return typoTolerance;
	}

	@Nullable
	public String getProximityPrecision() {
		return proximityPrecision;
	}

	@Nullable
	public Integer getSearchCutoffMs() {
		return searchCutoffMs;
	}

	@Nullable
	public List<String> getSeparatorTokens() {
		return separatorTokens;
	}

	@Nullable
	public List<String> getNonSeparatorTokens() {
		return nonSeparatorTokens;
	}

	@Nullable
	public List<LocalizedAttributeSettings> getLocalizedAttributes() {
		return localizedAttributes;
	}

	@Nullable
	public Map<String, EmbedderSettings> getEmbedders() {
		return embedders;
	}

	@Nullable
	private static <T> List<T> copyList(@Nullable List<T> source) {
		return source != null ? List.copyOf(source) : null;
	}

	@Nullable
	private static <T> Map<String, T> copyMap(@Nullable Map<String, T> source) {
		return source != null ? Map.copyOf(source) : null;
	}

	@Nullable
	private static Map<String, List<String>> copyListMap(@Nullable Map<String, List<String>> source) {

		if (source == null) {
			return null;
		}

		Map<String, List<String>> copy = new LinkedHashMap<>();
		source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
		return Map.copyOf(copy);
	}

	/** Builder for {@link MeilisearchIndexSettings}. */
	public static class Builder {

		@Nullable private List<String> searchableAttributes;
		@Nullable private List<String> displayedAttributes;
		@Nullable private List<String> sortableAttributes;
		@Nullable private List<String> rankingRules;
		@Nullable private String distinctAttribute;
		@Nullable private List<String> filterableAttributes;
		@Nullable private Map<String, List<String>> synonyms;
		@Nullable private List<String> stopWords;
		@Nullable private List<String> dictionary;
		@Nullable private PaginationSettings pagination;
		@Nullable private FacetingSettings faceting;
		@Nullable private TypoToleranceSettings typoTolerance;
		@Nullable private String proximityPrecision;
		@Nullable private Integer searchCutoffMs;
		@Nullable private List<String> separatorTokens;
		@Nullable private List<String> nonSeparatorTokens;
		@Nullable private List<LocalizedAttributeSettings> localizedAttributes;
		@Nullable private Map<String, EmbedderSettings> embedders;

		public Builder withSearchableAttributes(List<String> searchableAttributes) {

			Assert.notNull(searchableAttributes, "Searchable attributes must not be null");
			this.searchableAttributes = searchableAttributes;
			return this;
		}

		public Builder withDisplayedAttributes(List<String> displayedAttributes) {

			Assert.notNull(displayedAttributes, "Displayed attributes must not be null");
			this.displayedAttributes = displayedAttributes;
			return this;
		}

		public Builder withSortableAttributes(List<String> sortableAttributes) {

			Assert.notNull(sortableAttributes, "Sortable attributes must not be null");
			this.sortableAttributes = sortableAttributes;
			return this;
		}

		public Builder withRankingRules(List<String> rankingRules) {

			Assert.notNull(rankingRules, "Ranking rules must not be null");
			this.rankingRules = rankingRules;
			return this;
		}

		public Builder withDistinctAttribute(@Nullable String distinctAttribute) {

			this.distinctAttribute = distinctAttribute;
			return this;
		}

		public Builder withFilterableAttributes(List<String> filterableAttributes) {

			Assert.notNull(filterableAttributes, "Filterable attributes must not be null");
			this.filterableAttributes = filterableAttributes;
			return this;
		}

		public Builder withSynonyms(Map<String, List<String>> synonyms) {

			Assert.notNull(synonyms, "Synonyms must not be null");
			this.synonyms = synonyms;
			return this;
		}

		public Builder withStopWords(List<String> stopWords) {

			Assert.notNull(stopWords, "Stop words must not be null");
			this.stopWords = stopWords;
			return this;
		}

		public Builder withDictionary(List<String> dictionary) {

			Assert.notNull(dictionary, "Dictionary must not be null");
			this.dictionary = dictionary;
			return this;
		}

		public Builder withPagination(PaginationSettings pagination) {

			Assert.notNull(pagination, "Pagination settings must not be null");
			this.pagination = pagination;
			return this;
		}

		public Builder withFaceting(FacetingSettings faceting) {

			Assert.notNull(faceting, "Faceting settings must not be null");
			this.faceting = faceting;
			return this;
		}

		public Builder withTypoTolerance(TypoToleranceSettings typoTolerance) {

			Assert.notNull(typoTolerance, "Typo tolerance settings must not be null");
			this.typoTolerance = typoTolerance;
			return this;
		}

		public Builder withProximityPrecision(@Nullable String proximityPrecision) {

			this.proximityPrecision = proximityPrecision;
			return this;
		}

		public Builder withSearchCutoffMs(@Nullable Integer searchCutoffMs) {

			Assert.isTrue(searchCutoffMs == null || searchCutoffMs >= 0, "Search cutoff ms must not be negative");
			this.searchCutoffMs = searchCutoffMs;
			return this;
		}

		public Builder withSeparatorTokens(List<String> separatorTokens) {

			Assert.notNull(separatorTokens, "Separator tokens must not be null");
			this.separatorTokens = separatorTokens;
			return this;
		}

		public Builder withNonSeparatorTokens(List<String> nonSeparatorTokens) {

			Assert.notNull(nonSeparatorTokens, "Non-separator tokens must not be null");
			this.nonSeparatorTokens = nonSeparatorTokens;
			return this;
		}

		public Builder withLocalizedAttributes(List<LocalizedAttributeSettings> localizedAttributes) {

			Assert.notNull(localizedAttributes, "Localized attributes must not be null");
			this.localizedAttributes = localizedAttributes;
			return this;
		}

		public Builder withEmbedders(Map<String, EmbedderSettings> embedders) {

			Assert.notNull(embedders, "Embedders must not be null");
			this.embedders = embedders;
			return this;
		}

		public MeilisearchIndexSettings build() {
			return new MeilisearchIndexSettings(this);
		}
	}

	/** Pagination runtime settings. */
	public static class PaginationSettings {

		private final int maxTotalHits;

		public PaginationSettings(int maxTotalHits) {

			Assert.isTrue(maxTotalHits >= 0, "Max total hits must not be negative");
			this.maxTotalHits = maxTotalHits;
		}

		public int getMaxTotalHits() {
			return maxTotalHits;
		}
	}

	/** Faceting runtime settings. */
	public static class FacetingSettings {

		private final int maxValuesPerFacet;

		public FacetingSettings(int maxValuesPerFacet) {

			Assert.isTrue(maxValuesPerFacet >= 0, "Max values per facet must not be negative");
			this.maxValuesPerFacet = maxValuesPerFacet;
		}

		public int getMaxValuesPerFacet() {
			return maxValuesPerFacet;
		}
	}

	/** Typo tolerance runtime settings. */
	public static class TypoToleranceSettings {

		private final boolean enabled;
		@Nullable private final Integer oneTypo;
		@Nullable private final Integer twoTypos;
		@Nullable private final List<String> disableOnWords;
		@Nullable private final List<String> disableOnAttributes;

		public TypoToleranceSettings(boolean enabled, @Nullable Integer oneTypo, @Nullable Integer twoTypos,
				@Nullable List<String> disableOnWords, @Nullable List<String> disableOnAttributes) {

			this.enabled = enabled;
			this.oneTypo = oneTypo;
			this.twoTypos = twoTypos;
			this.disableOnWords = copyList(disableOnWords);
			this.disableOnAttributes = copyList(disableOnAttributes);
		}

		public boolean isEnabled() {
			return enabled;
		}

		@Nullable
		public Integer getOneTypo() {
			return oneTypo;
		}

		@Nullable
		public Integer getTwoTypos() {
			return twoTypos;
		}

		@Nullable
		public List<String> getDisableOnWords() {
			return disableOnWords;
		}

		@Nullable
		public List<String> getDisableOnAttributes() {
			return disableOnAttributes;
		}

	}

	/** Localized attribute runtime settings. */
	public static class LocalizedAttributeSettings {

		private final List<String> attributePatterns;
		private final List<String> locales;

		public LocalizedAttributeSettings(List<String> attributePatterns, List<String> locales) {

			Assert.notNull(attributePatterns, "Attribute patterns must not be null");
			Assert.notNull(locales, "Locales must not be null");
			this.attributePatterns = List.copyOf(attributePatterns);
			this.locales = List.copyOf(locales);
		}

		public List<String> getAttributePatterns() {
			return attributePatterns;
		}

		public List<String> getLocales() {
			return locales;
		}
	}

	/** Embedder source values supported by Meilisearch. */
	public enum EmbedderSource {

		OPEN_AI, HUGGING_FACE, OLLAMA, REST, USER_PROVIDED
	}

	/** Embedder input type values supported by Meilisearch. */
	public enum EmbedderInputType {

		TEXT, TEXT_ARRAY
	}

	/** Embedder distribution runtime settings. */
	public static class EmbedderDistributionSettings {

		@Nullable private final Double mean;
		@Nullable private final Double sigma;

		public EmbedderDistributionSettings(@Nullable Double mean, @Nullable Double sigma) {

			this.mean = mean;
			this.sigma = sigma;
		}

		@Nullable
		public Double getMean() {
			return mean;
		}

		@Nullable
		public Double getSigma() {
			return sigma;
		}
	}

	/** Embedder runtime settings. */
	public static class EmbedderSettings {

		@Nullable private final EmbedderSource source;
		@Nullable private final String apiKey;
		@Nullable private final String model;
		@Nullable private final String documentTemplate;
		@Nullable private final Integer dimensions;
		@Nullable private final EmbedderDistributionSettings distribution;
		@Nullable private final Map<String, Object> request;
		@Nullable private final Map<String, Object> response;
		@Nullable private final Integer documentTemplateMaxBytes;
		@Nullable private final String revision;
		@Nullable private final Map<String, String> headers;
		@Nullable private final Boolean binaryQuantized;
		@Nullable private final String url;
		@Nullable private final List<String> inputField;
		@Nullable private final EmbedderInputType inputType;
		@Nullable private final String query;

		private EmbedderSettings(EmbedderBuilder builder) {

			this.source = builder.source;
			this.apiKey = builder.apiKey;
			this.model = builder.model;
			this.documentTemplate = builder.documentTemplate;
			this.dimensions = builder.dimensions;
			this.distribution = builder.distribution;
			this.request = copyMap(builder.request);
			this.response = copyMap(builder.response);
			this.documentTemplateMaxBytes = builder.documentTemplateMaxBytes;
			this.revision = builder.revision;
			this.headers = copyMap(builder.headers);
			this.binaryQuantized = builder.binaryQuantized;
			this.url = builder.url;
			this.inputField = copyList(builder.inputField);
			this.inputType = builder.inputType;
			this.query = builder.query;
		}

		public static EmbedderBuilder builder() {
			return new EmbedderBuilder();
		}

		@Nullable
		public EmbedderSource getSource() {
			return source;
		}

		@Nullable
		public String getApiKey() {
			return apiKey;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		@Nullable
		public String getDocumentTemplate() {
			return documentTemplate;
		}

		@Nullable
		public Integer getDimensions() {
			return dimensions;
		}

		@Nullable
		public EmbedderDistributionSettings getDistribution() {
			return distribution;
		}

		@Nullable
		public Map<String, Object> getRequest() {
			return request;
		}

		@Nullable
		public Map<String, Object> getResponse() {
			return response;
		}

		@Nullable
		public Integer getDocumentTemplateMaxBytes() {
			return documentTemplateMaxBytes;
		}

		@Nullable
		public String getRevision() {
			return revision;
		}

		@Nullable
		public Map<String, String> getHeaders() {
			return headers;
		}

		@Nullable
		public Boolean getBinaryQuantized() {
			return binaryQuantized;
		}

		@Nullable
		public String getUrl() {
			return url;
		}

		@Nullable
		public List<String> getInputField() {
			return inputField;
		}

		@Nullable
		public EmbedderInputType getInputType() {
			return inputType;
		}

		@Nullable
		public String getQuery() {
			return query;
		}

		/** Builder for {@link EmbedderSettings}. */
		public static class EmbedderBuilder {

			@Nullable private EmbedderSource source;
			@Nullable private String apiKey;
			@Nullable private String model;
			@Nullable private String documentTemplate;
			@Nullable private Integer dimensions;
			@Nullable private EmbedderDistributionSettings distribution;
			@Nullable private Map<String, Object> request;
			@Nullable private Map<String, Object> response;
			@Nullable private Integer documentTemplateMaxBytes;
			@Nullable private String revision;
			@Nullable private Map<String, String> headers;
			@Nullable private Boolean binaryQuantized;
			@Nullable private String url;
			@Nullable private List<String> inputField;
			@Nullable private EmbedderInputType inputType;
			@Nullable private String query;

			public EmbedderBuilder withSource(@Nullable EmbedderSource source) {
				this.source = source;
				return this;
			}

			public EmbedderBuilder withApiKey(@Nullable String apiKey) {
				this.apiKey = apiKey;
				return this;
			}

			public EmbedderBuilder withModel(@Nullable String model) {
				this.model = model;
				return this;
			}

			public EmbedderBuilder withDocumentTemplate(@Nullable String documentTemplate) {
				this.documentTemplate = documentTemplate;
				return this;
			}

			public EmbedderBuilder withDimensions(@Nullable Integer dimensions) {
				this.dimensions = dimensions;
				return this;
			}

			public EmbedderBuilder withDistribution(@Nullable EmbedderDistributionSettings distribution) {
				this.distribution = distribution;
				return this;
			}

			public EmbedderBuilder withRequest(Map<String, Object> request) {

				Assert.notNull(request, "Request parameters must not be null");
				this.request = request;
				return this;
			}

			public EmbedderBuilder withResponse(Map<String, Object> response) {

				Assert.notNull(response, "Response parameters must not be null");
				this.response = response;
				return this;
			}

			public EmbedderBuilder withDocumentTemplateMaxBytes(@Nullable Integer documentTemplateMaxBytes) {
				this.documentTemplateMaxBytes = documentTemplateMaxBytes;
				return this;
			}

			public EmbedderBuilder withRevision(@Nullable String revision) {
				this.revision = revision;
				return this;
			}

			public EmbedderBuilder withHeaders(Map<String, String> headers) {

				Assert.notNull(headers, "Headers must not be null");
				this.headers = headers;
				return this;
			}

			public EmbedderBuilder withBinaryQuantized(@Nullable Boolean binaryQuantized) {
				this.binaryQuantized = binaryQuantized;
				return this;
			}

			public EmbedderBuilder withUrl(@Nullable String url) {
				this.url = url;
				return this;
			}

			public EmbedderBuilder withInputField(List<String> inputField) {

				Assert.notNull(inputField, "Input fields must not be null");
				this.inputField = inputField;
				return this;
			}

			public EmbedderBuilder withInputType(@Nullable EmbedderInputType inputType) {
				this.inputType = inputType;
				return this;
			}

			public EmbedderBuilder withQuery(@Nullable String query) {
				this.query = query;
				return this;
			}

			public EmbedderSettings build() {
				return new EmbedderSettings(this);
			}
		}
	}
}
