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
package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.msc.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.entities.Movie;

/**
 * HTTP-level tests for version-aware document ID retrieval.
 *
 * @author Junghoon Ban
 */
class MeilisearchBulkRetrievalUnitTests {

	private final ObjectMapper mapper = new ObjectMapper();
	private final AtomicInteger versionRequests = new AtomicInteger();
	private final AtomicInteger fetchRequests = new AtomicInteger();
	private final AtomicInteger directRequests = new AtomicInteger();
	private final List<Integer> batchSizes = new ArrayList<>();
	private HttpServer server;
	private String serverVersion = "1.14.0";
	private boolean denyVersion;
	private boolean failVersion;
	private boolean denyFetch;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", this::handle);
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test // GH-229
	void retrievesRequestedIdsInOrderWithOneVersionProbeAndBatchedRequests() {
		MeilisearchTemplate template = template();
		List<String> ids = IntStream.range(0, 1101).mapToObj(String::valueOf).toList();

		List<Movie> movies = template.multiGet(Movie.class, ids);

		assertThat(movies).extracting(Movie::getId).containsExactlyElementsOf(IntStream.range(0, 1101).boxed().toList());
		assertThat(batchSizes).containsExactly(500, 500, 101);
		assertThat(fetchRequests).hasValue(3);
		assertThat(directRequests).hasValue(0);
		assertThat(versionRequests).hasValue(1);

		assertThat(template.multiGet(Movie.class, List.of("7", "9999", "1", "7"), 0, 4)).extracting(Movie::getId)
				.containsExactly(7, 1, 7);
		assertThat(versionRequests).hasValue(1);
		assertThat(fetchRequests).hasValue(4);
		assertThat(template.multiGet(Movie.class, List.of("3", "2", "1", "0"), 1, 2)).extracting(Movie::getId)
				.containsExactly(2, 1);
		assertThat(fetchRequests).hasValue(5);
		assertThat(versionRequests).hasValue(1);
	}

	@Test // GH-229
	void usesDirectIdsImmediatelyBeforeBulkRetrievalSupport() {
		serverVersion = "1.13.3";
		MeilisearchTemplate template = template();

		assertThat(template.multiGet(Movie.class, List.of("23", "9999", "1"))).extracting(Movie::getId).containsExactly(23,
				1);
		assertThat(versionRequests).hasValue(1);
		assertThat(fetchRequests).hasValue(0);
		assertThat(directRequests).hasValue(3);
	}

	@Test // GH-229
	void preservesDirectLookupWhenVersionPermissionIsMissing() {
		denyVersion = true;
		MeilisearchTemplate template = template();

		assertThat(template.multiGet(Movie.class, List.of("23", "9999", "1"))).extracting(Movie::getId).containsExactly(23,
				1);
		assertThat(template.multiGet(Movie.class, List.of("1", "23"))).extracting(Movie::getId).containsExactly(1, 23);
		assertThat(versionRequests).hasValue(1);
		assertThat(fetchRequests).hasValue(0);
		assertThat(directRequests).hasValue(5);
	}

	@Test // GH-229
	void doesNotCacheTransientVersionFailuresAsLegacyCapability() {
		failVersion = true;
		MeilisearchTemplate template = template();

		assertThatThrownBy(() -> template.multiGet(Movie.class, List.of("1", "2")))
				.isInstanceOf(UncategorizedMeilisearchException.class);
		assertThat(directRequests).hasValue(0);
		failVersion = false;
		assertThat(template.multiGet(Movie.class, List.of("1", "2"))).extracting(Movie::getId).containsExactly(1, 2);
		assertThat(versionRequests).hasValue(2);
		assertThat(fetchRequests).hasValue(1);
	}

	@Test // GH-229
	void translatesBulkFetchApiErrors() {
		denyFetch = true;
		MeilisearchTemplate template = template();

		assertThatThrownBy(() -> template.multiGet(Movie.class, List.of("1", "2")))
				.isInstanceOf(UncategorizedMeilisearchException.class).hasMessageContaining("The API key is invalid");
		assertThat(fetchRequests).hasValue(1);
		assertThat(directRequests).hasValue(0);
	}

	private MeilisearchTemplate template() {
		ClientConfiguration config = ClientConfiguration.builder()
				.connectedTo("http://127.0.0.1:" + server.getAddress().getPort()).withApiKey("test-key").build();
		return new MeilisearchTemplate(new MeilisearchClient(config));
	}

	private void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		if (path.equals("/version")) {
			versionRequests.incrementAndGet();
			if (failVersion) {
				respond(exchange, 500, """
						{"message":"Unavailable","code":"internal","type":"internal","link":""}
						""");
				return;
			}
			if (denyVersion) {
				respond(exchange, 403, """
						{"message":"The API key is invalid","code":"invalid_api_key","type":"auth","link":""}
						""");
			} else {
				respond(exchange, 200, mapper.writeValueAsString(Map.of("pkgVersion", serverVersion)));
			}
			return;
		}
		if (path.equals("/indexes/movies/documents/fetch")) {
			fetchRequests.incrementAndGet();
			if (denyFetch) {
				respond(exchange, 403, """
						{"message":"The API key is invalid","code":"invalid_api_key","type":"auth","link":""}
						""");
				return;
			}
			JsonNode request = mapper.readTree(exchange.getRequestBody());
			JsonNode ids = request.path("ids");
			batchSizes.add(ids.size());
			assertThat(request.path("limit").asInt()).isEqualTo(ids.size());
			assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
			List<Map<String, Object>> documents = new ArrayList<>();
			for (JsonNode id : ids) {
				if (!id.asText().equals("9999")) {
					documents.add(Map.of("id", Integer.parseInt(id.asText()), "title", "Movie"));
				}
			}
			Collections.reverse(documents);
			respond(exchange, 200, mapper.writeValueAsString(Map.of("results", documents)));
			return;
		}
		if (path.startsWith("/indexes/movies/documents/")) {
			directRequests.incrementAndGet();
			String id = path.substring("/indexes/movies/documents/".length());
			if (id.equals("9999")) {
				respond(exchange, 404, """
						{"message":"Document not found","code":"document_not_found","type":"invalid_request","link":""}
						""");
			} else {
				respond(exchange, 200, mapper.writeValueAsString(Map.of("id", Integer.parseInt(id), "title", "Movie")));
			}
			return;
		}
		respond(exchange, 404, "{}");
	}

	private void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (var output = exchange.getResponseBody()) {
			output.write(bytes);
		}
	}
}
