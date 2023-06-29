package io.vanslog.spring.data.meilisearch.core.mapping;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

/**
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentEntityTests {

  private SimpleMeilisearchPersistentEntity<Movie> entityPersistentEntity;

  @BeforeEach
  void setup() {
    TypeInformation<Movie> entityTypeInfo = TypeInformation.of(Movie.class);
    entityPersistentEntity = new SimpleMeilisearchPersistentEntity<>(entityTypeInfo);
  }

  @Test
  void testGetIndexUid() {
    String indexUid = entityPersistentEntity.getIndexUid();
    Assertions.assertEquals("movie", indexUid);
  }

  @Test
  void testGetPrimaryKey() {
    String primaryKey = entityPersistentEntity.getPrimaryKey();
    Assertions.assertEquals("id", primaryKey);
  }

  @Document(indexUid = "movie")
  @SuppressWarnings("unused")
  private static class Movie {
    private String id;
    private String title;
    private String description;
    private String[] genres;
  }
}
