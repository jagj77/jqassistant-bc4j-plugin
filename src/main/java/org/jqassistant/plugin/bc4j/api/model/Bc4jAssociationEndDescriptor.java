package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Bc4jAssociationEnd")
public interface Bc4jAssociationEndDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("ownerEntityFqn")
    String getOwnerEntityFqn();
    void setOwnerEntityFqn(String ownerEntityFqn);

    @Property("cardinality")
    String getCardinality();
    void setCardinality(String cardinality);

    @Property("source")
    boolean isSource();
    void setSource(boolean source);

    @Property("attributes")
    String[] getAttributes();
    void setAttributes(String[] attributes);

    @Property("foreignKey")
    String getForeignKey();
    void setForeignKey(String foreignKey);

    @Property("aggregation")
    String getAggregation();
    void setAggregation(String aggregation);

    @Property("isUpdateable")
    boolean isUpdateable();
    void setUpdateable(boolean updateable);

    @Relation("REFERENCES_ENTITY")
    Bc4jEntityDescriptor getEntity();
    void setEntity(Bc4jEntityDescriptor entity);
}
