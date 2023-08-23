/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.mapping.model.BasicPersistentEntity;
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
		if (rawType.isAnnotationPresent(Document.class)) {
			Document document = rawType.getAnnotation(Document.class);
			Assert.hasText(document.indexUid(),
					"Unknown indexUid. Make sure the indexUid is defined." + "e.g @Document(indexUid=\"foo\")");
			this.indexUid = document.indexUid();
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
}
