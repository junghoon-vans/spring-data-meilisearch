/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vanslog.spring.data.meilisearch.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.QueryAnnotation;

/**
 * Declares a repository search using native Meilisearch filters and optional search text. Filter fields refer to stored
 * index field names. Positional placeholders such as {@code ?0} refer to method parameter positions and bind values,
 * not filter expression fragments. Declared searches replace method-name derivation and use the repository's usual
 * finder return forms, sorting, and paging. Complex search options remain available through operations.
 *
 * @author Junghoon Ban
 * @since 0.12.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {

	/**
	 * Native Meilisearch filter template. Alias for {@link #filter()}.
	 */
	@AliasFor("filter")
	String value() default "";

	/**
	 * Native Meilisearch filter template. Placeholders must be unquoted, complete values. Alias for {@link #value()}.
	 */
	@AliasFor("value")
	String filter() default "";

	/**
	 * Full-text search template. Text parameters are bound without filter literal quoting.
	 */
	String q() default "";
}
