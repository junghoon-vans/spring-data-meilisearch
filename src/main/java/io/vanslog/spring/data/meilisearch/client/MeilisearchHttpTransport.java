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
package io.vanslog.spring.data.meilisearch.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.exceptions.APIError;
import com.meilisearch.sdk.exceptions.MeilisearchApiException;
import com.meilisearch.sdk.exceptions.MeilisearchCommunicationException;
import com.meilisearch.sdk.exceptions.MeilisearchTimeoutException;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * HTTP transport for endpoints not exposed by the Java SDK. One instance belongs to each configured client.
 *
 * @author Junghoon Ban
 */
final class MeilisearchHttpTransport {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	private final HttpClient client;
	private final Config config;
	private final JsonHandler jsonHandler;

	MeilisearchHttpTransport(Config config, JsonHandler jsonHandler) {
		this.client = HttpClient.newHttpClient();
		this.config = config;
		this.jsonHandler = jsonHandler;
	}

	String post(String path, Object body) {
		try {
			return body(client.send(postRequest(path, body), HttpResponse.BodyHandlers.ofString()));
		} catch (HttpTimeoutException e) {
			throw new MeilisearchTimeoutException(e);
		} catch (IOException e) {
			throw new MeilisearchCommunicationException(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MeilisearchCommunicationException(e);
		}
	}

	CompletableFuture<HttpResponse<String>> postAsync(String path, Object body) {
		return client.sendAsync(postRequest(path, body), HttpResponse.BodyHandlers.ofString());
	}

	String body(HttpResponse<String> response) {
		if (response.statusCode() >= 400) {
			throw new MeilisearchApiException(jsonHandler.decode(response.body(), APIError.class));
		}
		return response.body();
	}

	private HttpRequest postRequest(String path, Object body) {
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(config.getHostUrl() + path))
				.timeout(REQUEST_TIMEOUT).header("Content-Type", "application/json");
		if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
			builder.header("Authorization", config.getBearerApiKey());
		}
		for (Map.Entry<String, String> header : config.getHeaders().entrySet()) {
			builder.header(header.getKey(), header.getValue());
		}
		return builder.POST(HttpRequest.BodyPublishers.ofString(jsonHandler.encode(body))).build();
	}
}
