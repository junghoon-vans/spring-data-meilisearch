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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import io.vanslog.spring.data.meilisearch.MeilisearchRestException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskObservationException;
import io.vanslog.spring.data.meilisearch.ReactiveTaskTimeoutException;
import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.client.ClientConfiguration;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Cold, non-blocking HTTP transport for reactive Meilisearch operations.
 *
 * @author Junghoon Ban
 */
final class ReactiveHttpTransport {

	private static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private static final String TASKS_PATH = "/tasks/";
	private static final String MALFORMED_TASK_RESPONSE = "Malformed Meilisearch task response.";

	private final HttpClient client;
	private final ClientConfiguration configuration;
	private final ObjectMapper objectMapper;
	private final String baseUrl;
	private final Scheduler scheduler;

	ReactiveHttpTransport(ClientConfiguration configuration, ObjectMapper objectMapper) {
		this(configuration, objectMapper, Schedulers.parallel());
	}

	ReactiveHttpTransport(ClientConfiguration configuration, ObjectMapper objectMapper, Scheduler scheduler) {
		Assert.notNull(configuration, "ClientConfiguration must not be null");
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		Assert.notNull(scheduler, "Task scheduler must not be null");
		Assert.hasText(configuration.getHostUrl(), "Meilisearch host URL must not be empty");

		this.client = HttpClient.newHttpClient();
		this.configuration = configuration;
		this.objectMapper = objectMapper;
		this.scheduler = scheduler;
		this.baseUrl = removeTrailingSlashes(configuration.getHostUrl());
	}

