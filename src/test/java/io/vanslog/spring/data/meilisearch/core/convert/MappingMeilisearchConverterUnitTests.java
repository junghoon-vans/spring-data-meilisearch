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
package io.vanslog.spring.data.meilisearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.core.document.MeilisearchDocument;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

/**
 * Unit tests for {@link MappingMeilisearchConverter}.
 *
 * @author Junghoon Ban
 */
class MappingMeilisearchConverterUnitTests {

	private MappingMeilisearchConverter converter;

	@BeforeEach
	void setUp() {
		converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
	}

	@Test
	void shouldReturnMappingContext() {
		assertThat(converter.getMappingContext()).isNotNull();
	}

	@Test
	void shouldReturnConversionService() {
		assertThat(converter.getConversionService()).isNotNull();
	}

	@Test
	void shouldWriteEntityPropertiesToDocument() {

		SampleBook entity = new SampleBook("book-1", "The Left Hand of Darkness", 1969);
		MeilisearchDocument document = MeilisearchDocument.create();

		converter.write(entity, document);

		assertThat(document).containsEntry("id", "book-1").containsEntry("title", "The Left Hand of Darkness")
				.containsEntry("publishedYear", 1969);
	}

	@Test
	void shouldReadEntityPropertiesFromDocument() {

		MeilisearchDocument document = MeilisearchDocument.create().append("id", "book-2").append("title", "Kindred")
				.append("publishedYear", 1979);

		SampleBook entity = converter.read(SampleBook.class, document);

		assertThat(entity.getId()).isEqualTo("book-2");
		assertThat(entity.getTitle()).isEqualTo("Kindred");
		assertThat(entity.getPublishedYear()).isEqualTo(1979);
	}

	@Test
	void shouldRoundTripNestedObjectAsNestedDocument() {

		BookWithAuthor source = new BookWithAuthor("book-3", "The Dispossessed", new Author("Ursula", "Le Guin"));
		MeilisearchDocument document = MeilisearchDocument.create();

		converter.write(source, document);

		assertThat(document.get("author")).isEqualTo(MeilisearchDocument.create().append("firstName", "Ursula")
				.append("lastName", "Le Guin"));

		BookWithAuthor result = converter.read(BookWithAuthor.class, document);

		assertThat(result.getTitle()).isEqualTo("The Dispossessed");
		assertThat(result.getAuthor().getFirstName()).isEqualTo("Ursula");
		assertThat(result.getAuthor().getLastName()).isEqualTo("Le Guin");
	}

	@Test
	void shouldPreserveCollectionsAndNestedListElements() {

		BookCollection source = new BookCollection("collection-1", List.of("science-fiction", "classic"),
				List.of(new Author("Octavia", "Butler"), new Author("Nnedi", "Okorafor")));
		MeilisearchDocument document = MeilisearchDocument.create();

		converter.write(source, document);

		assertThat(document.get("tags")).isEqualTo(List.of("science-fiction", "classic"));
		assertThat(document.get("contributors")).isEqualTo(List.of(
				MeilisearchDocument.create().append("firstName", "Octavia").append("lastName", "Butler"),
				MeilisearchDocument.create().append("firstName", "Nnedi").append("lastName", "Okorafor")));

		BookCollection result = converter.read(BookCollection.class, document);

		assertThat(result.getTags()).containsExactly("science-fiction", "classic");
		assertThat(result.getContributors()).extracting(Author::getLastName).containsExactly("Butler", "Okorafor");
	}

	@Test
	void shouldApplyCustomWritingConversionToPropertyValues() {

		registerPriceConversions();

		PricedBook source = new PricedBook("book-4", new Price(new BigDecimal("12.99"), "USD"));
		MeilisearchDocument document = MeilisearchDocument.create();

		converter.write(source, document);

		assertThat(document.get("price"))
				.isEqualTo(MeilisearchDocument.create().append("amount", "12.99").append("currency", "USD"));
	}

	@Test
	void shouldApplyCustomReadingConversionToPropertyValues() {

		registerPriceConversions();

		MeilisearchDocument document = MeilisearchDocument.create().append("id", "book-4").append("price",
				MeilisearchDocument.create().append("amount", "12.99").append("currency", "USD"));

		PricedBook result = converter.read(PricedBook.class, document);

		assertThat(result.getPrice().getAmount()).isEqualByComparingTo("12.99");
		assertThat(result.getPrice().getCurrency()).isEqualTo("USD");
	}

