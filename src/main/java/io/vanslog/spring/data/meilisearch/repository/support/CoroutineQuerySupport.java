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
package io.vanslog.spring.data.meilisearch.repository.support;

import java.lang.reflect.Method;

import org.springframework.core.KotlinDetector;
import org.springframework.data.util.ReactiveWrappers;

import kotlinx.coroutines.flow.Flow;

/**
 * Optional bridge to coroutine types and Kotlin reflection. Exact class identity checks are guarded by coroutine
 * availability, keeping Reactor repositories usable when Kotlin reflection and coroutine libraries are absent.
 *
 * @author Junghoon Ban
 */
final class CoroutineQuerySupport {

	private CoroutineQuerySupport() {}

	static boolean isFlow(Class<?> type) {
		return ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.KOTLIN_COROUTINES) && type == Flow.class;
	}

	static boolean isSuspend(Method method) {
		requireKotlinReflection();
		return CoroutineQueryMetadata.isSuspend(method);
	}

	static boolean hasUnresolvedMethodTypeParameter(Method method) {
		requireKotlinReflection();
		return CoroutineQueryMetadata.hasUnresolvedMethodTypeParameter(method);
	}

	private static void requireKotlinReflection() {
		if (!KotlinDetector.isKotlinReflectPresent()) {
			throw new IllegalStateException("Coroutine query metadata requires kotlin-reflect");
		}

	}
}
