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
package io.vanslog.spring.data.meilisearch.client.msc;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.model.FacetSearchable;
import com.meilisearch.sdk.model.MultiSearchResult;
import com.meilisearch.sdk.model.Results;
import com.meilisearch.sdk.model.SearchResult;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.SimilarDocumentsResults;

import io.vanslog.spring.data.meilisearch.core.SearchHit;
import io.vanslog.spring.data.meilisearch.core.SearchHits;
import io.vanslog.spring.data.meilisearch.core.TotalHitsRelation;
import io.vanslog.spring.data.meilisearch.core.FacetHit;
import io.vanslog.spring.data.meilisearch.core.convert.MappingMeilisearchConverter;
import io.vanslog.spring.data.meilisearch.core.convert.MeilisearchCustomConversions;
import io.vanslog.spring.data.meilisearch.core.document.Document;
import io.vanslog.spring.data.meilisearch.core.mapping.SimpleMeilisearchMappingContext;

/**
 * Unit tests for {@link ResponseConverter}.
 *
 * @author Junghoon Ban
 */
class ResponseConverterUnitTests {

	private final ResponseConverter converter = new ResponseConverter();

	@Test
	void shouldConvertPaginatedTotalHitsMetadataToSearchHits() {
		SearchResultPaginated result = new SearchResultPaginated();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hit("143", "Escape Room"));
		hits.add(hit("144", "Carol"));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "totalHits", 10);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getTotalHits()).isEqualTo(10);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.EQUAL_TO);
	}

	@Test
	void shouldConvertRawPaginatedTotalHitsMetadataToSearchHits() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		MeilisearchSearchResult result = MeilisearchSearchResult
				.from(objectMapper.readTree("{" + "\"hits\": [{\"id\": \"143\", \"title\": \"Escape Room\"}],"
						+ "\"processingTimeMs\": 25," + "\"query\": \"escape\"," + "\"totalHits\": 10" + "}"), objectMapper);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(10);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.EQUAL_TO);
	}

	@Test
	void shouldConvertRawFacetStatsToFacetRating() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		MeilisearchSearchResult result = MeilisearchSearchResult.from(
				objectMapper
						.readTree("{" + "\"hits\": [{\"id\": \"143\", \"title\": \"Escape Room\"}]," + "\"processingTimeMs\": 25,"
								+ "\"query\": \"escape\"," + "\"facetStats\": {\"rating\": {\"min\": 1, \"max\": 5}}" + "}"),
				objectMapper);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHit(0).getFacetStats().get("rating").getMin()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getFacetStats().get("rating").getMax()).isEqualTo(5);
	}

	@Test
	void shouldMapRequestedAndAbsentHitMetadataWithoutChangingWireFields() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();
		MeilisearchSearchResult result = MeilisearchSearchResult.from(objectMapper.readTree("""
				{
				  "hits": [
				    {
				      "id": "143",
				      "title": "Audit one",
				      "_formatted": {"title": "<em>Audit</em> one"},
				      "_matchesPosition": {"title": [{"start": 0, "length": 5}]},
				      "_rankingScore": 0.91,
				      "_rankingScoreDetails": {"words": {"order": 0, "matchingWords": 1}}
				    },
				    {"id": "144", "title": "Audit two"}
				  ],
				  "processingTimeMs": 25,
				  "query": "Audit"
				}
				"""), objectMapper);

		SearchHits<MetadataMovie> searchHits = converter.mapHits(result, MetadataMovie.class);
		SearchHit<MetadataMovie> requested = searchHits.getSearchHit(0);

		assertThat(requested.getFormattedFields()).containsEntry("title", "<em>Audit</em> one");
		assertThat(requested.getMatchesPosition()).containsEntry("title", List.of(Map.of("start", 0, "length", 5)));
		assertThat(requested.getRankingScore()).isEqualTo(0.91);
		assertThat(requested.getRankingScoreDetails()).containsEntry("words", Map.of("order", 0, "matchingWords", 1));
		assertThat(requested.getContent().getTitle()).isEqualTo("Audit one");
		assertThat(ReflectionTestUtils.getField(requested.getContent(), "_formatted"))
				.isEqualTo(requested.getFormattedFields());
		assertThat(ReflectionTestUtils.getField(requested.getContent(), "_matchesPosition"))
				.isEqualTo(requested.getMatchesPosition());
		assertThat(ReflectionTestUtils.getField(requested.getContent(), "_rankingScore"))
				.isEqualTo(requested.getRankingScore());
		assertThat(ReflectionTestUtils.getField(requested.getContent(), "_rankingScoreDetails"))
				.isEqualTo(requested.getRankingScoreDetails());

		@SuppressWarnings("unchecked")
		Map<String, Object> wireFormatted = (Map<String, Object>) result.getHits().get(0).get("_formatted");
		wireFormatted.put("title", "changed in source map");
		assertThat(requested.getFormattedFields()).containsEntry("title", "<em>Audit</em> one");
		assertThatThrownBy(() -> requested.getFormattedFields().put("title", "changed in returned metadata"))
				.isInstanceOf(UnsupportedOperationException.class);

		SearchHit<MetadataMovie> absent = searchHits.getSearchHit(1);
		assertThat(absent.getContent().getTitle()).isEqualTo("Audit two");
		assertThat(absent.getFormattedFields()).isNull();
		assertThat(absent.getMatchesPosition()).isNull();
		assertThat(absent.getRankingScore()).isNull();
		assertThat(absent.getRankingScoreDetails()).isNull();
	}

	@Test
	void shouldKeepTotalHitsOffForEstimatedSearchResults() {
		SearchResult result = new SearchResult();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hit("143", "Escape Room"));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "estimatedTotalHits", 15);

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	@Test
	void shouldConvertSimilarDocumentsResultsToSearchHits() {
		SimilarDocumentsResults result = new SimilarDocumentsResults();
		ArrayList<HashMap<String, Object>> hits = new ArrayList<>();
		hits.add(hitWithMetadata("143", "Escape Room", 0.84));

		ReflectionTestUtils.setField(result, "hits", hits);
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "estimatedTotalHits", 15);

		SearchHits<SimilarMovie> searchHits = converter.mapResult(result, SimilarMovie.class);

		assertThat(searchHits.getExecutionDuration().toMillis()).isEqualTo(25);
		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
		SearchHit<SimilarMovie> searchHit = searchHits.getSearchHit(0);
		assertThat(searchHit.getProcessingTimeMs()).isEqualTo(25);
		assertThat(searchHit.getQuery()).isEmpty();
		assertThat(searchHit.getContent().getId()).isEqualTo("143");
		assertThat(searchHit.getContent().getTitle()).isEqualTo("Escape Room");
		assertHitMetadata(searchHit, "Escape Room", 0.84);
	}

	@Test
	void shouldKeepTotalHitsOffForFlattenedMultiSearchResults() {
		MultiSearchResult result1 = new MultiSearchResult();
		ReflectionTestUtils.setField(result1, "hits", new ArrayList<>(List.of(hit("143", "Escape Room"))));
		ReflectionTestUtils.setField(result1, "processingTimeMs", 25);

		MultiSearchResult result2 = new MultiSearchResult();
		ReflectionTestUtils.setField(result2, "hits", new ArrayList<>(List.of(hit("144", "Carol"))));
		ReflectionTestUtils.setField(result2, "processingTimeMs", 15);

		Results<MultiSearchResult> results = new Results<>();
		ReflectionTestUtils.setField(results, "results", new MultiSearchResult[] { result1, result2 });

		SearchHits<SimilarMovie> searchHits = converter.mapResults(results, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(2);
		assertThat(searchHits.getTotalHits()).isEqualTo(2);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	@Test
	void shouldMapHitMetadataFromEachNonFederatedMultiSearchResponse() {
		MultiSearchResult first = new MultiSearchResult();
		ReflectionTestUtils.setField(first, "hits", new ArrayList<>(List.of(hitWithMetadata("143", "Audit one", 0.91))));
		ReflectionTestUtils.setField(first, "processingTimeMs", 25);
		ReflectionTestUtils.setField(first, "query", "Audit one");

		MultiSearchResult second = new MultiSearchResult();
		ReflectionTestUtils.setField(second, "hits", new ArrayList<>(List.of(hit("144", "Audit two"))));
		ReflectionTestUtils.setField(second, "processingTimeMs", 15);
		ReflectionTestUtils.setField(second, "query", "Audit two");

		Results<MultiSearchResult> results = new Results<>();
		ReflectionTestUtils.setField(results, "results", new MultiSearchResult[] { first, second });

		SearchHits<SimilarMovie> searchHits = converter.mapResults(results, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).extracting(hit -> hit.getContent().getTitle()).containsExactly("Audit one",
				"Audit two");
		assertHitMetadata(searchHits.getSearchHit(0), "<em>Audit</em> one", 0.91);
		assertThat(searchHits.getSearchHit(1).getFormattedFields()).isNull();
		assertThat(searchHits.getSearchHit(1).getMatchesPosition()).isNull();
		assertThat(searchHits.getSearchHit(1).getRankingScore()).isNull();
		assertThat(searchHits.getSearchHit(1).getRankingScoreDetails()).isNull();
	}

	@Test
	void shouldMapHitMetadataAlongsideFederationMetadata() {
		HashMap<String, Object> firstHit = hitWithMetadata("143", "Audit one", 0.91);
		firstHit.put("_federation", Map.of("indexUid", "movies", "queriesPosition", 0.0, "weightedRankingScore", 0.8));
		HashMap<String, Object> secondHit = hitWithMetadata("144", "Audit two", 0.82);
		secondHit.put("_federation", Map.of("indexUid", "comics", "queriesPosition", 1.0, "weightedRankingScore", 0.7));

		MultiSearchResult result = new MultiSearchResult();
		ReflectionTestUtils.setField(result, "hits", new ArrayList<>(List.of(firstHit, secondHit)));
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);
		ReflectionTestUtils.setField(result, "query", "Audit");

		SearchHits<SimilarMovie> searchHits = converter.mapResult(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).extracting(hit -> hit.getContent().getTitle()).containsExactly("Audit one",
				"Audit two");
		assertHitMetadata(searchHits.getSearchHit(0), "<em>Audit</em> one", 0.91);
		assertThat(searchHits.getSearchHit(0).getFederation().getIndexUid()).isEqualTo("movies");
		assertHitMetadata(searchHits.getSearchHit(1), "<em>Audit</em> two", 0.82);
		assertThat(searchHits.getSearchHit(1).getFederation().getIndexUid()).isEqualTo("comics");
	}

	private static HashMap<String, Object> hitWithMetadata(String id, String title, double rankingScore) {
		HashMap<String, Object> hit = hit(id, title);
		String formattedTitle = title.replace("Audit", "<em>Audit</em>");
		hit.put("_formatted", Map.of("title", formattedTitle));
		hit.put("_matchesPosition", Map.of("title", List.of(Map.of("start", 0, "length", 5))));
		hit.put("_rankingScore", rankingScore);
		hit.put("_rankingScoreDetails", Map.of("words", Map.of("order", 0, "matchingWords", 1)));
		return hit;
	}

	private static void assertHitMetadata(SearchHit<?> hit, String formattedTitle, double rankingScore) {
		assertThat(hit.getFormattedFields()).containsEntry("title", formattedTitle);
		assertThat(hit.getMatchesPosition()).containsEntry("title", List.of(Map.of("start", 0, "length", 5)));
		assertThat(hit.getRankingScore()).isEqualTo(rankingScore);
		assertThat(hit.getRankingScoreDetails()).containsEntry("words", Map.of("order", 0, "matchingWords", 1));
	}

	@Test
	void shouldKeepTotalHitsOffForFacetSearchResults() {
		FacetSearchable result = new FacetSearchable() {
			@Override
			public ArrayList<HashMap<String, Object>> getFacetHits() {
				return new ArrayList<>(List.of(hit("143", "Escape Room")));
			}

			@Override
			public int getProcessingTimeMs() {
				return 25;
			}

			@Override
			public String getFacetQuery() {
				return "Escape";
			}
		};

		SearchHits<SimilarMovie> searchHits = converter.mapHits(result, SimilarMovie.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getTotalHitsRelation()).isEqualTo(TotalHitsRelation.OFF);
	}

	@Test
	void shouldMapFacetHitsAsResponseDtos() {
		FacetSearchable result = new FacetSearchable() {
			@Override
			public ArrayList<HashMap<String, Object>> getFacetHits() {
				return new ArrayList<>(List.of(facetHit("Drama", 2)));
			}

			@Override
			public int getProcessingTimeMs() {
				return 25;
			}

			@Override
			public String getFacetQuery() {
				return "Dra";
			}
		};

		SearchHits<FacetHit> searchHits = converter.mapHits(result, FacetHit.class);

		assertThat(searchHits.getSearchHit(0).getContent().value()).isEqualTo("Drama");
		assertThat(searchHits.getSearchHit(0).getContent().count()).isEqualTo(2);
	}

	@Test
	void shouldMaterializeSearchHitsThroughMeilisearchConverter() {
		MappingMeilisearchConverter mappingConverter = new MappingMeilisearchConverter(
				new SimpleMeilisearchMappingContext());
		mappingConverter.setConversions(new MeilisearchCustomConversions(List.of(new DocumentToCodeConverter())));
		mappingConverter.afterPropertiesSet();
		ResponseConverter converter = new ResponseConverter(mappingConverter, new ObjectMapper());
		SearchResult result = new SearchResult();
		HashMap<String, Object> hit = hit("143", "Escape Room");
		hit.put("code", Map.of("raw", "custom-code"));

		ReflectionTestUtils.setField(result, "hits", new ArrayList<>(List.of(hit)));
		ReflectionTestUtils.setField(result, "processingTimeMs", 25);

		SearchHits<MovieWithConvertedProperty> searchHits = converter.mapHits(result, MovieWithConvertedProperty.class);

		assertThat(searchHits.getSearchHit(0).getContent().getCode().getValue()).isEqualTo("custom-code");
	}

	private static HashMap<String, Object> hit(String id, String title) {
		HashMap<String, Object> hit = new HashMap<>();
		hit.put("id", id);
		hit.put("title", title);
		return hit;
	}

	private static HashMap<String, Object> facetHit(String value, int count) {
		HashMap<String, Object> hit = new HashMap<>();
		hit.put("value", value);
		hit.put("count", count);
		return hit;
	}

	static class SimilarMovie {

		private String id;
		private String title;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	static class MetadataMovie {

		@Id private String id;
		private String title;
		private Map<String, Object> _formatted;
		private Map<String, Object> _matchesPosition;
		private Double _rankingScore;
		private Map<String, Object> _rankingScoreDetails;

		public String getTitle() {
			return title;
		}
	}

	@io.vanslog.spring.data.meilisearch.annotations.Document(indexUid = "converted-movies")
	static class MovieWithConvertedProperty {

		@Id private String id;
		private Code code;

		public String getId() {
			return id;
		}

		public Code getCode() {
			return code;
		}
	}

	static class Code {

		private final String value;

		Code(String value) {
			this.value = value;
		}

		String getValue() {
			return value;
		}
	}

	@ReadingConverter
	static class DocumentToCodeConverter implements Converter<Document, Code> {

		@Override
		public Code convert(Document source) {
			return new Code((String) source.get("raw"));
		}
	}
}
