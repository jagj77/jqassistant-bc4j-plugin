package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Bc4jViewUsage")
public interface Bc4jViewUsageDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("viewObjectFqn")
    String getViewObjectFqn();
    void setViewObjectFqn(String viewObjectFqn);

    @Relation("USES_VIEW_OBJECT")
    Bc4jViewObjectDescriptor getViewObject();
    void setViewObject(Bc4jViewObjectDescriptor viewObject);
}
