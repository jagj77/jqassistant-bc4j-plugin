package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

@Label("Bc4jViewAttribute")
public interface Bc4jViewAttributeDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("entityUsageName")
    String getEntityUsageName();
    void setEntityUsageName(String entityUsageName);

    @Property("entityAttributeName")
    String getEntityAttributeName();
    void setEntityAttributeName(String entityAttributeName);

    @Property("javaType")
    String getJavaType();
    void setJavaType(String javaType);

    @Property("columnName")
    String getColumnName();
    void setColumnName(String columnName);

    @Property("isPersistent")
    boolean isPersistent();
    void setPersistent(boolean persistent);

    @Property("isSelected")
    boolean isSelected();
    void setSelected(boolean selected);

    @Property("isUpdateable")
    boolean isUpdateable();
    void setUpdateable(boolean updateable);

    @Property("aliasName")
    String getAliasName();
    void setAliasName(String aliasName);

    @Property("precision")
    String getPrecision();
    void setPrecision(String precision);

    @Property("scale")
    String getScale();
    void setScale(String scale);

    @Property("isNotNull")
    boolean isNotNull();
    void setNotNull(boolean notNull);
}
