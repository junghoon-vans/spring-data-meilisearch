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

/**
 * Default implementation of {@link io.vanslog.spring.data.meilisearch.client.ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class DefaultClientConfiguration implements ClientConfiguration {

	private final String hostUrl;
	private final String apiKey;
	private final String[] clientAgents;
	private final int requestTimeout;
	private final int requestInterval;

	/**
	 * Create a new {@link DefaultClientConfiguration}.
	 *
	 * @param hostUrl the host url
	 * @param apiKey the api key
	 * @param clientAgents the client agents
	 */
	public DefaultClientConfiguration(String hostUrl, String apiKey, String[] clientAgents, int requestTimeout,
			int requestInterval) {
		this.hostUrl = hostUrl;
		this.apiKey = apiKey;
		this.clientAgents = clientAgents;
		this.requestTimeout = requestTimeout;
		this.requestInterval = requestInterval;
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
	public String[] getClientAgents() {
		return clientAgents;
	}

	@Override
	public int getRequestTimeout() {
		return requestTimeout;
	}

	@Override
	public int getRequestInterval() {
		return requestInterval;
	}
}
