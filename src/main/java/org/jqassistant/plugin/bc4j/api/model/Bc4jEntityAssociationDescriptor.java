package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jEntityAssociation")
public interface Bc4jEntityAssociationDescriptor extends Bc4jComponentDescriptor {

    @Relation("HAS_END")
    List<Bc4jAssociationEndDescriptor> getEnds();
}
