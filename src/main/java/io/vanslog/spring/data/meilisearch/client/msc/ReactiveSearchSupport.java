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

import com.fasterxml.jackson.core.type.TypeReference;
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
import io.vanslog.spring.data.meilisearch.core.SearchHitMetadata;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchHitsImpl;
import io.vanslog.spring.data.meilisearch.core.SearchResponseMetadata;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
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

	private static final TypeReference<LinkedHashMap<String, Object>> METADATA_MAP_TYPE = new TypeReference<>() {};

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
			return transport.request("POST", path, body).map(response -> mapSearch(response, clazz));
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
			return transport.request("POST", path, body).map(this::mapFacetSearch);
		});
	}

	<T> Mono<SearchHits<T>> similarSearch(SimilarQuery query, Class<T> clazz) {
		return Mono.defer(() -> {
			String path = indexPath(getIndexUidFor(clazz), "similar");
			String body = toSearchJson(requestConverter.similarSearchRequest(query)).toString();
			return transport.request("POST", path, body).map(response -> mapSimilarSearch(response, clazz));
		});
	}

	private <T> SearchHits<T> mapSearch(JsonNode response, Class<T> clazz) {

		MeilisearchSearchResult result = MeilisearchSearchResult.from(response, objectMapper);
		List<SearchHit<T>> hits = mapHitList(result, clazz, result.getFacetDistribution(), false);
		TotalHitsRelation relation = result.hasTotalHits() ? TotalHitsRelation.EQUAL_TO : TotalHitsRelation.OFF;
		SearchResponseMetadata metadata = readResponseMetadata(response, 0, 0, hits.size());
		return new SearchHitsImpl<>(Duration.ofMillis(result.getProcessingTimeMs()), hits, result.getTotalHits(), relation,
				List.of(metadata));
	}

	private <T> SearchHits<T> mapMultiSearchResults(JsonNode response, Class<T> clazz) {

		JsonNode results = response.path("results");
		if (!results.isArray()) {
			throw new UncategorizedMeilisearchException("Failed to read Meilisearch multi-search results.");
		}

		List<SearchHit<T>> hits = new ArrayList<>();
		List<SearchResponseMetadata> responseMetadata = new ArrayList<>(results.size());
		int maxProcessingTime = 0;
		int queryIndex = 0;
		for (JsonNode resultJson : results) {
			MeilisearchSearchResult result = MeilisearchSearchResult.from(resultJson, objectMapper);
			int firstHitIndex = hits.size();
			List<SearchHit<T>> queryHits = mapHitList(result, clazz, result.getFacetDistribution(), false);
			hits.addAll(queryHits);
			responseMetadata.add(readResponseMetadata(resultJson, queryIndex++, firstHitIndex, queryHits.size()));
			maxProcessingTime = Math.max(maxProcessingTime, result.getProcessingTimeMs());
		}

		return new SearchHitsImpl<>(Duration.ofMillis(maxProcessingTime), hits, hits.size(), TotalHitsRelation.OFF,
				responseMetadata);
	}

	private <T> SearchHits<T> mapFederatedSearch(JsonNode response, Class<T> clazz) {

		MeilisearchSearchResult result = MeilisearchSearchResult.from(response, objectMapper);
		Object facetDistribution = readFederatedFacetDistribution(response, result.getFacetDistribution());
		List<SearchHit<T>> hits = mapHitList(result, clazz, facetDistribution, true);
		SearchResponseMetadata metadata = readResponseMetadata(response, 0, 0, hits.size());
		return new SearchHitsImpl<>(Duration.ofMillis(result.getProcessingTimeMs()), hits, hits.size(),
				TotalHitsRelation.OFF, List.of(metadata));
	}

	private SearchHits<FacetHit> mapFacetSearch(JsonNode response) {

		SearchHits<FacetHit> mapped = responseConverter.mapHits(MeilisearchFacetSearchResult.from(response, objectMapper),
				FacetHit.class);
		SearchResponseMetadata metadata = readResponseMetadata(response, 0, 0, mapped.getSearchHits().size());
		return new SearchHitsImpl<>(mapped.getExecutionDuration(), mapped.getSearchHits(), mapped.getTotalHits(),
				mapped.getTotalHitsRelation(), List.of(metadata));
	}

	private <T> SearchHits<T> mapSimilarSearch(JsonNode response, Class<T> clazz) {

		SimilarDocumentsResults result = objectMapper.convertValue(response, SimilarDocumentsResults.class);
		List<SearchHit<T>> hits = new ArrayList<>(result.getHits().size());
		for (Map<String, Object> rawHit : result.getHits()) {
			SearchHitMetadata metadata = readHitMetadata(rawHit);
			hits.add(new SearchHit<>(readHit(rawHit, clazz), result.getProcessingTimeMs(), "", null, null, null, metadata));
		}

		SearchResponseMetadata metadata = readResponseMetadata(response, 0, 0, hits.size());
		return new SearchHitsImpl<>(Duration.ofMillis(result.getProcessingTimeMs()), hits, hits.size(),
				TotalHitsRelation.OFF, List.of(metadata));
	}

	private <T> List<SearchHit<T>> mapHitList(MeilisearchSearchResult result, Class<T> clazz,
			@Nullable Object facetDistribution, boolean federated) {

		List<SearchHit<T>> hits = new ArrayList<>(result.getHits().size());
		for (Map<String, Object> rawHit : result.getHits()) {
			Map<String, Object> source = federated ? new LinkedHashMap<>(rawHit) : rawHit;
			Object federationValue = federated ? source.remove("_federation") : null;
			FederationResponse federation = federationValue == null ? null
					: objectMapper.convertValue(federationValue, FederationResponse.class);
			SearchHitMetadata metadata = readHitMetadata(source);
			hits.add(new SearchHit<>(readHit(source, clazz), result.getProcessingTimeMs(), result.getQuery(),
					result.getFacetStats(), facetDistribution, federation, metadata));
		}
		return hits;
	}

	private SearchResponseMetadata readResponseMetadata(JsonNode response, int queryIndex, int firstHitIndex,
			int hitCount) {

		return new SearchResponseMetadata(queryIndex, firstHitIndex, hitCount, readInteger(response, "processingTimeMs"),
				readQuery(response), readText(response, "indexUid"), readLong(response, "totalHits"),
				readLong(response, "estimatedTotalHits"), readInteger(response, "page"), readInteger(response, "hitsPerPage"),
				readInteger(response, "totalPages"), readInteger(response, "offset"), readInteger(response, "limit"),
				readObject(response, "facetDistribution"), readObject(response, "facetStats"),
				readObject(response, "facetsByIndex"));
	}

	private SearchHitMetadata readHitMetadata(Map<String, Object> source) {

		Map<String, Object> formattedFields = readMap(source.get("_formatted"));
		Map<String, Object> matchesPosition = readMap(source.get("_matchesPosition"));
		Object rankingScoreValue = source.get("_rankingScore");
		Double rankingScore = rankingScoreValue instanceof Number number ? number.doubleValue() : null;
		Map<String, Object> rankingScoreDetails = readMap(source.get("_rankingScoreDetails"));
		if (formattedFields == null && matchesPosition == null && rankingScore == null && rankingScoreDetails == null) {
			return null;
		}
		return new SearchHitMetadata(formattedFields, matchesPosition, rankingScore, rankingScoreDetails);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private Map<String, Object> readMap(@Nullable Object value) {

		if (!(value instanceof Map<?, ?> source)) {
			return null;
		}

		// These maps come from JSON object fields, whose keys are strings; the metadata constructor snapshots them.
		return (Map<String, Object>) source;
	}

	@Nullable
	private Map<String, Object> readObject(JsonNode response, String field) {

		JsonNode value = response.get(field);
		return value == null || !value.isObject() ? null : objectMapper.convertValue(value, METADATA_MAP_TYPE);
	}

	@Nullable
	private Integer readInteger(JsonNode response, String field) {

		JsonNode value = response.get(field);
		return value == null || !value.canConvertToInt() ? null : value.intValue();
	}

	@Nullable
	private Long readLong(JsonNode response, String field) {

		JsonNode value = response.get(field);
		return value == null || !value.canConvertToLong() ? null : value.longValue();
	}

	@Nullable
	private String readQuery(JsonNode response) {

		String query = readText(response, "query");
		return query != null ? query : readText(response, "facetQuery");
	}

	@Nullable
	private String readText(JsonNode response, String field) {

		JsonNode value = response.get(field);
		return value == null || !value.isTextual() ? null : value.textValue();
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
