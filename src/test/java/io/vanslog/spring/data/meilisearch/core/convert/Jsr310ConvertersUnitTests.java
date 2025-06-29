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
package io.vanslog.spring.data.meilisearch.core.convert;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.core.document.Document;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

/**
 * Unit tests for JSR-310 converters in {@link MappingMeilisearchConverter}.
 *
 * @author Junghoon Ban
 */
class Jsr310ConvertersUnitTests {

	private MappingMeilisearchConverter converter;

	@BeforeEach
	void setUp() {
		converter = new MappingMeilisearchConverter(new SimpleMeilisearchMappingContext());
		converter.afterPropertiesSet();
	}

	@Test
	void shouldConvertLocalDateTimeCorrectly() {
		// given
		LocalDateTime dateTime = LocalDateTime.now();
		TestEntityWithLocalDateTime entity = new TestEntityWithLocalDateTime();
		entity.setId("test-id");
		entity.setDateTime(dateTime);

		// when
		Document result = Document.create();
		converter.write(entity, result);
		TestEntityWithLocalDateTime readEntity = converter.read(TestEntityWithLocalDateTime.class, result);

		// then
		assertThat(readEntity.getDateTime()).isEqualTo(dateTime);
	}

	@Test
	void shouldConvertLocalDateCorrectly() {
		// given
		LocalDate date = LocalDate.now();
		TestEntityWithLocalDate entity = new TestEntityWithLocalDate();
		entity.setId("test-id");
		entity.setDate(date);

		// when
		Document result = Document.create();
		converter.write(entity, result);
		TestEntityWithLocalDate readEntity = converter.read(TestEntityWithLocalDate.class, result);

		// then
		assertThat(readEntity.getDate()).isEqualTo(date);
	}

	@Test
	void shouldConvertInstantCorrectly() {
		// given
		Instant instant = Instant.now();
		TestEntityWithInstant entity = new TestEntityWithInstant();
		entity.setId("test-id");
		entity.setInstant(instant);

		// when
		Document result = Document.create();
		converter.write(entity, result);
		TestEntityWithInstant readEntity = converter.read(TestEntityWithInstant.class, result);

		// then
		assertThat(readEntity.getInstant()).isEqualTo(instant);
	}

	@Test
	void shouldConvertZonedDateTimeCorrectly() {
		// given
		ZonedDateTime zonedDateTime = ZonedDateTime.now();
		TestEntityWithZonedDateTime entity = new TestEntityWithZonedDateTime();
		entity.setId("test-id");
		entity.setZonedDateTime(zonedDateTime);

		// when
		Document result = Document.create();
		converter.write(entity, result);
		TestEntityWithZonedDateTime readEntity = converter.read(TestEntityWithZonedDateTime.class, result);

		// then
		assertThat(readEntity.getZonedDateTime()).isEqualTo(zonedDateTime);
	}

	static class TestEntityWithLocalDateTime {
		@Id
		private String id;
		private LocalDateTime dateTime;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public LocalDateTime getDateTime() {
			return dateTime;
		}

		public void setDateTime(LocalDateTime dateTime) {
			this.dateTime = dateTime;
		}
	}

	static class TestEntityWithLocalDate {
		@Id
		private String id;
		private LocalDate date;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}
	}

	static class TestEntityWithInstant {
		@Id
		private String id;
		private Instant instant;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Instant getInstant() {
			return instant;
		}

		public void setInstant(Instant instant) {
			this.instant = instant;
		}
	}

	static class TestEntityWithZonedDateTime {
		@Id
		private String id;
		private ZonedDateTime zonedDateTime;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public ZonedDateTime getZonedDateTime() {
			return zonedDateTime;
		}

		public void setZonedDateTime(ZonedDateTime zonedDateTime) {
			this.zonedDateTime = zonedDateTime;
		}
	}
}
