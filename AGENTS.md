# Repository Guidelines

## Project Overview

This is a Java 17 Maven plugin for jQAssistant. It scans BC4J XML definitions (`Entity`, `ViewObject`, and `ViewLink`) in application classpaths and records their metadata and database-table mappings as graph descriptors, supporting analysis alongside jQAssistant's Java/JPA model.

## Architecture & Data Flow

`Bc4jXmlScannerPlugin` is the scanner entry point, registered in `META-INF/jqassistant-plugin.xml`. It filters `.xml` resources by a short content sniff, parses accepted XML with external DTD/entity loading disabled, then creates a `Bc4jXmlFileDescriptor` and child `Bc4jViewObjectDescriptor` nodes through jQAssistant's `Store`. Entity and ViewObject attributes populate the table, Java type, SQL, and source-file properties; ViewLinks are represented as child descriptors too. Cross-file `EntityUsage` resolution is not performed by the scanner itself; the bundled `bc4j.xml` rules resource is currently empty.

## Key Directories

- `src/main/java/org/jqassistant/plugin/bc4j/impl/scanner/` — BC4J XML resource filtering and parsing.
- `src/main/java/org/jqassistant/plugin/bc4j/api/model/` — XO graph descriptor interfaces and their labels, properties, and relations.
- `src/main/resources/META-INF/` — jQAssistant plugin registration and packaged rule resources.

## Development Commands

Run from the repository root with JDK 17 and Maven installed:

```sh
mvn test                 # run the Maven test phase
mvn clean package        # build the plugin JAR
mvn clean verify         # run the full Maven verification lifecycle
mvn install              # install the JAR into the local Maven repository for a consumer project
```

The artifact is a plugin JAR, not a standalone application; exercise it through a jQAssistant host project. There is no Maven wrapper in the repository.

## Code Conventions & Common Patterns

- Use Java 17, four-space indentation, package paths under `org.jqassistant.plugin.bc4j`, and camelCase members.
- Model persisted graph types as interfaces annotated with XO `@Label`, `@Property`, and `@Relation`; properties use getter/setter pairs. Keep property names camelCase and graph relation names uppercase.
- Implement scanning with jQAssistant's `AbstractScannerPlugin`, `FileResource`, and `Store`; register scanner/model types in `jqassistant-plugin.xml`. The scanner is synchronous and has no injected collaborators.
- Keep XML parsing local to the scan and external DTD/entity loading disabled. Use SLF4J for diagnostics; the current scanner rejects unreadable candidates and warns then skips malformed XML.
- Existing source comments and JavaDoc are primarily in Spanish; preserve that language when extending nearby documentation.

## Important Files

- `pom.xml` — Maven coordinates, Java 17 compiler settings, provided jQAssistant dependencies, and JAR build configuration.
- `src/main/java/org/jqassistant/plugin/bc4j/impl/scanner/Bc4jXmlScannerPlugin.java` — scanner registration target and parsing/data-mapping logic.
- `src/main/java/org/jqassistant/plugin/bc4j/api/model/Bc4jXmlFileDescriptor.java` and `Bc4jViewObjectDescriptor.java` — graph schema.
- `src/main/resources/META-INF/jqassistant-plugin.xml` — plugin, model, and scanner registration.
- `src/main/resources/META-INF/jqassistant-rules/bc4j.xml` — packaged rules resource (currently empty).

## Runtime/Tooling Preferences

Use JDK 17 and Maven; the POM sets compiler source/target and release 17. jQAssistant, XO, XML-plugin, and SLF4J dependencies are `provided`, so they come from the host runtime rather than being bundled as application dependencies. The root has no Maven wrapper or repository-owned script/toolchain configuration.

## Testing & QA

There are no checked-in test sources or repository-specific test, lint, formatter, or coverage configurations in the module. `mvn test` is the standard test-phase command, but changes to scanner behavior should be exercised against representative BC4J XML through the jQAssistant host; cover candidate filtering, Entity/ViewObject/ViewLink mapping, and malformed input when tests are added.
