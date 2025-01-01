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

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * Uncategorized exception for Meilisearch.
 *
 * @author Junghoon Ban
 */
public class UncategorizedMeilisearchException extends UncategorizedDataAccessException {

	/**
	 * Constructor for UncategorizedMeilisearchException.
	 * 
	 * @param message the detail message
	 */
	public UncategorizedMeilisearchException(String message) {
		super(message, null);
	}

	/**
	 * Constructor for UncategorizedMeilisearchException.
	 * 
	 * @param message the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public UncategorizedMeilisearchException(String message, Throwable cause) {
		super(message, cause);
	}
}
