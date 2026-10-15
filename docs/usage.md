# Usage

## Install the plugin

The Maven coordinates are `org.jqassistant.plugin.bc4j:bc4j:1.0.2`. Publish the artifact to a Maven repository available to the analyzed build, or run `mvn install -Djqassistant.skip=true` in this checkout for local development. Run `mvn -Dtest=Bc4jScannerTest test` separately before installing. The skip flag avoids the plugin self-analysis constraint that currently expects a GitHub SCM owner under `jqassistant-plugin`, while this repository is under `jagj77`.

```yaml
jqassistant:
  plugins:
    - group-id: org.jqassistant.plugin.bc4j
      artifact-id: bc4j
      version: 1.0.2
```

This follows the [jQAssistant plugin configuration](https://jqassistant.github.io/jqassistant/current/#_plugins). The consumer project also needs the jQAssistant Maven plugin configured.

## Scan and analyze

Select the BC4J concepts needed by the project in `.jqassistant.yml`:

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

Then run from the consuming Maven project:

```sh
mvn jqassistant:scan jqassistant:analyze
```

The scanner accepts BC4J definition files encountered in the configured jQAssistant scan scopes, including XML metadata packaged in application archives. It does not execute SQL or require a BC4J runtime.

## Concepts

| Concept | Effect |
| --- | --- |
| `bc4j:ResolveEntityReferences` | Resolves each `EntityUsage` to an Entity Object and derives View Object table lineage. |
| `bc4j:ResolveViewAttributeMappings` | Links View Attributes to Entity Attributes; automatically requires `ResolveEntityReferences`. |
| `bc4j:ResolveAssociationReferences` | Resolves Entity Association ends to Entity Objects. |
| `bc4j:ResolveViewLinkReferences` | Resolves ViewLink ends to View Objects. |
| `bc4j:ResolveViewLinkAssociationReferences` | Links ViewLinks to their referenced Entity Associations. |
| `bc4j:ResolveApplicationModuleReferences` | Resolves ApplicationModule ViewUsages to View Objects. |
| `bc4j:ResolveApplicationModuleConfiguration` | Links `.xcfg` configuration entries to ApplicationModules. |
| `bc4j:ResolveProjectPackages` | Links `.jpx` package registrations to components in those packages. |
| `bc4j:ResolveProjectApplicationModules` | Links `.jpx`-declared ApplicationModules to their definitions. |

## Example queries

Inspect table lineage, including whether the set is complete:

```cypher
MATCH (viewObject:Bc4jViewObject)
UNWIND coalesce(viewObject.tableNames, []) AS tableName
RETURN viewObject.qualifiedName AS viewObject, tableName,
       viewObject.tableNamesComplete AS complete
ORDER BY viewObject, tableName
```

Inspect a View Object's Entity Usages:

```cypher
MATCH (viewObject:Bc4jViewObject)-[:HAS_ENTITY_USAGE]->
      (usage:Bc4jEntityUsage)-[:REFERENCES_ENTITY]->(entity:Bc4jEntity)
RETURN viewObject.qualifiedName AS viewObject, usage.name AS usage,
       entity.qualifiedName AS entity, entity.tableName AS table
```

Inspect attribute-level lineage:

```cypher
MATCH (viewObject:Bc4jViewObject)-[:HAS_ATTRIBUTE]->
      (viewAttribute:Bc4jViewAttribute)-[:MAPS_TO]->
      (entityAttribute:Bc4jEntityAttribute)
MATCH (entity:Bc4jEntity)-[:HAS_ATTRIBUTE]->(entityAttribute)
RETURN viewObject.name AS viewObject, viewAttribute.name AS viewAttribute,
       entity.tableName AS table, entityAttribute.columnName AS column
```

The plugin provides BC4J metadata for project-specific JPA checks. JPA labels and table normalization vary by consumer project, so define that final cross-model rule in the consuming project's rules rather than assuming a single JPA graph shape here.

## Lineage and configuration limits

- `tableNamesComplete=true` means all table sources exposed by the scanned metadata were resolved. For entity-based View Objects, unresolved Entity Usages or entities without a mapped table leave it false.
- `tableName` is a convenience property for exactly one complete table. Use `tableNames` for multi-table View Objects.
- JSqlParser handles `FromList` and `SQLQuery` structures, including joins, subqueries, and set operations. A parser failure yields incomplete lineage; raw `fromList` and `sqlQuery` values remain available for inspection.
- The plugin records declared object names; it does not query a database catalog to expand synonyms, views, or database links into their underlying tables.
- Runtime configuration records logical JDBC and safe pool/database properties. Attribute names suggesting connections, users, passwords, credentials, secrets, or URLs are excluded to avoid graphing sensitive values.
- The plugin models BC4J metadata, not the full ADF UI/runtime: page bindings, UI hints, validators, business events, and custom Java method behavior are not scanned.
