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
package io.vanslog.spring.data.meilisearch;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Exception raised when the Meilisearch REST API returns a non-success HTTP status.
 *
 * @author Junghoon Ban
 */
public class MeilisearchRestException extends DataAccessException {

	private final int statusCode;
	@Nullable private final String code;
	@Nullable private final String type;
	@Nullable private final String link;

	/**
	 * Create an exception for an HTTP response returned by the Meilisearch REST API.
	 *
	 * @param statusCode the HTTP status code
	 * @param code the Meilisearch error code, if available
	 * @param type the Meilisearch error type, if available
	 * @param link the Meilisearch error link, if available
	 */
	public MeilisearchRestException(int statusCode, @Nullable String code, @Nullable String type, @Nullable String link) {
		this(statusCode, code, type, link, null);
	}

	/**
	 * Create an exception for an HTTP response whose error body could not be parsed.
	 *
	 * @param statusCode the HTTP status code
	 * @param code the Meilisearch error code, if available
	 * @param type the Meilisearch error type, if available
	 * @param link the Meilisearch error link, if available
	 * @param cause the parsing cause, if the error body was malformed
	 */
	public MeilisearchRestException(int statusCode, @Nullable String code, @Nullable String type, @Nullable String link,
			@Nullable Throwable cause) {
		super("Meilisearch REST request failed with HTTP status " + statusCode + ".", cause);
		this.statusCode = statusCode;
		this.code = code;
		this.type = type;
		this.link = link;
	}

	/**
	 * Return the HTTP status code.
	 *
	 * @return the HTTP status code
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Return the Meilisearch error code, if available.
	 *
	 * @return the error code, or {@literal null}
	 */
	@Nullable
	public String getCode() {
		return code;
	}

	/**
	 * Return the Meilisearch error type, if available.
	 *
	 * @return the error type, or {@literal null}
	 */
	@Nullable
	public String getType() {
		return type;
	}

	/**
	 * Return the Meilisearch error link, if available.
	 *
	 * @return the error link, or {@literal null}
	 */
	@Nullable
	public String getLink() {
		return link;
	}
}
