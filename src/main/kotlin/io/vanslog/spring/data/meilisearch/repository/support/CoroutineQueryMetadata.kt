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
package io.vanslog.spring.data.meilisearch.repository.support

import java.lang.reflect.Method

import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

import org.springframework.data.util.KotlinReflectionUtils

/**
 * Kotlin metadata needed by the reactive repository bridge.
 *
 * @author Junghoon Ban
 */
internal object CoroutineQueryMetadata {

    @JvmStatic
    fun isSuspend(method: Method): Boolean = KotlinReflectionUtils.isSuspend(method)

    @JvmStatic
    fun hasUnresolvedMethodTypeParameter(method: Method): Boolean {
        val function = KotlinReflectionUtils.findKotlinFunction(method) ?: return true
        val methodTypeParameters = function.typeParameters
        if (methodTypeParameters.isEmpty()) {
            return false
        }

        return hasMethodTypeParameter(function.returnType, methodTypeParameters) ||
            function.parameters.any { parameter -> hasMethodTypeParameter(parameter.type, methodTypeParameters) }
    }

    private fun hasMethodTypeParameter(type: KType, methodTypeParameters: List<KTypeParameter>): Boolean {
        val classifier = type.classifier
        if (classifier is KTypeParameter && classifier in methodTypeParameters) {
            return true
        }
        return type.arguments.any { projection ->
            projection.type?.let { hasMethodTypeParameter(it, methodTypeParameters) } ?: false
        }
    }
}
