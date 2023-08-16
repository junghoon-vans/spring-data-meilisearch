package io.vanslog.spring.data.meilisearch.core;

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;
import io.vanslog.spring.data.meilisearch.entities.Movie;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTest;
import io.vanslog.spring.data.meilisearch.junit.jupiter.MeilisearchTestConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

@MeilisearchTest
@ContextConfiguration(classes = {MeilisearchTestConfiguration.class})
class MeilisearchTemplateTest {

    @Autowired
    MeilisearchOperations meilisearchTemplate;

    Movie movie1 = new Movie(1, "Carol", "A love story",
            new String[] {"Romance", "Drama"});
    Movie movie2 = new Movie(2, "Wonder Woman", "A superhero film",
            new String[] {"Action", "Adventure"});
    Movie movie3 = new Movie(3, "Life of Pi", "A survival film",
            new String[] {"Adventure", "Drama"});

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
        meilisearchTemplate.save(List.of(movie1, movie2));
        List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);
        assertThat(saved.size()).isEqualTo(2);
    }

    @Test
    void shouldDeleteDocument() {
        meilisearchTemplate.save(movie1);
        meilisearchTemplate.delete(movie1);

        Throwable thrown =
                catchThrowable(() -> meilisearchTemplate.get("1", Movie.class));

        assertThat(thrown).isInstanceOf(
                UncategorizedMeilisearchException.class);
    }

    @Test
    void shouldDeleteDocuments() {
        meilisearchTemplate.save(List.of(movie1, movie2, movie3));
        meilisearchTemplate.delete(Movie.class, List.of("1", "2"));

        List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

        assertThat(saved.size()).isEqualTo(1);
    }

    @Test
    void shouldDeleteAllDocuments() {
        meilisearchTemplate.save(List.of(movie1, movie2));
        meilisearchTemplate.deleteAll(Movie.class);

        List<Movie> saved = meilisearchTemplate.multiGet(Movie.class);

        assertThat(saved.size()).isZero();
    }

    @Test
    void shouldCountDocuments() {
        meilisearchTemplate.save(List.of(movie1, movie2));
        long count = meilisearchTemplate.count(Movie.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldExistsDocument() {
        meilisearchTemplate.save(movie1);
        boolean exists = meilisearchTemplate.exists("1", Movie.class);
        assertThat(exists).isTrue();
    }
}
