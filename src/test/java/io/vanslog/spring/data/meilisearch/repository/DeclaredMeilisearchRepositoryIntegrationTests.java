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
package io.vanslog.spring.data.meilisearch.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.QueryCreationException;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;

import io.vanslog.spring.data.meilisearch.consumer.declared.DeclaredProduct;
import io.vanslog.spring.data.meilisearch.consumer.declared.DeclaredProductRepository;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import io.vanslog.spring.data.meilisearch.repository.config.EnableMeilisearchRepositories;
import io.vanslog.spring.data.meilisearch.repository.support.MeilisearchRepositoryFactory;

/**
 * Exercises declared searches and named query loading against Meilisearch.
 *
 * @author Junghoon Ban
 */
@MeilisearchTest
@ContextConfiguration(classes = DeclaredMeilisearchRepositoryIntegrationTests.Config.class)
class DeclaredMeilisearchRepositoryIntegrationTests {

	@Autowired private DeclaredProductRepository repository;
	@Autowired private MeilisearchOperations operations;

	@BeforeEach
	void setUp() {
		repository.deleteAll();
	}

	@Test
	void shouldCombineSearchTextAndTypedFiltersWithPagingAndSorting() {
		repository.saveAll(List.of(product("1", "Running shoes", "Sports", 30), product("2", "Running shoes", "Sports", 10),
				product("3", "Running shoes", "Sports", 200), product("4", "Winter boots", "Sports", 5),
				product("5", "Running shoes", "Casual", 15)));

		Page<DeclaredProduct> page = repository.search("running", "Sports", 100, PageRequest.of(0, 1, Sort.by("price")));

		assertThat(page.getContent()).extracting(DeclaredProduct::getId).containsExactly("2");
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(repository.search("running", "Sports", 100, PageRequest.of(1, 1, Sort.by("price"))).getContent())
				.extracting(DeclaredProduct::getId).containsExactly("1");
		assertThat(repository.inRange(10, 30, "Running")).extracting(DeclaredProduct::getId).containsExactlyInAnyOrder("1",
				"2", "5");
	}

	@Test
	void shouldTreatInjectionTextAsOneFilterLiteralAndCheckSingleResultCardinality() {
		String literal = "Director's \"cut\" \\ edition\" OR active = true OR title = \"";
		repository.saveAll(List.of(product("literal", literal, "Film", 10), product("other", "Other", "Film", 20)));

		assertThat(repository.byTitle(literal)).get().extracting(DeclaredProduct::getId).isEqualTo("literal");
		assertThat(repository.byTitle("missing")).isEmpty();
		repository.save(product("duplicate", literal, "Film", 30));
		assertThatThrownBy(() -> repository.byTitle(literal)).isInstanceOf(IncorrectResultSizeDataAccessException.class);
	}

	@Test
	void shouldPreserveControlCharactersInDeclaredAndDerivedFilterValues() {
		String literal = "quote\" slash\\ newline\n carriage\r tab\t backspace\b formfeed\f control" + (char) 1;
		String escapedText = "quote\" slash\\ newline\\n carriage\\r tab\\t backspace\\b formfeed\\f control\\u0001";
		repository
				.saveAll(List.of(product("controls", literal, "Film", 10), product("escaped-text", escapedText, "Film", 20)));
		MeilisearchRepositoryFactory factory = new MeilisearchRepositoryFactory(operations);
		factory.setQueryLookupStrategyKey(QueryLookupStrategy.Key.CREATE);
		StrategyRepository derivedRepository = factory.getRepository(StrategyRepository.class);

		assertThat(repository.byTitle(literal)).get().extracting(DeclaredProduct::getId).isEqualTo("controls");
		assertThat(derivedRepository.findByTitle(literal)).extracting(DeclaredProduct::getId).containsExactly("controls");
		assertThat(repository.byTitle(escapedText)).get().extracting(DeclaredProduct::getId).isEqualTo("escaped-text");
		assertThat(derivedRepository.findByTitle(escapedText)).extracting(DeclaredProduct::getId)
				.containsExactly("escaped-text");
	}

