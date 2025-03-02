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
package io.vanslog.spring.data.meilisearch.core.query;

import org.springframework.lang.Nullable;

import com.meilisearch.sdk.model.MatchingStrategy;

/**
 * Query interface for Meilisearch operations
 *
 * @author Junghoon Ban
 */
public interface Query {

	/**
	 * Get the search query text
	 *
	 * @return null if not set
	 */
	@Nullable
	String getQ();

	/**
	 * Set the search query text
	 *
	 * @param q search query text
	 */
	void setQ(@Nullable String q);

	/**
	 * Get the search filter
	 * 
	 * @return null if not set
	 */
	@Nullable
	String[] getFilter();

	/**
	 * Set the search filter
	 *
	 * @param filter search filter
	 */
	void setFilter(@Nullable String[] filter);

	/**
	 * Get the search filter array
	 * 
	 * @return null if not set
	 */
	@Nullable
	String[][] getFilterArray();

	/**
	 * Set the search filter array
	 *
	 * @param filterArray search filter array
	 */
	void setFilterArray(@Nullable String[][] filterArray);

	/**
	 * Get the search sort
	 *
	 * @return null if not set
	 */
	@Nullable
	MatchingStrategy getMatchingStrategy();

	/**
	 * Set the search sort
	 *
	 * @param matchingStrategy search sort
	 */
	void setMatchingStrategy(@Nullable MatchingStrategy matchingStrategy);

	/**
	 * Get the search attributes
	 *
	 * @return null if not set
	 */
	@Nullable
	String[] getAttributesToSearchOn();

	/**
	 * Set the search attributes
	 *
	 * @param attributesToSearchOn search attributes
	 */
	void setAttributesToSearchOn(@Nullable String[] attributesToSearchOn);
}
