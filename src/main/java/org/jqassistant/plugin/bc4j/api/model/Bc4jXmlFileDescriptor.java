package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.plugin.xml.api.model.XmlFileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;

@Label("Bc4jXmlFile")
public interface Bc4jXmlFileDescriptor extends XmlFileDescriptor {

    @Property("pathInJar")
    String getPathInJar();
    void setPathInJar(String pathInJar);

    @Property("bc4jRootElement")
    String getBc4jRootElement();
    void setBc4jRootElement(String rootElement);
}
