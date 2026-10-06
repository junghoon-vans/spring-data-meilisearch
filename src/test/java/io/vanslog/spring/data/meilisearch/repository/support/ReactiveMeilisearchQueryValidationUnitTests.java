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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.QueryLookupStrategy;

import io.vanslog.spring.data.meilisearch.repository.Query;
import io.vanslog.spring.data.meilisearch.core.ReactiveMeilisearchOperations;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Validates the concrete reactive query wrappers and special repository parameters.
 *
 * @author Junghoon Ban
 */
class ReactiveMeilisearchQueryValidationUnitTests {

	@Test
	void resolvesConcreteTypesInInheritedReactiveFinderMethods() throws Exception {
		assertThat(shape(GenericProductRepository.class, "findByTitle"))
			.isEqualTo(MeilisearchReactiveQueryReturnShape.FLUX);
		assertThat(shape(GenericProductRepository.class, "readByTitle"))
			.isEqualTo(MeilisearchReactiveQueryReturnShape.MONO);
	}

	@Test
	void rejectsUnsupportedWrappedAndProjectionReturns() {
		for (String name : List.of("findNestedListByTitle", "findPageByTitle", "findWrongMonoProjectionByTitle",
				"countByTitle", "deleteByTitle")) {
			assertThatThrownBy(() -> shape(InvalidRepository.class, name)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("return type");
		}
	}

	@Test
	void rejectsInvalidSpecialParametersAtBootstrap() throws Exception {
		assertQueryRejected("findByCategory");
		assertQueryRejected("countWithSortByCategory");
		assertQueryRejected("findByTitle");
	}

	@Test
	void keepsDeclaredQueriesSearchOnly() {
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(InvalidRepository.class);
		Method method = findMethod(InvalidRepository.class, "declaredCountByTitle");
		assertThatThrownBy(() -> MeilisearchReactiveQueryReturnShape.resolveFinder(method, metadata))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("return type");
	}

	@Test
	void followsCreateAndUseDeclaredLookupStrategies() throws Exception {
		var converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		ReactiveMeilisearchOperations operations = operations(converter);
		NamedQueries noNamedQueries = new NamedQueries() {

			@Override
			public boolean hasQuery(String queryName) {
				return false;
			}

			@Override
			public String getQuery(String queryName) {
				throw new AssertionError("No named query is expected");
			}
		};
		var metadata = new DefaultRepositoryMetadata(StrategyRepository.class);
		var projectionFactory = new SpelAwareProxyProjectionFactory();
		Method annotated = StrategyRepository.class.getMethod("countByTitle", String.class);
		ReactiveMeilisearchQueryLookupStrategy create = new ReactiveMeilisearchQueryLookupStrategy(
				QueryLookupStrategy.Key.CREATE, operations, type -> Mono.empty());
		assertThatThrownBy(() -> create.resolveQuery(annotated, metadata, projectionFactory, noNamedQueries))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("return type");

		Method derived = StrategyRepository.class.getMethod("findByTitle", String.class);
		ReactiveMeilisearchQueryLookupStrategy declaredOnly = new ReactiveMeilisearchQueryLookupStrategy(
				QueryLookupStrategy.Key.USE_DECLARED_QUERY, operations, type -> Mono.empty());
		assertThatThrownBy(() -> declaredOnly.resolveQuery(derived, metadata, projectionFactory, noNamedQueries))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("No declared Meilisearch query");
	}

	@Test
	void reportsInvalidDerivedArgumentsAsReactiveErrorsBeforeSearch() throws Exception {
		Flux<?> result = (Flux<?>) query("findByTitleStartingWith").execute(new Object[] { null });
		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();
	}

	private static MeilisearchReactiveQueryReturnShape shape(Class<?> repositoryType, String name) throws Exception {
		Method method = findMethod(repositoryType, name);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryType);
		var tree = new org.springframework.data.repository.query.parser.PartTree(method.getName(),
				metadata.getDomainType());
		return MeilisearchReactiveQueryReturnShape.resolve(tree, method, metadata);
	}

	private static void assertQueryRejected(String name) throws Exception {
		assertThatThrownBy(() -> query(name)).isInstanceOf(IllegalArgumentException.class);
	}

	private static ReactiveMeilisearchQuery query(String name) throws Exception {
		Class<?> repositoryType = InvalidRepository.class;
		Method method = findMethod(repositoryType, name);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryType);
		var converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		ReactiveMeilisearchOperations operations = operations(converter);
		return new ReactiveMeilisearchQuery(method, metadata, new SpelAwareProxyProjectionFactory(), operations,
				Mono.empty(), false, "", "");
	}

	private static Method findMethod(Class<?> repositoryType, String name) {
		for (Method method : repositoryType.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}
		throw new IllegalArgumentException("No method " + name + " on " + repositoryType.getName());
	}

	private static ReactiveMeilisearchOperations operations(MeilisearchConverter converter) {
		return (ReactiveMeilisearchOperations) Proxy.newProxyInstance(
				ReactiveMeilisearchOperations.class.getClassLoader(),
				new Class<?>[] { ReactiveMeilisearchOperations.class }, (proxy, method, arguments) -> {
					if (method.getName().equals("getMeilisearchConverter")) {
						return converter;
					}
					throw new UnsupportedOperationException(method.toString());
				});
	}

	@NoRepositoryBean
	interface InvalidRepository extends Repository<Product, String> {

		Mono<List<Product>> findNestedListByTitle(String title);

		Mono<Page<Product>> findPageByTitle(String title);

		Mono<Boolean> findWrongMonoProjectionByTitle(String title);

		Flux<Long> countByTitle(String title);

		Flux<Product> deleteByTitle(String title);

		Mono<Product> findByCategory(String category, Pageable pageable);

		Mono<Long> countWithSortByCategory(String category, Sort sort);

		<T extends Product> Flux<Product> findByTitle(String title, Class<T> projection);

		@Query(q = "?0")
		Mono<Long> declaredCountByTitle(String title);

		Flux<Product> findByTitleStartingWith(String title);

	}

	@NoRepositoryBean
	interface GenericRepository<T> extends Repository<T, String> {

		Flux<T> findByTitle(String title);

		Mono<T> readByTitle(String title);

	}

	@NoRepositoryBean
	interface GenericProductRepository extends GenericRepository<Product> {

	}

	@NoRepositoryBean
	interface StrategyRepository extends Repository<Product, String> {

		@Query(q = "?0")
		Flux<Product> countByTitle(String title);

		Flux<Product> findByTitle(String title);

	}

	static class Product {

		@Id
		private String id;

		private String title;

		private String category;

	}

}
