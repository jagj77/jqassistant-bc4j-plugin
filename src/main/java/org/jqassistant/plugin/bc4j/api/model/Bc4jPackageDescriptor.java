package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

@Label("Bc4jPackage")
public interface Bc4jPackageDescriptor extends Descriptor {

    @Property("name")
    String getName();
    void setName(String name);

    @Property("qualifiedName")
    String getQualifiedName();
    void setQualifiedName(String qualifiedName);

    @Property("entityPackage")
    boolean isEntityPackage();
    void setEntityPackage(boolean entityPackage);

    @Property("viewObjectPackage")
    boolean isViewObjectPackage();
    void setViewObjectPackage(boolean viewObjectPackage);

    @Property("applicationModulePackage")
    boolean isApplicationModulePackage();
    void setApplicationModulePackage(boolean applicationModulePackage);
}
