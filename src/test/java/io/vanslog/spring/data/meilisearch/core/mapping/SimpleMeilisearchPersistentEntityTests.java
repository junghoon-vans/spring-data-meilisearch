package io.vanslog.spring.data.meilisearch.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentEntity}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentEntityTests {

  @Test
  void shouldReturnIndexUid() {
    TypeInformation<Movie> entityTypeInfo = TypeInformation.of(Movie.class);
    SimpleMeilisearchPersistentEntity<Movie> entityPersistentEntity = new SimpleMeilisearchPersistentEntity<>(entityTypeInfo);
    String indexUid = entityPersistentEntity.getIndexUid();
    assertThat(indexUid).isEqualTo("movies");
  }

  @Test
  void shouldThrowExceptionWhenIndexUidIsBlank() {
    TypeInformation<EmptyIndexUidDocument> entityTypeInfo = TypeInformation.of(
        EmptyIndexUidDocument.class);
    assertThatThrownBy(() -> new SimpleMeilisearchPersistentEntity<>(entityTypeInfo))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Document(indexUid = "")
  static class EmptyIndexUidDocument { }
}
