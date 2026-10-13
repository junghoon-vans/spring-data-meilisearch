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

import java.time.Duration;
import java.util.Objects;

import org.springframework.dao.DataAccessException;

/**
 * Exception raised when a task's final outcome is not observed before the configured wait timeout.
 *
 * @author Junghoon Ban
 */
public class ReactiveTaskTimeoutException extends DataAccessException {

	private final long taskUid;
	private final Duration timeout;

	/**
	 * Create a task observation timeout exception.
	 *
	 * @param taskUid the Meilisearch task UID
	 * @param timeout the configured observation timeout
	 */
	public ReactiveTaskTimeoutException(long taskUid, Duration timeout) {
		super("Timed out after " + Objects.requireNonNull(timeout, "Timeout must not be null").toMillis()
				+ " ms while waiting for Meilisearch task " + taskUid + "; the task outcome is unknown.");
		this.taskUid = taskUid;
		this.timeout = timeout;
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
	 * Return the configured observation timeout.
	 *
	 * @return the timeout duration
	 */
	public Duration getTimeout() {
		return timeout;
	}
}
