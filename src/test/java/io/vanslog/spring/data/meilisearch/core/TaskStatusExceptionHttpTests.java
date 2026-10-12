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

import io.vanslog.spring.data.meilisearch.TaskStatusException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.msc.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.entities.Movie;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Verifies task failure diagnostics through the Meilisearch SDK HTTP boundary.
 *
 * @author Junghoon Ban
 */
class TaskStatusExceptionHttpTests {

	private static final int FIRST_TASK_UID = 101;

	@Test
	void reportsCompletedTaskDiagnosticsAcrossTemplateOperations() throws Exception {
		try (TaskServer server = new TaskServer(List.of( //
				TaskOutcome.failed("invalid_document_id", "Document ID contains a slash."), // save
				TaskOutcome.withoutError("canceled"), // apply settings
				TaskOutcome.failed("invalid_document_id", "Document ID contains a slash."), // delete by filter
				TaskOutcome.failed("invalid_index_uid", "The index UID is invalid."), // create index
				TaskOutcome.withoutError("canceled"), // update index
				TaskOutcome.failed("invalid_settings", "The settings are invalid."), // update settings
				TaskOutcome.withoutError("canceled"), // reset settings
				TaskOutcome.failed("index_not_found", "The index does not exist.")))) {
			MeilisearchTemplate template = newTemplate(server, 1000, 1);

			assertTaskFailure(() -> template.save(new Movie(1, "Title", "Description", new String[] { "Drama" })), 101,
					"failed", "invalid_document_id", "Document ID contains a slash.");
			assertTaskFailure(() -> template.applySettings(Movie.class), 102, "canceled", null, null);
			assertTaskFailure(() -> template.deleteByFilter(Movie.class, "genres = Drama"), 103, "failed",
					"invalid_document_id", "Document ID contains a slash.");

			MeilisearchIndexOperations index = template.indexOps("movies");
			assertTaskFailure(() -> index.create(), 104, "failed", "invalid_index_uid", "The index UID is invalid.");
			assertTaskFailure(() -> index.update(new MeilisearchIndexUpdateRequest("id")), 105, "canceled", null, null);
			assertTaskFailure(() -> index.updateSettings(MeilisearchIndexSettings.builder().build()), 106, "failed",
					"invalid_settings", "The settings are invalid.");
			assertTaskFailure(() -> index.resetSettings(), 107, "canceled", null, null);

			assertThat(template.indexOps("movies").delete()).isFalse();
		}
	}

	@Test
	void keepsTaskWaitTimeoutDistinctFromACompletedFailure() throws Exception {
		try (TaskServer server = new TaskServer(List.of(TaskOutcome.withoutError("enqueued")))) {
			MeilisearchTemplate template = newTemplate(server, 40, 5);

			Throwable failure = catchThrowable(
					() -> template.save(new Movie(1, "Title", "Description", new String[] { "Drama" })));

			assertThat(failure).isInstanceOf(UncategorizedMeilisearchException.class)
					.isNotInstanceOf(TaskStatusException.class);
		}
	}

	@Test // GH-219
	void rejectsMissingFinalTaskResponseAsAnUnconfirmedOutcome() throws Exception {
		AtomicInteger taskReads = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				boolean readTask = exchange.getRequestURI().getPath().equals("/tasks/" + FIRST_TASK_UID);
				String json = readTask
						? taskReads.incrementAndGet() == 1 ? "{\"uid\":" + FIRST_TASK_UID + ",\"status\":\"succeeded\"}" : "null"
						: "{\"taskUid\":" + FIRST_TASK_UID + ",\"status\":\"enqueued\"}";
				TaskServer.respond(exchange, readTask ? 200 : 202, json);
			}
		});
		server.start();
		try {
			ClientConfiguration configuration = ClientConfiguration.builder()
					.connectedTo("http://127.0.0.1:" + server.getAddress().getPort()).withApiKey("test-key")
					.withRequestTimeout(1000).withRequestInterval(1).build();
			MeilisearchTemplate template = new MeilisearchTemplate(new MeilisearchClient(configuration));

			Throwable failure = catchThrowable(
					() -> template.save(new Movie(1, "Title", "Description", new String[] { "Drama" })));

			assertThat(failure).isInstanceOf(UncategorizedMeilisearchException.class)
					.isNotInstanceOf(TaskStatusException.class).hasMessageContaining(Integer.toString(FIRST_TASK_UID));
		} finally {
			server.stop(0);
		}
	}

	private static MeilisearchTemplate newTemplate(TaskServer server, int requestTimeout, int requestInterval) {
		ClientConfiguration configuration = ClientConfiguration.builder().connectedTo(server.hostUrl())
				.withApiKey("test-key").withRequestTimeout(requestTimeout).withRequestInterval(requestInterval).build();
		return new MeilisearchTemplate(new MeilisearchClient(configuration));
	}

	private static void assertTaskFailure(ThrowingCallable operation, int taskUid, String status, String errorCode,
			String serverMessage) {
		Throwable failure = catchThrowable(operation);
		assertThat(failure).isInstanceOf(TaskStatusException.class);

		TaskStatusException exception = (TaskStatusException) failure;
		assertThat(exception.getTaskUid()).isEqualTo(taskUid);
		assertThat(exception.getStatus()).isEqualTo(status);
		assertThat(exception.getErrorCode()).isEqualTo(errorCode);
		if (serverMessage != null) {
			assertThat(exception.getMessage()).contains(serverMessage);
		}
	}

	private static final class TaskServer implements AutoCloseable {

		private final HttpServer server;
		private final List<TaskOutcome> outcomes;
		private final AtomicInteger submittedTasks = new AtomicInteger();

		TaskServer(List<TaskOutcome> outcomes) throws IOException {
			this.outcomes = outcomes;
			this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			this.server.createContext("/", this::handle);
			this.server.start();
		}

		String hostUrl() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private void handle(HttpExchange exchange) throws IOException {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				String path = exchange.getRequestURI().getPath();
				if ("GET".equals(exchange.getRequestMethod()) && path.startsWith("/tasks/")) {
					int taskUid = Integer.parseInt(path.substring("/tasks/".length()));
					TaskOutcome outcome = outcomes.get(taskUid - FIRST_TASK_UID);
					respond(exchange, 200, outcome.toTaskJson(taskUid));
					return;
				}

				int taskUid = FIRST_TASK_UID + submittedTasks.getAndIncrement();
				respond(exchange, 202, "{\"taskUid\":" + taskUid + ",\"status\":\"enqueued\"}");
			}
		}

		private static void respond(HttpExchange exchange, int status, String json) throws IOException {
			byte[] body = json.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length);
			try (OutputStream output = exchange.getResponseBody()) {
				output.write(body);
			}
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}

	private record TaskOutcome(String status, String errorCode, String errorMessage) {

		static TaskOutcome failed(String errorCode, String errorMessage) {
			return new TaskOutcome("failed", errorCode, errorMessage);
		}

		static TaskOutcome withoutError(String status) {
			return new TaskOutcome(status, null, null);
		}

		String toTaskJson(int taskUid) {
			String error = errorCode == null ? ""
					: ",\"error\":{\"code\":\"" + errorCode + "\",\"message\":\"" + errorMessage + "\"}";
			return "{\"uid\":" + taskUid + ",\"status\":\"" + status + "\"" + error + "}";
		}
	}
}
