package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jViewLink")
public interface Bc4jViewLinkDescriptor extends Bc4jComponentDescriptor {

    @Property("entityAssociationFqn")
    String getEntityAssociationFqn();
    void setEntityAssociationFqn(String entityAssociationFqn);

    @Relation("HAS_END")
    List<Bc4jViewLinkEndDescriptor> getEnds();
}
