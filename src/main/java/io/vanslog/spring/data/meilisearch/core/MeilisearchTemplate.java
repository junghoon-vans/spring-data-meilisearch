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

package io.vanslog.spring.data.meilisearch.core;

import io.vanslog.spring.data.meilisearch.DocumentAccessException;
import io.vanslog.spring.data.meilisearch.IndexAccessException;
import io.vanslog.spring.data.meilisearch.TaskStatusException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;

/**
 * Implementation of {@link io.vanslog.spring.data.meilisearch.core.MeilisearchOperations}.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Client
 * @see com.meilisearch.sdk.Index
 */
public class MeilisearchTemplate implements MeilisearchOperations {

	private final MeilisearchClient client;
	private final MeilisearchConverter meilisearchConverter;

	public MeilisearchTemplate(MeilisearchClient client) {
		this.client = client;
		this.meilisearchConverter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
	}

	public MeilisearchTemplate(MeilisearchClient client, MeilisearchConverter meilisearchConverter) {
		this.client = client;
		this.meilisearchConverter = meilisearchConverter;
	}

	@Override
	public <T> T save(T entity) {
		this.save(Collections.singletonList(entity));
		return entity;
	}

	@Override
	public <T> List<T> save(List<T> entities) {
		Class<?> clazz = entities.iterator().next().getClass();
		Index index = getIndexFor(clazz);

		MeilisearchPersistentProperty idProperty = getPersistentEntityFor(clazz).getIdProperty();
		Assert.notNull(idProperty, "Id property must not be null.");
		String primaryKey = Objects.requireNonNull(idProperty.getField()).getName();

		TaskInfo taskInfo = execute(i -> i.addDocuments(client.getJsonHandler().encode(entities), primaryKey), index);

		if (!isTaskSucceeded(index, taskInfo)) {
			throw new TaskStatusException(taskInfo.getStatus(), "Failed to save entities.");
		}
		return entities;
	}

	@Override
	@Nullable
	public <T> T get(String documentId, Class<T> clazz) {
		Index index = getIndexFor(clazz);
		try {
			return execute(i -> i.getDocument(documentId, clazz), index);
		} catch (DocumentAccessException e) {
			return null;
		}
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz) {
		Index index = getIndexFor(clazz);
		T[] results = execute(i -> i.getDocuments(clazz).getResults(), index);
		return Arrays.asList(results);
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz, List<String> documentIds) {
		List<T> entities = multiGet(clazz);
		return entities.stream().filter(entity -> documentIds.contains(getDocumentIdFor(entity))).toList();
	}

	@Override
	public boolean exists(String documentId, Class<?> clazz) {
		return this.get(documentId, clazz) != null;
	}

	@Override
	public long count(Class<?> clazz) {
		Index index = getIndexFor(clazz);
		return execute(i -> i.getDocuments(clazz).getTotal(), index).longValue();
	}

	@Override
	public boolean delete(String documentId, Class<?> clazz) {
		Index index = getIndexFor(clazz);
		TaskInfo taskInfo = execute(i -> i.deleteDocument(documentId), index);
		return isTaskSucceeded(index, taskInfo);
	}

	@Override
	public <T> boolean delete(T entity) {
		Class<?> clazz = entity.getClass();
		String documentId = getDocumentIdFor(entity);
		return this.delete(documentId, clazz);
	}

	@Override
	public boolean delete(Class<?> clazz, List<String> documentIds) {
		Index index = getIndexFor(clazz);
		TaskInfo taskInfo = execute(i -> i.deleteDocuments(documentIds), index);
		return isTaskSucceeded(index, taskInfo);
	}

	@Override
	public <T> boolean delete(List<T> entities) {
		Class<?> clazz = entities.iterator().next().getClass();
		List<String> documentIds = entities.stream().map(this::getDocumentIdFor).toList();
		return this.delete(clazz, documentIds);
	}

	@Override
	public boolean deleteAll(Class<?> clazz) {
		Index index = getIndexFor(clazz);
		TaskInfo taskInfo = execute(Index::deleteAllDocuments, index);
		return isTaskSucceeded(index, taskInfo);
	}

	/**
	 * Execute the given {@link MeilisearchCallback} within the {@link Index} for the given {@link Class}.
	 * 
	 * @param callback must not be {@literal null}.
	 * @param index must not be {@literal null}.
	 * @return a result object returned by the action or {@literal null}.
	 * @param <T> the type of the result object
	 */
	public <T> T execute(MeilisearchCallback<T> callback, Index index) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithIndex(index);
		} catch (MeilisearchException e) {
			MeilisearchApiException ex = (MeilisearchApiException) e;

			if (ex.getCode().equals("document_not_found")) {
				throw new DocumentAccessException(ex.getMessage(), ex.getCause());
			}
			throw new UncategorizedMeilisearchException(ex.getMessage(), ex.getCause());
		}
	}

	/**
	 * Checks if the given {@link TaskInfo} is succeeded.
	 * 
	 * @param index the {@link Index} to check
	 * @param taskInfo the {@link TaskInfo} to check
	 * @return {@literal true} if the task is succeeded
	 */
	private boolean isTaskSucceeded(Index index, TaskInfo taskInfo) {
		int taskUid = taskInfo.getTaskUid();

		execute((MeilisearchCallback<Void>) i -> {
			i.waitForTask(taskUid, client.getRequestTimeout(), client.getRequestInterval());
			return null;
		}, index);

		TaskStatus taskStatus = execute(i -> i.getTask(taskUid).getStatus(), index);
		return taskStatus == TaskStatus.SUCCEEDED;
	}

	private <T> String getDocumentIdFor(T entity) {
		PersistentProperty<MeilisearchPersistentProperty> idProperty = getPersistentEntityFor(entity.getClass())
				.getIdProperty();
		Assert.notNull(idProperty, "Document must have an id property.");

		try {
			Method getter = idProperty.getGetter();
			Object id = Objects.requireNonNull(getter).invoke(entity);
			return getMeilisearchConverter().convertId(id);
		} catch (Exception e) {
			throw new UncategorizedMeilisearchException("Failed to get id.", e);
		}
	}

	private <T> Index getIndexFor(Class<T> clazz) {
		MeilisearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);
		String uid = persistentEntity.getIndexUid();
		try {
			return client.getClient().index(uid);
		} catch (MeilisearchException e) {
			throw new IndexAccessException(uid);
		}
	}

	private MeilisearchPersistentEntity<?> getPersistentEntityFor(Class<?> clazz) {
		Assert.hasText(clazz.getAnnotation(Document.class).indexUid(),
				"Given class must be annotated with @Document(indexUid = \"foo\")!");

		return meilisearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	@Override
	public MeilisearchConverter getMeilisearchConverter() {
		return meilisearchConverter;
	}
}
