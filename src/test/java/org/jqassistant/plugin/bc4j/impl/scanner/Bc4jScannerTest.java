package org.jqassistant.plugin.bc4j.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class Bc4jScannerTest extends AbstractPluginIT {

    @Test
    void resolvesViewObjectEntityUsageToEntityAndMappedTable() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/EmployeeEO.xml");
        scanFixture(scanner, "bc4j/model/EmployeeVO.xml");

        applyConcept("bc4j:ResolveViewAttributeMappings");

        assertThat(query("""
                MATCH (:Bc4jViewObject {name: 'EmployeeVO'})-[:HAS_ATTRIBUTE]->
                      (viewAttribute:Bc4jViewAttribute)-[:MAPS_TO]->
                      (entityAttribute:Bc4jEntityAttribute)
                RETURN viewAttribute.entityUsageName AS usageName,
                       entityAttribute.columnName AS columnName,
                       entityAttribute.primaryKey AS primaryKey
                """).getColumn("columnName"))
                .containsExactly("EMPLOYEE_ID");
        assertThat(query("""
                MATCH (:Bc4jViewObject {name: 'EmployeeVO'})-[:HAS_ATTRIBUTE]->
                      (:Bc4jViewAttribute)-[:MAPS_TO]->(entityAttribute:Bc4jEntityAttribute)
                RETURN entityAttribute.primaryKey AS primaryKey
                """).getColumn("primaryKey"))
                .containsExactly(true);

        assertThat(query("""
                MATCH (:Bc4jViewObject {name: 'EmployeeVO'})-[:HAS_ENTITY_USAGE]->
                      (:Bc4jEntityUsage)-[:REFERENCES_ENTITY]->(entity:Bc4jEntity)
                RETURN entity.qualifiedName AS qualifiedName, entity.tableName AS tableName
                """).getColumn("qualifiedName"))
                .containsExactly("model.EmployeeEO");
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'EmployeeVO'})
                RETURN viewObject.tableName AS tableName
                """).getColumn("tableName"))
                .containsExactly("EMPLOYEE");
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'EmployeeVO'})
                RETURN viewObject.tableNamesComplete AS complete
                """).getColumn("complete"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'EmployeeVO'})
                RETURN viewObject.javaTypeFqn AS javaType
                """).getColumn("javaType"))
                .containsExactly("model.EmployeeVOImpl");
    }

    @Test
    void resolvesEveryEntityUsageWithoutInventingASingleTable() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/entities/DepartmentEO.xml");
        scanFixture(scanner, "bc4j/model/entities/EmployeeEO.xml");
        scanFixture(scanner, "bc4j/model/DepartmentEmployeeVO.xml");

        applyConcept("bc4j:ResolveEntityReferences");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'DepartmentEmployeeVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("DEPARTMENT", "EMPLOYEE");
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'DepartmentEmployeeVO'})
                RETURN viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("hasNoSingleTable"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'DepartmentEmployeeVO'})
                RETURN viewObject.tableNamesComplete AS complete
                """).getColumn("complete"))
                .containsExactly(true);
    }

    @Test
    void marksPartialEntityUsageTableLineageIncomplete() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/entities/DepartmentEO.xml");
        scanFixture(scanner, "bc4j/model/DepartmentEmployeeVO.xml");

        applyConcept("bc4j:ResolveEntityReferences");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'DepartmentEmployeeVO'})
                RETURN viewObject.tableNamesComplete AS complete,
                       viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("complete"))
                .containsExactly(false);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'DepartmentEmployeeVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactly("DEPARTMENT");
    }

    @Test
    void preservesEveryDirectFromListTable() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderProductsVO.xml");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderProductsVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("ORDERS", "PRODUCTS", "CATEGORIES");
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderProductsVO'})
                RETURN viewObject.tableNamesComplete AS complete
                """).getColumn("complete"))
                .containsExactly(true);

    }
    @Test
    void extractsTablesFromExpertSqlQuery() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderTotalsVO.xml");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderTotalsVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("ORDERS", "ORDER_ITEMS");
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderTotalsVO'})
                RETURN viewObject.tableNamesComplete AS complete
                """).getColumn("complete"))
                .containsExactly(true);
    }

    @Test
    void extractsTablesFromUnionQuery() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderArchiveUnionVO.xml");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderArchiveUnionVO'})
                RETURN viewObject.tableNamesComplete AS complete
                """).getColumn("complete"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderArchiveUnionVO'})
                RETURN viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("hasNoSingleTable"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderArchiveUnionVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("ORDERS", "ARCHIVE_ORDERS");
    }

    @Test
    void usesExpertSqlInsteadOfConflictingFromList() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderSqlOverridesFromListVO.xml");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderSqlOverridesFromListVO'})
                RETURN viewObject.tableNamesComplete AS complete,
                       viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("complete"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderSqlOverridesFromListVO'})
                RETURN viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("hasNoSingleTable"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderSqlOverridesFromListVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("ARCHIVE_ORDERS", "PRODUCTS");
    }

    @Test
    void extractsTablesFromNestedSqlSources() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderNestedQueryVO.xml");

        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderNestedQueryVO'})
                RETURN viewObject.tableNamesComplete AS complete,
                       viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("complete"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderNestedQueryVO'})
                RETURN viewObject.tableName IS NULL AS hasNoSingleTable
                """).getColumn("hasNoSingleTable"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (viewObject:Bc4jViewObject {name: 'OrderNestedQueryVO'})
                UNWIND viewObject.tableNames AS tableName
                RETURN tableName
                """).getColumn("tableName"))
                .containsExactlyInAnyOrder("ORDERS", "PRODUCTS");
    }

    @Test
    void recordsViewLinkEndpointsAndAttributePairs() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderVO.xml");
        scanFixture(scanner, "bc4j/model/OrderItemVO.xml");
        scanFixture(scanner, "bc4j/model/OrderItemsLink.xml");
        scanFixture(scanner, "bc4j/model/OrderItemsAssociation.xml");

        applyConcept("bc4j:ResolveViewLinkReferences");
        applyConcept("bc4j:ResolveViewLinkAssociationReferences");

        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:HAS_END]->
                      (end:Bc4jViewLinkEnd)-[:REFERENCES_VIEW_OBJECT]->(viewObject:Bc4jViewObject)
                RETURN viewObject.qualifiedName AS owner, end.source AS source,
                       end.attributes[0] AS attribute
                """).getColumn("owner"))
                .containsExactlyInAnyOrder("model.OrderVO", "model.OrderItemVO");
        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:HAS_END]->(end:Bc4jViewLinkEnd)
                RETURN end.source AS source
                """).getColumn("source"))
                .containsExactlyInAnyOrder(true, false);
        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:HAS_END]->(end:Bc4jViewLinkEnd)
                RETURN end.attributes[0] AS attribute
                """).getColumn("attribute"))
                .containsExactlyInAnyOrder("model.OrderVO.OrderId", "model.OrderItemVO.OrderId");
        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:BASED_ON_ASSOCIATION]->
                      (association:Bc4jEntityAssociation)
                RETURN association.qualifiedName AS association
                """).getColumn("association"))
                .containsExactly("model.OrderItemsAssociation");
        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:HAS_END]->
                      (end:Bc4jViewLinkEnd {name: 'Orders'})
                RETURN end.finderName AS finderName, end.isUpdateable AS updateable
                """).getColumn("finderName"))
                .containsExactly("OrderItems");
        assertThat(query("""
                MATCH (:Bc4jViewLink {name: 'OrderItemsLink'})-[:HAS_END]->
                      (end:Bc4jViewLinkEnd {name: 'Orders'})
                RETURN end.isUpdateable AS updateable
                """).getColumn("updateable"))
                .containsExactly(true);
    }

    @Test
    void resolvesApplicationModuleViewUsages() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderVO.xml");
        scanFixture(scanner, "bc4j/model/OrderItemVO.xml");
        scanFixture(scanner, "bc4j/model/OrderModule.xml");

        applyConcept("bc4j:ResolveApplicationModuleReferences");

        assertThat(query("""
                MATCH (module:Bc4jApplicationModule {name: 'OrderModule'})-[:HAS_VIEW_USAGE]->
                      (usage:Bc4jViewUsage)-[:USES_VIEW_OBJECT]->(viewObject:Bc4jViewObject)
                RETURN usage.name AS instanceName, viewObject.qualifiedName AS definition
                """).getColumn("instanceName"))
                .containsExactlyInAnyOrder("Orders", "OrderItems");
        assertThat(query("""
                MATCH (:Bc4jApplicationModule {name: 'OrderModule'})-[:HAS_VIEW_USAGE]->
                      (usage:Bc4jViewUsage)-[:USES_VIEW_OBJECT]->(viewObject:Bc4jViewObject)
                RETURN viewObject.qualifiedName AS definition
                """).getColumn("definition"))
                .containsExactlyInAnyOrder("model.OrderVO", "model.OrderItemVO");
        assertThat(query("""
                MATCH (module:Bc4jApplicationModule {name: 'OrderModule'})
                RETURN module.javaTypeFqn AS javaType
                """).getColumn("javaType"))
                .containsExactly("model.OrderModuleImpl");
    }

    @Test
    void recordsEntityAssociationEndsAndMappedAttributes() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/entities/DepartmentEO.xml");
        scanFixture(scanner, "bc4j/model/entities/EmployeeEO.xml");
        scanFixture(scanner, "bc4j/model/entities/DepartmentEmployeeAssociation.xml");

        applyConcept("bc4j:ResolveAssociationReferences");

        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd)-[:REFERENCES_ENTITY]->(entity:Bc4jEntity)
                RETURN entity.qualifiedName AS owner, end.cardinality AS cardinality,
                       end.source AS source, end.attributes[0] AS attribute
                """).getColumn("owner"))
                .containsExactlyInAnyOrder("model.entities.DepartmentEO", "model.entities.EmployeeEO");
        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd)
                RETURN end.source AS source
                """).getColumn("source"))
                .containsExactlyInAnyOrder(true, false);
        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd)
                RETURN end.attributes[0] AS attribute
                """).getColumn("attribute"))
                .containsExactlyInAnyOrder("model.entities.DepartmentEO.DepartmentId",
                        "model.entities.EmployeeEO.DepartmentId");
        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd {name: 'Department'})
                RETURN end.foreignKey AS foreignKey, end.aggregation AS aggregation,
                       end.isUpdateable AS updateable
                """).getColumn("foreignKey"))
                .containsExactly("model.entities.DepartmentEO.DepartmentFk");
        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd {name: 'Department'})
                RETURN end.aggregation AS aggregation
                """).getColumn("aggregation"))
                .containsExactly("0");
        assertThat(query("""
                MATCH (:Bc4jEntityAssociation {name: 'DepartmentEmployeeAssociation'})-[:HAS_END]->
                      (end:Bc4jAssociationEnd {name: 'Department'})
                RETURN end.isUpdateable AS updateable
                """).getColumn("updateable"))
                .containsExactly(true);
    }

    @Test
    void recordsRuntimeConfigurationAndLinksItToApplicationModule() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/OrderModule.xml");
        scanFixture(scanner, "bc4j/model/common/bc4j.xcfg");

        applyConcept("bc4j:ResolveApplicationModuleConfiguration");

        assertThat(query("""
                MATCH (configuration:Bc4jApplicationModuleConfig)-[:CONFIGURES]->
                      (module:Bc4jApplicationModule)
                RETURN configuration.name AS configName, configuration.jdbcName AS jdbcName,
                       configuration.deployPlatform AS deployPlatform, module.qualifiedName AS module
                """).getColumn("configName"))
                .containsExactly("OrderModuleLocal");
        assertThat(query("""
                MATCH (configuration:Bc4jApplicationModuleConfig {name: 'OrderModuleLocal'})
                RETURN configuration.poolProperties[0] AS poolProperty
                """).getColumn("poolProperty"))
                .containsExactly("jbo.ampool.maxpoolsize=10");
        assertThat(query("""
                MATCH (configuration:Bc4jApplicationModuleConfig {name: 'OrderModuleLocal'})
                UNWIND configuration.databaseProperties AS databaseProperty
                RETURN databaseProperty
                """).getColumn("databaseProperty"))
                .containsExactly("jbo.TypeMapEntries=Java");
    }

    @Test
    void connectsProjectPackageRegistriesToBC4jComponents() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/entities/DepartmentEO.xml");
        scanFixture(scanner, "bc4j/model/Model.jpx");
        scanFixture(scanner, "bc4j/model/OrderModule.xml");

        applyConcept("bc4j:ResolveProjectPackages");
        applyConcept("bc4j:ResolveProjectApplicationModules");

        assertThat(query("""
                MATCH (:Bc4jProject {name: 'Model'})-[:DECLARES_PACKAGE]->
                      (package:Bc4jPackage)-[:CONTAINS_COMPONENT]->(entity:Bc4jEntity)
                RETURN package.qualifiedName AS packageName, package.entityPackage AS entityPackage,
                       entity.qualifiedName AS component
                """).getColumn("packageName"))
                .containsExactly("model.entities");
        assertThat(query("""
                MATCH (:Bc4jProject {name: 'Model'})-[:DECLARES_PACKAGE]->
                      (package:Bc4jPackage)-[:CONTAINS_COMPONENT]->(entity:Bc4jEntity)
                RETURN entity.qualifiedName AS component
                """).getColumn("component"))
                .containsExactly("model.entities.DepartmentEO");
        assertThat(query("""
                MATCH (project:Bc4jProject {name: 'Model'})-[:DEFINED_IN]->(file:Bc4jXmlFile)
                RETURN file.pathInJar AS path
                """).getColumn("path"))
                .containsExactly("model/Model.jpx");
        assertThat(query("""
                MATCH (:Bc4jProject {name: 'Model'})-[:DECLARES_APPLICATION_MODULE]->
                      (module:Bc4jApplicationModule)
                RETURN module.qualifiedName AS module
                """).getColumn("module"))
                .containsExactly("model.OrderModule");
    }

    @Test
    void readsPrimaryKeysDeclaredByKeyElements() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/KeyedEntity.xml");

        assertThat(query("""
                MATCH (:Bc4jEntity {name: 'KeyedEntity'})-[:HAS_ATTRIBUTE]->
                      (attribute:Bc4jEntityAttribute {name: 'Id'})
                RETURN attribute.primaryKey AS primaryKey
                """).getColumn("primaryKey"))
                .containsExactly(true);
        assertThat(query("""
                MATCH (:Bc4jEntity {name: 'KeyedEntity'})-[:HAS_ATTRIBUTE]->
                      (attribute:Bc4jEntityAttribute {name: 'Id'})
                RETURN attribute.precision AS precision, attribute.scale AS scale,
                       attribute.tableName AS tableName
                """).getColumn("precision"))
                .containsExactly("10");
        assertThat(query("""
                MATCH (:Bc4jEntity {name: 'KeyedEntity'})-[:HAS_ATTRIBUTE]->
                      (attribute:Bc4jEntityAttribute {name: 'Id'})
                RETURN attribute.scale AS scale
                """).getColumn("scale"))
                .containsExactly("0");
        assertThat(query("""
                MATCH (:Bc4jEntity {name: 'KeyedEntity'})-[:HAS_ATTRIBUTE]->
                      (attribute:Bc4jEntityAttribute {name: 'Id'})
                RETURN attribute.tableName AS tableName
                """).getColumn("tableName"))
                .containsExactly("KEYED_ENTITY");
    }

    @Test
    void scansPrefixedNamespacedBc4jDefinitions() throws Exception {
        Scanner scanner = getScanner();
        scanFixture(scanner, "bc4j/model/PrefixedEmployeeVO.xml");

        assertThat(query("""
                MATCH (:Bc4jViewObject {name: 'PrefixedEmployeeVO'})-[:HAS_ENTITY_USAGE]->
                      (usage:Bc4jEntityUsage)
                RETURN usage.entityFqn AS entityFqn
                """).getColumn("entityFqn"))
                .containsExactly("model.EmployeeEO");
        assertThat(query("""
                MATCH (:Bc4jViewObject {name: 'PrefixedEmployeeVO'})-[:HAS_ATTRIBUTE]->
                      (attribute:Bc4jViewAttribute)
                RETURN attribute.entityAttributeName AS entityAttribute
                """).getColumn("entityAttribute"))
                .containsExactly("EmployeeId");
    }

    private void scanFixture(Scanner scanner, String resourceName) throws IOException, URISyntaxException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        assertThat(resourceUrl).as("BC4J fixture %s", resourceName).isNotNull();
        Path fixturePath = Paths.get(resourceUrl.toURI());
        FileResource resource = new FileResource() {
            @Override
            public InputStream createStream() throws IOException {
                return Files.newInputStream(fixturePath);
            }

            @Override
            public File getFile() {
                return fixturePath.toFile();
            }

            @Override
            public void close() {
            }
        };
        Scope scope = new Scope() {
            @Override
            public String getPrefix() {
                return "";
            }

            @Override
            public String getName() {
                return "test";
            }
        };
        scanner.scan(resource, resourceName.substring("bc4j/".length()), scope);
    }
}
