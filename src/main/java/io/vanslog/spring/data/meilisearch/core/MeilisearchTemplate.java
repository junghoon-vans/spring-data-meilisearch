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

import com.meilisearch.sdk.model.DocumentsQuery;
import io.vanslog.spring.data.meilisearch.DocumentAccessException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.SearchRequest;
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

	private final MeilisearchClient meilisearchClient;
	private final MeilisearchConverter meilisearchConverter;
	private final ObjectMapper objectMapper;

	public MeilisearchTemplate(MeilisearchClient meilisearchClient) {
		this(meilisearchClient, null);
	}

	public MeilisearchTemplate(MeilisearchClient meilisearchClient, @Nullable MeilisearchConverter meilisearchConverter) {

		this.meilisearchClient = meilisearchClient;
		this.meilisearchConverter = meilisearchConverter != null ? meilisearchConverter
				: new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public <T> T save(T entity) {
		this.save(Collections.singletonList(entity));
		return entity;
	}

	@Override
	public <T> List<T> save(List<T> entities) {
		Class<?> clazz = entities.iterator().next().getClass();
		String indexUid = getPersistentEntityFor(clazz).getIndexUid();
		MeilisearchPersistentProperty idProperty = getPersistentEntityFor(clazz).getIdProperty();
		Assert.notNull(idProperty, "Id property must not be null.");
		String primaryKey = Objects.requireNonNull(idProperty.getField()).getName();

		TaskInfo taskInfo = execute(client -> {
			String document = meilisearchClient.getJsonHandler().encode(entities);
			return client.index(indexUid).addDocuments(document, primaryKey);
		});

		if (!isTaskSucceeded(indexUid, taskInfo)) {
			throw new TaskStatusException(taskInfo.getStatus(), "Failed to save entities.");
		}
		return entities;
	}

	@Override
	@Nullable
	public <T> T get(String documentId, Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		try {
			return execute(client -> client.index(indexUid).getDocument(documentId, clazz));
		} catch (DocumentAccessException e) {
			return null;
		}
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz) {
		return multiGet(clazz, -1, -1);
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz, int offset, int limit) {
		String indexUid = getIndexUidFor(clazz);
		DocumentsQuery query = new DocumentsQuery();
		query.setOffset(offset);
		query.setLimit(limit);

		T[] results = execute(client -> client.index(indexUid).getDocuments(query, clazz).getResults());
		return Arrays.asList(results);
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz, List<String> documentIds) {
		return multiGet(clazz, documentIds, -1, -1);
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz, List<String> documentIds, int offset, int limit) {
		List<T> entities = multiGet(clazz, offset, limit);
		return entities.stream().filter(entity -> documentIds.contains(getDocumentIdFor(entity))).toList();
	}

	@Override
	public boolean exists(String documentId, Class<?> clazz) {
		return this.get(documentId, clazz) != null;
	}

	@Override
	public long count(Class<?> clazz) {
		String indexUid = getIndexUidFor(clazz);
		return execute(client -> client.index(indexUid).getDocuments(clazz).getTotal()).longValue();
	}

	@Override
	public boolean delete(String documentId, Class<?> clazz) {
		String indexUid = getIndexUidFor(clazz);
		TaskInfo taskInfo = execute(client -> client.index(indexUid).deleteDocument(documentId));
		return isTaskSucceeded(indexUid, taskInfo);
	}

	@Override
	public <T> boolean delete(T entity) {
		Class<?> clazz = entity.getClass();
		String documentId = getDocumentIdFor(entity);
		return this.delete(documentId, clazz);
	}

	@Override
	public boolean delete(Class<?> clazz, List<String> documentIds) {
		String indexUid = getIndexUidFor(clazz);
		TaskInfo taskInfo = execute(client -> client.index(indexUid).deleteDocuments(documentIds));
		return isTaskSucceeded(indexUid, taskInfo);
	}

	@Override
	public <T> boolean delete(List<T> entities) {
		Class<?> clazz = entities.iterator().next().getClass();
		List<String> documentIds = entities.stream().map(this::getDocumentIdFor).toList();
		return this.delete(clazz, documentIds);
	}

	@Override
	public boolean deleteAll(Class<?> clazz) {
		String indexUid = getIndexUidFor(clazz);
		TaskInfo taskInfo = execute(client -> client.index(indexUid).deleteAllDocuments());
		return isTaskSucceeded(indexUid, taskInfo);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> search(SearchRequest searchRequest, Class<?> clazz) {
		String indexUid = getIndexUidFor(clazz);
		List<HashMap<String, Object>> hits = execute(client -> client.index(indexUid).search(searchRequest)).getHits();

		return hits.stream() //
				.map(hit -> (T) objectMapper.convertValue(hit, clazz)) //
				.toList();
	}

	/**
	 * Execute the given {@link MeilisearchCallback}.
	 * 
	 * @param callback must not be {@literal null}.
	 * @return a result object returned by the action or {@literal null}.
	 * @param <T> the type of the result object
	 */
	public <T> T execute(MeilisearchCallback<T> callback) {

		Assert.notNull(callback, "callback must not be null");

		try {
			return callback.doWithClient(meilisearchClient);
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
	 * @param indexUid the index uid
	 * @param taskInfo the {@link TaskInfo} to check
	 * @return {@literal true} if the task is succeeded
	 */
	private boolean isTaskSucceeded(String indexUid, TaskInfo taskInfo) {
		int taskUid = taskInfo.getTaskUid();

		execute(client -> {
			client.index(indexUid).waitForTask( //
					taskUid, meilisearchClient.getRequestTimeout(), //
					meilisearchClient.getRequestInterval() //
			);
			return null;
		});

		TaskStatus taskStatus = execute(client -> client.index(indexUid).getTask(taskUid).getStatus());
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

	private <T> String getIndexUidFor(Class<T> clazz) {
		MeilisearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);
		return persistentEntity.getIndexUid();
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
