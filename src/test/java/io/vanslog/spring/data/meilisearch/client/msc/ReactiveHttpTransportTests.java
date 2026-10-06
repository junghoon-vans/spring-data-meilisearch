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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanslog.spring.data.meilisearch.MeilisearchRestException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskTimeoutException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

/**
 * Behavior tests for the reactive Meilisearch HTTP transport.
 *
 * @author Junghoon Ban
 */
class ReactiveHttpTransportTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Test // GH-214
	void requestsAreColdAndEncodeAuthenticatedRoutesAgainstTheConfiguredBasePath() throws Exception {
		try (TestServer server = new TestServer()) {
			AtomicInteger requests = new AtomicInteger();
			AtomicReference<String> rawPath = new AtomicReference<>();
			AtomicReference<String> authorization = new AtomicReference<>();
			AtomicReference<String> userAgent = new AtomicReference<>();
			server.setHandler(exchange -> {
				try (exchange) {
					requests.incrementAndGet();
					rawPath.set(exchange.getRequestURI().getRawPath());
					authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
					userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
					respond(exchange, 200, "{\"found\":true}");
				}
			});

			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url() + "/api/root/", 1000, 20),
					OBJECT_MAPPER);
			String pathSegment = ReactiveHttpTransport.encodePathSegment("movie /+雪");
			Mono<JsonNode> response = transport.request("GET", "/indexes/" + pathSegment, null);

			assertThat(requests).hasValue(0);
			assertThat(response.block(Duration.ofSeconds(2)).path("found").booleanValue()).isTrue();
			assertThat(response.block(Duration.ofSeconds(2)).path("found").booleanValue()).isTrue();
			assertThat(requests).hasValue(2);
			assertThat(rawPath).hasValue("/api/root/indexes/movie%20%2F%2B%E9%9B%AA");
			assertThat(authorization).hasValue("Bearer test-key");
			assertThat(userAgent).hasValue("reactive-transport, behavior-test");
		}
	}

	@Test // GH-214
	void delayedExchangeDoesNotBlockSubscription() throws Exception {
		try (TestServer server = new TestServer()) {
			CountDownLatch received = new CountDownLatch(1);
			CountDownLatch release = new CountDownLatch(1);
			CountDownLatch completed = new CountDownLatch(1);
			AtomicReference<JsonNode> result = new AtomicReference<>();
			AtomicReference<Throwable> failure = new AtomicReference<>();
			server.setHandler(exchange -> {
				try (exchange) {
					received.countDown();
					try {
						if (!release.await(4, TimeUnit.SECONDS)) {
							throw new IOException("Test response was not released");
						}
					} catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						throw new IOException(exception);
					}
					respond(exchange, 200, "{\"ready\":true}");
				}
			});

			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 20), OBJECT_MAPPER);
			long startNanos = System.nanoTime();
			Disposable subscription = transport.request("GET", "/delayed", null).subscribe(result::set, failure::set,
					completed::countDown);
			Duration subscribeDuration = Duration.ofNanos(System.nanoTime() - startNanos);
			try {
				assertThat(subscribeDuration).isLessThan(Duration.ofSeconds(1));
				assertThat(received.await(2, TimeUnit.SECONDS)).isTrue();
				assertThat(completed.await(100, TimeUnit.MILLISECONDS)).isFalse();
			} finally {
				release.countDown();
			}
			assertThat(completed.await(2, TimeUnit.SECONDS)).isTrue();
			assertThat(failure.get()).isNull();
			assertThat(result.get().path("ready").booleanValue()).isTrue();
			subscription.dispose();
		}
	}

	@Test // GH-214
	void writeReturnsFinalSuccessfulTaskAndPreservesRawJsonBody() throws Exception {
		try (TestServer server = new TestServer()) {
			AtomicReference<String> submittedBody = new AtomicReference<>();
			AtomicInteger polls = new AtomicInteger();
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestURI().getPath().equals("/api/writes")) {
						submittedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
						respond(exchange, 202, "{\"taskUid\":23,\"status\":\"enqueued\"}");
					} else {
						int poll = polls.incrementAndGet();
						if (poll == 1) {
							respond(exchange, 200, "{\"uid\":23,\"status\":\"processing\"}");
						} else {
							respond(exchange, 200, "{\"uid\":23,\"status\":\"succeeded\",\"details\":{\"deletedDocuments\":3}}");
						}
					}
				}
			});

			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url() + "/api", 1000, 10),
					OBJECT_MAPPER);
			JsonNode task = transport.write("POST", "/writes", "{\"filter\": [1, 2]}").block(Duration.ofSeconds(2));

			assertThat(task.path("status").textValue()).isEqualTo("succeeded");
			assertThat(task.path("details").path("deletedDocuments").intValue()).isEqualTo(3);
			assertThat(submittedBody).hasValue("{\"filter\": [1, 2]}");
			assertThat(polls).hasValue(2);
		}
	}

	@Test // GH-214
	void taskWaitTimeoutStartsAfterTheWriteReceipt() throws Exception {
		VirtualTimeScheduler clock = VirtualTimeScheduler.create();
		try (TestServer server = new TestServer()) {
			CountDownLatch received = new CountDownLatch(1);
			CountDownLatch release = new CountDownLatch(1);
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						received.countDown();
						try {
							if (!release.await(5, TimeUnit.SECONDS)) {
								throw new IOException("Submission was not released");
							}
						} catch (InterruptedException exception) {
							Thread.currentThread().interrupt();
							throw new IOException(exception);
						}
						respond(exchange, 202, "{\"taskUid\":24,\"status\":\"enqueued\"}");
					} else {
						respond(exchange, 200, "{\"uid\":24,\"status\":\"succeeded\"}");
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 50, 10), OBJECT_MAPPER,
					clock);
			CompletableFuture<JsonNode> result = transport.write("POST", "/writes", "{}").toFuture();
			try {
				assertThat(received.await(2, TimeUnit.SECONDS)).isTrue();
				clock.advanceTimeBy(Duration.ofHours(1));
				assertThat(result).isNotDone();
			} finally {
				release.countDown();
			}
			assertThat(result.get(2, TimeUnit.SECONDS).path("status").textValue()).isEqualTo("succeeded");
		} finally {
			clock.dispose();
		}
	}

	@Test // GH-214
	void failedAndCanceledTasksAreDistinctErrorsWithTaskErrorDetails() throws Exception {
		try (TestServer server = new TestServer()) {
			AtomicReference<String> status = new AtomicReference<>("failed");
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						respond(exchange, 202, "{\"taskUid\":41,\"status\":\"enqueued\"}");
					} else {
						respond(exchange, 200,
								"{\"uid\":41,\"status\":\"" + status.get()
										+ "\",\"error\":{\"code\":\"invalid_document\",\"message\":\"Rejected\","
										+ "\"type\":\"document\",\"link\":\"https://example.test/errors\"}}");
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 10), OBJECT_MAPPER);

			Throwable failed = catchThrowable(() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));
			assertThat(failed).isInstanceOf(ReactiveTaskException.class);
			assertThat(((ReactiveTaskException) failed).getTaskUid()).isEqualTo(41);
			assertThat(((ReactiveTaskException) failed).getStatus()).isEqualTo("failed");
			assertThat(((ReactiveTaskException) failed).getErrorCode()).isEqualTo("invalid_document");
			assertThat(((ReactiveTaskException) failed).getErrorMessage()).isEqualTo("Rejected");
			assertThat(((ReactiveTaskException) failed).getErrorType()).isEqualTo("document");
			assertThat(((ReactiveTaskException) failed).getErrorLink()).isEqualTo("https://example.test/errors");

			status.set("canceled");
			Throwable canceled = catchThrowable(() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));
			assertThat(canceled).isInstanceOf(ReactiveTaskException.class);
			assertThat(((ReactiveTaskException) canceled).getStatus()).isEqualTo("canceled");
		}
	}

	@Test // GH-214
	void submissionHttpErrorsExposeStatusAndErrorCodeWithoutResponsePayload() throws Exception {
		try (TestServer server = new TestServer()) {
			AtomicInteger polls = new AtomicInteger();
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						respond(exchange, 400, "{\"message\":\"invalid input\",\"code\":\"invalid_request\",\"type\":\"request\","
								+ "\"link\":\"https://example.test/errors\"}");
					} else {
						polls.incrementAndGet();
						respond(exchange, 200, "{}");
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 10), OBJECT_MAPPER);

			Throwable failure = catchThrowable(() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));

			assertThat(failure).isInstanceOf(MeilisearchRestException.class);
			MeilisearchRestException restException = (MeilisearchRestException) failure;
			assertThat(restException.getStatusCode()).isEqualTo(400);
			assertThat(restException.getCode()).isEqualTo("invalid_request");
			assertThat(restException.getType()).isEqualTo("request");
			assertThat(restException.getLink()).isEqualTo("https://example.test/errors");
			assertThat(restException.getMessage()).doesNotContain("invalid input");
			assertThat(polls).hasValue(0);
		}
	}

	@Test // GH-214
	void malformedTaskEnvelopesAreRejectedWithoutLeakingTheirPayload() throws Exception {
		try (TestServer server = new TestServer()) {
			server.setHandler(exchange -> {
				try (exchange) {
					respond(exchange, 202, "{\"status\":\"enqueued\",\"secret\":\"do-not-expose\"}");
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 10), OBJECT_MAPPER);

			Throwable failure = catchThrowable(() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));

			assertThat(failure).isInstanceOf(UncategorizedMeilisearchException.class);
			assertThat(failure.getMessage()).doesNotContain("do-not-expose");
		}

		try (TestServer server = new TestServer()) {
			AtomicReference<String> taskStatus = new AtomicReference<>();
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						respond(exchange, 202, "{\"taskUid\":51,\"status\":\"enqueued\"}");
					} else {
						String status = taskStatus.get();
						String taskBody = status == null ? "{\"uid\":51}" : "{\"uid\":51,\"status\":\"" + status + "\"}";
						respond(exchange, 200, taskBody);
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 10), OBJECT_MAPPER);

			Throwable missingStatus = catchThrowable(
					() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));
			assertThat(missingStatus).isInstanceOf(UncategorizedMeilisearchException.class);

			taskStatus.set("unknown-status");
			Throwable unknownStatus = catchThrowable(
					() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));
			assertThat(unknownStatus).isInstanceOf(UncategorizedMeilisearchException.class);
		}
	}

	@Test // GH-214
	void invalidTaskWaitConfigurationFailsBeforeSubmittingWrites() throws Exception {
		try (TestServer server = new TestServer()) {
			AtomicInteger requests = new AtomicInteger();
			server.setHandler(exchange -> {
				try (exchange) {
					requests.incrementAndGet();
					respond(exchange, 202, "{\"taskUid\":60,\"status\":\"enqueued\"}");
				}
			});

			for (ClientConfiguration invalidConfiguration : new ClientConfiguration[] { configuration(server.url(), 0, 10),
					configuration(server.url(), 10, 0) }) {
				ReactiveHttpTransport transport = new ReactiveHttpTransport(invalidConfiguration, OBJECT_MAPPER);
				Throwable failure = catchThrowable(() -> transport.write("POST", "/writes", "{}").block(Duration.ofSeconds(2)));
				assertThat(failure).isInstanceOf(IllegalArgumentException.class);
			}

			assertThat(requests).hasValue(0);
		}
	}

	@Test // GH-214
	void malformedJsonPreservesTheParserCause() throws Exception {
		try (TestServer server = new TestServer()) {
			server.setHandler(exchange -> {
				try (exchange) {
					respond(exchange, 200, "{ malformed");
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 1000, 10), OBJECT_MAPPER);

			Throwable failure = catchThrowable(
					() -> transport.request("GET", "/bad-json", null).block(Duration.ofSeconds(2)));

			assertThat(failure).isInstanceOf(UncategorizedMeilisearchException.class);
			assertThat(failure.getCause()).isInstanceOf(JsonProcessingException.class);
		}
	}

	@Test // GH-214
	void taskTimeoutStopsObservationAndReportsUnknownOutcome() throws Exception {
		VirtualTimeScheduler clock = VirtualTimeScheduler.create();
		try (TestServer server = new TestServer()) {
			AtomicInteger polls = new AtomicInteger();
			CountDownLatch firstPoll = new CountDownLatch(1);
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						respond(exchange, 202, "{\"taskUid\":88,\"status\":\"enqueued\"}");
					} else {
						polls.incrementAndGet();
						firstPoll.countDown();
						respond(exchange, 200, "{\"uid\":88,\"status\":\"processing\"}");
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 80, 1000), OBJECT_MAPPER,
					clock);
			CompletableFuture<JsonNode> result = transport.write("POST", "/writes", "{}").toFuture();
			assertThat(firstPoll.await(2, TimeUnit.SECONDS)).isTrue();
			clock.advanceTimeBy(Duration.ofMillis(80));
			Throwable failure = result.handle((task, error) -> error).get(2, TimeUnit.SECONDS);

			assertThat(failure).isInstanceOf(ReactiveTaskTimeoutException.class);
			assertThat(((ReactiveTaskTimeoutException) failure).getTaskUid()).isEqualTo(88);
			assertThat(((ReactiveTaskTimeoutException) failure).getTimeout()).isEqualTo(Duration.ofMillis(80));
			clock.advanceTimeBy(Duration.ofSeconds(5));
			assertThat(polls).hasValue(1);
		} finally {
			clock.dispose();
		}
	}

	@Test // GH-214
	void cancelingTaskObservationStopsThePendingPollTimer() throws Exception {
		VirtualTimeScheduler clock = VirtualTimeScheduler.create();
		try (TestServer server = new TestServer()) {
			AtomicInteger polls = new AtomicInteger();
			CountDownLatch firstPoll = new CountDownLatch(1);
			server.setHandler(exchange -> {
				try (exchange) {
					if (exchange.getRequestMethod().equals("POST")) {
						respond(exchange, 202, "{\"taskUid\":89,\"status\":\"enqueued\"}");
					} else {
						polls.incrementAndGet();
						firstPoll.countDown();
						respond(exchange, 200, "{\"uid\":89,\"status\":\"processing\"}");
					}
				}
			});
			ReactiveHttpTransport transport = new ReactiveHttpTransport(configuration(server.url(), 2000, 150), OBJECT_MAPPER,
					clock);
			Disposable subscription = transport.write("POST", "/writes", "{}").subscribe();
			assertThat(firstPoll.await(2, TimeUnit.SECONDS)).isTrue();
			subscription.dispose();
			clock.advanceTimeBy(Duration.ofSeconds(10));
			assertThat(polls).hasValue(1);
		} finally {
			clock.dispose();
		}
	}

	private ClientConfiguration configuration(String hostUrl, int requestTimeout, int requestInterval) {
		return ClientConfiguration.builder().connectedTo(hostUrl).withApiKey("test-key")
				.withClientAgents(new String[] { "reactive-transport", "behavior-test" }).withRequestTimeout(requestTimeout)
				.withRequestInterval(requestInterval).build();
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] response = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, response.length);
		exchange.getResponseBody().write(response);
	}

	private static final class TestServer implements AutoCloseable {

		private final HttpServer server;
		private volatile ExchangeHandler handler = exchange -> respond(exchange, 500, "{}");

		TestServer() throws IOException {
			this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			this.server.createContext("/", exchange -> handler.handle(exchange));
			this.server.start();
		}

		String url() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		void setHandler(ExchangeHandler handler) {
			this.handler = handler;
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}

	@FunctionalInterface
	private interface ExchangeHandler {

		void handle(HttpExchange exchange) throws IOException;
	}
}
