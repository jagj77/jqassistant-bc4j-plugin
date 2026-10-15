package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.jqassistant.plugin.xml.api.model.XmlFileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

/**
 * Descriptor del archivo XML BC4J que fue parseado. Es la cabecera del
 * grupo de Bc4jViewObject que se encontraron dentro.
 *
 * <p>Extiende {@link XmlFileDescriptor} para poder encadenarse al
 * descriptor ya creado por el plugin XML de jQAssistant, evitando el
 * conflicto de tipos cuando ambos plugins procesan el mismo archivo.</p>
 *
 * <p>Cada archivo *.xml BC4J (Entity.xml, ViewObject.xml, ViewLink.xml, etc.)
 * genera un nodo con label :Bc4jXmlFile ademas de los labels del plugin
 * XML. Los Bc4jViewObject que contiene cuelgan de el por la arista
 * :DEFINED_IN.</p>
 */
@Label("Bc4jXmlFile")
public interface Bc4jXmlFileDescriptor extends XmlFileDescriptor {

    /** Path completo del archivo dentro del .jar (ej: com/foo/Metadata.xml). */
    @Property("pathInJar")
    String getPathInJar();
    void setPathInJar(String pathInJar);

    /** Tipo raiz del XML: "Entity" | "ViewObject" | "ViewLink" | "Unknown". */
    @Property("bc4jRootElement")
    String getBc4jRootElement();
    void setBc4jRootElement(String rootElement);

    /** View Objects definidos en este archivo. */
    @Relation("DEFINES")
    List<Bc4jViewObjectDescriptor> getViewObjects();
}
