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
package io.vanslog.spring.data.meilisearch.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.JsonDecodingException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;

/**
 * Extension of {@link Client} that allows to configure the additional attributes that are required for request timeout
 * and retry handling. And also provides {@link JsonHandler} to convert the request and response body.
 *
 * @author Junghoon Ban
 */
public class MeilisearchClient extends Client {
	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	private final JsonHandler jsonHandler;
	private final MeilisearchHttpTransport httpTransport;
	private final int requestTimeout;
	private final int requestInterval;

	public MeilisearchClient(ClientConfiguration clientConfiguration) {
		this(clientConfiguration, new GsonJsonHandler());
	}

	public MeilisearchClient(ClientConfiguration clientConfiguration, JsonHandler jsonHandler) {
		this(
				new Config(clientConfiguration.getHostUrl(), clientConfiguration.getApiKey(),
						new MetadataAwareJsonHandler(jsonHandler), clientConfiguration.getClientAgents()),
				clientConfiguration, jsonHandler);
	}

	private MeilisearchClient(Config config, ClientConfiguration clientConfiguration, JsonHandler jsonHandler) {

		super(config);

		this.httpTransport = new MeilisearchHttpTransport(config, jsonHandler);
		this.requestTimeout = clientConfiguration.getRequestTimeout();
		this.requestInterval = clientConfiguration.getRequestInterval();
		this.jsonHandler = jsonHandler;
	}