	/**
	 * Send a cold HTTP request and decode its response as JSON.
	 *
	 * @param method the HTTP method
	 * @param path the path relative to the configured host, including any query string
	 * @param jsonBody the raw JSON request body, or {@literal null} for no body
	 * @return the response body
	 */
	Mono<JsonNode> request(String method, String path, @Nullable String jsonBody) {
		return Mono.defer(() -> Mono
				.fromFuture(() -> client.sendAsync(buildRequest(method, path, jsonBody),
						HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)))
				.flatMap(this::decodeResponse).onErrorMap(this::asLibraryCommunicationException));
	}

	/**
	 * Submit a cold write request and complete only after its task succeeds.
	 *
	 * @param method the HTTP method
	 * @param path the path relative to the configured host
	 * @param jsonBody the raw JSON request body, or {@literal null} for no body
	 * @return the final successful task response
	 */
	Mono<JsonNode> write(String method, String path, @Nullable String jsonBody) {
		return Mono.defer(() -> {
			Duration timeout = taskTimeout();
			Duration interval = taskInterval();
			return request(method, path, jsonBody).flatMap(receipt -> {
				long taskUid = taskUid(receipt);
				readTaskStatus(receipt);
				return observeTask(taskUid, timeout, interval);
			});
		});
	}

	/**
	 * Percent-encode one UTF-8 path segment using RFC 3986 unreserved characters. Unlike {@link java.net.URLEncoder},
	 * this leaves spaces as percent escapes rather than plus signs.
	 *
	 * @param value the path segment
	 * @return the encoded path segment
	 */
	static String encodePathSegment(String value) {
		Assert.notNull(value, "Path segment must not be null");
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		StringBuilder encoded = new StringBuilder(bytes.length);
		for (byte valueByte : bytes) {
			int unsigned = valueByte & 0xff;
			if (isUnreserved(unsigned)) {
				encoded.append((char) unsigned);
			} else {
				encoded.append('%');
				encoded.append(Character.toUpperCase(Character.forDigit(unsigned >>> 4, 16)));
				encoded.append(Character.toUpperCase(Character.forDigit(unsigned & 0x0f, 16)));
			}
		}
		return encoded.toString();
	}

	private Mono<JsonNode> observeTask(long taskUid, Duration timeout, Duration interval) {
		return pollTask(taskUid, interval)
				.onErrorMap(error -> !(error instanceof ReactiveTaskException),
						error -> new ReactiveTaskObservationException(taskUid, error))
				.timeout(timeout, Mono.error(new ReactiveTaskTimeoutException(taskUid, timeout)), scheduler);
	}

	private Mono<JsonNode> pollTask(long taskUid, Duration interval) {
		return request("GET", TASKS_PATH + taskUid, null).flatMap(task -> {
			if (taskUid(task) != taskUid) {
				throw malformedTaskResponse();
			}
			String status = readTaskStatus(task);
			return switch (status.toLowerCase(Locale.ROOT)) {
				case "succeeded" -> Mono.just(task);
				case "failed", "canceled" -> Mono.error(taskException(taskUid, status, task));
				default -> Mono.empty();
			};
		}).repeatWhenEmpty(repeats -> repeats.delayElements(interval, scheduler));
	}

	private HttpRequest buildRequest(String method, String path, @Nullable String jsonBody) {
		Assert.hasText(method, "HTTP method must not be empty");
		Assert.hasText(path, "HTTP path must not be empty");
		HttpRequest.Builder builder = HttpRequest.newBuilder(requestUri(path)).timeout(HTTP_REQUEST_TIMEOUT);
		if (jsonBody != null) {
			builder.header("Content-Type", "application/json");
		}

		String apiKey = configuration.getApiKey();
		if (apiKey != null && !apiKey.isEmpty()) {
			builder.header("Authorization", "Bearer " + apiKey);
		}

		String[] agents = configuration.getClientAgents();
		if (agents != null) {
			String userAgent = Arrays.stream(agents).filter(agent -> agent != null && !agent.isBlank())
					.collect(Collectors.joining(", "));
			if (!userAgent.isEmpty()) {
				builder.header("User-Agent", userAgent);
			}
		}

		HttpRequest.BodyPublisher body = jsonBody == null ? HttpRequest.BodyPublishers.noBody()
				: HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8);
		return builder.method(method, body).build();
	}

	private URI requestUri(String path) {
		String relativePath = path.startsWith("/") ? path : "/" + path;
		return URI.create(baseUrl + relativePath);
	}

	private Mono<JsonNode> decodeResponse(HttpResponse<String> response) {
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw restException(response);
		}
		return Mono.just(readJson(response.body()));
	}

	private MeilisearchRestException restException(HttpResponse<String> response) {
		try {
			JsonNode error = readJson(response.body());
			return new MeilisearchRestException(response.statusCode(), text(error, "code"), text(error, "type"),
					text(error, "link"));
		} catch (UncategorizedMeilisearchException exception) {
			return new MeilisearchRestException(response.statusCode(), null, null, null, exception.getCause());
		}
	}

	private JsonNode readJson(String body) {
		if (body.isBlank()) {
			return NullNode.getInstance();
		}
		try {
			JsonNode node = objectMapper.readTree(body);
			return node == null ? NullNode.getInstance() : node;
		} catch (IOException exception) {
			throw new UncategorizedMeilisearchException("Failed to parse Meilisearch REST response.", exception);
		}
	}

	private RuntimeException asLibraryCommunicationException(Throwable throwable) {
		Throwable cause = Exceptions.unwrap(throwable);
		if (cause instanceof MeilisearchRestException restException) {
			return restException;
		}
		if (cause instanceof UncategorizedMeilisearchException meilisearchException) {
			return meilisearchException;
		}
		return new UncategorizedMeilisearchException("Failed to communicate with the Meilisearch REST API.", cause);
	}

	private long taskUid(JsonNode task) {
		if (task == null || !task.isObject()) {
			throw malformedTaskResponse();
		}
		JsonNode receiptUid = task.get("taskUid");
		JsonNode uid = task.get("uid");
		if (receiptUid == null || receiptUid.isNull()) {
			return numericUid(uid);
		}
		long value = numericUid(receiptUid);
		if (uid != null && !uid.isNull() && numericUid(uid) != value) {
			throw malformedTaskResponse();
		}
		return value;
	}

	private long numericUid(@Nullable JsonNode uid) {
		if (uid == null || !uid.isIntegralNumber() || !uid.canConvertToLong() || uid.longValue() < 0) {
			throw malformedTaskResponse();
		}
		return uid.longValue();
	}

	private String readTaskStatus(JsonNode task) {
		if (task == null || !task.isObject()) {
			throw malformedTaskResponse();
		}
		JsonNode statusNode = task.get("status");
		if (statusNode == null || !statusNode.isTextual() || statusNode.textValue().isBlank()) {
			throw malformedTaskResponse();
		}

		String status = statusNode.textValue();
		String normalized = status.toLowerCase(Locale.ROOT);
		if (!normalized.equals("enqueued") && !normalized.equals("processing") && !normalized.equals("succeeded")
				&& !normalized.equals("failed") && !normalized.equals("canceled")) {
			throw malformedTaskResponse();
		}
		return status;
	}

	private ReactiveTaskException taskException(long taskUid, String status, JsonNode task) {
		JsonNode error = task.get("error");
		return new ReactiveTaskException(taskUid, status, text(error, "code"), text(error, "message"), text(error, "type"),
				text(error, "link"));
	}

	@Nullable
	private String text(@Nullable JsonNode node, String field) {
		if (node == null || !node.isObject()) {
			return null;
		}
		JsonNode value = node.get(field);
		return value != null && value.isTextual() ? value.textValue() : null;
	}

	private UncategorizedMeilisearchException malformedTaskResponse() {
		return new UncategorizedMeilisearchException(MALFORMED_TASK_RESPONSE, null);
	}

	private Duration taskTimeout() {
		int timeout = configuration.getRequestTimeout();
		Assert.isTrue(timeout > 0, "Meilisearch task wait timeout must be greater than zero");
		return Duration.ofMillis(timeout);
	}

	private Duration taskInterval() {
		int interval = configuration.getRequestInterval();
		Assert.isTrue(interval > 0, "Meilisearch task polling interval must be greater than zero");
		return Duration.ofMillis(interval);
	}

	private static boolean isUnreserved(int value) {
		return value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z' || value >= '0' && value <= '9' || value == '-'
				|| value == '.' || value == '_' || value == '~';
	}

	private static String removeTrailingSlashes(String hostUrl) {
		int end = hostUrl.length();
		while (end > 0 && hostUrl.charAt(end - 1) == '/') {
			end--;
		}
		return hostUrl.substring(0, end);
	}

}
