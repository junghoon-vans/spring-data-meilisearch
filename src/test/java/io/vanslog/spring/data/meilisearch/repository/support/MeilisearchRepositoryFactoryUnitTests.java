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
package io.vanslog.spring.data.meilisearch.repository.support;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.NoRepositoryBean;

class MeilisearchRepositoryFactoryUnitTests {

	private MeilisearchRepositoryFactory repositoryFactory;

	@BeforeEach
	void setUp() {
		MappingMeilisearchConverter converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		MeilisearchOperations operations = operationsWithConverter(converter);

		repositoryFactory = new MeilisearchRepositoryFactory(operations);
	}

	@Test
	void shouldCreateRepositoryForBaseRepositoryMethods() {
		assertThat(repositoryFactory.getRepository(MovieRepository.class)).isNotNull();
	}

	@Test
	void shouldRejectDerivedQueryMethodsWithClearMessage() {
		assertThatThrownBy(() -> repositoryFactory.getRepository(DerivedMovieRepository.class))
				.hasMessageContaining("Meilisearch repository query methods are not supported")
				.hasMessageContaining("findByTitle");
	}

	private MeilisearchOperations operationsWithConverter(MappingMeilisearchConverter converter) {
		return (MeilisearchOperations) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { MeilisearchOperations.class }, (proxy, method, args) -> {
					if (method.getName().equals("getMeilisearchConverter")) {
						return converter;
					}
					if (method.getName().equals("applySettings")) {
						return null;
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}

	@NoRepositoryBean
	interface MovieRepository extends MeilisearchRepository<Movie, Integer> {
	}

	@NoRepositoryBean
	interface DerivedMovieRepository extends MeilisearchRepository<Movie, Integer> {

		Movie findByTitle(String title);
	}
}
