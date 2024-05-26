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
package io.vanslog.spring.data.meilisearch.repository.config;

import io.vanslog.spring.data.meilisearch.repository.support.MeilisearchRepositoryFactoryBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Annotation to enable Meilisearch repositories.
 * {@link org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource} will scan the package of
 * the annotated configuration class for Spring Data repositories by default.
 *
 * @author Junghoon Ban
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(MeilisearchRepositoriesRegistrar.class)
public @interface EnableMeilisearchRepositories {

	/**
	 * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
	 * {@code @EnableMeilisearchRepositories("org.my.pkg")} instead of
	 * {@code @EnableMeilisearchRepositories(basePackages="org.my.pkg")}.
	 *
	 * @return String[]
	 */
	String[] value() default {};

	/**
	 * Base packages to scan for annotated components. {@link #value()} is an alias for (and mutually exclusive with) this
	 * attribute. Use {@link #basePackageClasses()} for a type-safe alternative to text-based package names.
	 *
	 * @return String[]
	 */
	String[] basePackages() default {};

	/**
	 * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
	 * package of each class specified will be scanned. Consider creating a special no-op marker class or interface in
	 * each package that serves no purpose other than being referenced by this attribute.
	 *
	 * @return {@literal Class<?>[]}
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * Specifies which types are eligible for component scanning. Further narrows the set of candidate components from
	 * everything in {@link #basePackages()} to everything in the base packages that matches the given filter or filters.
	 *
	 * @return Filter[]
	 */
	Filter[] includeFilters() default {};

	/**
	 * Specifies which types are not eligible for component scanning.
	 *
	 * @return Filter[]
	 */
	Filter[] excludeFilters() default {};

	/**
	 * Returns the postfix to be used when looking up custom repository implementations. Defaults to {@literal Impl}. So
	 * for a repository named {@code PersonRepository} the corresponding implementation class will be looked up scanning
	 * for {@code PersonRepositoryImpl}.
	 *
	 * @return String
	 */
	String repositoryImplementationPostfix() default "Impl";

	/**
	 * Configures the location of where to find the Spring Data named queries properties file.
	 *
	 * @return String
	 */
	String namedQueriesLocation() default "";

	/**
	 * Returns the key of the {@link org.springframework.data.repository.query.QueryLookupStrategy} to be used for lookup
	 * queries for query methods. Defaults to
	 * {@link org.springframework.data.repository.query.QueryLookupStrategy.Key#CREATE_IF_NOT_FOUND}.
	 *
	 * @return Key
	 */
	Key queryLookupStrategy() default Key.CREATE_IF_NOT_FOUND;

	/**
	 * Returns the {@link org.springframework.beans.factory.FactoryBean} class to be used for each repository instance.
	 * Defaults to {@code MeilisearchRepositoryFactoryBean}.
	 *
	 * @return {@literal Class<?>[]}
	 */
	Class<?> repositoryFactoryBeanClass() default MeilisearchRepositoryFactoryBean.class;

	/**
	 * Returns the {@link io.vanslog.spring.data.meilisearch.core.MeilisearchTemplate} bean name. This name will be used
	 * to create Meilisearch repositories discovered through this annotation. Defaults to {@code  meilisearchTemplate}.
	 *
	 * @return String
	 */
	String meilisearchTemplateRef() default "meilisearchTemplate";

	/**
	 * Configures whether nested repository-interfaces (e.g. defined as inner classes) should be discovered by the
	 * repositories infrastructure.
	 *
	 * @return boolean
	 */
	boolean considerNestedRepositories() default false;
}
