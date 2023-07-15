package io.vanslog.spring.data.meilisearch.core.support;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import io.vanslog.spring.data.meilisearch.entities.Movie;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultEntityMapperTest {

  private static final String JSON_STRING = "{\"id\":\"1\",\"title\":\"Carol\","
          + "\"description\":\"romantic period drama film\",\"genres\":[\"Drama\",\"Romance\"]}";
  private static final Movie MOVIE = new Movie();

  DefaultEntityMapper defaultEntityMapper;

  @BeforeEach
  void setUp() {
    defaultEntityMapper = new DefaultEntityMapper();

    MOVIE.setId("1");
    MOVIE.setTitle("Carol");
    MOVIE.setDescription("romantic period drama film");
    MOVIE.setGenres(new String[]{"Drama", "Romance"});
  }

  @Test
  void toJson() throws IOException {
    String actual = defaultEntityMapper.toJson(MOVIE);

    assertThat(actual).isEqualTo(JSON_STRING);
  }

  @Test
  void fromJson() throws IOException {
    Movie actual = defaultEntityMapper.fromJson(JSON_STRING, Movie.class);

    assertThat(actual.getId()).isEqualTo(MOVIE.getId());
    assertThat(actual.getTitle()).isEqualTo(MOVIE.getTitle());
    assertThat(actual.getDescription()).isEqualTo(MOVIE.getDescription());
    assertThat(actual.getGenres()).isEqualTo(MOVIE.getGenres());
  }
}