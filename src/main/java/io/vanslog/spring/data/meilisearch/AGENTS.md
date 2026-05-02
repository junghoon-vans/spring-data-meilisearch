# Agent Guide

## OVERVIEW

Architectural root for the Spring Data Meilisearch module: public annotations/client/config/template/repository APIs plus internal adapters.

## STRUCTURE

```text
meilisearch/
├── annotations/      # @Document and settings annotations consumed by core.mapping
├── client/           # client config, SDK wrapper, SDK request/response converters
├── config/           # JavaConfig and XML namespace bootstrap
├── core/             # operations/template/search hits/query/mapping/convert
└── repository/       # Spring Data repository API/config/support
```

## WHERE TO LOOK

| Task | Location | Notes |
|------|----------|-------|
| Add consumer-facing setup option | `client/ClientConfiguration*`, `config/MeilisearchConfiguration` | keep JavaConfig and XML namespace paths aligned |
| Add entity setting annotation | `annotations/` then `core/mapping/SimpleMeilisearchPersistentEntity` | mapping is where annotations become SDK `Settings` |
| Add query/search option | `core/query/`, `client/msc/RequestConverter`, tests/docs | update both normal and multi-search paths |
| Change repository behavior | `repository/support/SimpleMeilisearchRepository` | repository methods should remain template-backed |
| Change XML namespace | `config/MeilisearchNamespaceHandler`, parser, resources XSD | docs must match exact attribute names/defaults |
| Change SDK conversion | `client/msc/RequestConverter`, `ResponseConverter` | keep SDK-facing; mapping/repository decisions stay elsewhere |

## CONVENTIONS

- Public integration starts with subclassing `MeilisearchConfiguration` and using `@EnableMeilisearchRepositories`.
- `MeilisearchConfiguration` bean names matter: `meilisearchClientConfiguration`, `meilisearchClient`, `meilisearchOperations`, `meilisearchTemplate`, `jsonHandler`.
- `@Document` is both a domain marker and repository-identifying annotation.
- `MeilisearchTemplate` owns runtime SDK calls; lower packages prepare metadata/query/request objects for it.
- `package-info.java` commonly marks packages with `@NonNullApi` / `@NonNullFields`; keep nullable points explicit with `@Nullable`.
- JavaConfig bean names are public contract; XML parser/resource/docs depend on `meilisearchTemplate`, `jsonHandler`, and `api-key` semantics.
- `client/msc` is the SDK adapter boundary: request conversion maps query/page/sort/federation options; response conversion maps SDK hits/federation metadata to `SearchHits`.
- Query changes usually span `core/query`, `client/msc/RequestConverter`, docs, and integration tests; do not add builder-only state.
- Mapping changes usually span `annotations`, `core/mapping`, mapping unit tests, and settings docs; `id` is the only implicit ID fallback and associations are unsupported.
- Repository config uses `meilisearchTemplateRef` / `meilisearch-template-ref`; runtime repositories delegate to `MeilisearchOperations` and may apply settings on construction.

## ANTI-PATTERNS

- Do not add direct SDK calls in repository infrastructure.
- Do not duplicate setting semantics in annotations without updating mapping/tests/docs.
- Do not rename bean defaults without updating `EnableMeilisearchRepositories`, XML resources, docs, and tests.
- Do not add Boot starter conventions here; this module is manual JavaConfig/XML namespace integration.
- Do not edit XML namespace attributes/defaults without updating parser, XSD, docs, and namespace tests together.
- Do not add mapping or repository logic to `client/msc`; it should remain an SDK request/response adapter.
- Do not assume repository read methods are equivalent: `findById`/`findAllById` use document APIs, while `findAll*` uses search APIs.

## VERIFY

```bash
./mvnw test
./mvnw verify -Pci
```
