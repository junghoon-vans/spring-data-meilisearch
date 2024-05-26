/*
 * Copyright 2023-2024 the original author or authors.
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

import com.meilisearch.sdk.model.TaskStatus;

/**
 * Exception indicating that a task status was not successful.
 *
 * @author Junghoon Ban
 */
public class TaskStatusException extends DataAccessException {

	private final TaskStatus taskStatus;

	/**
	 * Constructor for TaskStatusException.
	 *
	 * @param taskStatus the task status
	 * @param msg the detail message
	 */
	public TaskStatusException(TaskStatus taskStatus, String msg) {
		super(msg);
		this.taskStatus = taskStatus;
	}

	/**
	 * Constructor for TaskStatusException.
	 *
	 * @param taskStatus the task status
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public TaskStatusException(TaskStatus taskStatus, String msg, Throwable cause) {
		super(msg, cause);
		this.taskStatus = taskStatus;
	}

	/**
	 * Return the task status.
	 * 
	 * @return the task status
	 */
	public TaskStatus getTaskStatus() {
		return taskStatus;
	}

	@Override
	public String toString() {
		return "TaskStatusException{" + "taskStatus=" + taskStatus + "} " + super.toString();
	}
}
