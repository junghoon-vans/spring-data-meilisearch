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
package io.vanslog.spring.data.meilisearch.core;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

public class SearchHitSupport {

	private SearchHitSupport() {}

	/**
	 * unwraps the data contained in a SearchHit for different types containing SearchHits if possible
	 *
	 * @param result the object, list, page or whatever containing SearchHit objects
	 * @return a corresponding object where the SearchHits are replaced by their content if possible, otherwise the
	 *         original object
	 */
	@Nullable
	public static Object unwrapSearchHits(@Nullable Object result) {

		if (result == null) {
			return null;
		}

		if (result instanceof SearchHit<?> searchHit) {
			return searchHit.content();
		}

		if (result instanceof List<?> list) {
			return list.stream() //
					.map(SearchHitSupport::unwrapSearchHits) //
					.toList();
		}

		if (result instanceof Stream<?> stream) {
			return stream.map(SearchHitSupport::unwrapSearchHits);
		}

		if (result instanceof SearchHits<?> searchHits) {
			return unwrapSearchHits(searchHits.getSearchHits());
		}

		if (result instanceof SearchPage<?> searchPage) {
			List<?> content = (List<?>) SearchHitSupport.unwrapSearchHits(searchPage.getSearchHits());
			return new PageImpl<>(content, searchPage.getPageable(), searchPage.getTotalElements());
		}

		return result;
	}

	public static <T> SearchPage<T> searchPageFor(SearchHits<T> searchHits, @Nullable Pageable pageable) {
		return new SearchPageImpl<>(searchHits, (pageable != null) ? pageable : Pageable.unpaged());
	}

	static class SearchPageImpl<T> extends PageImpl<SearchHit<T>> implements SearchPage<T> {

		private final SearchHits<T> searchHits;

		public SearchPageImpl(SearchHits<T> searchHits, Pageable pageable) {
			super(searchHits.getSearchHits(), pageable, searchHits.getTotalHits());
			this.searchHits = searchHits;
		}

		@Override
		public SearchHits<T> getSearchHits() {
			return searchHits;
		}

		/*
		 * return the same instance as in getSearchHits().getSearchHits()
		 */
		@Override
		public List<SearchHit<T>> getContent() {
			return searchHits.getSearchHits();
		}
	}
}
