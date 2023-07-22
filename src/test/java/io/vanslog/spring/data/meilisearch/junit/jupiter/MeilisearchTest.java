package io.vanslog.spring.data.meilisearch.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to activate Meilisearch integration tests.
 *
 * @author Junghoon Ban
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(MeilisearchExtension.class)
public @interface MeilisearchTest {
}
