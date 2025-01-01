/*
 * Copyright 2023-2025 the original author or authors.
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

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
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
	private final int requestTimeout;
	private final int requestInterval;

	public MeilisearchClient(ClientConfiguration clientConfiguration) {
		this(clientConfiguration, new GsonJsonHandler());
	}

	public MeilisearchClient(ClientConfiguration clientConfiguration, JsonHandler jsonHandler) {

		super(new Config(clientConfiguration.getHostUrl(), clientConfiguration.getApiKey(), jsonHandler,
				clientConfiguration.getClientAgents()));

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
}
