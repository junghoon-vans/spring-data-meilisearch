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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP transport behavior for endpoints not covered by the SDK.
 *
 * @author Junghoon Ban
 */
class MeilisearchHttpTransportTests {

	@Test // GH-229
	void sendsAuthenticatedRequestAsynchronouslyWithoutBlockingTheCaller() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		CountDownLatch received = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		server.createContext("/indexes/movies/documents/fetch", exchange -> {
			try (exchange) {
				assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
				assertThat(exchange.getRequestHeaders().getFirst("User-Agent")).contains("bulk-lookup");
				received.countDown();
				if (!release.await(5, TimeUnit.SECONDS)) {
					throw new IOException("Request was not released");
				}
				byte[] result = "{\"results\":[]}".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, result.length);
				exchange.getResponseBody().write(result);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		});
		server.start();
		try {
			Config config = new Config("http://127.0.0.1:" + server.getAddress().getPort(), "test-key", new GsonJsonHandler(),
					new String[] { "bulk-lookup" });
			MeilisearchHttpTransport transport = new MeilisearchHttpTransport(config, config.getJsonHandler());
			CompletableFuture<HttpResponse<String>> response = transport.postAsync("/indexes/movies/documents/fetch",
					Map.of("ids", new String[] { "1" }));
			assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(response).isNotDone();
			release.countDown();
			assertThat(transport.body(response.get(5, TimeUnit.SECONDS))).isEqualTo("{\"results\":[]}");
		} finally {
			release.countDown();
			server.stop(0);
		}
	}
}
