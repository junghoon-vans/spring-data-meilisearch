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
package io.vanslog.spring.data.meilisearch.client;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.meilisearch.sdk.IndexSearchRequest;
import com.meilisearch.sdk.MultiSearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JacksonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.sun.net.httpserver.HttpServer;

import io.vanslog.spring.data.meilisearch.client.msc.ResponseConverter;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.SearchResponseMetadata;
import io.vanslog.spring.data.meilisearch.core.SearchResponseTotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;

/**
 * Tests the blocking SDK multi-search response decoder.
 *
 * @author Junghoon Ban
 */
class MeilisearchClientMultiSearchMetadataTests {

	private static final String MULTI_SEARCH_RESPONSE = """
			{
			  "results": [
			    {
			      "hits": [],
			      "processingTimeMs": 13,
			      "query": "exact",
			      "indexUid": "movies",
			      "totalHits": 5,
			      "page": 1,
			      "hitsPerPage": 1,
			      "totalPages": 5
			    },
			    {
			      "hits": [],
			      "processingTimeMs": 14,
			      "query": "estimated",
			      "indexUid": "movies",
			      "estimatedTotalHits": 7,
			      "offset": 0,
			      "limit": 10
			    }
			  ]
			}
			""";

	@Test // GH-284
	void shouldPreserveExactAndEstimatedMetadataWithDefaultJsonHandler() throws IOException, MeilisearchException {
		assertMetadata(new GsonJsonHandler());
	}

	@Test // GH-284
	void shouldPreserveExactAndEstimatedMetadataWithConfiguredJsonHandler() throws IOException, MeilisearchException {
		assertMetadata(new JacksonJsonHandler());
	}

	@Test // GH-284
	void shouldKeepTheSdkEmptyMultiSearchResultReturn() throws Exception {
		HttpServer server = serverRespondingWith("{\"results\":[]}");
		try {
			ClientConfiguration configuration = ClientConfiguration.builder()
					.connectedTo("http://127.0.0.1:" + server.getAddress().getPort()).withApiKey("test-key").build();
			MeilisearchClient client = new MeilisearchClient(configuration, new GsonJsonHandler());

			Results<MultiSearchResult> results = client.multiSearch(new MultiSearchRequest());

			assertThat(results.getResults()).isEmpty();
		} finally {
			server.stop(0);
		}
	}

	private static void assertMetadata(JsonHandler jsonHandler) throws IOException, MeilisearchException {
		HttpServer server = serverRespondingWith(MULTI_SEARCH_RESPONSE);

		try {
			ClientConfiguration configuration = ClientConfiguration.builder()
					.connectedTo("http://127.0.0.1:" + server.getAddress().getPort()).withApiKey("test-key").build();
			MeilisearchClient client = new MeilisearchClient(configuration, jsonHandler);
			MultiSearchRequest request = new MultiSearchRequest();
			request.addQuery(IndexSearchRequest.builder().indexUid("movies").q("exact").page(1).hitsPerPage(1).build());
			request.addQuery(IndexSearchRequest.builder().indexUid("movies").q("estimated").offset(0).limit(10).build());
			Results<MultiSearchResult> sdkResults = client.multiSearch(request);
			SearchHits<String> searchHits = new ResponseConverter().mapResults(sdkResults, String.class);

			assertThat(searchHits.getSearchHits()).isEmpty();
			assertThat(searchHits.getTotalHits()).isZero();
			assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
			assertThat(searchHits.getResponseMetadata()).hasSize(2);

			SearchResponseMetadata exact = searchHits.getResponseMetadata().get(0);
			assertThat(exact.getQueryIndex()).isZero();
			assertThat(exact.getTotalHits()).isEqualTo(5L);
			assertThat(exact.getEstimatedTotalHits()).isNull();
			assertThat(exact.getTotalHitsRelation()).isEqualTo(SearchResponseTotalHitsRelation.EQUAL_TO);
			assertThat(exact.getPage()).isEqualTo(1);
			assertThat(exact.getHitsPerPage()).isEqualTo(1);
			assertThat(exact.getTotalPages()).isEqualTo(5);

			SearchResponseMetadata estimated = searchHits.getResponseMetadata().get(1);
			assertThat(estimated.getQueryIndex()).isEqualTo(1);
			assertThat(estimated.getTotalHits()).isNull();
			assertThat(estimated.getEstimatedTotalHits()).isEqualTo(7L);
			assertThat(estimated.getTotalHitsRelation()).isEqualTo(SearchResponseTotalHitsRelation.ESTIMATED);
			assertThat(estimated.getPage()).isNull();
			assertThat(estimated.getOffset()).isZero();
			assertThat(estimated.getLimit()).isEqualTo(10);
		} finally {
			server.stop(0);
		}
	}

	private static HttpServer serverRespondingWith(String responseBody) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/multi-search", exchange -> {
			byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (OutputStream body = exchange.getResponseBody()) {
				body.write(response);
			}
		});
		server.start();
		return server;
	}

}
