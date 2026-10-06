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
 * Exception raised when an accepted Meilisearch task does not succeed.
 *
 * @author Junghoon Ban
 */
public class ReactiveTaskException extends DataAccessException {

	private final long taskUid;
	private final String status;
	@Nullable private final String errorCode;
	@Nullable private final String errorMessage;
	@Nullable private final String errorType;
	@Nullable private final String errorLink;

	/**
	 * Create an exception for a failed or canceled task.
	 *
	 * @param taskUid the Meilisearch task UID
	 * @param status the terminal task status
	 * @param errorCode the task error code, if available
	 * @param errorMessage the task error message, if available
	 * @param errorType the task error type, if available
	 * @param errorLink the task error link, if available
	 */
	public ReactiveTaskException(long taskUid, String status, @Nullable String errorCode, @Nullable String errorMessage,
			@Nullable String errorType, @Nullable String errorLink) {
		super("Meilisearch task " + taskUid + " ended with status " + status + ".");
		this.taskUid = taskUid;
		this.status = status;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.errorType = errorType;
		this.errorLink = errorLink;
	}

	/**
	 * Return the Meilisearch task UID.
	 *
	 * @return the task UID
	 */
	public long getTaskUid() {
		return taskUid;
	}

	/**
	 * Return the terminal task status.
	 *
	 * @return the task status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Return the task error code, if available.
	 *
	 * @return the error code, or {@literal null}
	 */
	@Nullable
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Return the task error message, if available.
	 *
	 * @return the error message, or {@literal null}
	 */
	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Return the task error type, if available.
	 *
	 * @return the error type, or {@literal null}
	 */
	@Nullable
	public String getErrorType() {
		return errorType;
	}

	/**
	 * Return the task error link, if available.
	 *
	 * @return the error link, or {@literal null}
	 */
	@Nullable
	public String getErrorLink() {
		return errorLink;
	}
}
