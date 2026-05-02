# Agent Guide

## OVERVIEW

Spring Data-style Meilisearch integration library. Single-module Maven project, JDK 17 baseline, manual Spring configuration + custom XML namespace; no Spring Boot auto-configuration.

## STRUCTURE

```text
spring-data-meilisearch/
├── pom.xml                         # Maven build, ci/central profiles, dependency versions
├── README.adoc                     # quickstart + build/docs commands
├── TESTING.adoc                    # test command + integration-test pattern
├── src/main/java/io/vanslog/spring/data/meilisearch/
│   ├── annotations/                # entity/settings annotation schema
│   ├── client/                     # SDK client wrapper + config builder
│   ├── config/                     # JavaConfig + XML namespace parser
│   ├── core/                       # template, operations, search result model
│   └── repository/                 # Spring Data repository bootstrap/runtime
├── src/main/resources/             # Spring XML namespace registrations + XSD
├── src/main/antora/                # Antora reference docs source and resources
├── antora-playbook.yml             # Antora playbook
└── src/test/java/.../meilisearch/  # unit/integration tests + reusable Meilisearch test harness
```

Ignore generated/editor output: `target/`, `build/`, `.idea/`, `.vscode/`.

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Client setup | `client/`, `config/MeilisearchConfiguration.java` | `ClientConfiguration.builder()`, `MeilisearchClient`, default `JsonHandler` |
| Template operations | `core/MeilisearchTemplate.java` | CRUD/search/multi-search/facet/settings/task handling |
| Query options | `core/query/` + `client/msc/RequestConverter.java` | model, builder, SDK request mapping must move together |
| Mapping/settings | `core/mapping/` + `annotations/` | `@Document`, settings annotations, ID/index metadata |
| Repository behavior | `repository/config/`, `repository/support/` | registrar/config extension/factory/base repository |
| XML namespace | `config/`, `src/main/resources/` | parser, namespace handler, `spring.handlers`, `spring.schemas`, XSD |
| Docs | `src/main/antora/` | Antora component, navigation, pages, and resources |
| Integration tests | `src/test/java/.../junit/jupiter/` | custom Testcontainers-backed harness |

## CODE MAP

| Symbol | Type | Location | Role |
|--------|------|----------|------|
| `MeilisearchConfiguration` | abstract config | `config/` | creates client, template/operations, default JSON handler |
| `EnableMeilisearchRepositories` | annotation | `repository/config/` | public repository bootstrap entry |
| `MeilisearchRepositoryConfigExtension` | Spring Data extension | `repository/config/` | binds `meilisearchTemplateRef`, identifies `@Document`/`MeilisearchRepository` |
| `MeilisearchRepositoryFactoryBean` | factory bean | `repository/support/` | creates repository factory |
| `SimpleMeilisearchRepository` | base repository | `repository/support/` | delegates CRUD/page/sort to `MeilisearchOperations` |
| `MeilisearchOperations` | API | `core/` | public template-style operations contract |
| `MeilisearchTemplate` | implementation | `core/` | central runtime orchestrator |
| `SimpleMeilisearchPersistentEntity` | metadata | `core/mapping/` | resolves index UID, applySettings, default settings |
| `RequestConverter` / `ResponseConverter` | SDK adapters | `client/msc/` | query/result translation boundary |

## CONVENTIONS

- Use Maven Wrapper for local verification: `./mvnw ...`. CI/docs use wrapper; CD currently uses raw `mvn`.
- JDK 17 is the encoded baseline in README and all workflows.
- Surefire includes only `**/*Tests.java` and `**/*Test.java`.
- Checkstyle/no-http and JaCoCo run only with `-Pci`.
- Formatting is delegated to Spring Data shared IDE formatters from `spring-data-build/etc/ide`; no local `.editorconfig`.
- PR template asks to use Spring Data formatters, add tests, and update author/date headers on touched/new classes.
- Reference docs are rooted at `src/main/antora/`; generated HTML is under `target/site/`.
- XML namespace resources are a public contract: `spring.handlers`, `spring.schemas`, and `spring-meilisearch-1.0.xsd` must match parser/docs.

