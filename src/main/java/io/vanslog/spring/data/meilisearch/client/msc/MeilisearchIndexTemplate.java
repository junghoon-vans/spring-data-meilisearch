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

import org.springframework.util.Assert;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.IndexesQuery;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;

import io.vanslog.spring.data.meilisearch.UncategorizedMeilisearchException;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndex;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexCreateRequest;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexList;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexOperations;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexQuery;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexStats;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexUpdateRequest;

/**
 * Meilisearch Java client-backed operations bound to one index.
 *
 * @author Junghoon Ban
 */
class MeilisearchIndexTemplate implements MeilisearchIndexOperations {

	private final String indexUid;
	private final MeilisearchClientExecutor executor;
	private final IndexRequestConverter requestConverter;
	private final IndexSettingsResponseConverter settingsResponseConverter;
	private final InstanceResponseConverter responseConverter;
	private final int requestTimeout;
	private final int requestInterval;

	MeilisearchIndexTemplate(String indexUid, MeilisearchClientExecutor executor,
			InstanceResponseConverter responseConverter, int requestTimeout, int requestInterval) {

		Assert.hasText(indexUid, "Index uid must not be empty");
		Assert.notNull(executor, "MeilisearchClientExecutor must not be null");
		Assert.notNull(responseConverter, "InstanceResponseConverter must not be null");
		this.indexUid = indexUid;
		this.executor = executor;
		this.requestConverter = new IndexRequestConverter();
		this.settingsResponseConverter = new IndexSettingsResponseConverter();
		this.responseConverter = responseConverter;
		this.requestTimeout = requestTimeout;
		this.requestInterval = requestInterval;
	}

	@Override
	public MeilisearchIndex create() {
		return create(new MeilisearchIndexCreateRequest());
	}

	@Override
	public MeilisearchIndex create(MeilisearchIndexCreateRequest request) {

		Assert.notNull(request, "MeilisearchIndexCreateRequest must not be null");

		TaskInfo taskInfo = executor.execute(client -> client.createIndex(indexUid, request.getPrimaryKey()));

		Task task = waitForTask(taskInfo);
		if (task.getStatus() != TaskStatus.SUCCEEDED) {
			throw TaskStatusExceptionSupport.fromCompletedTask(task, "Failed to create index.");
		}

		return get();
	}

	@Override
	public MeilisearchIndex get() {
		return responseConverter.mapIndex(executor.execute(client -> client.getIndex(indexUid)));
	}

	@Override
	public MeilisearchIndexList list() {
		return responseConverter.mapIndexes(executor.execute(client -> client.getIndexes()));
	}

	@Override
	public MeilisearchIndexList list(MeilisearchIndexQuery query) {

		Assert.notNull(query, "MeilisearchIndexQuery must not be null");

		IndexesQuery indexesQuery = requestConverter.indexesQuery(query);
		return responseConverter.mapIndexes(executor.execute(client -> client.getIndexes(indexesQuery)));
	}

	@Override
	public MeilisearchIndex update(MeilisearchIndexUpdateRequest request) {

		Assert.notNull(request, "MeilisearchIndexUpdateRequest must not be null");

		TaskInfo taskInfo = executor.execute(client -> client.updateIndex(indexUid, request.getPrimaryKey()));

		Task task = waitForTask(taskInfo);
		if (task.getStatus() != TaskStatus.SUCCEEDED) {
			throw TaskStatusExceptionSupport.fromCompletedTask(task, "Failed to update index.");
		}

		return get();
	}

	@Override
	public boolean delete() {

		TaskInfo taskInfo = executor.execute(client -> client.deleteIndex(indexUid));

		return waitForTask(taskInfo).getStatus() == TaskStatus.SUCCEEDED;
	}

	@Override
	public MeilisearchIndexStats stats() {
		return responseConverter.mapIndexStats(executor.execute(client -> client.index(indexUid).getStats()));
	}

	@Override
	public MeilisearchIndexSettings getSettings() {
		return settingsResponseConverter.fromSettings(executor.execute(client -> client.index(indexUid).getSettings()));
	}

	@Override
	public MeilisearchIndexSettings updateSettings(MeilisearchIndexSettings settings) {

		Assert.notNull(settings, "MeilisearchIndexSettings must not be null");

		Settings sdkSettings = requestConverter.toSettings(settings);
		TaskInfo taskInfo = executor.execute(client -> client.index(indexUid).updateSettings(sdkSettings));

		Task task = waitForTask(taskInfo);
		if (task.getStatus() != TaskStatus.SUCCEEDED) {
			throw TaskStatusExceptionSupport.fromCompletedTask(task, "Failed to update index settings.");
		}

		return getSettings();
	}

	@Override
	public MeilisearchIndexSettings resetSettings() {

		TaskInfo taskInfo = executor.execute(client -> client.index(indexUid).resetSettings());

		Task task = waitForTask(taskInfo);
		if (task.getStatus() != TaskStatus.SUCCEEDED) {
			throw TaskStatusExceptionSupport.fromCompletedTask(task, "Failed to reset index settings.");
		}

		return getSettings();
	}

	private Task waitForTask(TaskInfo taskInfo) {

		int taskUid = taskInfo.getTaskUid();

		Task task = executor.execute((Client client) -> {
			Index index = client.index(indexUid);
			index.waitForTask(taskUid, requestTimeout, requestInterval);
			return index.getTask(taskUid);
		});
		if (task == null) {
			throw new UncategorizedMeilisearchException("Failed to retrieve completed task " + taskUid + ".");
		}
		return task;
	}
}
