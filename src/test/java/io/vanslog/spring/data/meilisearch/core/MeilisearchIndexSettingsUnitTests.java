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
package io.vanslog.spring.data.meilisearch.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.FacetingSettings;
import io.vanslog.spring.data.meilisearch.core.MeilisearchIndexSettings.PaginationSettings;

/**
 * Unit tests for {@link MeilisearchIndexSettings}.
 *
 * @author Junghoon Ban
 */
class MeilisearchIndexSettingsUnitTests {

	@Test
	void shouldAllowNullSearchCutoffMs() {

		MeilisearchIndexSettings settings = MeilisearchIndexSettings.builder().withSearchCutoffMs(null).build();

		assertThat(settings.getSearchCutoffMs()).isNull();
	}

	@Test
	void shouldAllowZeroNumericSettings() {

		MeilisearchIndexSettings settings = MeilisearchIndexSettings.builder() //
				.withSearchCutoffMs(0) //
				.withPagination(new PaginationSettings(0)) //
				.withFaceting(new FacetingSettings(0)) //
				.build();

		assertThat(settings.getSearchCutoffMs()).isZero();
		assertThat(settings.getPagination().getMaxTotalHits()).isZero();
		assertThat(settings.getFaceting().getMaxValuesPerFacet()).isZero();
	}

	@Test
	void shouldRejectNegativeSearchCutoffMs() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> MeilisearchIndexSettings.builder().withSearchCutoffMs(-1)) //
				.withMessageContaining("Search cutoff ms must not be negative");
	}

	@Test
	void shouldRejectNegativeMaxTotalHits() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new PaginationSettings(-1)) //
				.withMessageContaining("Max total hits must not be negative");
	}

	@Test
	void shouldRejectNegativeMaxValuesPerFacet() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new FacetingSettings(-1)) //
				.withMessageContaining("Max values per facet must not be negative");
	}

}
