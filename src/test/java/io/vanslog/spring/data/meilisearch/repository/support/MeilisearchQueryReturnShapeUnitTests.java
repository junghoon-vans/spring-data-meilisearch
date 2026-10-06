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

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Validates the immediate entity type that finder execution can actually materialize.
 *
 * @author Junghoon Ban
 */
class MeilisearchQueryReturnShapeUnitTests {

	@Test
	void rejectsNestedEntityContainers() {
		for (String method : List.of("nestedOptional", "optionalCollection", "nestedCollection")) {
			assertThatThrownBy(() -> shape(InvalidRepository.class, method)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void rejectsEntitySubtypeResultsThatCannotBeMaterialized() {
		for (String method : List.of("subtype", "optionalSubtype", "collectionSubtype")) {
			assertThatThrownBy(() -> shape(InvalidRepository.class, method)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void rejectsUnresolvedMethodGenericResults() {
		for (String method : List.of("genericEntity", "genericOptional", "genericCollection")) {
			assertThatThrownBy(() -> shape(InvalidRepository.class, method)).isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void resolvesDomainTypeInInheritedGenericFinderSignatures() throws Exception {
		assertThat(shape(GenericProductRepository.class, "entity")).isEqualTo(MeilisearchQueryReturnShape.ENTITY);
		assertThat(shape(GenericProductRepository.class, "optional")).isEqualTo(MeilisearchQueryReturnShape.OPTIONAL);
		assertThat(shape(GenericProductRepository.class, "collection")).isEqualTo(MeilisearchQueryReturnShape.LIST);
	}

	private static MeilisearchQueryReturnShape shape(Class<?> repositoryType, String name) throws Exception {
		Method method = repositoryType.getMethod(name);
		var metadata = new DefaultRepositoryMetadata(repositoryType);
		return MeilisearchQueryReturnShape.resolveFinder(method, metadata);
	}

	@NoRepositoryBean
	interface InvalidRepository extends Repository<Product, String> {

		Optional<Optional<Product>> nestedOptional();

		Optional<List<Product>> optionalCollection();

		List<List<Product>> nestedCollection();

		SpecialProduct subtype();

		Optional<SpecialProduct> optionalSubtype();

		List<SpecialProduct> collectionSubtype();

		<S extends Product> S genericEntity();

		<S extends Product> Optional<S> genericOptional();

		<S extends Product> List<S> genericCollection();
	}

	@NoRepositoryBean
	interface GenericRepository<T> extends Repository<T, String> {

		T entity();

		Optional<T> optional();

		List<T> collection();
	}

	@NoRepositoryBean
	interface GenericProductRepository extends GenericRepository<Product> {}

	static class Product {}

	static class SpecialProduct extends Product {}
}
