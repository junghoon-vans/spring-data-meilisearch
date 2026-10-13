/*
 * Copyright 2023-2026 the original author or authors.
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

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.exceptions.MeilisearchException;

/**
 * Supported low-level, SDK-coupled callback executed by
 * {@link io.vanslog.spring.data.meilisearch.client.msc.MeilisearchTemplate#execute(MeilisearchCallback)}. Prefer
 * {@link MeilisearchOperations} and repositories for regular mapped application access.
 * <p>
 * The callback supplies native requests and interprets SDK results. The template does not automatically map entities,
 * wait for task completion, or validate completed task status. SDK {@link MeilisearchException} failures escaping the
 * callback are translated by the template; direct SDK calls outside the template do not receive that translation.
 *
 * @author Junghoon Ban
 * @param <T> return type
 */
@FunctionalInterface
public interface MeilisearchCallback<T> {

	/**
	 * Execute native SDK work using the configured client. SDK API and model compatibility remain the caller's concern.
	 *
	 * @param client the configured SDK client
	 * @return the callback result
	 * @throws MeilisearchException an SDK failure to be translated by the calling template
	 */
	T doWithClient(Client client) throws MeilisearchException;
}
