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

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.meilisearch.sdk.IndexSearchRequest;
import com.meilisearch.sdk.MultiSearchRequest;
import com.meilisearch.sdk.model.SimilarDocumentsResults;

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchHitsImpl;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.federation.Federation;
import io.vanslog.spring.data.meilisearch.core.federation.FederationResponse;
import io.vanslog.spring.data.meilisearch.core.mapping.MeilisearchPersistentEntity;
import io.vanslog.spring.data.meilisearch.core.query.BaseQuery;
import io.vanslog.spring.data.meilisearch.core.query.FacetQuery;
import io.vanslog.spring.data.meilisearch.core.query.SimilarQuery;
import reactor.core.publisher.Mono;

/**
 * Cold, finite-response reactive implementations of search endpoints.
 *
 * @author Junghoon Ban
 */
final class ReactiveSearchSupport {

	private final ReactiveHttpTransport transport;
	private final ObjectMapper objectMapper;
	private final ObjectMapper requestObjectMapper;
	private final MeilisearchConverter meilisearchConverter;
	private final RequestConverter requestConverter = new RequestConverter();
	private final ResponseConverter responseConverter;

	ReactiveSearchSupport(ReactiveHttpTransport transport, ObjectMapper objectMapper,
			MeilisearchConverter meilisearchConverter, ObjectMapper requestObjectMapper) {

		Assert.notNull(transport, "ReactiveHttpTransport must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.notNull(meilisearchConverter, "MeilisearchConverter must not be null");
		Assert.notNull(requestObjectMapper, "Request ObjectMapper must not be null");

		this.transport = transport;
		this.objectMapper = objectMapper;
		this.requestObjectMapper = requestObjectMapper;
		this.meilisearchConverter = meilisearchConverter;
		this.responseConverter = new ResponseConverter(meilisearchConverter, objectMapper);
	}

	<T, Q extends BaseQuery> Mono<SearchHits<T>> search(Q query, Class<T> clazz) {
		return Mono.defer(() -> {
			String indexUid = getIndexUidFor(clazz);
			String path = indexPath(indexUid, "search");
			String body = toSearchJson(requestConverter.searchRequest(query)).toString();
			return transport.request("POST", path, body)
					.map(response -> responseConverter.mapHits(MeilisearchSearchResult.from(response, objectMapper), clazz));
		});
	}

	<T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries, Class<T> clazz) {
		return Mono.defer(() -> {
			String indexUid = getIndexUidFor(clazz);
			MultiSearchRequest request = requestConverter.multiSearchRequest(queries, indexUid, false);
			return transport.request("POST", "/multi-search", toMultiSearchJson(request).toString())
					.map(response -> mapMultiSearchResults(response, clazz));
		});
	}

	<T, Q extends BaseQuery> Mono<SearchHits<T>> multiSearch(List<Q> queries, Federation federation, Class<T> clazz) {
		return Mono.defer(() -> {
			String indexUid = getIndexUidFor(clazz);
			MultiSearchRequest request = requestConverter.multiSearchRequest(queries, indexUid, true);
			ObjectNode body = toMultiSearchJson(request);
			body.set("federation", requestObjectMapper.valueToTree(federation));
			return transport.request("POST", "/multi-search", body.toString())
					.map(response -> mapFederatedSearch(response, clazz));
		});
	}

	Mono<SearchHits<FacetHit>> facetSearch(FacetQuery query, Class<?> clazz) {
		return Mono.defer(() -> {
			String path = indexPath(getIndexUidFor(clazz), "facet-search");
			String body = toSearchJson(requestConverter.searchRequest(query)).toString();
			return transport.request("POST", path, body).map(response -> responseConverter
					.mapHits(MeilisearchFacetSearchResult.from(response, objectMapper), FacetHit.class));
		});
	}

	<T> Mono<SearchHits<T>> similarSearch(SimilarQuery query, Class<T> clazz) {
		return Mono.defer(() -> {
			String path = indexPath(getIndexUidFor(clazz), "similar");
			String body = toSearchJson(requestConverter.similarSearchRequest(query)).toString();
			return transport.request("POST", path, body).map(response -> responseConverter
					.mapResult(objectMapper.convertValue(response, SimilarDocumentsResults.class), clazz));
		});
	}

