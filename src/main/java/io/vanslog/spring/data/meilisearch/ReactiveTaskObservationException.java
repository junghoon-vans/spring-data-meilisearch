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

/**
 * Failure to observe an accepted task. The server's final outcome remains unknown.
 *
 * @author Junghoon Ban
 */
public class ReactiveTaskObservationException extends DataAccessException {

	private final long taskUid;

	/**
	 * Create an accepted-task observation failure preserving its cause.
	 *
	 * @param taskUid the accepted Meilisearch task UID
	 * @param cause the polling failure
	 */
	public ReactiveTaskObservationException(long taskUid, Throwable cause) {
		super("Failed to observe Meilisearch task " + taskUid + "; the task outcome is unknown.", cause);
		this.taskUid = taskUid;
	}

	/**
	 * Return the accepted Meilisearch task UID.
	 *
	 * @return the task UID
	 */
	public long getTaskUid() {
		return taskUid;
	}
}
