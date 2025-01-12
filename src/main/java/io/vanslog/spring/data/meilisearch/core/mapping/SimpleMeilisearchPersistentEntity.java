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
	@Nullable private String indexUid;
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
	public Settings getDefaultSettings() {
		if (settingParameter == null) {
			return null;
		}

		return settingParameter.toSettings();
	}

	@Nullable
	private SettingsParameter buildSettingsParameter(Class<?> clazz) {

		SettingsParameter settingsParameter = new SettingsParameter();
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		if (settingAnnotation == null) {
			return null;
		}

		settingsParameter.sortAttributes = settingAnnotation.sortAttributes();
		settingsParameter.distinctAttribute = settingAnnotation.distinctAttribute();
		settingsParameter.searchableAttributes = settingAnnotation.searchableAttributes();
		settingsParameter.displayedAttributes = settingAnnotation.displayedAttributes();
		settingsParameter.rankingRules = settingAnnotation.rankingRules();
		settingsParameter.stopWords = settingAnnotation.stopWords();
		settingsParameter.pagination = settingAnnotation.pagination();
		settingsParameter.filterableAttributes = settingAnnotation.filterableAttributes();
		settingsParameter.synonyms = settingAnnotation.synonyms();
		settingsParameter.typoTolerance = settingAnnotation.typoTolerance();
		settingsParameter.faceting = settingAnnotation.faceting();
		settingsParameter.dictionary = settingAnnotation.dictionary();
		settingsParameter.proximityPrecision = settingAnnotation.proximityPrecision();
		settingsParameter.searchCutoffMs = settingAnnotation.searchCutoffMs();
		settingsParameter.separatorTokens = settingAnnotation.separatorTokens();
		settingsParameter.nonSeparatorTokens = settingAnnotation.nonSeparatorTokens();

		return settingsParameter;
	}

	private static class SettingsParameter {
		@Nullable private String[] sortAttributes;
		@Nullable private String distinctAttribute;
		@Nullable private String[] searchableAttributes;
		@Nullable private String[] displayedAttributes;
		@Nullable private String[] rankingRules;
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

			Optional.ofNullable(sortAttributes).ifPresent(settings::setSortableAttributes);
			Optional.ofNullable(distinctAttribute).ifPresent(settings::setDistinctAttribute);
			Optional.ofNullable(searchableAttributes).ifPresent(settings::setSearchableAttributes);
			Optional.ofNullable(displayedAttributes).ifPresent(settings::setDisplayedAttributes);
			Optional.ofNullable(rankingRules).ifPresent(settings::setRankingRules);
			Optional.ofNullable(stopWords).ifPresent(settings::setStopWords);
			Optional.ofNullable(filterableAttributes).ifPresent(settings::setFilterableAttributes);
			Optional.ofNullable(dictionary).ifPresent(settings::setDictionary);
			Optional.ofNullable(proximityPrecision).ifPresent(settings::setProximityPrecision);
			Optional.ofNullable(separatorTokens).ifPresent(settings::setSeparatorTokens);
			Optional.ofNullable(nonSeparatorTokens).ifPresent(settings::setNonSeparatorTokens);

			Optional.ofNullable(pagination).ifPresent(p -> {
				var meiliPagination = new com.meilisearch.sdk.model.Pagination();
				meiliPagination.setMaxTotalHits(p.maxTotalHits());
				settings.setPagination(meiliPagination);
			});

			Optional.ofNullable(synonyms).filter(s -> s.length > 0).ifPresent(s -> {
				var synonymMap = new HashMap<String, String[]>();
				for (Synonym synonym : s) {
					synonymMap.put(synonym.word(), synonym.synonyms());
				}
				settings.setSynonyms(synonymMap);
			});

			Optional.ofNullable(typoTolerance).ifPresent(t -> {
				var meiliTypoTolerance = new com.meilisearch.sdk.model.TypoTolerance();
				meiliTypoTolerance.setEnabled(t.enabled());
				var minWordSizeForTypos = new HashMap<String, Integer>();
				minWordSizeForTypos.put("oneTypo", t.minWordSizeForTypos().oneTypo());
				minWordSizeForTypos.put("twoTypos", t.minWordSizeForTypos().twoTypos());
				meiliTypoTolerance.setMinWordSizeForTypos(minWordSizeForTypos);
				meiliTypoTolerance.setDisableOnWords(t.disableOnWords());
				meiliTypoTolerance.setDisableOnAttributes(t.disableOnAttributes());
				settings.setTypoTolerance(meiliTypoTolerance);
			});

			Optional.ofNullable(faceting).ifPresent(f -> {
				var meliFaceting = new com.meilisearch.sdk.model.Faceting();
				meliFaceting.setMaxValuesPerFacet(f.maxValuesPerFacet());
				settings.setFaceting(meliFaceting);
			});

			Optional.ofNullable(searchCutoffMs).filter(ms -> ms != -1).ifPresent(settings::setSearchCutoffMs);

			return settings;
		}
	}
}
