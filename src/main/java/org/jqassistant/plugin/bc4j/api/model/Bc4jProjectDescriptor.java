package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

@Label("Bc4jProject")
public interface Bc4jProjectDescriptor extends Bc4jComponentDescriptor {

    @Property("packageName")
    String getPackageName();
    void setPackageName(String packageName);

    @Property("applicationModuleNames")
    String[] getApplicationModuleNames();
    void setApplicationModuleNames(String[] applicationModuleNames);

    @Relation("DECLARES_PACKAGE")
    List<Bc4jPackageDescriptor> getPackages();
}
