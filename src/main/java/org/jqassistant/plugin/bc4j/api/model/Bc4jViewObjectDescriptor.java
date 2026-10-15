package org.jqassistant.plugin.bc4j.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

import java.util.List;

/**
 * Descriptor de un View Object BC4J (Entity o ViewObject) detectado al
 * parsear los XML de definicion BC4J.
 *
 * <p>Un nodo se crea por cada elemento &lt;Entity&gt; o &lt;ViewObject&gt;
 * dentro de los archivos *.xml BC4J de cualquier .jar/.war del classpath.</p>
 *
 * <p>Propiedades:</p>
 * <ul>
 *   <li>{@code name} - nombre BC4J del View Object (atributo Name="...")</li>
 *   <li>{@code tableName} - tabla SQL apuntada (DBObjectName para Entity,
 *       o la primera tabla del FromList para ViewObject). Nulo si no se
 *       pudo resolver (tipicamente: ViewObject con SQL ad-hoc sin entity).</li>
 *   <li>{@code dbObjectType} - "table" | "view" | "synonym" | "sql" -
 *       tipo del origen de datos, segun el atributo DBObjectType del
 *       XML o "sql" para ViewObjects con FromList puro.</li>
 *   <li>{@code bc4jKind} - "Entity" | "ViewObject" | "ViewLink" -
 *       tipo de elemento BC4J al que pertenece este nodo.</li>
 *   <li>{@code javaTypeFqn} - FQN de la clase Java asociada (RowClass para
 *       Entity; ComponentClass/RowClass para ViewObject). Nulo si no esta
 *       declarada en el XML.</li>
 *   <li>{@code fromList} - SQL FromList (atributo FromList del ViewObject).
 *       Nulo para Entity.</li>
 *   <li>{@code whereClause} - SQL Where (atributo Where del ViewObject).
 *       Nulo para Entity.</li>
 *   <li>{@code sourceFile} - path del .jar + nombre del .xml donde se
 *       declaro este View Object. Sirve para trazabilidad.</li>
 * </ul>
 */
@Label("Bc4jViewObject")
public interface Bc4jViewObjectDescriptor
        extends com.buschmais.jqassistant.plugin.common.api.model.NamedDescriptor {

    /** Tabla SQL apuntada (DBObjectName para Entity, primera tabla del FromList para ViewObject). */
    @Property("tableName")
    String getTableName();
    void setTableName(String tableName);

    /** "table" | "view" | "synonym" | "sql". */
    @Property("dbObjectType")
    String getDbObjectType();
    void setDbObjectType(String dbObjectType);

    /** "Entity" | "ViewObject" | "ViewLink". */
    @Property("bc4jKind")
    String getBc4jKind();
    void setBc4jKind(String bc4jKind);

    /** FQN de la clase Java que implementa este BC4J. */
    @Property("javaTypeFqn")
    String getJavaTypeFqn();
    void setJavaTypeFqn(String javaTypeFqn);

    /** SQL FromList (solo para ViewObject, nulo para Entity). */
    @Property("fromList")
    String getFromList();
    void setFromList(String fromList);

    /** SQL Where (solo para ViewObject, nulo para Entity). */
    @Property("whereClause")
    String getWhereClause();
    void setWhereClause(String whereClause);

    /** Path del .jar + nombre del .xml donde se declaro. */
    @Property("sourceFile")
    String getSourceFile();
    void setSourceFile(String sourceFile);

    /** Arista hacia el Entity referenciado por un ViewObject (resolucion por nombre). */
    @Relation("REFERENCES_ENTITY")
    Bc4jViewObjectDescriptor getReferencedEntity();
    void setReferencedEntity(Bc4jViewObjectDescriptor referencedEntity);

    /** Arista desde el archivo XML que declaro este View Object. */
    @Relation("DEFINED_IN")
    Bc4jXmlFileDescriptor getXmlFile();
    void setXmlFile(Bc4jXmlFileDescriptor xmlFile);
}
