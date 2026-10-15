package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

@Label("Bc4jEntityAttribute")
public interface Bc4jEntityAttributeDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("columnName")
    String getColumnName();
    void setColumnName(String columnName);

    @Property("javaType")
    String getJavaType();
    void setJavaType(String javaType);

    @Property("columnType")
    String getColumnType();
    void setColumnType(String columnType);

    @Property("sqlType")
    String getSqlType();
    void setSqlType(String sqlType);

    @Property("isNotNull")
    boolean isNotNull();
    void setNotNull(boolean notNull);

    @Property("primaryKey")
    boolean isPrimaryKey();
    void setPrimaryKey(boolean primaryKey);

    @Property("tableName")
    String getTableName();
    void setTableName(String tableName);

    @Property("precision")
    String getPrecision();
    void setPrecision(String precision);

    @Property("scale")
    String getScale();
    void setScale(String scale);

    @Property("isUpdateable")
    boolean isUpdateable();
    void setUpdateable(boolean updateable);
}
