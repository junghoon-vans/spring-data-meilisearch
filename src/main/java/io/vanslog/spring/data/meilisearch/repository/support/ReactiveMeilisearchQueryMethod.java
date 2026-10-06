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
package io.vanslog.spring.data.meilisearch.repository.support;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import reactor.core.publisher.Flux;

/**
 * Reactor query metadata whose Pageable parameter selects a streamed window rather than a
 * blocking Page wrapper. Concrete Mono/Flux payloads and supported special parameters are
 * validated by the reactive query executor.
 *
 * @author Junghoon Ban
 */
final class ReactiveMeilisearchQueryMethod extends QueryMethod {

	private final boolean collection;

	ReactiveMeilisearchQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
		this.collection = method.getReturnType() == Flux.class;
	}

	@Override
	public boolean isStreamQuery() {
		return true;
	}

	@Override
	public boolean isCollectionQuery() {
		return collection;
	}

}