	private <T> SearchHits<T> mapMultiSearchResults(JsonNode response, Class<T> clazz) {

		JsonNode results = response.path("results");
		if (!results.isArray()) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch multi-search results.");
		}

		List<SearchHit<T>> hits = new ArrayList<>();
		int maxProcessingTime = 0;
		for (JsonNode resultJson : results) {
			MeilisearchSearchResult result = MeilisearchSearchResult.from(resultJson, objectMapper);
			hits.addAll(responseConverter.mapHitList(result, clazz));
			maxProcessingTime = Math.max(maxProcessingTime, result.getProcessingTimeMs());
		}

		return new SearchHitsImpl<>(Duration.ofMillis(maxProcessingTime), hits);
	}

	private <T> SearchHits<T> mapFederatedSearch(JsonNode response, Class<T> clazz) {

		MeilisearchSearchResult result = MeilisearchSearchResult.from(response, objectMapper);
		Object facetDistribution = readFederatedFacetDistribution(response, result.getFacetDistribution());
		List<SearchHit<T>> hits = new ArrayList<>(result.getHits().size());

		for (Map<String, Object> hit : result.getHits()) {
			Map<String, Object> source = new LinkedHashMap<>(hit);
			Object federationValue = source.remove("_federation");
			FederationResponse federation = federationValue == null ? null
					: objectMapper.convertValue(federationValue, FederationResponse.class);
			hits.add(new SearchHit<>(readHit(source, clazz), result.getProcessingTimeMs(), result.getQuery(),
					result.getFacetStats(), facetDistribution, federation));
		}

		return new SearchHitsImpl<>(Duration.ofMillis(result.getProcessingTimeMs()), hits);
	}

	@Nullable
	private Object readFederatedFacetDistribution(JsonNode response, @Nullable Object mergedFacetDistribution) {

		JsonNode facetsByIndex = response.get("facetsByIndex");
		if (facetsByIndex == null || facetsByIndex.isNull()) {
			return mergedFacetDistribution;
		}

		Map<String, Object> facetMetadata = new LinkedHashMap<>();
		if (mergedFacetDistribution != null) {
			facetMetadata.put("facetDistribution", mergedFacetDistribution);
		}
		facetMetadata.put("facetsByIndex", objectMapper.convertValue(facetsByIndex, Object.class));
		return facetMetadata;
	}

	private <T> T readHit(Map<String, Object> hit, Class<T> clazz) {

		if (FacetHit.class.equals(clazz)) {
			return objectMapper.convertValue(hit, clazz);
		}

		io.vanslog.spring.data.meilisearch.core.document.Document document = io.vanslog.spring.data.meilisearch.core.document.Document
				.create();
		document.putAll(hit);
		return meilisearchConverter.read(clazz, document);
	}

	private ObjectNode toSearchJson(Object request) {

		ObjectNode requestJson = requestObjectMapper.valueToTree(request);
		// Meilisearch uses "filter" for both forms; filterArray is only a separate SDK DTO field.
		// Match SDK single-search serialization by letting the nested array replace the string form.
		JsonNode filterArray = requestJson.remove("filterArray");
		if (filterArray != null && !filterArray.isNull()) {
			requestJson.set("filter", filterArray);
		}
		return requestJson;
	}

	private ObjectNode toMultiSearchJson(MultiSearchRequest request) {

		ObjectNode requestJson = requestObjectMapper.createObjectNode();
		ArrayNode queries = requestJson.putArray("queries");
		for (IndexSearchRequest query : request.getQueries()) {
			queries.add(toSearchJson(query));
		}
		return requestJson;
	}

	private String getIndexUidFor(Class<?> clazz) {

		Assert.notNull(clazz, "Class must not be null");
		Document document = clazz.getAnnotation(Document.class);
		Assert.notNull(document, "Given class must be annotated with @Document(indexUid = \"foo\")!");
		Assert.hasText(document.indexUid(), "Given class must be annotated with @Document(indexUid = \"foo\")!");

		MeilisearchPersistentEntity<?> persistentEntity = meilisearchConverter.getMappingContext()
				.getRequiredPersistentEntity(clazz);
		return persistentEntity.getIndexUid();
	}

	private String indexPath(String indexUid, String operation) {
		return "/indexes/" + ReactiveHttpTransport.encodePathSegment(indexUid) + "/" + operation;
	}
}
