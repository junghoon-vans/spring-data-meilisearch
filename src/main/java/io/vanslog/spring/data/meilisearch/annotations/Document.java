package io.vanslog.spring.data.meilisearch.annotations;

import org.springframework.data.annotation.Persistent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a domain object to be persisted to Meilisearch.
 *
 * @author Junghoon Ban
 * @since 1.0.0
 */

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Document {

    /**
     * UID of the Meilisearch index.
     * @return Index UID
     */
    String indexUid();
}
