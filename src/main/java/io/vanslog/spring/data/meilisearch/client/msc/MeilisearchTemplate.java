/*
 * Copyright 2023-2026 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.client.msc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.FacetSearchRequest;
import com.meilisearch.sdk.MultiSearchFederation;
import com.meilisearch.sdk.MultiSearchRequest;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.SimilarDocumentRequest;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.DocumentsQuery;
import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.Searchable;
import com.meilisearch.sdk.model.SimilarDocumentsResults;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;

import io.vanslog.spring.data.meilisearch.DocumentAccessException;
import io.vanslog.spring.data.meilisearch.TaskStatusException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.MeilisearchCallback;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchInstanceOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.document.MeilisearchDocument;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;

/**
 * Meilisearch Java client-backed implementation of {@link MeilisearchOperations}.
 *
 * @author Junghoon Ban
 * @see com.meilisearch.sdk.Client
 * @see com.meilisearch.sdk.Index
 */
public class MeilisearchTemplate implements MeilisearchOperations {

	private final MeilisearchClient meilisearchClient;
	private final MeilisearchConverter meilisearchConverter;
	private final ObjectMapper objectMapper;
	private final RequestConverter requestConverter;
	private final ResponseConverter responseConverter;
	private final InstanceResponseConverter instanceResponseConverter;
	private final MeilisearchInstanceOperations instanceOperations;

	public MeilisearchTemplate(MeilisearchClient meilisearchClient) {
		this(meilisearchClient, null, new ObjectMapper());
	}

	public MeilisearchTemplate(MeilisearchClient meilisearchClient, @Nullable MeilisearchConverter meilisearchConverter) {
		this(meilisearchClient, meilisearchConverter, new ObjectMapper());
	}

	public MeilisearchTemplate(MeilisearchClient meilisearchClient, ObjectMapper objectMapper) {
		this(meilisearchClient, null, objectMapper);
	}

	public MeilisearchTemplate(MeilisearchClient meilisearchClient, @Nullable MeilisearchConverter meilisearchConverter,
			ObjectMapper objectMapper) {

		Assert.notNull(meilisearchClient, "MeilisearchClient must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");

		this.meilisearchClient = meilisearchClient;
		this.meilisearchConverter = meilisearchConverter != null ? meilisearchConverter
				: new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		this.objectMapper = objectMapper;
		this.requestConverter = new RequestConverter();
		this.responseConverter = new ResponseConverter(this.meilisearchConverter, objectMapper);
		this.instanceResponseConverter = new InstanceResponseConverter(meilisearchClient.getJsonHandler());
		this.instanceOperations = new MeilisearchInstanceTemplate(this::execute, instanceResponseConverter);
	}

	@Override
	public MeilisearchInstanceOperations instanceOps() {
		return instanceOperations;
	}

	@Override
	public MeilisearchIndexOperations indexOps(Class<?> entityClass) {

		Assert.notNull(entityClass, "Entity class must not be null");
		return indexOps(getIndexUidFor(entityClass));
	}

