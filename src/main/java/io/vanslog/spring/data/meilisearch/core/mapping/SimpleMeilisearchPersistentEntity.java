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
import io.vanslog.spring.data.meilisearch.annotations.Pagination;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
import io.vanslog.spring.data.meilisearch.annotations.Synonym;
import io.vanslog.spring.data.meilisearch.annotations.TypoTolerance;

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
	@Nullable private final Document document;
	@Nullable private final SettingsParameter settingParameter;
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
		document = AnnotatedElementUtils.findMergedAnnotation(rawType, Document.class);

		if (document != null) {
			Assert.hasText(document.indexUid(),
					"Unknown indexUid. Make sure the indexUid is defined." + "e.g @Document(indexUid=\"foo\")");
			this.indexUid = document.indexUid();
			this.applySettings = document.applySettings();
			this.settingParameter = buildSettingsParameter(rawType);
		} else {
			this.applySettings = false;
			this.settingParameter = null;
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
		return indexUid;
	}

	@Override
	public boolean isApplySettings() {
		return applySettings && settingParameter != null;
	}

	@Override
	@Nullable
	public Settings getDefaultSettings() {
		if (settingParameter == null) {
			return null;
		}

		return settingParameter.toSettings();
	}

	@Nullable
	private SettingsParameter buildSettingsParameter(Class<?> clazz) {
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		if (settingAnnotation == null) {
			return null;
		}

		return new SettingsParameter(settingAnnotation);
	}

	private static class SettingsParameter {
		private final String[] sortableAttributes;
		private final String distinctAttribute;
		private final String[] searchableAttributes;
		private final String[] displayedAttributes;
		private final String[] rankingRules;
		private final String[] stopWords;
		@Nullable private final Pagination pagination;
		private final String[] filterableAttributes;
		private final Synonym[] synonyms;
		@Nullable private final TypoTolerance typoTolerance;
		@Nullable private final Faceting faceting;
		private final String[] dictionary;
		private final String proximityPrecision;
		private final Integer searchCutoffMs;
		private final String[] separatorTokens;
		private final String[] nonSeparatorTokens;

		public SettingsParameter(Setting settingAnnotation) {
			this.sortableAttributes = settingAnnotation.sortableAttributes();
			this.distinctAttribute = settingAnnotation.distinctAttribute();
			this.searchableAttributes = settingAnnotation.searchableAttributes();
			this.displayedAttributes = settingAnnotation.displayedAttributes();
			this.rankingRules = settingAnnotation.rankingRules();
			this.stopWords = settingAnnotation.stopWords();
			this.pagination = settingAnnotation.pagination();
			this.filterableAttributes = settingAnnotation.filterableAttributes();
			this.synonyms = settingAnnotation.synonyms();
			this.typoTolerance = settingAnnotation.typoTolerance();
			this.faceting = settingAnnotation.faceting();
			this.dictionary = settingAnnotation.dictionary();
			this.proximityPrecision = settingAnnotation.proximityPrecision();
			this.searchCutoffMs = settingAnnotation.searchCutoffMs();
			this.separatorTokens = settingAnnotation.separatorTokens();
			this.nonSeparatorTokens = settingAnnotation.nonSeparatorTokens();
		}

		Settings toSettings() {
			Settings settings = new Settings();

			if (sortableAttributes.length > 0) {
				settings.setSortableAttributes(sortableAttributes);
			}
			
			if (!distinctAttribute.isEmpty()) {
				settings.setDistinctAttribute(distinctAttribute);
			}
			
			if (searchableAttributes.length > 0) {
				settings.setSearchableAttributes(searchableAttributes);
			}
			
			if (displayedAttributes.length > 0) {
				settings.setDisplayedAttributes(displayedAttributes);
			}
			
			if (rankingRules.length > 0) {
				settings.setRankingRules(rankingRules);
			}
			
			if (stopWords.length > 0) {
				settings.setStopWords(stopWords);
			}
			
			if (filterableAttributes.length > 0) {
				settings.setFilterableAttributes(filterableAttributes);
			}
			
			if (dictionary.length > 0) {
				settings.setDictionary(dictionary);
			}
			
			if (!proximityPrecision.isEmpty()) {
				settings.setProximityPrecision(proximityPrecision);
			}
			
			if (separatorTokens.length > 0) {
				settings.setSeparatorTokens(separatorTokens);
			}
			
			if (nonSeparatorTokens.length > 0) {
				settings.setNonSeparatorTokens(nonSeparatorTokens);
			}
			
			if (pagination != null) {
				settings.setPagination(createMeiliPagination(pagination));
			}
			
			if (synonyms.length > 0) {
				settings.setSynonyms(createSynonymMap(synonyms));
			}
			
			if (typoTolerance != null) {
				settings.setTypoTolerance(createMeiliTypoTolerance(typoTolerance));
			}
			
			if (faceting != null) {
				settings.setFaceting(createMeiliFaceting(faceting));
			}

			if (searchCutoffMs != -1) {
				settings.setSearchCutoffMs(searchCutoffMs);
			}

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
	}
}
