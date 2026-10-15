package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jEntity")
public interface Bc4jEntityDescriptor extends Bc4jComponentDescriptor {

    @Property("tableName")
    String getTableName();
    void setTableName(String tableName);

    @Property("dbObjectType")
    String getDbObjectType();
    void setDbObjectType(String dbObjectType);

    @Property("javaTypeFqn")
    String getJavaTypeFqn();
    void setJavaTypeFqn(String javaTypeFqn);

    @Relation("HAS_ATTRIBUTE")
    List<Bc4jEntityAttributeDescriptor> getAttributes();
}
