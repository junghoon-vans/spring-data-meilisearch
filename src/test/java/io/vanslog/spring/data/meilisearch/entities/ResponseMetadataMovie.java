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
package io.vanslog.spring.data.meilisearch.entities;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;

import org.springframework.data.annotation.Id;

/**
 * Search fixture with both categorical and numeric facets.
 *
 * @author Junghoon Ban
 */
@Setting(filterableAttributes = { "category", "rank" })
@Document(indexUid = "response-metadata-movies")
@SuppressWarnings("unused")
public class ResponseMetadataMovie {

	@Id private String id;
	private String title;
	private String category;
	private int rank;

	public ResponseMetadataMovie() {}

	public ResponseMetadataMovie(String id, String title, String category, int rank) {
		this.id = id;
		this.title = title;
		this.category = category;
		this.rank = rank;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
}
