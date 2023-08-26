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

import io.vanslog.spring.data.meilisearch.IndexAccessException;
import io.vanslog.spring.data.meilisearch.TaskStatusException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
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

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
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

	private final Client client;
	private final MeilisearchConverter meilisearchConverter;

	private final JsonHandler jsonHandler;

	public MeilisearchTemplate(Client client, JsonHandler jsonHandler) {
		this(client, null, jsonHandler);
	}

	public MeilisearchTemplate(Client client, @Nullable MeilisearchConverter meilisearchConverter,
			@Nullable JsonHandler jsonHandler) {
		this.client = client;
		this.meilisearchConverter = meilisearchConverter != null ? meilisearchConverter
				: new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		this.jsonHandler = jsonHandler != null ? jsonHandler : new GsonJsonHandler();
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

		try {
			TaskInfo taskInfo = index.addDocuments(jsonHandler.encode(entities), primaryKey);
			int taskUid = taskInfo.getTaskUid();
			index.waitForTask(taskUid);
			TaskStatus taskStatus = index.getTask(taskUid).getStatus();

			if (taskStatus != TaskStatus.SUCCEEDED) {
				throw new TaskStatusException(taskStatus, "Failed to save entities.");
			}
		} catch (RuntimeException | MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to save entities.", e);
		}
		return entities;
	}

	@Override
	@Nullable
	public <T> T get(String documentId, Class<T> clazz) {
		Index index = getIndexFor(clazz);
		try {
			return index.getDocument(documentId, clazz);
		} catch (MeilisearchException e) {
			MeilisearchApiException ex = (MeilisearchApiException) e;
			if (ex.getCode().equals("document_not_found")) {
				return null;
			}
			throw new UncategorizedMeilisearchException("Failed to get entity.", e);
		}
	}

	@Override
	public <T> List<T> multiGet(Class<T> clazz) {
		Index index = getIndexFor(clazz);
		try {
			return Arrays.asList(index.getDocuments(clazz).getResults());
		} catch (RuntimeException | MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to get entities.", e);
		}
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
		try {
			return index.getDocuments(clazz).getTotal();
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to count entities.", e);
		}
	}

	@Override
	public boolean delete(String documentId, Class<?> clazz) {
		Index index = getIndexFor(clazz);
		try {
			TaskInfo taskInfo = index.deleteDocument(documentId);
			int taskUid = taskInfo.getTaskUid();
			index.waitForTask(taskUid);
			TaskStatus taskStatus = index.getTask(taskUid).getStatus();

			return taskStatus == TaskStatus.SUCCEEDED;
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to delete entity.", e);
		}
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
		try {
			TaskInfo taskInfo = index.deleteDocuments(documentIds);
			int taskUid = taskInfo.getTaskUid();
			index.waitForTask(taskUid);
			TaskStatus taskStatus = index.getTask(taskUid).getStatus();

			return taskStatus == TaskStatus.SUCCEEDED;
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to delete entities.", e);
		}
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
		try {
			TaskInfo taskInfo = index.deleteAllDocuments();
			int taskUid = taskInfo.getTaskUid();
			index.waitForTask(taskUid);
			TaskStatus taskStatus = index.getTask(taskUid).getStatus();

			return taskStatus == TaskStatus.SUCCEEDED;
		} catch (MeilisearchException e) {
			throw new UncategorizedMeilisearchException("Failed to delete all entities.", e);
		}
	}

	private <T> String getDocumentIdFor(T entity) {
		PersistentProperty<MeilisearchPersistentProperty> idProperty = getPersistentEntityFor(entity.getClass())
				.getIdProperty();
		Assert.notNull(idProperty, "Document must have an id property.");

		try {
			Method getter = idProperty.getGetter();
			Object id = Objects.requireNonNull(getter).invoke(entity);
			return id.toString();
		} catch (Exception e) {
			throw new UncategorizedMeilisearchException("Failed to get id.", e);
		}
	}

	private <T> Index getIndexFor(Class<T> clazz) {
		MeilisearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);
		String uid = persistentEntity.getIndexUid();
		try {
			return client.index(uid);
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
