# Agent Guide

## OVERVIEW

Single Maven test source set containing unit-style tests, container-backed integration tests, shared entities, and custom JUnit Jupiter support.

## STRUCTURE

```text
test/.../meilisearch/
├── client/              # client config unit tests
├── config/              # Java/XML config tests
├── core/                # template integration + converter/mapping unit tests
├── entities/            # shared test domain fixtures
├── junit/jupiter/       # Meilisearch Testcontainers harness
└── repository/          # repository integration + support unit tests
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Integration harness | `junit/jupiter/MeilisearchTest.java` | meta-annotation for live Meilisearch tests |
| Container lifecycle | `junit/jupiter/MeilisearchConnection.java` | `getmeili/meilisearch:v1.12.3`, `masterKey` |
| Spring test wiring | `junit/jupiter/MeilisearchTestConfiguration.java` | builds client config from container connection |
| Shared entities | `entities/` | `Movie`, `ComicsMovie`, `SortableMovie`, `TotalHitsLimited` |
| Template contract | `core/MeilisearchTemplateIntegrationTests.java` | broad runtime behavior coverage |
| Repository contract | `repository/*IntegrationTests.java` | CRUD/sort/page repository behavior |
| XML namespace fixture | `src/test/resources/.../namespace.xml` | used by config namespace tests |

## CONVENTIONS

- JUnit 5 + AssertJ dominate.
- Surefire runs both `*Test.java` and `*Tests.java`.
- Integration tests use `*IntegrationTests`, `@MeilisearchTest`, and `@ContextConfiguration(...)`.
- Unit-style tests usually use `*UnitTests`; legacy singular `*UnitTest` files exist and are included.
- Integration tests clean state in `@BeforeEach` (`deleteAll()` / `deleteAllDocuments()`).
- No separate `src/integrationTest`, no Failsafe split, no integration-test Maven profile/tag.
- No Mockito convention was found; tests prefer real objects, Spring contexts, or live container.

## ANTI-PATTERNS

- Do not create a second integration-test source set unless build config changes too.
- Do not bypass `@MeilisearchTest` for live Meilisearch tests.
- Do not change testcontainer image/master key without updating all integration assumptions.
- Do not rely on test ordering despite `@TestMethodOrder`; each test should clean/setup its own data.

## COMMANDS

```bash
./mvnw test
./mvnw -Dtest=MeilisearchTemplateIntegrationTests test
./mvnw -Dtest='*RepositoryIntegrationTests' test
```
