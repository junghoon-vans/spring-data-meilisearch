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
package io.vanslog.spring.data.meilisearch.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import io.vanslog.spring.data.meilisearch.repository.support.ReactiveMeilisearchRepositoryFactoryBean;

/**
 * Enables reactive Meilisearch repositories without constructor-time network initialization.
 *
 * @author Junghoon Ban
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ReactiveMeilisearchRepositoriesRegistrar.class)
public @interface EnableReactiveMeilisearchRepositories {

	String[] value() default {};

	String[] basePackages() default {};

	Class<?>[] basePackageClasses() default {};

	Filter[] includeFilters() default {};

	Filter[] excludeFilters() default {};

	String repositoryImplementationPostfix() default "Impl";

	String namedQueriesLocation() default "";

	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	Class<?> repositoryFactoryBeanClass() default ReactiveMeilisearchRepositoryFactoryBean.class;

	/** Name of the reactive operations/template bean used by discovered repositories. */
	String reactiveMeilisearchTemplateRef() default "reactiveMeilisearchTemplate";

	boolean considerNestedRepositories() default false;

}
