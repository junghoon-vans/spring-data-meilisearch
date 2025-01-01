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
 * Interface for Meilisearch Configuration.
 *
 * @author Junghoon Ban
 */
public interface ClientConfiguration {

	/**
	 * Create a new {@link ClientConfigurationBuilder}.
	 *
	 * @return ClientConfigurationBuilder
	 */
	static ClientConfigurationBuilder builder() {
		return new ClientConfigurationBuilder();
	}

	/**
	 * Get the hostUrl.
	 *
	 * @return hostUrl
	 */
	String getHostUrl();

	/**
	 * Get the apiKey.
	 *
	 * @return apiKey
	 */
	String getApiKey();

	/**
	 * Get the clientAgents.
	 *
	 * @return clientAgents
	 */
	String[] getClientAgents();

	/**
	 * Get the request timeout to wait for task to complete.
	 *
	 * @return requestTimeout in milliseconds
	 */
	int getRequestTimeout();

	/**
	 * Get the request interval to wait for task to complete.
	 *
	 * @return requestInterval in milliseconds
	 */
	int getRequestInterval();
}
