package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Bc4jViewLinkEnd")
public interface Bc4jViewLinkEndDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("ownerViewObjectFqn")
    String getOwnerViewObjectFqn();
    void setOwnerViewObjectFqn(String ownerViewObjectFqn);

    @Property("cardinality")
    String getCardinality();
    void setCardinality(String cardinality);

    @Property("source")
    boolean isSource();
    void setSource(boolean source);

    @Property("attributes")
    String[] getAttributes();
    void setAttributes(String[] attributes);

    @Property("finderName")
    String getFinderName();
    void setFinderName(String finderName);

    @Property("isUpdateable")
    boolean isUpdateable();
    void setUpdateable(boolean updateable);

    @Relation("REFERENCES_VIEW_OBJECT")
    Bc4jViewObjectDescriptor getViewObject();
    void setViewObject(Bc4jViewObjectDescriptor viewObject);
}
