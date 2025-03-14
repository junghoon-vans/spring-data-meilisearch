/*
 * Copyright 2023-2025 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Persistent;

/**
 * Meilisearch Setting
 *
 * @author Junghoon Ban
 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings">Settings</a>
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Setting {

	/**
	 * attributes to be used for sorting
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#sortable-attributes">Sortable attributes</a>
	 */
	String[] sortableAttributes() default {};

	/**
	 * attribute to be used for distinct
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#distinct-attribute">Distinct attribute</a>
	 */
	String distinctAttribute() default "";

	/**
	 * attributes to be used for searching
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#searchable-attributes">Searchable
	 *      attributes</a>
	 */
	String[] searchableAttributes() default { "*" };

	/**
	 * attributes to be displayed in the search results
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#displayed-attributes">Displayed
	 *      attributes</a>
	 */
	String[] displayedAttributes() default { "*" };

	/**
	 * defines the ranking rules
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#ranking-rules">Ranking rules</a>
	 */
	String[] rankingRules() default { "words", "typo", "proximity", "attribute", "sort", "exactness" };

	/**
	 * defines the stop words
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#stop-words">Stop words</a>
	 */
	String[] stopWords() default {};

	/**
	 * defines the pagination behavior
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#pagination">Pagination</a>
	 */
	Pagination pagination() default @Pagination();

	/**
	 * defines the filterable attributes
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#filterable-attributes">Filterable
	 *      attributes</a>
	 */
	String[] filterableAttributes() default {};

	/**
	 * defines the synonyms
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#synonyms">Synonyms</a>
	 */
	Synonym[] synonyms() default {};

	/**
	 * defines the typo tolerance settings
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#typo-tolerance">Typo tolerance</a>
	 */
	TypoTolerance typoTolerance() default @TypoTolerance();

	/**
	 * defines the faceting settings
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#faceting">Faceting</a>
	 */
	Faceting faceting() default @Faceting();

	/**
	 * defines the dictionary
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#dictionary">Dictionary</a>
	 */
	String[] dictionary() default {};

	/**
	 * defines the proximity precision
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#proximity-precision">Proximity precision</a>
	 */
	String proximityPrecision() default "byWord";

	/**
	 * defines the search cutoff time in milliseconds if value is -1, this setting will be disabled
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#search-cutoff-ms">Search cutoff ms</a>
	 */
	int searchCutoffMs() default -1;

	/**
	 * defines the separator tokens
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#separator-tokens">Separator tokens</a>
	 */
	String[] separatorTokens() default {};

	/**
	 * defines the non-separator tokens
	 *
	 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#non-separator-tokens">Non-separator
	 *      tokens</a>
	 */
	String[] nonSeparatorTokens() default {};
}
