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
package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.NonTransientDataAccessResourceException;

/**
 * Exception indicating that an index could not be found.
 *
 * @author Junghoon Ban
 */
public class IndexAccessException extends NonTransientDataAccessResourceException {

	private final String indexUid;

	/**
	 * Constructor for IndexAccessException.
	 * 
	 * @param indexUid the index uid
	 */
	public IndexAccessException(String indexUid) {
		super(String.format("Index %s not should be created.", indexUid));
		this.indexUid = indexUid;
	}

	/**
	 * Constructor for IndexAccessException.
	 * 
	 * @param indexUid the index uid
	 * @param cause the root cause from the data access API in use
	 */
	public IndexAccessException(String indexUid, Throwable cause) {
		super(String.format("Index %s not should be created.", indexUid), cause);
		this.indexUid = indexUid;
	}

	public String getIndexUid() {
		return indexUid;
	}
}
