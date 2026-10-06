/*
 * Copyright 2026 the original author or authors.
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.model.Settings;

import io.vanslog.spring.data.meilisearch.MeilisearchRestException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentProperty;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Non-blocking REST-backed implementation of {@link ReactiveMeilisearchOperations}.
 *
 * @author Junghoon Ban
 */
public class ReactiveMeilisearchTemplate implements ReactiveMeilisearchOperations {

	private static final int DEFAULT_BATCH_SIZE = 100;
	private static final String DOCUMENTS_PATH = "/indexes/%s/documents";
	private static final GsonJsonHandler SETTINGS_JSON_HANDLER = new GsonJsonHandler();

	private final MeilisearchConverter meilisearchConverter;
	private final ObjectMapper objectMapper;
	private final ReactiveHttpTransport transport;
	private final ReactiveSearchSupport searchSupport;
	private final int batchSize;

	public ReactiveMeilisearchTemplate(ClientConfiguration clientConfiguration) {
		this(clientConfiguration, null, new ObjectMapper(), DEFAULT_BATCH_SIZE);
	}

	public ReactiveMeilisearchTemplate(ClientConfiguration clientConfiguration,
			@Nullable MeilisearchConverter meilisearchConverter, ObjectMapper objectMapper) {
		this(clientConfiguration, meilisearchConverter, objectMapper, DEFAULT_BATCH_SIZE);
	}

	public ReactiveMeilisearchTemplate(ClientConfiguration clientConfiguration,
			@Nullable MeilisearchConverter meilisearchConverter, ObjectMapper objectMapper, int batchSize) {

		Assert.notNull(clientConfiguration, "ClientConfiguration must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.isTrue(batchSize > 0, "Batch size must be greater than zero");

		this.meilisearchConverter = meilisearchConverter != null ? meilisearchConverter
				: new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		this.objectMapper = objectMapper;
		this.transport = new ReactiveHttpTransport(clientConfiguration, objectMapper);
		this.searchSupport = new ReactiveSearchSupport(transport, objectMapper, this.meilisearchConverter);
		this.batchSize = batchSize;
	}

	@Override
	public <T> Mono<T> save(T entity) {
		return Mono.defer(() -> {
			Assert.notNull(entity, "Entity must not be null");
			return saveBatch(List.of(entity)).thenReturn(entity);
		});
	}

	@Override
	public <T> Flux<T> saveAll(Publisher<T> entities) {
		return Flux.defer(() -> {
			Assert.notNull(entities, "Entities publisher must not be null");
			return Flux.from(entities).buffer(batchSize)
					.concatMap(batch -> saveBatch(batch).thenMany(Flux.fromIterable(batch)), 1);
		});
	}

	@Override
	public <T> Mono<T> get(String documentId, Class<T> clazz) {
		return Mono.defer(() -> {
			Assert.hasText(documentId, "Document ID must not be empty");
			String path = documentsPath(getIndexUidFor(clazz)) + "/" + ReactiveHttpTransport.encodePathSegment(documentId);
			return transport.request("GET", path, null).map(response -> readDocument(response, clazz)).onErrorResume(
					MeilisearchRestException.class,
					error -> "document_not_found".equals(error.getCode()) ? Mono.empty() : Mono.error(error));
		});
	}

	@Override
	public <T> Flux<T> multiGet(Class<T> clazz, Publisher<String> documentIds) {
		return Flux.defer(() -> {
			Assert.notNull(documentIds, "Document IDs publisher must not be null");
			MeilisearchPersistentEntity<?> entity = getPersistentEntityFor(clazz);
			String path = documentsPath(entity.getIndexUid()) + "/fetch";
			String idField = getIdFieldName(entity);
			return Flux.from(documentIds).buffer(batchSize).concatMap(ids -> fetchRequestedIds(path, idField, clazz, ids), 1);
		});
	}

	@Override
	public <T> Flux<T> findAll(Class<T> clazz) {
		return findAll(clazz, Sort.unsorted());
	}

	@Override
	public <T> Flux<T> findAll(Class<T> clazz, Sort sort) {
		return Flux.defer(() -> {
			Assert.notNull(sort, "Sort must not be null");
			MeilisearchPersistentEntity<?> entity = getPersistentEntityFor(clazz);
			String path = documentsPath(entity.getIndexUid()) + "/fetch";
			String[] sortOptions = sortOptions(entity, sort);
			return fetchPage(path, sortOptions, 0)
					.expand(page -> isFinalPage(page) ? Mono.empty() : fetchPage(path, sortOptions, page.offset() + batchSize), 1)
					.concatMap(page -> Flux.fromIterable(page.documents()).map(document -> readDocument(document, clazz)), 0);
		});
	}

	@Override
	public Mono<Boolean> exists(String documentId, Class<?> clazz) {
		return Mono.defer(() -> get(documentId, clazz).map(ignored -> true).defaultIfEmpty(false));
	}

	@Override
	public Mono<Long> count(Class<?> clazz) {
		return countDocuments(clazz, null);
	}

	@Override
	public Mono<Long> count(Class<?> clazz, String filter) {
		return Mono.defer(() -> {
			Assert.hasText(filter, "Filter must not be empty");
			return countDocuments(clazz, filter);
		});
	}

	private Mono<Long> countDocuments(Class<?> clazz, @Nullable String filter) {
		return Mono.defer(() -> {
			String path = documentsPath(getIndexUidFor(clazz)) + "?offset=0&limit=0";
			if (filter != null) {
				path += "&filter=" + encodeQueryParameter(filter);
			}
			return transport.request("GET", path, null).map(this::readTotal);
		});
	}

	@Override
	public Mono<Void> delete(String documentId, Class<?> clazz) {
		return Mono.defer(() -> {
			Assert.hasText(documentId, "Document ID must not be empty");
			String path = documentsPath(getIndexUidFor(clazz)) + "/" + ReactiveHttpTransport.encodePathSegment(documentId);
			return transport.write("DELETE", path, null).then();
		});
	}

	@Override
	public <T> Mono<Void> delete(T entity) {
		return Mono.defer(() -> {
			Assert.notNull(entity, "Entity must not be null");
			return delete(getDocumentId(entity), entity.getClass());
		});
	}

	@Override
	public Mono<Void> deleteAllById(Publisher<String> documentIds, Class<?> clazz) {
		return Flux.defer(() -> {
			Assert.notNull(documentIds, "Document IDs publisher must not be null");
			String path = documentsPath(getIndexUidFor(clazz)) + "/delete-batch";
			return Flux.from(documentIds).buffer(batchSize).concatMap(ids -> deleteIdBatch(path, ids), 1);
		}).then();
	}

	@Override
	public Mono<Void> deleteAll(Class<?> clazz) {
		return Mono.defer(() -> transport.write("DELETE", documentsPath(getIndexUidFor(clazz)), null).then());
	}

	@Override
	public Mono<Long> deleteByFilter(Class<?> clazz, String filter) {
		return Mono.defer(() -> {
			Assert.hasText(filter, "Filter must not be empty");
			Map<String, Object> request = Map.of("filter", filter);
			return transport.write("POST", documentsPath(getIndexUidFor(clazz)) + "/delete", writeJson(request))
					.map(this::readDeletedDocuments);
		});
	}

	@Override
	public <T, Q extends BaseQuery> Mono<SearchHits<T>> search(Q query, Class<T> clazz) {
		return searchSupport.search(query, clazz);
	}

	@Override
	public <T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries, Class<T> clazz) {
		return searchSupport.multiSearch(queries, clazz);
	}