	public JsonHandler getJsonHandler() {
		return jsonHandler;
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public int getRequestInterval() {
		return requestInterval;
	}

	/**
	 * Fetch raw documents by ID through the internal HTTP transport. Native ID-list retrieval requires Meilisearch 1.14
	 * or later.
	 *
	 * @param indexUid index containing the documents
	 * @param documentIds requested document IDs
	 * @return the raw fetch response
	 * @throws MeilisearchException if the request fails
	 */
	public String getRawDocumentsByIds(String indexUid, List<String> documentIds) throws MeilisearchException {
		return httpTransport.post("/indexes/" + indexUid + "/documents/fetch",
				Map.of("ids", documentIds, "limit", documentIds.size()));
	}

	/**
	 * Fetch raw documents through the documents/fetch endpoint. Sorting requires Meilisearch 1.16 or later.
	 *
	 * @param indexUid index containing the documents
	 * @param offset number of documents to skip, or negative to use the server default
	 * @param limit maximum number of documents to return, or negative to use the server default
	 * @param sort sort expressions in {@code attribute:direction} form, or {@literal null} for an unsorted listing
	 * @return the raw fetch response
	 * @throws MeilisearchException if the request fails
	 */
	public String getRawDocuments(String indexUid, int offset, int limit, @Nullable String[] sort)
			throws MeilisearchException {
		Map<String, Object> request = new HashMap<>();
		if (offset >= 0) {
			request.put("offset", offset);
		}
		if (limit >= 0) {
			request.put("limit", limit);
		}
		if (sort != null) {
			request.put("sort", sort);
		}
		return httpTransport.post("/indexes/" + indexUid + "/documents/fetch", request);
	}

	/**
	 * Metadata captured from one non-federated SDK multi-search response.
	 */
	public record MultiSearchResultMetadata(@Nullable Integer processingTimeMs, @Nullable String query,
			@Nullable String indexUid, @Nullable Long totalHits, @Nullable Long estimatedTotalHits, @Nullable Integer page,
			@Nullable Integer hitsPerPage, @Nullable Integer totalPages, @Nullable Integer offset, @Nullable Integer limit) {
	}

	/**
	 * SDK result subtype used to retain fields omitted by the 0.21 SDK model.
	 */
	public static final class MultiSearchResultWithMetadata extends MultiSearchResult {

		@Nullable private MultiSearchResultMetadata responseMetadata;

		public MultiSearchResultWithMetadata() {}

		@Nullable
		public MultiSearchResultMetadata getResponseMetadata() {
			return responseMetadata;
		}

		private void setResponseMetadata(MultiSearchResultMetadata responseMetadata) {
			this.responseMetadata = responseMetadata;
		}
	}

	/**
	 * Delegates the configured codec except for the SDK's non-federated multi-search result type, whose 0.21 model omits
	 * pagination and exact-total fields.
	 */
	private static final class MetadataAwareJsonHandler implements JsonHandler {

		private final JsonHandler delegate;

		private MetadataAwareJsonHandler(JsonHandler delegate) {
			this.delegate = delegate;
		}

		@Override
		public String encode(Object source) throws MeilisearchException {
			return delegate.encode(source);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T decode(Object source, Class<T> clazz, Class<?>... types) throws MeilisearchException {

			if (clazz != Results.class || types == null || types.length != 1 || types[0] != MultiSearchResult.class) {
				return delegate.decode(source, clazz, types);
			}

			List<MultiSearchResultMetadata> metadata = readMultiSearchMetadata(source);
			Results<MultiSearchResult> results = (Results<MultiSearchResult>) delegate.decode(source, Results.class,
					MultiSearchResultWithMetadata.class);
			MultiSearchResult[] responses = results.getResults();
			if (responses == null || responses.length != metadata.size()) {
				throw new JsonDecodingException("The multi-search response metadata did not match its result count.");
			}

			for (int i = 0; i < responses.length; i++) {
				if (responses[i] == null) {
					continue;
				}
				if (!(responses[i] instanceof MultiSearchResultWithMetadata result)) {
					throw new JsonDecodingException(
							"The configured JSON handler did not retain the extended multi-search result type.");
				}
				result.setResponseMetadata(metadata.get(i));
			}
			return (T) results;
		}
	}

	/**
	 * Projects response metadata while streaming past hits, avoiding a second materialized copy of hit documents.
	 */
	private static List<MultiSearchResultMetadata> readMultiSearchMetadata(Object source) {

		if (!(source instanceof String json)) {
			throw new JsonDecodingException("The multi-search response body was not JSON text.");
		}

		List<MultiSearchResultMetadata> metadata = new ArrayList<>();
		try (JsonParser parser = JSON_FACTORY.createParser(json)) {
			if (parser.nextToken() != JsonToken.START_OBJECT) {
				return metadata;
			}
			JsonToken token;
			while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
				if (parser.currentToken() == JsonToken.FIELD_NAME && "results".equals(parser.currentName())) {
					if (parser.nextToken() == JsonToken.START_ARRAY) {
						JsonToken resultToken;
						while ((resultToken = parser.nextToken()) != null && resultToken != JsonToken.END_ARRAY) {
							if (parser.currentToken() == JsonToken.START_OBJECT) {
								metadata.add(readResultMetadata(parser));
							} else {
								parser.skipChildren();
								metadata.add(new MultiSearchResultMetadata(null, null, null, null, null, null, null, null, null, null));
							}
						}
						if (resultToken == null) {
							throw new JsonDecodingException("The multi-search response results array was incomplete.");
						}
					} else {
						parser.skipChildren();
					}
				} else {
					parser.skipChildren();
				}
			}
			if (token == null) {
				throw new JsonDecodingException("The multi-search response object was incomplete.");
			}
			return metadata;
		} catch (IOException e) {
			throw new JsonDecodingException(e);
		}
	}

	private static MultiSearchResultMetadata readResultMetadata(JsonParser parser) throws IOException {

		Integer processingTimeMs = null;
		String query = null;
		String indexUid = null;
		Long totalHits = null;
		Long estimatedTotalHits = null;
		Integer page = null;
		Integer hitsPerPage = null;
		Integer totalPages = null;
		Integer offset = null;
		Integer limit = null;

		JsonToken token;
		while ((token = parser.nextToken()) != null && token != JsonToken.END_OBJECT) {
			if (parser.currentToken() != JsonToken.FIELD_NAME) {
				parser.skipChildren();
				continue;
			}

			String field = parser.currentName();
			JsonToken valueToken = parser.nextToken();
			if (valueToken == null) {
				throw new JsonDecodingException("The multi-search result object was incomplete.");
			}
			switch (field) {
				case "processingTimeMs" -> processingTimeMs = readInteger(parser, valueToken);
				case "query" -> query = readString(parser, valueToken);
				case "indexUid" -> indexUid = readString(parser, valueToken);
				case "totalHits" -> totalHits = readLong(parser, valueToken);
				case "estimatedTotalHits" -> estimatedTotalHits = readLong(parser, valueToken);
				case "page" -> page = readInteger(parser, valueToken);
				case "hitsPerPage" -> hitsPerPage = readInteger(parser, valueToken);
				case "totalPages" -> totalPages = readInteger(parser, valueToken);
				case "offset" -> offset = readInteger(parser, valueToken);
				case "limit" -> limit = readInteger(parser, valueToken);
				default -> parser.skipChildren();
			}
		}
		if (token == null) {
			throw new JsonDecodingException("The multi-search result object was incomplete.");
		}

		return new MultiSearchResultMetadata(processingTimeMs, query, indexUid, totalHits, estimatedTotalHits, page,
				hitsPerPage, totalPages, offset, limit);
	}

	@Nullable
	private static Integer readInteger(JsonParser parser, JsonToken token) throws IOException {
		if (token == JsonToken.VALUE_NUMBER_INT) {
			return parser.getIntValue();
		}
		parser.skipChildren();
		return null;
	}

	@Nullable
	private static Long readLong(JsonParser parser, JsonToken token) throws IOException {
		if (token == JsonToken.VALUE_NUMBER_INT) {
			return parser.getLongValue();
		}
		parser.skipChildren();
		return null;
	}

	@Nullable
	private static String readString(JsonParser parser, JsonToken token) throws IOException {
		if (token == JsonToken.VALUE_STRING) {
			return parser.getText();
		}
		parser.skipChildren();
		return null;
	}
}
