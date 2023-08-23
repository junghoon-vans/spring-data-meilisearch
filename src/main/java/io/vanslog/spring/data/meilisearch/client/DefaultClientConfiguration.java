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

import org.springframework.lang.Nullable;

import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * Default implementation of {@link ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class DefaultClientConfiguration implements ClientConfiguration {

	private final String hostUrl;
	private final String apiKey;
	private final String[] clientAgents;
	@Nullable private JsonHandler jsonHandler;

	/**
	 * Create a new {@link DefaultClientConfiguration}.
	 *
	 * @param hostUrl the host url
	 * @param apiKey the api key
	 * @param clientAgents the client agents
	 */
	public DefaultClientConfiguration(String hostUrl, String apiKey, String[] clientAgents) {
		this.hostUrl = hostUrl;
		this.apiKey = apiKey;
		this.jsonHandler = null;
		this.clientAgents = clientAgents;
	}

	@Override
	public String getHostUrl() {
		return hostUrl;
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	@Override
	@Nullable
	public JsonHandler getJsonHandler() {
		return jsonHandler;
	}

	@Override
	public String[] getClientAgents() {
		return clientAgents;
	}

	@Override
	public Config getConfig() {
		if (jsonHandler == null) {
			return new Config(hostUrl, apiKey, clientAgents);
		}
		return new Config(hostUrl, apiKey, jsonHandler, clientAgents);
	}

	@Override
	public ClientConfiguration withJsonHandler(JsonHandler jsonHandler) {
		this.jsonHandler = jsonHandler;
		return this;
	}
}
