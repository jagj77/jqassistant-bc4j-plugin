# jQAssistant BC4J Plugin

The BC4J plugin scans Oracle ADF Business Components metadata and stores typed nodes and relationships in jQAssistant's graph. Its core use is tracing Entity Objects and View Objects to tables and attributes, so an analyzed project can compare BC4J access with its JPA model.

## What it scans

- `.xml` definitions rooted at `Entity`, `ViewObject`, `Association`, `ViewLink`, or `AppModule`.
- `.xcfg` runtime configuration rooted at `BC4JConfig`.
- `.jpx` project registries rooted at `JboProject`.

The scanner records relationships and query metadata; it does not execute BC4J SQL or connect to the application database. See [Usage](docs/usage.md) and the [Graph Model Reference](docs/model.md).

## Quick start

Install this artifact in the repository available to the consuming jQAssistant build (`mvn install -Djqassistant.skip=true` for a local checkout), then add it to the consumer's `.jqassistant.yml`. Run `mvn -Dtest=Bc4jScannerTest test` separately before installing; the plugin's self-analysis enforces a GitHub SCM owner under `jqassistant-plugin`, while this checkout is under `jagj77`.

```yaml
jqassistant:
  plugins:
    - group-id: org.jqassistant.plugin.bc4j
      artifact-id: bc4j
      version: 1.0.2
  analyze:
    concepts:
      - bc4j:ResolveEntityReferences
      - bc4j:ResolveViewAttributeMappings
      - bc4j:ResolveAssociationReferences
      - bc4j:ResolveViewLinkReferences
      - bc4j:ResolveViewLinkAssociationReferences
      - bc4j:ResolveApplicationModuleReferences
      - bc4j:ResolveApplicationModuleConfiguration
      - bc4j:ResolveProjectPackages
      - bc4j:ResolveProjectApplicationModules
```

Run the jQAssistant Maven goals from the consuming project:

```sh
mvn jqassistant:scan jqassistant:analyze
```

`ResolveViewAttributeMappings` automatically requires `ResolveEntityReferences`. The other concepts can be selected independently when their relationships are needed. The plugin targets jQAssistant 2.9.1 APIs.

## Table lineage

View Objects expose `tableNames` and `tableNamesComplete`. A scalar `tableName` is set only when one complete table is known. SQL table extraction uses JSqlParser; parse failures and unresolved Entity Usages remain explicitly incomplete instead of being reported as a complete mapping.

Do not store database credentials in source-controlled ADF configuration. Database settings whose property names indicate connections or credentials are omitted from the graph; the logical `JDBCName` is retained.

## Documentation

- [Usage guide](docs/usage.md) — installation, concepts, queries, and limits.
- [Graph Model Reference](docs/model.md) — labels, properties, relationships, and lineage semantics.
- [Documentation home](docs/index.md) — navigation for repository browsing and GitHub Pages.

The Markdown docs can be published from `/docs` with GitHub Pages without a documentation generator; enabling Pages requires repository settings access. GitHub renders the model's Mermaid diagram in the repository view; a Pages theme/build integration is needed if the published site must render that diagram too.