	@Test
	void shouldBindCollectionsAndBooleansWithoutDroppingConditions() {
		repository.saveAll(List.of(product("1", "Running shoes", "Sports", 30), product("2", "Walking shoes", "Casual", 10),
				new DeclaredProduct("3", "Retired shoes", "Sports", 5, false), product("4", "Office shoes", "Business", 20)));

		assertThat(repository.inCategories(List.of("Sports", "Casual"), true, Sort.by("price")))
				.extracting(DeclaredProduct::getId).containsExactly("2", "1");
	}

	@Test
	void shouldLoadNamedFilterTextAndCombinedSearchesAndPreferAnnotations() {
		repository.saveAll(List.of(product("1", "Running shoes", "Sports", 30), product("2", "Winter boots", "Sports", 10),
				product("3", "Running shoes", "Casual", 15)));

		assertThat(repository.namedSearch("running", "Sports")).extracting(DeclaredProduct::getId).containsExactly("1");
		assertThat(repository.namedFilter("Sports")).extracting(DeclaredProduct::getId).containsExactlyInAnyOrder("1", "2");
		assertThat(repository.namedText("running")).extracting(DeclaredProduct::getId).containsExactlyInAnyOrder("1", "3");
		assertThat(repository.preferred("running", "Sports")).extracting(DeclaredProduct::getId).containsExactly("1");
	}

	@Test
	void shouldReplaceMethodNamePredicatesUnlessCreateStrategyIsSelected() {
		repository.saveAll(
				List.of(product("category", "Different title", "Sports", 10), product("title", "Sports", "Casual", 20)));

		assertThat(repository.findByTitle("Sports")).extracting(DeclaredProduct::getId).containsExactly("category");
		MeilisearchRepositoryFactory factory = new MeilisearchRepositoryFactory(operations);
		factory.setQueryLookupStrategyKey(QueryLookupStrategy.Key.CREATE);
		assertThat(factory.getRepository(StrategyRepository.class).findByTitle("Sports")).extracting(DeclaredProduct::getId)
				.containsExactly("title");

		MeilisearchRepositoryFactory declaredOnlyFactory = new MeilisearchRepositoryFactory(operations);
		declaredOnlyFactory.setQueryLookupStrategyKey(QueryLookupStrategy.Key.USE_DECLARED_QUERY);
		assertThatThrownBy(() -> declaredOnlyFactory.getRepository(DerivedOnlyRepository.class))
				.isInstanceOf(QueryCreationException.class);
	}

	@Test
	void shouldKeepFullTextQueryAcrossEveryUnpagedResultChunk() {
		List<DeclaredProduct> products = IntStream.range(0, 1001)
				.mapToObj(i -> product("match-" + i, "Running shoes", "Sports", i)).toList();
		repository.saveAll(products);
		repository.save(product("not-a-match", "Winter boots", "Sports", 1));

		assertThat(repository.fullText("running")).extracting(DeclaredProduct::getId)
				.containsExactlyInAnyOrderElementsOf(products.stream().map(DeclaredProduct::getId).toList());
	}

	@Test
	void shouldRejectNullAndBlankSearchArgumentsRatherThanSearchingEverything() {
		repository.save(product("1", "Running shoes", "Sports", 10));

		assertThatThrownBy(() -> repository.fullText(" ")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> repository.byTitle(null)).isInstanceOf(IllegalArgumentException.class);
	}

	private static DeclaredProduct product(String id, String title, String category, int price) {
		return new DeclaredProduct(id, title, category, price, true);
	}

	@NoRepositoryBean
	interface StrategyRepository extends MeilisearchRepository<DeclaredProduct, String> {

		@Query(filter = "category = ?0")
		List<DeclaredProduct> findByTitle(String value);
	}

	@NoRepositoryBean
	interface DerivedOnlyRepository extends MeilisearchRepository<DeclaredProduct, String> {

		List<DeclaredProduct> findByTitle(String value);
	}

	@Configuration
	@Import(MeilisearchTestConfiguration.class)
	@EnableMeilisearchRepositories(basePackageClasses = DeclaredProductRepository.class,
			namedQueriesLocation = "classpath:io/vanslog/spring/data/meilisearch/declared-queries.properties",
			queryLookupStrategy = QueryLookupStrategy.Key.USE_DECLARED_QUERY)
	static class Config {}
}
