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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.json.JsonHandler;

/**
 * FactoryBean class that creates a {@link io.vanslog.spring.data.meilisearch.client.MeilisearchClient}. The Meilisearch
 * client is created by setting the host URL, API key, JSON handler, and client agents.
 *
 * @author Junghoon Ban
 * @see Client
 */
public final class MeilisearchClientFactoryBean implements FactoryBean<MeilisearchClient>, InitializingBean {

	@Nullable private String hostUrl;
	@Nullable private String apiKey;
	@Nullable private JsonHandler jsonHandler;
	private String[] clientAgents;
	private int requestTimeout;
	private int requestInterval;
	@Nullable private MeilisearchClient meilisearchClient;

	private MeilisearchClientFactoryBean() {
		this.clientAgents = new String[0];
	}

	@Override
	public MeilisearchClient getObject() {
		return meilisearchClient;
	}

	@Override
	public Class<? extends Client> getObjectType() {
		return Client.class;
	}

	@Override
	public void afterPropertiesSet() {
		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo(hostUrl).withApiKey(apiKey)
				.withClientAgents(clientAgents).withRequestTimeout(requestTimeout).withRequestInterval(requestInterval).build();

		meilisearchClient = new MeilisearchClient(clientConfiguration, jsonHandler);
	}

	/**
	 * Set the host URL.
	 * 
	 * @param hostUrl the host URL
	 */
	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	/**
	 * Set the API key.
	 * 
	 * @param apiKey the API key
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Set the JSON handler.
	 * 
	 * @param jsonHandler the JSON handler
	 */
	public void setJsonHandler(JsonHandler jsonHandler) {
		this.jsonHandler = jsonHandler;
	}

	/**
	 * Set the client agents.
	 * 
	 * @param clientAgents the client agents
	 */
	public void setClientAgents(String[] clientAgents) {
		this.clientAgents = clientAgents;
	}

	/**
	 * Set request timeout to wait for task to complete.
	 * 
	 * @param requestTimeout in milliseconds
	 */
	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	/**
	 * Set request interval to wait for task to complete.
	 *
	 * @param requestInterval in milliseconds
	 */
	public void setRequestInterval(int requestInterval) {
		this.requestInterval = requestInterval;
	}
}
