# Repository Guidelines

## Project Overview

This is a Java 17 Maven plugin for jQAssistant that scans Oracle BC4J/ADF Business Components metadata and emits graph descriptors for Entity Objects, View Objects, Entity Associations, View Links, and Application Modules. The graph supports table/attribute lineage and analysis alongside JPA.

## Architecture & Data Flow

`Bc4jXmlScannerPlugin` is registered in `META-INF/jqassistant-plugin.xml`. It accepts BC4J `.xml`, `.xcfg`, and `.jpx` files, parses XML with external DTD/entity loading disabled, then creates typed XO graph nodes through jQAssistant's `Store`. `EntityUsage` and `ViewAttribute` metadata link View Objects to Entities and mapped attributes; association and ViewLink ends retain their owners, cardinalities, and attribute pairs; ApplicationModules retain ViewUsages; runtime settings and JboProject package registrations are captured. Packaged `bc4j.xml` concepts resolve cross-file references; `bc4j:ResolveViewAttributeMappings` depends on `bc4j:ResolveEntityReferences`. `tableNamesComplete` distinguishes fully resolved sources from unresolved EntityUsages or SQL parse failures; `tableName` is only set when one complete table is known. Database settings with connection or credential names are excluded; the logical `JDBCName` is retained.

## Key Directories

- `src/main/java/org/jqassistant/plugin/bc4j/impl/scanner/` — BC4J metadata discovery, secure XML parsing, graph creation, and direct table-source extraction.
- `src/main/java/org/jqassistant/plugin/bc4j/api/model/` — typed XO descriptors for BC4J components, attributes, relationship ends, runtime configuration, and project packages.
- `src/main/resources/META-INF/` — jQAssistant plugin/model registration and packaged Cypher concepts.
- `src/test/java/` and `src/test/resources/bc4j/` — graph integration tests and BC4J XML fixtures.

## Development Commands

Run from the repository root with JDK 17 and Maven installed:

```sh
mvn -Dtest=Bc4jScannerTest test  # run BC4J graph integration tests
mvn test                         # run the Maven test phase
mvn clean package                # build the plugin JAR
mvn clean verify                 # run the full Maven verification lifecycle
mvn install -Djqassistant.skip=true  # install locally without the host-specific plugin self-analysis constraint
```

The artifact is a plugin JAR, not a standalone application; exercise it through a jQAssistant host project. There is no Maven wrapper in the repository.
The local self-analysis enforces a GitHub SCM owner under `jqassistant-plugin`; this repository currently lives under `jagj77`. Run the focused integration tests separately before installing with the skip flag.

## Code Conventions & Common Patterns

- Use Java 17, four-space indentation, packages under `org.jqassistant.plugin.bc4j`, and camelCase members.
- Model persisted graph types as interfaces annotated with XO `@Label`, `@Property`, and `@Relation`; properties use getter/setter pairs. Keep graph relationship names uppercase.
- Implement scanning with jQAssistant's `AbstractScannerPlugin`, `FileResource`, and `Store`; register every descriptor and rule resource in `jqassistant-plugin.xml`.
- Keep XML parsing secure: external DTD/entity loading stays disabled. Use SLF4J; unreadable candidates are rejected and malformed XML is warned then skipped.
- Use JSqlParser 5.4 for SQL source extraction; do not add handwritten SQL token scanning. Preserve raw `FromList`/`SQLQuery`; parse failures leave `tableNamesComplete=false` rather than claiming partial lineage is complete.
- Do not persist database connection strings, usernames, passwords, tokens, or credentials from ADF configuration metadata; retain only non-sensitive settings and the logical JDBC name.
- Existing source comments and JavaDoc are primarily in Spanish; preserve that language when extending nearby documentation.

## Important Files

- `pom.xml` — Maven coordinates, Java 17 compiler settings, provided jQAssistant dependencies, and test harness dependency.
- `src/main/java/org/jqassistant/plugin/bc4j/impl/scanner/Bc4jXmlScannerPlugin.java` — scanner entry point and component-to-graph mapping.
- `src/main/java/org/jqassistant/plugin/bc4j/impl/scanner/Bc4jTableSourceParser.java` — JSqlParser-based table extraction from BC4J `FromList` and `SQLQuery`.
- `src/main/java/org/jqassistant/plugin/bc4j/api/model/` — typed graph schema; key types include `Bc4jEntityDescriptor`, `Bc4jViewObjectDescriptor`, `Bc4jEntityAssociationDescriptor`, `Bc4jViewLinkDescriptor`, and `Bc4jApplicationModuleDescriptor`.
- `src/main/resources/META-INF/jqassistant-plugin.xml` and `jqassistant-rules/bc4j.xml` — plugin registration and cross-file resolution concepts.
- `src/test/java/org/jqassistant/plugin/bc4j/impl/scanner/Bc4jScannerTest.java` — embedded graph behavior tests; fixtures are under `src/test/resources/bc4j/`.

## Runtime/Tooling Preferences

Use JDK 17 and Maven; the POM sets compiler source/target and release 17. jQAssistant, XO, XML-plugin, and SLF4J dependencies are `provided`; JSqlParser is a regular Maven dependency delivered transitively to the host. The root has no Maven wrapper or repository-owned script/toolchain configuration.

## Testing & QA

Graph-level tests use jQAssistant's `AbstractPluginIT` and an embedded Neo4j store; assert typed nodes, properties, table lineage completeness, cross-file relationships, project metadata, and configuration redaction from fixture scans. Run `mvn -Dtest=Bc4jScannerTest test` for the focused suite. The module has no separate lint or formatter configuration.
