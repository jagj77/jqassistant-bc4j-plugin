package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jViewObject")
public interface Bc4jViewObjectDescriptor extends Bc4jComponentDescriptor {

    @Property("tableName")
    String getTableName();
    void setTableName(String tableName);

    @Property("tableNames")
    String[] getTableNames();
    void setTableNames(String[] tableNames);

    @Property("tableNamesComplete")
    boolean isTableNamesComplete();
    void setTableNamesComplete(boolean tableNamesComplete);

    @Property("fromList")
    String getFromList();
    void setFromList(String fromList);

    @Property("whereClause")
    String getWhereClause();
    void setWhereClause(String whereClause);

    @Property("sqlQuery")
    String getSqlQuery();
    void setSqlQuery(String sqlQuery);

    @Property("javaTypeFqn")
    String getJavaTypeFqn();
    void setJavaTypeFqn(String javaTypeFqn);

    @Relation("HAS_ENTITY_USAGE")
    List<Bc4jEntityUsageDescriptor> getEntityUsages();

    @Relation("HAS_ATTRIBUTE")
    List<Bc4jViewAttributeDescriptor> getAttributes();
}
