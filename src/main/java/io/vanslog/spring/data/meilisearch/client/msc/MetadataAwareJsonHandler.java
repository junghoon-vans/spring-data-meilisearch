/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.client.msc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.meilisearch.sdk.exceptions.JsonDecodingException;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.JsonHandler;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;

/**
 * Delegates decoding with the SDK's original result types, then captures pagination and exact-total fields omitted by
 * the 0.21 SDK model alongside the decoded results.
 *
 * @author Junghoon Ban
 */
public final class MetadataAwareJsonHandler implements JsonHandler {

	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	private final JsonHandler delegate;

	/**
	 * @param delegate configured codec used to encode requests and decode SDK result objects
	 */
	public MetadataAwareJsonHandler(JsonHandler delegate) {
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
		Results<MultiSearchResult> results = (Results<MultiSearchResult>) delegate.decode(source, clazz, types);
		MultiSearchResult[] responses = results.getResults();
		if (responses == null || responses.length != metadata.size()) {
			throw new JsonDecodingException("The multi-search response metadata did not match its result count.");
		}

		return (T) new MultiSearchResultsWithMetadata(results, metadata);
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
