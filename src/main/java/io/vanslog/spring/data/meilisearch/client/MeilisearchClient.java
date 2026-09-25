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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Extension of {@link Client} that allows to configure the additional attributes that are required for request timeout
 * and retry handling. And also provides {@link JsonHandler} to convert the request and response body.
 *
 * @author Junghoon Ban
 */
public class MeilisearchClient extends Client {

	private final JsonHandler jsonHandler;
	private final MeilisearchHttpTransport httpTransport;
	private final int requestTimeout;
	private final int requestInterval;

	public MeilisearchClient(ClientConfiguration clientConfiguration) {
		this(clientConfiguration, new GsonJsonHandler());
	}

	public MeilisearchClient(ClientConfiguration clientConfiguration, JsonHandler jsonHandler) {
		this(new Config(clientConfiguration.getHostUrl(), clientConfiguration.getApiKey(), jsonHandler,
				clientConfiguration.getClientAgents()), clientConfiguration, jsonHandler);
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
	 * Fetch raw documents in server sort order. The SDK's {@code DocumentsQuery} does not yet expose sorting. Sorting
	 * documents requires Meilisearch 1.16 or later.
	 *
	 * @param indexUid index containing the documents
	 * @param offset number of documents to skip, or negative to use the server default
	 * @param limit maximum number of documents to return, or negative to use the server default
	 * @param sort sort expressions in {@code attribute:direction} form
	 * @return the raw fetch response
	 * @throws MeilisearchException if the request fails
	 */
	public String getRawDocuments(String indexUid, int offset, int limit, String[] sort) throws MeilisearchException {
		Map<String, Object> request = new HashMap<>();
		if (offset >= 0) {
			request.put("offset", offset);
		}
		if (limit >= 0) {
			request.put("limit", limit);
		}
		request.put("sort", sort);
		return httpTransport.post("/indexes/" + indexUid + "/documents/fetch", request);
	}
}
