package io.vanslog.spring.data.meilisearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.annotation.Persistent;

@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
/**
 * Identifies a domain object to be persisted to Meilisearch.
 *
 * @since 1.0.0
 * @auther Junghoon Ban
 */
public @interface Document {
  String indexUid();
}
