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
package io.vanslog.spring.data.meilisearch.config;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Namespace based configuration test.
 *
 * @author Junghoon Ban
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("namespace.xml")
class MeilisearchNamespaceHandlerUnitTests {

	@Autowired private ApplicationContext context;

	@Test
	void shouldCreateMeilisearchClient() {
		assertThat(context.getBean(MeilisearchClientFactoryBean.class)).isInstanceOf(MeilisearchClientFactoryBean.class);
	}

	@Test
	void shouldCreateMeilisearchTemplate() {
		assertThat(context.getBean(MeilisearchTemplate.class)).isInstanceOf(MeilisearchTemplate.class);
	}

	@Test
	void shouldUseGsonJsonHandler() throws NoSuchFieldException, IllegalAccessException {
		MeilisearchClient meilisearchClient = (MeilisearchClient) context.getBean("meilisearchClient");

		Field jsonHandlerField = meilisearchClient.getClass().getDeclaredField("jsonHandler");
		jsonHandlerField.setAccessible(true);
		assertThat(jsonHandlerField.get(meilisearchClient)).isInstanceOf(com.meilisearch.sdk.json.GsonJsonHandler.class);
	}

	@Test
	void shouldCreateMeilisearchRepository() {
		assertThat(context.getBean(ApplySettingsFalseRepository.class)).isInstanceOf(ApplySettingsFalseRepository.class);
	}

	interface ApplySettingsFalseRepository extends MeilisearchRepository<ApplySettingsFalseEntity, String> {}

	@Document(indexUid = "test-index-config-namespace", applySettings = false)
	record ApplySettingsFalseEntity(@Id String id) {
	}
}
