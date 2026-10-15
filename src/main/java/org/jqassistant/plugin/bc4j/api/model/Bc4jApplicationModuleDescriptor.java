package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jApplicationModule")
public interface Bc4jApplicationModuleDescriptor extends Bc4jComponentDescriptor {

    @Property("javaTypeFqn")
    String getJavaTypeFqn();
    void setJavaTypeFqn(String javaTypeFqn);

    @Relation("HAS_VIEW_USAGE")
    List<Bc4jViewUsageDescriptor> getViewUsages();
}
