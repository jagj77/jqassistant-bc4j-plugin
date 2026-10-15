package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

public interface Bc4jComponentDescriptor extends NamedDescriptor {

    @Property("qualifiedName")
    String getQualifiedName();
    void setQualifiedName(String qualifiedName);

    @Property("sourceFile")
    String getSourceFile();
    void setSourceFile(String sourceFile);

    @Relation("DEFINED_IN")
    Bc4jXmlFileDescriptor getXmlFile();
    void setXmlFile(Bc4jXmlFileDescriptor xmlFile);
}
