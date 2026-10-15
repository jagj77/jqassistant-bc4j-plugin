package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Bc4jEntityUsage")
public interface Bc4jEntityUsageDescriptor extends NamedDescriptor {

    @Property("entityFqn")
    String getEntityFqn();
    void setEntityFqn(String entityFqn);

    @Property("joinType")
    String getJoinType();
    void setJoinType(String joinType);

    @Relation("REFERENCES_ENTITY")
    Bc4jEntityDescriptor getEntity();
    void setEntity(Bc4jEntityDescriptor entity);
}
