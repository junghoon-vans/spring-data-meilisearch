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
package io.vanslog.spring.data.meilisearch.config;

import static org.assertj.core.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClient;
import io.vanslog.spring.data.meilisearch.client.MeilisearchClientFactoryBean;
import io.vanslog.spring.data.meilisearch.client.msc.MeilisearchTemplate;
import io.vanslog.spring.data.meilisearch.core.MeilisearchOperations;
import io.vanslog.spring.data.meilisearch.repository.MeilisearchRepository;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchConverter;

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
		assertThat(context.getBean("meilisearchTemplate", MeilisearchOperations.class)).isNotNull();
	}

	@Test
	void shouldWireConfiguredConverterAndObjectMapperIntoMeilisearchTemplate() {
		MeilisearchTemplate template = context.getBean(MeilisearchTemplate.class);

		assertThat(ReflectionTestUtils.getField(template, "meilisearchConverter"))
				.isSameAs(context.getBean(MeilisearchConverter.class));
		assertThat(ReflectionTestUtils.getField(template, "objectMapper")).isSameAs(context.getBean(ObjectMapper.class));
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

	@Test // GH-261
	void supportsUnauthenticatedXmlClientsWithOmittedAndEmptyApiKeys() throws Exception {
		List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/indexes/movies/documents/fetch", exchange -> {
			try (exchange) {
				authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
				byte[] response = "{\"results\":[]}".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				exchange.getResponseBody().write(response);
			}
		});
		server.start();

		try (GenericXmlApplicationContext xmlContext = new GenericXmlApplicationContext()) {
			String host = "http://127.0.0.1:" + server.getAddress().getPort();
			xmlContext.getEnvironment().getPropertySources().addFirst(
					new MapPropertySource("test", Map.of("MEILISEARCH_API_KEY", "", "MEILISEARCH_HOST_URL", host)));

			xmlContext.load("classpath:io/vanslog/spring/data/meilisearch/config/unauthenticated/namespace.xml");
			xmlContext.refresh();

			for (String id : List.of("omitted", "empty", "resolved", "authenticated")) {
				xmlContext.getBean(id, MeilisearchClient.class).getRawDocuments("movies", 0, 1, null);
			}
			assertThat(authorizationHeaders).containsExactly(null, null, null, "Bearer test-key");
		} finally {
			server.stop(0);
		}
	}

	interface ApplySettingsFalseRepository extends MeilisearchRepository<ApplySettingsFalseEntity, String> {}

	@Document(indexUid = "test-index-config-namespace", applySettings = false)
	record ApplySettingsFalseEntity(@Id String id) {
	}
}
