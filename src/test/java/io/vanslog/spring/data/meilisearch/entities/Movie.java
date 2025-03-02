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
package io.vanslog.spring.data.meilisearch.entities;

import io.vanslog.spring.data.meilisearch.annotations.Document;
import io.vanslog.spring.data.meilisearch.annotations.Setting;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.data.annotation.Id;

/**
 * Movie entity for tests.
 *
 * @author Junghoon Ban
 */
@Setting(filterableAttributes = { "genres" })
@Document(indexUid = "movies")
@SuppressWarnings("unused")
public class Movie {

	@Id private int id;
	private String title;
	private String description;
	private String[] genres;

	public Movie() {}

	public Movie(int id, String title, String description, String[] genres) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.genres = genres;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String[] getGenres() {
		return genres;
	}

	public void setGenres(String[] genres) {
		this.genres = genres;
	}

	@Override
	public boolean equals(Object object) {

		if (this == object) {
			return true;
		}

		if (object == null || getClass() != object.getClass()) {
			return false;
		}

		Movie movie = (Movie) object;
		return id == movie.id && Objects.equals(title, movie.title) && Objects.equals(description, movie.description)
				&& Arrays.equals(genres, movie.genres);
	}

	@Override
	public int hashCode() {

		int result = Objects.hash(id, title, description);
		result = 31 * result + Arrays.hashCode(genres);
		return result;
	}
}
