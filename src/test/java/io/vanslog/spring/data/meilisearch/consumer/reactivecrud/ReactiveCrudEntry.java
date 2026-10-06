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
package io.vanslog.spring.data.meilisearch.consumer.reactivecrud;

import org.springframework.data.annotation.Id;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;

/**
 * Consumer fixture for reactive repository CRUD integration coverage.
 *
 * @author Junghoon Ban
 */
@Setting(filterableAttributes = { "category" }, sortableAttributes = { "rank" })
@Document(indexUid = "gh215-reactive-crud")
public class ReactiveCrudEntry {

	@Id
	private Long id;

	private String title;

	private String category;

	private int rank;

	public ReactiveCrudEntry() {
	}

	public ReactiveCrudEntry(Long id, String title, String category, int rank) {
		this.id = id;
		this.title = title;
		this.category = category;
		this.rank = rank;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getCategory() {
		return category;
	}

	public int getRank() {
		return rank;
	}

}
