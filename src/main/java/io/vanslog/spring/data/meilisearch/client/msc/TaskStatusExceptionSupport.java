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
package io.vanslog.spring.data.meilisearch.client.msc;

import io.vanslog.spring.data.meilisearch.TaskStatusException;

import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskError;

/**
 * Creates public task failure diagnostics from the completed SDK task.
 *
 * @author Junghoon Ban
 */
final class TaskStatusExceptionSupport {

	private TaskStatusExceptionSupport() {}

	static TaskStatusException fromCompletedTask(Task task, String operationMessage) {
		TaskError error = task.getError();
		String errorMessage = error != null ? error.getMessage() : null;
		String message = operationMessage;
		if (errorMessage != null && !errorMessage.isBlank()) {
			message += " Meilisearch reported: " + errorMessage;
		}

		return new TaskStatusException(task.getUid(), task.getStatus().toString(), error != null ? error.getCode() : null,
				message);
	}
}
