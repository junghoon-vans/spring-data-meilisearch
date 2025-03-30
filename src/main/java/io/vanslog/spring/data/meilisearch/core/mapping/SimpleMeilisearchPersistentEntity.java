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

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Faceting;
import io.vanslog.spring.data.meilisearch.annotations.LocalizedAttribute;
import io.vanslog.spring.data.meilisearch.annotations.Pagination;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.annotations.Synonym;
import io.vanslog.spring.data.meilisearch.annotations.TypoTolerance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.meilisearch.sdk.model.Settings;

/**
 * Meilisearch specific {@link BasicPersistentEntity} implementation holding.
 *
 * @param <T>
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentEntity<T> extends BasicPersistentEntity<T, MeilisearchPersistentProperty>
		implements MeilisearchPersistentEntity<T>, ApplicationContextAware {

	private final StandardEvaluationContext context;
	private final SettingsParameter settingParameter;
	private final boolean applySettings;
	@Nullable private String indexUid;

	/**
	 * Creates a new {@link SimpleMeilisearchPersistentEntity} with the given {@link TypeInformation}.
	 *
	 * @param information must not be {@literal null}.
	 */
	public SimpleMeilisearchPersistentEntity(TypeInformation<T> information) {
		super(information);
		this.context = new StandardEvaluationContext();

		Class<T> rawType = information.getType();
		Document document = AnnotatedElementUtils.findMergedAnnotation(rawType, Document.class);
		this.settingParameter = buildSettingsParameter(rawType);

		if (document != null) {
			Assert.hasText(document.indexUid(),
					"Unknown indexUid. Make sure the indexUid is defined." + "e.g @Document(indexUid=\"foo\")");
			this.indexUid = document.indexUid();
			this.applySettings = document.applySettings();
		} else {
			this.applySettings = false;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context.addPropertyAccessor(new BeanFactoryAccessor());
		context.setBeanResolver(new BeanFactoryResolver(applicationContext));
		context.setRootObject(applicationContext);
	}

	@Override
	public String getIndexUid() {
		return indexUid != null ? indexUid : getTypeInformation().getType().getSimpleName();
	}

	@Override
	public boolean isApplySettings() {
		return applySettings;
	}

	@Override
	public Settings getDefaultSettings() {
		return settingParameter.toSettings();
	}

	private SettingsParameter buildSettingsParameter(Class<?> clazz) {
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);
		Pagination paginationAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Pagination.class);
		TypoTolerance typoToleranceAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, TypoTolerance.class);
		Faceting facetingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Faceting.class);

		return new SettingsParameter(settingAnnotation, paginationAnnotation, typoToleranceAnnotation, facetingAnnotation);
	}

	private static class SettingsParameter {

		@Nullable private final Pagination pagination;
		@Nullable private final TypoTolerance typoTolerance;
		@Nullable private final Faceting faceting;

		@Nullable private String[] sortableAttributes;
		@Nullable private String distinctAttribute;
		@Nullable private String[] searchableAttributes;
		@Nullable private String[] displayedAttributes;
		@Nullable private String[] rankingRules;
		@Nullable private String[] stopWords;
		@Nullable private String[] filterableAttributes;
		@Nullable private Synonym[] synonyms;
		@Nullable private String[] dictionary;
		@Nullable private String proximityPrecision;
		@Nullable private Integer searchCutoffMs;
		@Nullable private String[] separatorTokens;
		@Nullable private String[] nonSeparatorTokens;
		@Nullable private LocalizedAttribute[] localizedAttributes;

		public SettingsParameter(@Nullable Setting setting, @Nullable Pagination pagination,
				@Nullable TypoTolerance typoTolerance, @Nullable Faceting faceting) {

			this.pagination = pagination;
			this.typoTolerance = typoTolerance;
			this.faceting = faceting;

			if (setting != null) {
				this.sortableAttributes = setting.sortableAttributes();
				this.distinctAttribute = setting.distinctAttribute();
				this.searchableAttributes = setting.searchableAttributes();
				this.displayedAttributes = setting.displayedAttributes();
				this.rankingRules = setting.rankingRules();
				this.stopWords = setting.stopWords();
				this.filterableAttributes = setting.filterableAttributes();
				this.synonyms = setting.synonyms();
				this.dictionary = setting.dictionary();
				this.proximityPrecision = setting.proximityPrecision();
				this.searchCutoffMs = setting.searchCutoffMs();
				this.separatorTokens = setting.separatorTokens();
				this.nonSeparatorTokens = setting.nonSeparatorTokens();
				this.localizedAttributes = setting.localizedAttributes();
			}
		}

		Settings toSettings() {
			Settings settings = new Settings();

			Optional.ofNullable(pagination).ifPresent(it -> settings.setPagination(createMeiliPagination(it)));
			Optional.ofNullable(typoTolerance).ifPresent(it -> settings.setTypoTolerance(createMeiliTypoTolerance(it)));
			Optional.ofNullable(faceting).ifPresent(it -> settings.setFaceting(createMeiliFaceting(it)));

			Optional.ofNullable(sortableAttributes).filter(it -> it.length > 0).ifPresent(settings::setSortableAttributes);
			Optional.ofNullable(distinctAttribute).filter(it -> !it.isEmpty()).ifPresent(settings::setDistinctAttribute);
			Optional.ofNullable(searchableAttributes).filter(it -> it.length > 0)
					.ifPresent(settings::setSearchableAttributes);
			Optional.ofNullable(displayedAttributes).filter(it -> it.length > 0).ifPresent(settings::setDisplayedAttributes);
			Optional.ofNullable(rankingRules).filter(it -> it.length > 0).ifPresent(settings::setRankingRules);
			Optional.ofNullable(stopWords).filter(it -> it.length > 0).ifPresent(settings::setStopWords);
			Optional.ofNullable(filterableAttributes).filter(it -> it.length > 0)
					.ifPresent(settings::setFilterableAttributes);
			Optional.ofNullable(synonyms).filter(it -> it.length > 0)
					.ifPresent(it -> settings.setSynonyms(createSynonymMap(it)));
			Optional.ofNullable(dictionary).filter(it -> it.length > 0).ifPresent(settings::setDictionary);
			Optional.ofNullable(proximityPrecision).filter(it -> !it.isEmpty()).ifPresent(settings::setProximityPrecision);
			Optional.ofNullable(searchCutoffMs).filter(it -> it > 0).ifPresent(settings::setSearchCutoffMs);
			Optional.ofNullable(separatorTokens).filter(it -> it.length > 0).ifPresent(settings::setSeparatorTokens);
			Optional.ofNullable(nonSeparatorTokens).filter(it -> it.length > 0).ifPresent(settings::setNonSeparatorTokens);
			Optional.ofNullable(localizedAttributes).filter(it -> it.length > 0)
					.map(attrs -> Arrays.stream(attrs).map(this::createMeiliLocalizedAttribute)
							.toArray(com.meilisearch.sdk.model.LocalizedAttribute[]::new))
					.ifPresent(settings::setLocalizedAttributes);

			return settings;
		}

		private com.meilisearch.sdk.model.Pagination createMeiliPagination(Pagination pagination) {
			var meiliPagination = new com.meilisearch.sdk.model.Pagination();
			meiliPagination.setMaxTotalHits(pagination.maxTotalHits());
			return meiliPagination;
		}

		private HashMap<String, String[]> createSynonymMap(Synonym[] synonyms) {
			var synonymMap = new HashMap<String, String[]>();
			for (Synonym synonym : synonyms) {
				synonymMap.put(synonym.word(), synonym.synonyms());
			}
			return synonymMap;
		}

		private com.meilisearch.sdk.model.TypoTolerance createMeiliTypoTolerance(TypoTolerance typoTolerance) {
			var meiliTypoTolerance = new com.meilisearch.sdk.model.TypoTolerance();
			meiliTypoTolerance.setEnabled(typoTolerance.enabled());

			var minWordSizeForTypos = new HashMap<String, Integer>();
			minWordSizeForTypos.put("oneTypo", typoTolerance.minWordSizeForTypos().oneTypo());
			minWordSizeForTypos.put("twoTypos", typoTolerance.minWordSizeForTypos().twoTypos());
			meiliTypoTolerance.setMinWordSizeForTypos(minWordSizeForTypos);

			meiliTypoTolerance.setDisableOnWords(typoTolerance.disableOnWords());
			meiliTypoTolerance.setDisableOnAttributes(typoTolerance.disableOnAttributes());

			return meiliTypoTolerance;
		}

		private com.meilisearch.sdk.model.Faceting createMeiliFaceting(Faceting faceting) {
			var meliFaceting = new com.meilisearch.sdk.model.Faceting();
			meliFaceting.setMaxValuesPerFacet(faceting.maxValuesPerFacet());
			return meliFaceting;
		}

		private com.meilisearch.sdk.model.LocalizedAttribute createMeiliLocalizedAttribute(
				LocalizedAttribute localizedAttribute) {
			var meiliLocalizedAttributes = new com.meilisearch.sdk.model.LocalizedAttribute();
			meiliLocalizedAttributes.setAttributePatterns(localizedAttribute.attributePatterns());
			meiliLocalizedAttributes.setLocales(localizedAttribute.locales());
			return meiliLocalizedAttributes;
		}
	}
}
