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
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;

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
		converter.afterPropertiesSet();
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
	void shouldWriteEntityToMap() {
		// given
		TestEntity entity = new TestEntity();
		entity.setId("test-id");
		entity.setName("Test Name");
		entity.setAge(30);
		entity.setActive(true);
		entity.setTags(Arrays.asList("tag1", "tag2"));

		// when
		Map<String, Object> result = new HashMap<>();
		converter.write(entity, result);

		// then
		assertThat(result).containsEntry("id", "test-id");
		assertThat(result).containsEntry("name", "Test Name");
		assertThat(result).containsEntry("age", 30);
		assertThat(result).containsEntry("active", true);
		assertThat(result).containsEntry("tags", Arrays.asList("tag1", "tag2"));
	}

	@Test
	void shouldReadMapToEntity() {
		// given
		Map<String, Object> source = new HashMap<>();
		source.put("id", "test-id");
		source.put("name", "Test Name");
		source.put("age", 30);
		source.put("active", true);
		source.put("tags", Arrays.asList("tag1", "tag2"));

		// when
		TestEntity result = converter.read(TestEntity.class, source);

		// then
		assertThat(result.getId()).isEqualTo("test-id");
		assertThat(result.getName()).isEqualTo("Test Name");
		assertThat(result.getAge()).isEqualTo(30);
		assertThat(result.isActive()).isEqualTo(true);
		assertThat(result.getTags()).containsExactly("tag1", "tag2");
	}

	@Test
	void shouldConvertUUIDCorrectly() {
		// given
		UUID uuid = UUID.randomUUID();
		TestEntityWithUUID entity = new TestEntityWithUUID();
		entity.setId("test-id");
		entity.setUuid(uuid);

		// when
		Map<String, Object> result = new HashMap<>();
		converter.write(entity, result);
		TestEntityWithUUID readEntity = converter.read(TestEntityWithUUID.class, result);

		// then
		assertThat(readEntity.getUuid()).isEqualTo(uuid);
	}

	@Test
	void shouldConvertBigDecimalCorrectly() {
		// given
		BigDecimal decimal = new BigDecimal("123.456");
		TestEntityWithBigDecimal entity = new TestEntityWithBigDecimal();
		entity.setId("test-id");
		entity.setDecimal(decimal);

		// when
		Map<String, Object> result = new HashMap<>();
		converter.write(entity, result);
		TestEntityWithBigDecimal readEntity = converter.read(TestEntityWithBigDecimal.class, result);

		// then
		assertThat(readEntity.getDecimal()).isEqualTo(decimal);
	}

	static class TestEntity {
		@Id
		private String id;
		private String name;
		private int age;
		private boolean active;
		private List<String> tags;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}
	}

	static class TestEntityWithUUID {
		@Id
		private String id;
		private UUID uuid;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public UUID getUuid() {
			return uuid;
		}

		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}
	}

	static class TestEntityWithBigDecimal {
		@Id
		private String id;
		private BigDecimal decimal;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public BigDecimal getDecimal() {
			return decimal;
		}

		public void setDecimal(BigDecimal decimal) {
			this.decimal = decimal;
		}
	}
}
