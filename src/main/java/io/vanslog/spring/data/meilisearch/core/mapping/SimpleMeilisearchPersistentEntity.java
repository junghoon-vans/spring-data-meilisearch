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
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.Lazy;
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
	private final Lazy<SettingsParameter> settingParameter;
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

		this.settingParameter = Lazy.of(() -> buildSettingsParameter(rawType));

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
		return indexUid;
	}

	@Override
	public boolean isApplySettings() {
		return applySettings;
	}

	@Override
	public Settings getDefaultSettings() {
		return settingParameter.get().toSettings();
	}

	private SettingsParameter buildSettingsParameter(Class<?> clazz) {

		SettingsParameter settingsParameter = new SettingsParameter();
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		// default values
		settingsParameter.searchableAttributes = new String[] { "*" };
		settingsParameter.displayedAttributes = new String[] { "*" };
		settingsParameter.rankingRules = new String[] { "words", "typo", "proximity", "attribute", "sort", "exactness" };
		settingsParameter.proximityPrecision = "byWord";
		settingsParameter.searchCutoffMs = -1;

		if (settingAnnotation != null) {
			processSettingAnnotation(settingAnnotation, settingsParameter);
		}

		return settingsParameter;
	}

	private void processSettingAnnotation(Setting settingAnnotation, SettingsParameter settingsParameter) {
		settingsParameter.searchableAttributes = settingAnnotation.searchableAttributes();
		settingsParameter.displayedAttributes = settingAnnotation.displayedAttributes();
		settingsParameter.rankingRules = settingAnnotation.rankingRules();
		settingsParameter.pagination = settingAnnotation.pagination();
		settingsParameter.typoTolerance = settingAnnotation.typoTolerance();
		settingsParameter.faceting = settingAnnotation.faceting();
		settingsParameter.proximityPrecision = settingAnnotation.proximityPrecision();

		String[] sortAttributes = settingAnnotation.sortAttributes();
		String distinctAttribute = settingAnnotation.distinctAttribute();
		String[] stopWords = settingAnnotation.stopWords();
		String[] filterableAttributes = settingAnnotation.filterableAttributes();
		Synonym[] synonyms = settingAnnotation.synonyms();
		String[] dictionary = settingAnnotation.dictionary();
		String[] separatorTokens = settingAnnotation.separatorTokens();
		String[] nonSeparatorTokens = settingAnnotation.nonSeparatorTokens();
		int searchCutoffMs = settingAnnotation.searchCutoffMs();

		if (sortAttributes.length > 0) {
			settingsParameter.sortAttributes = sortAttributes;
		}
		if (!distinctAttribute.isEmpty()) {
			settingsParameter.distinctAttribute = distinctAttribute;
		}
		if (stopWords.length > 0) {
			settingsParameter.stopWords = stopWords;
		}
		if (filterableAttributes.length > 0) {
			settingsParameter.filterableAttributes = filterableAttributes;
		}
		if (synonyms.length > 0) {
			settingsParameter.synonyms = synonyms;
		}
		if (dictionary.length > 0) {
			settingsParameter.dictionary = dictionary;
		}
		if (separatorTokens.length > 0) {
			settingsParameter.separatorTokens = separatorTokens;
		}
		if (nonSeparatorTokens.length > 0) {
			settingsParameter.nonSeparatorTokens = nonSeparatorTokens;
		}
		if (searchCutoffMs > 0) {
			settingsParameter.searchCutoffMs = searchCutoffMs;
		}
	}

	private static class SettingsParameter {
		@Nullable private String[] sortAttributes;
		@Nullable private String distinctAttribute;
		private String[] searchableAttributes;
		private String[] displayedAttributes;
		private String[] rankingRules;
		@Nullable private String[] stopWords;
		@Nullable private Pagination pagination;
		@Nullable private String[] filterableAttributes;
		@Nullable private Synonym[] synonyms;
		@Nullable private TypoTolerance typoTolerance;
		@Nullable private Faceting faceting;
		@Nullable private String[] dictionary;
		@Nullable private String proximityPrecision;
		@Nullable private Integer searchCutoffMs;
		@Nullable private String[] separatorTokens;
		@Nullable private String[] nonSeparatorTokens;

		Settings toSettings() {
			Settings settings = new Settings();
			settings.setSearchableAttributes(searchableAttributes);
			settings.setDisplayedAttributes(displayedAttributes);
			settings.setRankingRules(rankingRules);

			if (sortAttributes != null) {
				settings.setSortableAttributes(sortAttributes);
			}
			if (distinctAttribute != null) {
				settings.setDistinctAttribute(distinctAttribute);
			}
			if (stopWords != null) {
				settings.setStopWords(stopWords);
			}
			if (pagination != null) {
				var meiliPagination = new com.meilisearch.sdk.model.Pagination();
				meiliPagination.setMaxTotalHits(this.pagination.maxTotalHits());
				settings.setPagination(meiliPagination);
			}
			if (filterableAttributes != null) {
				settings.setFilterableAttributes(filterableAttributes);
			}
			if (synonyms != null && synonyms.length > 0) {
				// Convert Synonym[] to HashMap<String, String[]>
				HashMap<String, String[]> synonymMap = new HashMap<>();
				for (Synonym synonym : synonyms) {
					synonymMap.put(synonym.word(), synonym.synonyms());
				}
				settings.setSynonyms(synonymMap);
			}
			if (typoTolerance != null) {
				var meiliTypoTolerance = new com.meilisearch.sdk.model.TypoTolerance();
				meiliTypoTolerance.setEnabled(typoTolerance.enabled());
				var minWordSizeForTypos = new HashMap<String, Integer>();
				minWordSizeForTypos.put("oneTypo", typoTolerance.minWordSizeForTypos().oneTypo());
				minWordSizeForTypos.put("twoTypos", typoTolerance.minWordSizeForTypos().twoTypos());
				meiliTypoTolerance.setMinWordSizeForTypos(minWordSizeForTypos);
				meiliTypoTolerance.setDisableOnWords(typoTolerance.disableOnWords());
				meiliTypoTolerance.setDisableOnAttributes(typoTolerance.disableOnAttributes());
				settings.setTypoTolerance(meiliTypoTolerance);
			}
			if (faceting != null) {
				var meliFaceting = new com.meilisearch.sdk.model.Faceting();
				meliFaceting.setMaxValuesPerFacet(faceting.maxValuesPerFacet());
				settings.setFaceting(meliFaceting);
			}
			if (dictionary != null) {
				settings.setDictionary(dictionary);
			}
			if (proximityPrecision != null) {
				settings.setProximityPrecision(proximityPrecision);
			}
			if (searchCutoffMs != null && searchCutoffMs != -1) {
				settings.setSearchCutoffMs(searchCutoffMs);
			}
			if (separatorTokens != null) {
				settings.setSeparatorTokens(separatorTokens);
			}
			if (nonSeparatorTokens != null) {
				settings.setNonSeparatorTokens(nonSeparatorTokens);
			}

			return settings;
		}
	}
}
