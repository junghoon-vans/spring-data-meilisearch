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
import org.springframework.util.Assert;

/**
 * A builder for Meilisearch {@link io.vanslog.spring.data.meilisearch.client.ClientConfiguration}.
 *
 * @author Junghoon Ban
 */
public class ClientConfigurationBuilder {

	@Nullable private String hostUrl;
	@Nullable private String apiKey;
	private String[] clientAgents;

	/**
	 * Create a new {@link ClientConfigurationBuilder}.
	 */
	public ClientConfigurationBuilder() {
		this.clientAgents = new String[0];
	}

	/**
	 * Connect to localhost:7700.
	 *
	 * @return {@link ClientConfigurationBuilder}
	 */
	public ClientConfigurationBuilder connectedToLocalhost() {
		this.hostUrl = "http://localhost:7700";
		return this;
	}

	/**
	 * Connect to the given hostUrl.
	 *
	 * @param hostUrl the hostUrl to connect to
	 * @return {@link ClientConfigurationBuilder}
	 */
	public ClientConfigurationBuilder connectedTo(String hostUrl) {
		this.hostUrl = hostUrl;
		return this;
	}

	/**
	 * Configure API key to access Meilisearch instance.
	 *
	 * @param apiKey the apiKey to use
	 * @return {@link ClientConfigurationBuilder}
	 */
	public ClientConfigurationBuilder withApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	/**
	 * Configure client agents that will be sent with User-Agent header.
	 *
	 * @param clientAgents String array of client agents
	 * @return {@link ClientConfigurationBuilder}
	 */
	public ClientConfigurationBuilder withClientAgents(String[] clientAgents) {
		this.clientAgents = clientAgents;
		return this;
	}

	/**
	 * Build a {@link io.vanslog.spring.data.meilisearch.client.ClientConfiguration} with the given parameters.
	 *
	 * @return {@link io.vanslog.spring.data.meilisearch.client.ClientConfiguration}
	 */
	public ClientConfiguration build() {
		Assert.notNull(this.hostUrl, "Host URL must not be null");
		Assert.notNull(this.apiKey, "API Key must not be null");
		return new DefaultClientConfiguration(this.hostUrl, this.apiKey, this.clientAgents);
	}
}