	@Override
	public <T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries,
			io.vanslog.spring.data.meilisearch.core.federation.Federation federation, Class<T> clazz) {
		return searchSupport.multiSearch(queries, federation, clazz);
	}

	@Override
	public Mono<SearchHits<FacetHit>> facetSearch(FacetQuery query, Class<?> clazz) {
		return searchSupport.facetSearch(query, clazz);
	}

	@Override
	public <T> Mono<SearchHits<T>> similarSearch(SimilarQuery query, Class<T> clazz) {
		return searchSupport.similarSearch(query, clazz);
	}

	@Override
	public <T> Mono<Void> applySettings(Class<T> clazz) {
		return Mono.defer(() -> {
			MeilisearchPersistentEntity<?> entity = getPersistentEntityFor(clazz);
			Settings settings = entity.getDefaultSettings();
			if (settings == null) {
				return Mono.empty();
			}
			String json = SETTINGS_JSON_HANDLER.encode(settings);
			String path = "/indexes/" + ReactiveHttpTransport.encodePathSegment(entity.getIndexUid()) + "/settings";
			return transport.write("PATCH", path, json).then();
		});
	}

	@Override
	public MeilisearchConverter getMeilisearchConverter() {
		return meilisearchConverter;
	}

	private <T> Mono<Void> saveBatch(List<T> entities) {
		return Mono.defer(() -> {
			if (entities.isEmpty()) {
				return Mono.empty();
			}

			T first = entities.get(0);
			Assert.notNull(first, "Entity must not be null");
			Class<?> entityClass = first.getClass();
			MeilisearchPersistentEntity<?> entity = getPersistentEntityFor(entityClass);
			String primaryKey = getIdFieldName(entity);
			List<io.vanslog.spring.data.meilisearch.core.document.Document> documents = new ArrayList<>(entities.size());
			for (T value : entities) {
				Assert.notNull(value, "Entity must not be null");
				Assert.isTrue(value.getClass() == entityClass, "All entities in a save batch must have the same runtime type");
				io.vanslog.spring.data.meilisearch.core.document.Document document = io.vanslog.spring.data.meilisearch.core.document.Document
						.create();
				meilisearchConverter.write(value, document);
				documents.add(document);
			}

			String path = documentsPath(entity.getIndexUid()) + "?primaryKey=" + encodeQueryParameter(primaryKey);
			return transport.write("POST", path, writeJson(documents)).then();
		});
	}

	private <T> Flux<T> fetchRequestedIds(String path, String idField, Class<T> clazz, List<String> ids) {
		return Flux.defer(() -> {
			for (String id : ids) {
				Assert.hasText(id, "Document ID must not be empty");
			}
			Map<String, Object> request = new LinkedHashMap<>();
			request.put("ids", ids);
			request.put("limit", ids.size());
			return transport.request("POST", path, writeJson(request)).flatMapMany(response -> {
				List<io.vanslog.spring.data.meilisearch.core.document.Document> documents = readDocuments(response,
						"documents/fetch");
				Map<String, io.vanslog.spring.data.meilisearch.core.document.Document> documentsById = new HashMap<>();
				for (io.vanslog.spring.data.meilisearch.core.document.Document document : documents) {
					String id = documentId(document, idField);
					if (id != null) {
						documentsById.putIfAbsent(id, document);
					}
				}
				List<T> ordered = new ArrayList<>(ids.size());
				for (String id : ids) {
					io.vanslog.spring.data.meilisearch.core.document.Document document = documentsById.get(id);
					if (document != null) {
						ordered.add(readDocument(document, clazz));
					}
				}
				return Flux.fromIterable(ordered);
			});
		});
	}

	private Mono<DocumentPage> fetchPage(String path, @Nullable String[] sort, long offset) {
		return Mono.defer(() -> {
			if (offset > Integer.MAX_VALUE) {
				return Mono.error(
						new UncategorizedMeilisearchException("Meilisearch document offset exceeded the supported integer range."));
			}
			Map<String, Object> request = new LinkedHashMap<>();
			request.put("offset", offset);
			request.put("limit", batchSize);
			if (sort != null && sort.length > 0) {
				request.put("sort", sort);
			}
			return transport.request("POST", path, writeJson(request)).map(response -> readDocumentPage(response, offset));
		});
	}

	private boolean isFinalPage(DocumentPage page) {
		return page.documents().isEmpty() || page.documents().size() < batchSize
				|| page.total() != null && page.offset() + page.documents().size() >= page.total();
	}

	private Mono<Void> deleteIdBatch(String path, List<String> ids) {
		return Mono.defer(() -> {
			for (String id : ids) {
				Assert.hasText(id, "Document ID must not be empty");
			}
			return transport.write("POST", path, writeJson(ids)).then();
		});
	}

	private String[] sortOptions(MeilisearchPersistentEntity<?> entity, Sort sort) {
		List<String> options = new ArrayList<>();
		for (Sort.Order order : sort) {
			MeilisearchPersistentProperty property = entity.getPersistentProperty(order.getProperty());
			String fieldName = property != null ? property.getFieldName() : order.getProperty();
			options.add(fieldName + ":" + (order.isAscending() ? "asc" : "desc"));
		}
		return options.isEmpty() ? null : options.toArray(String[]::new);
	}

	private DocumentPage readDocumentPage(JsonNode response, long offset) {
		List<io.vanslog.spring.data.meilisearch.core.document.Document> documents = readDocuments(response,
				"documents/fetch");
		JsonNode total = response.isObject() ? response.get("total") : null;
		if (total == null || total.isNull()) {
			return new DocumentPage(documents, null, offset);
		}
		if (!total.canConvertToLong() || total.asLong() < 0) {
			throw new UncategorizedMeilisearchException("Malformed Meilisearch documents total.");
		}
		return new DocumentPage(documents, total.asLong(), offset);
	}

	private List<io.vanslog.spring.data.meilisearch.core.document.Document> readDocuments(JsonNode response,
			String description) {
		JsonNode results = response.isArray() ? response : response.isObject() ? response.get("results") : null;
		if (results == null || !results.isArray()) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch " + description + " results.");
		}
		List<io.vanslog.spring.data.meilisearch.core.document.Document> documents = new ArrayList<>(results.size());
		for (JsonNode result : results) {
			if (!result.isObject()) {
				throw new UncategorizedMeilisearchException("Malformed Meilisearch " + description + " document.");
			}
			documents.add(readDocumentMap(result));
		}
		return documents;
	}

	private io.vanslog.spring.data.meilisearch.core.document.Document readDocumentMap(JsonNode source) {
		try {
			return objectMapper.treeToValue(source, io.vanslog.spring.data.meilisearch.core.document.Document.class);
		} catch (JsonProcessingException e) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch document.", e);
		}
	}

	private <T> T readDocument(JsonNode source, Class<T> clazz) {
		return readDocument(readDocumentMap(source), clazz);
	}

	private <T> T readDocument(io.vanslog.spring.data.meilisearch.core.document.Document source, Class<T> clazz) {
		try {
			return meilisearchConverter.read(clazz, source);
		} catch (RuntimeException e) {
			throw new UncategorizedMeilisearchException("Failed to map Meilisearch document.", e);
		}
	}

	private long readTotal(JsonNode response) {
		JsonNode total = response.isObject() ? response.get("total") : null;
		if (total == null || !total.canConvertToLong() || total.asLong() < 0) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch documents total.");
		}
		return total.asLong();
	}

	private long readDeletedDocuments(JsonNode task) {
		JsonNode deletedDocuments = task.path("details").get("deletedDocuments");
		if (deletedDocuments == null || !deletedDocuments.canConvertToLong() || deletedDocuments.asLong() < 0) {
			throw new UncategorizedMeilisearchException("Failed to read deleted-document count from Meilisearch task.");
		}
		return deletedDocuments.asLong();
	}

	private String documentId(io.vanslog.spring.data.meilisearch.core.document.Document document, String idField) {
		Object rawId = document.get(idField);
		if (rawId == null) {
			return null;
		}
		if (rawId instanceof JsonNode jsonId) {
			if (jsonId.isNull() || !jsonId.isValueNode()) {
				return null;
			}
			return jsonId.asText();
		}
		return meilisearchConverter.convertId(rawId);
	}

	private <T> String getDocumentId(T source) {
		@SuppressWarnings("unchecked")
		MeilisearchPersistentEntity<T> entity = (MeilisearchPersistentEntity<T>) getPersistentEntityFor(source.getClass());
		MeilisearchPersistentProperty idProperty = entity.getIdProperty();
		Assert.notNull(idProperty, "Document must have an id property.");
		PersistentPropertyAccessor<T> accessor = entity.getPropertyAccessor(source);
		Object id = accessor.getProperty(idProperty);
		return meilisearchConverter.convertId(Objects.requireNonNull(id, "Document ID must not be null."));
	}

	private String getIdFieldName(MeilisearchPersistentEntity<?> entity) {
		MeilisearchPersistentProperty idProperty = entity.getIdProperty();
		Assert.notNull(idProperty, "Document must have an id property.");
		return idProperty.getFieldName();
	}

	private String getIndexUidFor(Class<?> clazz) {
		return getPersistentEntityFor(clazz).getIndexUid();
	}

	private MeilisearchPersistentEntity<?> getPersistentEntityFor(Class<?> clazz) {
		Assert.notNull(clazz, "Entity class must not be null");
		Document document = clazz.getAnnotation(Document.class);
		Assert.notNull(document, "Given class must be annotated with @Document(indexUid = \"foo\")!");
		Assert.hasText(document.indexUid(), "Given class must be annotated with @Document(indexUid = \"foo\")!");
		return meilisearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	private String documentsPath(String indexUid) {
		return String.format(DOCUMENTS_PATH, ReactiveHttpTransport.encodePathSegment(indexUid));
	}

	private String writeJson(Object source) {
		try {
			return objectMapper.writeValueAsString(source);
		} catch (JsonProcessingException e) {
			throw new UncategorizedMeilisearchException("Failed to serialize Meilisearch request.", e);
		}
	}

	private static String encodeQueryParameter(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private record DocumentPage(List<io.vanslog.spring.data.meilisearch.core.document.Document> documents,
			@Nullable Long total, long offset) {
	}
}
