/*
 * Copyright 2023 the original author or authors.
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
 * Wrapper class for a Meilisearch client.
 *
 * @author Junghoon Ban
 */
public class MeilisearchClient {

	private final Client client;
	private final JsonHandler jsonHandler;
	private final int timeout;
	private final int interval;

	public MeilisearchClient(ClientConfiguration config) {
		this.client = new Client(new Config(config.getHostUrl(), config.getApiKey(), config.getClientAgents()));
		this.timeout = config.getTimeout();
		this.interval = config.getInterval();
		this.jsonHandler = new GsonJsonHandler();
	}

	public MeilisearchClient(ClientConfiguration config, JsonHandler jsonHandler) {
		this.client = new Client(
				new Config(config.getHostUrl(), config.getApiKey(), jsonHandler, config.getClientAgents()));
		this.timeout = config.getTimeout();
		this.interval = config.getInterval();
		this.jsonHandler = jsonHandler;
	}

	public Client getClient() {
		return client;
	}

	public JsonHandler getJsonHandler() {
		return jsonHandler;
	}

	public int getTimeout() {
		return timeout;
	}

	public int getInterval() {
		return interval;
	}
}
