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
package io.vanslog.spring.data.meilisearch.consumer.declared;

import org.springframework.data.annotation.Id;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Pagination;
import io.vanslog.spring.data.meilisearch.annotations.Setting;

/**
 * Isolated product index for declared repository search integration tests.
 *
 * @author Junghoon Ban
 */
@Document(indexUid = "declared-products")
@Setting(filterableAttributes = { "title", "category", "price", "active" }, sortableAttributes = { "price" },
		searchableAttributes = { "title" })
@Pagination(maxTotalHits = 2000)
public class DeclaredProduct {

	@Id private String id;
	private String title;
	private String category;
	private int price;
	private boolean active;

	public DeclaredProduct() {}

	public DeclaredProduct(String id, String title, String category, int price, boolean active) {

		this.id = id;
		this.title = title;
		this.category = category;
		this.price = price;
		this.active = active;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getCategory() {
		return category;
	}

	public int getPrice() {
		return price;
	}

	public boolean isActive() {
		return active;
	}
}
