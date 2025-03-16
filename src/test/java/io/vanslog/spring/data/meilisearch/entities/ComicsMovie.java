/*
 * Copyright 2025 the original author or authors.
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
package io.vanslog.spring.data.meilisearch.entities;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;

/**
 * ComicsMovie entity for tests.
 *
 * @author Junghoon Ban
 */
@Setting(filterableAttributes = { "genres" })
@Document(indexUid = "comics")
@SuppressWarnings("unused")
public class ComicsMovie extends Movie {

	public ComicsMovie(int id, String title, String description, String[] genres) {
		super(id, title, description, genres);
	}
}
