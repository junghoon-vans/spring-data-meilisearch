package io.vanslog.spring.data.meilisearch.core;

import com.meilisearch.sdk.Client;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@MeilisearchTest
@ContextConfiguration(classes = {MeilisearchTestConfiguration.class})
class MeilisearchTemplateTest {

    @Autowired
    MeilisearchOperations meilisearchTemplate;

    @Autowired
    Client meilisearchClient;

    Movie movie1 = new Movie(1, "Carol", "A love story",
            new String[]{"Romance", "Drama"});
    Movie movie2 = new Movie(2, "Wonder Woman", "A superhero film",
            new String[]{"Action", "Adventure"});

    @BeforeEach
    void setUp() {
        meilisearchTemplate.deleteAll(Movie.class);
    }

    @Test
    void shouldSaveDocument() {
        meilisearchTemplate.save(movie1);
        Movie saved = meilisearchTemplate.get("1", Movie.class);

        assertThat(saved.getId()).isEqualTo(movie1.getId());
        assertThat(saved.getTitle()).isEqualTo(movie1.getTitle());
        assertThat(saved.getDescription()).isEqualTo(movie1.getDescription());
        assertThat(saved.getGenres()).isEqualTo(movie1.getGenres());
    }

    @Test
    void shouldSaveDocuments() {
        List<Movie> movies = new ArrayList<>();
        movies.add(movie1);
        movies.add(movie2);

        meilisearchTemplate.save(movies);
        List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

        assertThat(saved.size()).isEqualTo(2);
    }
}