	@Override
	public MeilisearchIndexOperations indexOps(String indexUid) {

		Assert.hasText(indexUid, "Index uid must not be empty");
		return new MeilisearchIndexTemplate(indexUid, this::execute, instanceResponseConverter,
				meilisearchClient.getRequestTimeout(), meilisearchClient.getRequestInterval());
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
			List<MeilisearchDocument> documents = entities.stream().map(this::toDocument).toList();
			String document = writeJson(documents);
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
			String document = execute(client -> client.index(indexUid).getRawDocument(documentId));
			return readDocument(document, clazz);
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

		String results = execute(client -> client.index(indexUid).getRawDocuments(query));
		return readDocuments(results, clazz);
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

		DocumentsQuery query = new DocumentsQuery();
		query.setOffset(0);
		query.setLimit(0);

		return readTotal(execute(client -> client.index(indexUid).getRawDocuments(query)));
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

	@Override
	public <T, Q extends BaseQuery> SearchHits<T> search(Q query, Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		SearchRequest request = requestConverter.searchRequest(query);
		Searchable result = readSearchResult(execute(client -> client.index(indexUid).rawSearch(request)));
		return responseConverter.mapHits(result, clazz);
	}

	@Override
	public <T, Q extends BaseQuery> SearchHits<T> multiSearch(List<Q> queries, Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		MultiSearchRequest request = requestConverter.multiSearchRequest(queries, indexUid, false);
		Results<MultiSearchResult> results = execute(client -> client.multiSearch(request));
		return responseConverter.mapResults(results, clazz);
	}

	@Override
	public <T, Q extends BaseQuery> SearchHits<T> multiSearch(List<Q> queries, MultiSearchFederation federation,
			Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		MultiSearchRequest request = requestConverter.multiSearchRequest(queries, indexUid, true);
		MultiSearchResult result = execute(client -> client.multiSearch(request, federation));
		return responseConverter.mapResult(result, clazz);
	}

	@Override
	public SearchHits<FacetHit> facetSearch(FacetQuery query, Class<?> clazz) {
		String indexUid = getIndexUidFor(clazz);
		FacetSearchRequest request = requestConverter.searchRequest(query);
		FacetSearchable result = readFacetSearchResult(execute(client -> client.index(indexUid).rawFacetSearch(request)));
		return responseConverter.mapHits(result, FacetHit.class);
	}

	@Override
	public <T> SearchHits<T> similarSearch(SimilarQuery query, Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		SimilarDocumentRequest request = requestConverter.similarSearchRequest(query);
		SimilarDocumentsResults result = execute(client -> client.index(indexUid).searchSimilarDocuments(request));
		return responseConverter.mapResult(result, clazz);
	}

	@Override
	public <T> void applySettings(Class<T> clazz) {
		String indexUid = getIndexUidFor(clazz);
		MeilisearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);
		Settings settings = persistentEntity.getDefaultSettings();

		TaskInfo taskInfo = execute(client -> client.index(indexUid).updateSettings(settings));

		if (!isTaskSucceeded(indexUid, taskInfo)) {
			throw new TaskStatusException(taskInfo.getStatus(), "Failed to apply settings");
		}
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
			if (e instanceof MeilisearchApiException) {
				MeilisearchApiException ex = (MeilisearchApiException) e;
				handleApiException(ex);
			}
			throw new UncategorizedMeilisearchException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Handle the given {@link MeilisearchApiException}.
	 * 
	 * @param e the {@link MeilisearchApiException} to handle
	 */
	private void handleApiException(MeilisearchApiException e) {
		if (e.getCode().equals("document_not_found")) {
			throw new DocumentAccessException(e.getMessage(), e.getCause());
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

	private MeilisearchDocument toDocument(Object entity) {

		MeilisearchDocument document = MeilisearchDocument.create();
		meilisearchConverter.write(entity, document);
		return document;
	}

	private String writeJson(List<MeilisearchDocument> documents) {

		try {
			return objectMapper.writeValueAsString(documents);
		} catch (JsonProcessingException e) {
			throw new UncategorizedMeilisearchException("Failed to write Meilisearch documents.", e);
		}
	}

	private <T> T readDocument(String source, Class<T> clazz) {

		try {
			MeilisearchDocument document = objectMapper.readValue(source, MeilisearchDocument.class);
			return meilisearchConverter.read(clazz, document);
		} catch (IOException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch document.", e);
		}
	}

	private <T> List<T> readDocuments(String source, Class<T> clazz) {

		try {
			JsonNode results = objectMapper.readTree(source).get("results");
			if (results == null || !results.isArray()) {
				throw new UncategorizedMeilisearchException("Failed to read Meilisearch documents results.");
			}
			List<MeilisearchDocument> documents = objectMapper.readerForListOf(MeilisearchDocument.class)
					.readValue(results);
			return documents.stream().map(document -> meilisearchConverter.read(clazz, document)).toList();
		} catch (IOException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch documents.", e);
		}
	}

	private long readTotal(String source) {

		try {
			JsonNode total = objectMapper.readTree(source).get("total");
			if (total == null || !total.canConvertToLong()) {
				throw new UncategorizedMeilisearchException("Failed to read Meilisearch documents total.");
			}
			return total.asLong();
		} catch (IOException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch documents total.", e);
		}
	}

	private Searchable readSearchResult(String source) {

		try {
			return MeilisearchSearchResult.from(objectMapper.readTree(source), objectMapper);
		} catch (IOException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch search result.", e);
		}
	}

	private FacetSearchable readFacetSearchResult(String source) {

		try {
			return MeilisearchFacetSearchResult.from(objectMapper.readTree(source), objectMapper);
		} catch (IOException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch facet search result.", e);
		}
	}

	private <T> String getIndexUidFor(Class<T> clazz) {
		MeilisearchPersistentEntity<?> persistentEntity = getPersistentEntityFor(clazz);
		return persistentEntity.getIndexUid();
	}

	private MeilisearchPersistentEntity<?> getPersistentEntityFor(Class<?> clazz) {
		Document document = clazz.getAnnotation(Document.class);
		Assert.notNull(document, "Given class must be annotated with @Document(indexUid = \"foo\")!");
		Assert.hasText(document.indexUid(), "Given class must be annotated with @Document(indexUid = \"foo\")!");

		return meilisearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	@Override
	public MeilisearchConverter getMeilisearchConverter() {
		return meilisearchConverter;
	}

}
