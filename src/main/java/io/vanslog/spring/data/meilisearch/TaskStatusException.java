/*
 * Copyright 2023-2026 the original author or authors.
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
 * Exception indicating that a completed Meilisearch task did not succeed.
 *
 * @author Junghoon Ban
 */
public class TaskStatusException extends DataAccessException {

	private final int taskUid;
	private final String status;
	@Nullable private final String errorCode;

	/**
	 * Constructor for TaskStatusException.
	 *
	 * @param taskUid the task UID
	 * @param status the server-reported task status
	 * @param errorCode the server error code, or {@literal null} if unavailable
	 * @param msg the detail message
	 */
	public TaskStatusException(int taskUid, String status, @Nullable String errorCode, String msg) {
		this(taskUid, status, errorCode, msg, null);
	}

	/**
	 * Constructor for TaskStatusException.
	 *
	 * @param taskUid the task UID
	 * @param status the server-reported task status
	 * @param errorCode the server error code, or {@literal null} if unavailable
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public TaskStatusException(int taskUid, String status, @Nullable String errorCode, String msg,
			@Nullable Throwable cause) {
		super(msg, cause);
		this.taskUid = taskUid;
		this.status = status;
		this.errorCode = errorCode;
	}

	/**
	 * Return the task UID.
	 *
	 * @return the task UID
	 */
	public int getTaskUid() {
		return taskUid;
	}

	/**
	 * Return the status reported for the completed task.
	 *
	 * @return the server-reported task status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Return the server error code when available.
	 *
	 * @return the server error code, or {@literal null} if the task has no error details
	 */
	@Nullable
	public String getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return "TaskStatusException{" + "taskUid=" + taskUid + ", status='" + status + "', errorCode='" + errorCode + "'} "
				+ super.toString();
	}
}