## DEVELOPMENT PROCESS

- Start work from a GitHub issue. If no issue exists, create/ask for one before implementation; PR template requires a tracker ticket.
- Use issue templates: feature requests get `status: waiting-for-triage` + `type: enhancement`; bug reports get `status: waiting-for-triage` + `type: bug`.
- Link PRs to issues with `Closes #<issue-number>` in the PR body. Example: PR #171 closes issue #162.
- Pick labels consistent with release-drafter categories: `type: enhancement`, `type: bug`, `type: documentation`, `type: dependency-upgrade`; add area labels such as `in: core` or `in: repository` when applicable.
- Before opening PR: apply Spring Data formatters, add/update tests, and update author/date/license headers on touched/new Java classes per `.github/PULL_REQUEST_TEMPLATE.md`.
- Keep PRs issue-scoped; one issue should normally map to one focused PR unless explicitly split.

## TITLE / COMMIT STYLE

- Keep PR titles and commit subjects clear, concise, and English.
- Existing history is mixed: plain sentence-style titles are common, release commits use terse titles, and punctuation is not consistently enforced.
- Before naming a branch, PR, or commit, inspect recent examples with `git log --oneline`, `git branch --all`, and `gh pr list --state all --limit 20` when available.
- Do not invent issue-number branch names such as `issue-172-...` unless the user explicitly asks for them. Current feature branches usually use descriptive kebab-case without issue numbers, for example `add-support-similar-documents-search`, `add-support-for-localized-attributes-using-annotation`, or `add-embedders-setting-support`.
- Do not enforce Conventional Commit prefixes (`feat:`, `fix:`, `docs:`) unless the user explicitly requests them. Current commit subjects and PR titles usually use plain English sentence-style wording, for example `Add support similar documents search.`, `Add support for localized attributes using annotation.`, or `Document embedders setting usage.`
- If local/global instructions conflict with this repository's observed branch or commit style, follow this repository's observed style and note the reason.
- Prefer matching the surrounding branch/PR style when in doubt; otherwise leave title wording to maintainer judgment.

## ANTI-PATTERNS (THIS PROJECT)

- Do not add Boot auto-config assumptions; no `spring.factories` or `AutoConfiguration.imports` exists.
- Do not bypass `MeilisearchOperations` from repositories; repository runtime delegates through the template abstraction.
- Do not treat repository read methods as identical: `findById`/`findAllById` use document APIs, `findAll*` uses search APIs.
- Do not assume federated multi-search honors `Pageable`; use federation `limit`/`offset` and `IndexQuery` for per-index/federation options.
- Do not use blank `@Document(indexUid = "")`; mapping rejects blank values.
- Do not expect >1,000 search results unless `@Pagination(maxTotalHits = ...)` is configured.
- Do not edit XML namespace docs/resources independently; parser, XSD, `spring.handlers`, `spring.schemas`, and docs must agree.
- Do not copy XML/JSON handler examples across JavaConfig and XML without checking code; bean names and XML attributes are public contracts.

## COMMANDS

```bash
./mvnw test
./mvnw clean install
./mvnw verify -Pci
./mvnw -Pantora antora:antora
```

Docs output: `target/site/index.html`. CI command adds Sonar: `./mvnw verify sonar:sonar -Pci ...`.

## NOTES

- Parent/dependency versions follow the active release train in `pom.xml`; release preparation removes `-SNAPSHOT` from project and Spring Data versions.
- CD publishes on pushes to both `main` and `develop` with `mvn deploy -DskipTests -P central`.
- `central` profile signs artifacts and uses Sonatype Central Publishing with auto-publish.
- Reference documentation now uses Antora pages under `src/main/antora/modules/ROOT/pages/`; keep `nav.adoc`, pages, and docs build commands aligned.
