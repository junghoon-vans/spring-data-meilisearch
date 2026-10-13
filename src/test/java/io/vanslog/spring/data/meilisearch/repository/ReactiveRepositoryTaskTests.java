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
package io.vanslog.spring.data.meilisearch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanslog.spring.data.meilisearch.ReactiveTaskException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskTimeoutException;
import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.msc.ReactiveMeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.repository.support.ReactiveMeilisearchRepositoryFactory;
import reactor.test.StepVerifier;

/**
 * Exercises accepted server tasks through actual HTTP and the public repository proxy.
 *
 * @author Junghoon Ban
 */
class ReactiveRepositoryTaskTests {

	@Test
	void acceptedSaveTimesOutWithoutEmittingAnEntityOrResubmittingTheWrite() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger submissions = new AtomicInteger();
		server.createContext("/", exchange -> {
			try (exchange) {
				if (exchange.getRequestURI().getPath().equals("/tasks/73")) {
					respond(exchange, 200, "{\"uid\":73,\"status\":\"processing\"}");
				} else {
					submissions.incrementAndGet();
					respond(exchange, 202, "{\"taskUid\":73,\"status\":\"enqueued\"}");
				}
			}
		});
		server.start();
		try {
			TaskRepository repository = repository(server, 100);
			var save = repository.save(new TaskEntry("one", "Pending"));
			assertThat(submissions).hasValue(0);
			StepVerifier.create(save).expectErrorSatisfies(error -> {
				assertThat(error).isInstanceOf(ReactiveTaskTimeoutException.class);
				assertThat(((ReactiveTaskTimeoutException) error).getTaskUid()).isEqualTo(73);
			}).verify(Duration.ofSeconds(5));
			assertThat(submissions).hasValue(1);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void failedAcceptedDeleteTaskIsAnErrorRatherThanSuccessfulVoidCompletion() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger submissions = new AtomicInteger();
		server.createContext("/", exchange -> {
			try (exchange) {
				if (exchange.getRequestURI().getPath().equals("/tasks/74")) {
					respond(exchange, 200,
							"{\"uid\":74,\"status\":\"failed\",\"error\":{\"code\":\"index_not_found\",\"message\":\"Index was removed\"}}");
				} else {
					submissions.incrementAndGet();
					respond(exchange, 202, "{\"taskUid\":74,\"status\":\"enqueued\"}");
				}
			}
		});
		server.start();
		try {
			TaskRepository repository = repository(server, 2000);
			StepVerifier.create(repository.deleteById("one")).expectErrorSatisfies(error -> {
				assertThat(error).isInstanceOf(ReactiveTaskException.class);
				ReactiveTaskException taskError = (ReactiveTaskException) error;
				assertThat(taskError.getTaskUid()).isEqualTo(74);
				assertThat(taskError.getStatus()).isEqualTo("failed");
				assertThat(taskError.getErrorCode()).isEqualTo("index_not_found");
			}).verify(Duration.ofSeconds(5));
			assertThat(submissions).hasValue(1);
		} finally {
			server.stop(0);
		}
	}

	private static TaskRepository repository(HttpServer server, int timeout) {
		ClientConfiguration configuration = ClientConfiguration.builder()
				.connectedTo("http://127.0.0.1:" + server.getAddress().getPort()).withApiKey("").withRequestTimeout(timeout)
				.withRequestInterval(5).build();
		return new ReactiveMeilisearchRepositoryFactory(new ReactiveMeilisearchTemplate(configuration))
				.getRepository(TaskRepository.class);
	}

	private static void respond(HttpExchange exchange, int status, String json) throws IOException {
		byte[] body = json.getBytes(StandardCharsets.UTF_8);
		exchange.getRequestBody().readAllBytes();
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, body.length);
		exchange.getResponseBody().write(body);
	}

	@Document(indexUid = "gh215-repository-task", applySettings = false)
	record TaskEntry(@Id String id, String title) {
	}

	interface TaskRepository extends ReactiveMeilisearchRepository<TaskEntry, String> {

	}

}
