package io.vanslog.spring.data.meilisearch.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanslog.spring.data.meilisearch.entities.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentEntity}.
 *
 * @author Junghoon Ban
 */
public class SimpleMeilisearchPersistentEntityTests {

  @Test
  void shouldReturnIndexUid() {
    TypeInformation<Movie> entityTypeInfo = TypeInformation.of(Movie.class);
    SimpleMeilisearchPersistentEntity<Movie> entityPersistentEntity = new SimpleMeilisearchPersistentEntity<>(entityTypeInfo);
    String indexUid = entityPersistentEntity.getIndexUid();
    assertThat(indexUid).isEqualTo("movie");
  }
}
