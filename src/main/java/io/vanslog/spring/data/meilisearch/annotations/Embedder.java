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
package io.vanslog.spring.data.meilisearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Persistent;

/**
 * Meilisearch embedder setting.
 *
 * @author Junghoon Ban
 * @see <a href="https://www.meilisearch.com/docs/reference/api/settings#embedders">Embedders</a>
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface Embedder {

	String name();

	Source source() default Source.DEFAULT;

	String apiKey() default "";

	String model() default "";

	String documentTemplate() default "";

	int dimensions() default -1;

	double distributionMean() default Double.NaN;

	double distributionSigma() default Double.NaN;

	EmbedderParameter[] request() default {};

	EmbedderParameter[] response() default {};

	int documentTemplateMaxBytes() default -1;

	String revision() default "";

	EmbedderParameter[] headers() default {};

	TriState binaryQuantized() default TriState.DEFAULT;

	String url() default "";

	String[] inputField() default {};

	InputType inputType() default InputType.DEFAULT;

	String query() default "";

	enum Source {

		DEFAULT, OPEN_AI, HUGGING_FACE, OLLAMA, REST, USER_PROVIDED
	}

	enum InputType {

		DEFAULT, TEXT, TEXT_ARRAY
	}

	enum TriState {

		DEFAULT, TRUE, FALSE
	}
}
