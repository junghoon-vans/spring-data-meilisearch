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

import com.meilisearch.sdk.model.Settings;
import io.vanslog.spring.data.meilisearch.annotations.Document;

import io.vanslog.spring.data.meilisearch.annotations.Pagination;
import io.vanslog.spring.data.meilisearch.annotations.Setting;
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

/**
 * Meilisearch specific {@link org.springframework.data.mapping.model.BasicPersistentEntity} implementation holding.
 *
 * @param <T>
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentEntity<T> extends BasicPersistentEntity<T, MeilisearchPersistentProperty>
		implements MeilisearchPersistentEntity<T>, ApplicationContextAware {

	private final StandardEvaluationContext context;
	@Nullable private final Document document;
	@Nullable private String indexUid;
	private final Lazy<SettingsParameter> settingParameter;
	private boolean applySettings;

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

		this.settingParameter = Lazy.of(() -> buildSettingParameter(rawType));

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

	private SettingsParameter buildSettingParameter(Class<?> clazz) {

		SettingsParameter settingsParameter = new SettingsParameter();
		Setting settingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Setting.class);

		// default values
		settingsParameter.searchableAttributes = new String[] { "*" };
		settingsParameter.displayedAttributes = new String[] { "*" };
		settingsParameter.rankingRules = new String[] { "words", "typo", "proximity", "attribute", "sort", "exactness" };

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

		String[] sortAttributes = settingAnnotation.sortAttributes();
		String distinctAttribute = settingAnnotation.distinctAttribute();
		String[] stopWords = settingAnnotation.stopWords();

		if (sortAttributes.length > 0) {
			settingsParameter.sortAttributes = settingAnnotation.sortAttributes();
		}

		if (!distinctAttribute.isEmpty()) {
			settingsParameter.distinctAttribute = settingAnnotation.distinctAttribute();
		}

		if (stopWords.length > 0) {
			settingsParameter.stopWords = settingAnnotation.stopWords();
		}
	}

	private static class SettingsParameter {
		@Nullable private String[] sortAttributes;
		@Nullable private String distinctAttribute;
		private String[] searchableAttributes;
		private String[] displayedAttributes;
		private String[] rankingRules;
		@Nullable private String[] stopWords;
		private Pagination pagination;

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

			var meiliPagination = new com.meilisearch.sdk.model.Pagination();
			meiliPagination.setMaxTotalHits(this.pagination.maxTotalHits());
			settings.setPagination(meiliPagination);

			return settings;
		}
	}
}
