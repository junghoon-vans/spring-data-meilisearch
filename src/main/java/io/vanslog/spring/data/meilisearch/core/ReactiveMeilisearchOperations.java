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
package io.vanslog.spring.data.meilisearch.core;

import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import reactor.core.publisher.Mono;

/**
 * Reactive document/search operations over an asynchronous REST transport. No SDK client execution or public task
 * handles are required. Local mapping metadata access is synchronous.
 *
 * @author Junghoon Ban
 */
public interface ReactiveMeilisearchOperations extends ReactiveDocumentOperations, ReactiveSearchOperations {

	/** Apply annotated default settings and complete only after the settings task succeeds. */
	<T> Mono<Void> applySettings(Class<T> clazz);

	/** Return the local entity converter without a server request. */
	MeilisearchConverter getMeilisearchConverter();
}
