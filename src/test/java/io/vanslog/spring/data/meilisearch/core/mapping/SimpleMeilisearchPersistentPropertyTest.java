package io.vanslog.spring.data.meilisearch.core.mapping;

import io.vanslog.spring.data.meilisearch.entities.Movie;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SimpleMeilisearchPersistentProperty}.
 *
 * @author Junghoon Ban
 */
class SimpleMeilisearchPersistentPropertyTest {
    private final SimpleMeilisearchMappingContext context =
            new SimpleMeilisearchMappingContext();

    @Test
    void shouldReturnTrueWhenPropertyIsId() {
        SimpleMeilisearchPersistentEntity<?> entity =
                context.getRequiredPersistentEntity(Movie.class);
        SimpleMeilisearchPersistentProperty property =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "id");
        assertThat(property.isIdProperty()).isTrue();
    }

    @Test
    void shouldReturnFieldName() {
        SimpleMeilisearchPersistentEntity<?> entity =
                context.getRequiredPersistentEntity(Movie.class);

        SimpleMeilisearchPersistentProperty idProperty =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "id");
        SimpleMeilisearchPersistentProperty titleProperty =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "title");
        SimpleMeilisearchPersistentProperty descriptionProperty =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "description");
        SimpleMeilisearchPersistentProperty genresProperty =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "genres");

        assertThat(idProperty).isNotNull();
        assertThat(titleProperty).isNotNull();
        assertThat(descriptionProperty).isNotNull();
        assertThat(genresProperty).isNotNull();
    }

    @Test
    void shouldReturnTrueWhenPropertyIsIdAndHasIdAnnotation() {
        SimpleMeilisearchPersistentEntity<?> entity =
                context.getRequiredPersistentEntity(NoIdAnnotation.class);
        SimpleMeilisearchPersistentProperty property =
                (SimpleMeilisearchPersistentProperty) entity.getPersistentProperty(
                        "id");
        assertThat(property.isIdProperty()).isTrue();
    }

    @SuppressWarnings("unused")
    static class NoIdAnnotation {
        private String id;
    }
}