	@Test
	void shouldReadConstructorBoundEntityPropertiesFromDocument() {

		MeilisearchDocument document = MeilisearchDocument.create().append("id", "immutable-1")
				.append("title", "Parable of the Sower");

		ConstructorBoundBook result = converter.read(ConstructorBoundBook.class, document);

		assertThat(result.getId()).isEqualTo("immutable-1");
		assertThat(result.getTitle()).isEqualTo("Parable of the Sower");
	}

	private void registerPriceConversions() {

		converter.setConversions(new MeilisearchCustomConversions(
				List.of(new PriceToDocumentConverter(), new DocumentToPriceConverter())));
		converter.afterPropertiesSet();
	}

	@Document(indexUid = "sample-books")
	@SuppressWarnings("unused")
	private static class SampleBook {

		@Id private String id;
		private String title;
		private int publishedYear;

		@SuppressWarnings("unused")
		SampleBook() {}

		SampleBook(String id, String title, int publishedYear) {
			this.id = id;
			this.title = title;
			this.publishedYear = publishedYear;
		}

		String getId() {
			return id;
		}

		String getTitle() {
			return title;
		}

		int getPublishedYear() {
			return publishedYear;
		}
	}

	@Document(indexUid = "books-with-author")
	@SuppressWarnings("unused")
	private static class BookWithAuthor {

		@Id private String id;
		private String title;
		private Author author;

		@SuppressWarnings("unused")
		BookWithAuthor() {}

		BookWithAuthor(String id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		Author getAuthor() {
			return author;
		}

		String getTitle() {
			return title;
		}
	}

	@SuppressWarnings("unused")
	private static class Author {

		private String firstName;
		private String lastName;

		@SuppressWarnings("unused")
		Author() {}

		Author(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		String getFirstName() {
			return firstName;
		}

		String getLastName() {
			return lastName;
		}
	}

	@Document(indexUid = "book-collections")
	@SuppressWarnings("unused")
	private static class BookCollection {

		@Id private String id;
		private List<String> tags;
		private List<Author> contributors;

		@SuppressWarnings("unused")
		BookCollection() {}

		BookCollection(String id, List<String> tags, List<Author> contributors) {
			this.id = id;
			this.tags = tags;
			this.contributors = contributors;
		}

		List<String> getTags() {
			return tags;
		}

		List<Author> getContributors() {
			return contributors;
		}
	}

	@Document(indexUid = "priced-books")
	@SuppressWarnings("unused")
	private static class PricedBook {

		@Id private String id;
		private Price price;

		@SuppressWarnings("unused")
		PricedBook() {}

		PricedBook(String id, Price price) {
			this.id = id;
			this.price = price;
		}

		Price getPrice() {
			return price;
		}
	}

	private static class Price {

		private final BigDecimal amount;
		private final String currency;

		Price(BigDecimal amount, String currency) {
			this.amount = amount;
			this.currency = currency;
		}

		BigDecimal getAmount() {
			return amount;
		}

		String getCurrency() {
			return currency;
		}
	}

	@Document(indexUid = "constructor-bound-books")
	private static class ConstructorBoundBook {

		@Id private final String id;
		private final String title;

		@PersistenceCreator
		ConstructorBoundBook(String id, String title) {
			this.id = id;
			this.title = title;
		}

		String getId() {
			return id;
		}

		String getTitle() {
			return title;
		}
	}

	@WritingConverter
	private static class PriceToDocumentConverter implements Converter<Price, MeilisearchDocument> {

		@Override
		public MeilisearchDocument convert(Price source) {
			return MeilisearchDocument.create().append("amount", source.getAmount().toPlainString()).append("currency",
					source.getCurrency());
		}
	}

	@ReadingConverter
	private static class DocumentToPriceConverter implements Converter<MeilisearchDocument, Price> {

		@Override
		public Price convert(MeilisearchDocument source) {
			return new Price(new BigDecimal((String) source.get("amount")), (String) source.get("currency"));
		}
	}
}
