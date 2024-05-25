/*
 * Copyright 2023 the original author or authors.
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
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Setting {

	/**
	 * attributes to define an index sorting
	 */
	String[] sortAttributes() default {};

	String[] filterableAttributes() default {};

	String distinctAttribute() default "";

	String[] searchableAttributes() default { "*" };

	String[] displayedAttributes() default { "*" };

	String[] rankingRules() default { "words", "typo", "proximity", "attribute", "sort", "exactness" };

	String[] stopWords() default {};
}
