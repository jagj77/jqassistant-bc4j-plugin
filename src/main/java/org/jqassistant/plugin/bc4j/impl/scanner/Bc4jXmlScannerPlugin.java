package org.jqassistant.plugin.bc4j.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.buschmais.jqassistant.plugin.xml.api.model.XmlFileDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewObjectDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jXmlFileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scanner de archivos XML BC4J. Detecta archivos *.xml cuyo elemento raiz
 * es &lt;Entity&gt;, &lt;ViewObject&gt; o &lt;ViewLink&gt; (los DTDs BC4J
 * son: {@code jbo_03_01.dtd} o {@code jbo_03_02.dtd}), los parsea y
 * emite un nodo :Bc4jXmlFile con sus :Bc4jViewObject colgando.
 *
 * <p>El scanner NO resuelve referencias cross-archivo (un ViewObject que
 * referencia un Entity por nombre). Esa resolucion la hace un concepto
 * jQA (:bc4j:ResolveEntityReferences) porque el scanner es stateless
 * respecto a la lista completa de archivos.</p>
 *
 * <h2>Forma de los nodos emitidos</h2>
 * <pre>
 * (:File:Resource) -[:CONTAINS]-&gt; (:Bc4jXmlFile {fileName, pathInJar, rootElement})
 *                                          -[:DEFINES]-&gt;
 *                                          (:Bc4jViewObject {
 *                                              name, tableName?, dbObjectType,
 *                                              bc4jKind, javaTypeFqn?,
 *                                              fromList?, whereClause?, sourceFile
 *                                          })
 * </pre>
 */
public class Bc4jXmlScannerPlugin
        extends AbstractScannerPlugin<FileResource, Bc4jXmlFileDescriptor> {

    private static final Logger LOG = LoggerFactory.getLogger(Bc4jXmlScannerPlugin.class);

    /**
     * Regex para el nombre corto desde un atributo Entity="<fqn>".
     * Tomamos el segmento despues del ultimo punto.
     */
    private static final Pattern SHORT_NAME = Pattern.compile("([^.]+)$");

    /**
     * Constructor por defecto requerido por jQA.
     */
    public Bc4jXmlScannerPlugin() {
    }

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) {
        // Filtro rapido por extension. El filtro real (DOCTYPE jbo_* o root
        // element Entity/ViewObject/ViewLink) se hace dentro de accepts()
        // tambien, leyendo solo los primeros bytes. Esto evita que el plugin
        // XML de jQA compita con nosotros por los archivos no-BC4J (que
        // ese plugin SI debe procesar). Para los BC4J, este plugin retorna
        // true primero y el XML plugin nunca los ve.
        if (path == null) {
            return false;
        }
        if (!path.toLowerCase().endsWith(".xml")) {
            return false;
        }
        try (InputStream in = item.createStream()) {
            byte[] head = new byte[512];
            int n = in.read(head);
            if (n <= 0) {
                return false;
            }
            String sniff = new String(head, 0, n, "ISO-8859-1");
            return isBc4jXml(sniff);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Bc4jXmlFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner)
            throws IOException {
        // El scanner no recibe un descriptor previo de tipo Bc4jXmlFileDescriptor
        // (somos los primeros en procesar BC4J). Pero como nuestro descriptor
        // extiende XmlFileDescriptor, el plugin XML puede encadenarse despues
        // de nosotros. Esa parte la hace el plugin XML solito, no nosotros.
        return doScan(item, path, scope, scanner);
    }

    /**
     * Encadenamiento: si otro plugin (tipicamente el plugin XML) ya creo
     * un descriptor, lo recibimos aqui y le agregamos el label :Bc4jXmlFile
     * + las relaciones a los Bc4jViewObject. Esto evita el conflicto de
     * tipos que tiene XO cuando dos plugins crean descriptores diferentes
     * para el mismo nodo.
     */
    public Bc4jXmlFileDescriptor scan(FileResource item, Bc4jXmlFileDescriptor previous, String path, Scope scope, Scanner scanner)
            throws IOException {
        Bc4jXmlFileDescriptor created = doScan(item, path, scope, scanner);
        // Si doScan devolvio algo y previous existe y es distinto, los
        // conceptos cargan los Bc4jViewObject que creo doScan; no hay
        // que hacer nada mas.
        return created != null ? created : previous;
    }

    private Bc4jXmlFileDescriptor doScan(FileResource item, String path, Scope scope, Scanner scanner)
            throws IOException {

        // Lee todo el contenido en memoria. Los XMLs BC4J observados
        // pesan entre 0.5 y 5 KB, asi que es seguro.
        String xml;
        try (InputStream in = item.createStream()) {
            xml = new String(readAllBytes(in), "ISO-8859-1");
        }

        if (!isBc4jXml(xml)) {
            // No es BC4J, ignora silenciosamente.
            return null;
        }

        LOG.debug("BC4J scanner: detectando {}", path);

        Document doc;
        try {
            doc = parseXml(xml);
        } catch (Exception e) {
            LOG.warn("BC4J scanner: no se pudo parsear {}: {}", path, e.getMessage());
            return null;
        }

        Element root = doc.getDocumentElement();
        String rootName = root.getTagName();
        String rootElement = switch (rootName) {
            case "Entity" -> "Entity";
            case "ViewObject" -> "ViewObject";
            case "ViewLink" -> "ViewLink";
            default -> "Unknown";
        };

        if ("Unknown".equals(rootElement)) {
            // Otro XML que tiene DOCTYPE jbo_* pero no es Entity/ViewObject/ViewLink.
            // Por las dudas lo registramos igual como Bc4jXmlFile para que quede
            // trazabilidad, pero sin view objects.
            rootElement = rootName;
        }

        ScannerContext ctx = getScannerContext();
        Store store = ctx.getStore();

        // 1) Crear el nodo :Bc4jXmlFile. Como Bc4jXmlFileDescriptor extiende
        //    XmlFileDescriptor, XO crea un nodo con ambos labels y lo
        //    conecta por CONTAINS al File:Resource.
        Bc4jXmlFileDescriptor xmlFile = store.create(Bc4jXmlFileDescriptor.class);
        xmlFile.setFileName(extractFileName(path));
        xmlFile.setPathInJar(path);
        xmlFile.setBc4jRootElement(rootElement);

        // 2) Procesar el contenido segun el tipo de raiz
        switch (rootName) {
            case "Entity" -> processEntity(root, path, xmlFile, store);
            case "ViewObject" -> processViewObject(root, path, xmlFile, store);
            case "ViewLink" -> processViewLink(root, path, xmlFile, store);
            default -> LOG.debug("BC4J scanner: root element {} no soportado en {}", rootName, path);
        }

        return xmlFile;
    }

    // ------------------------------------------------------------------
    // Procesamiento por tipo de raiz
    // ------------------------------------------------------------------

    private void processEntity(Element entity, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = entity.getAttribute("Name");
        String tableName = entity.getAttribute("DBObjectName");
        String dbObjectType = entity.getAttribute("DBObjectType");
        String rowClass = entity.getAttribute("RowClass");

        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: Entity sin Name en {}", path);
            return;
        }

        Bc4jViewObjectDescriptor vo = store.create(Bc4jViewObjectDescriptor.class);
        vo.setName(name);
        if (!tableName.isEmpty()) {
            vo.setTableName(tableName);
        }
        vo.setDbObjectType(dbObjectType.isEmpty() ? "table" : dbObjectType);
        vo.setBc4jKind("Entity");
        if (!rowClass.isEmpty()) {
            vo.setJavaTypeFqn(rowClass);
        }
        vo.setSourceFile(path);
        vo.setXmlFile(xmlFile);
    }

    private void processViewObject(Element viewObject, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = viewObject.getAttribute("Name");
        String fromList = viewObject.getAttribute("FromList");
        String whereClause = viewObject.getAttribute("Where");
        String componentClass = viewObject.getAttribute("ComponentClass");
        String rowClass = viewObject.getAttribute("RowClass");

        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: ViewObject sin Name en {}", path);
            return;
        }

        // Resolucion de tabla para el ViewObject:
        //   1) Si tiene <EntityUsage Entity="<fqn>"> y existe un Entity
        //      con name igual al segmento final del fqn, ese Entity
        //      manda. PERO esta resolucion la hace el concepto
        //      :bc4j:ResolveEntityReferences en una segunda pasada
        //      (porque los archivos pueden venir en cualquier orden
        //      del classpath). Por ahora solo guardamos el nombre
        //      corto como propiedad "entityUsageName" para que la
        //      regla lo encuentre.
        //   2) Si el FromList es de la forma "TABLA alias" o
        //      "TABLA1 alias1, TABLA2 alias2", tomamos la primera.
        String resolvedTable = null;
        String entityUsageName = null;
        NodeList euList = viewObject.getElementsByTagName("EntityUsage");
        if (euList.getLength() > 0) {
            Element eu = (Element) euList.item(0);
            String entityRef = eu.getAttribute("Entity");
            Matcher m = SHORT_NAME.matcher(entityRef);
            if (m.find()) {
                entityUsageName = m.group(1);
            }
        }
        if (entityUsageName == null) {
            resolvedTable = extractFirstTableFromFromList(fromList);
        }

        Bc4jViewObjectDescriptor vo = store.create(Bc4jViewObjectDescriptor.class);
        vo.setName(name);
        // Logica de tabla:
        //   - Si hay <EntityUsage>, la tabla final la determina la entidad
        //     referenciada. El scanner no puede resolverlo (orden de archivos
        //     no garantizado), asi que pone como placeholder el nombre corto
        //     de la entidad. La regla :bc4j:ResolveEntityReferences lo
        //     sobreescribe con la tabla real de la entidad.
        //   - Si no hay <EntityUsage>, la tabla viene del FromList.
        if (entityUsageName != null) {
            vo.setTableName(entityUsageName); // placeholder, se sobreescribe
        } else if (resolvedTable != null) {
            vo.setTableName(resolvedTable);
        }
        vo.setDbObjectType("sql"); // ViewObject: SQL dinamico
        vo.setBc4jKind("ViewObject");
        String javaType = !componentClass.isEmpty() ? componentClass : rowClass;
        if (!javaType.isEmpty()) {
            vo.setJavaTypeFqn(javaType);
        }
        if (!fromList.isEmpty()) {
            vo.setFromList(fromList);
        }
        if (!whereClause.isEmpty()) {
            vo.setWhereClause(whereClause);
        }
        vo.setSourceFile(path);
        vo.setXmlFile(xmlFile);
    }

    private void processViewLink(Element viewLink, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = viewLink.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: ViewLink sin Name en {}", path);
            return;
        }
        Bc4jViewObjectDescriptor vo = store.create(Bc4jViewObjectDescriptor.class);
        vo.setName(name);
        vo.setDbObjectType("link");
        vo.setBc4jKind("ViewLink");
        vo.setSourceFile(path);
        vo.setXmlFile(xmlFile);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Detecta si el XML es un archivo BC4J. Criterios:
     *   - El DOCTYPE referencia un DTD jbo_*.dtd (jbo_03_01, jbo_03_02).
     *   - O el elemento raiz es Entity / ViewObject / ViewLink
     *     (algunos BC4J XMLs generados no tienen DOCTYPE).
     */
    private static boolean isBc4jXml(String xml) {
        if (xml.contains("jbo_")) {
            return true; // DOCTYPE jbo_*.dtd detectado
        }
        // Fallback: detectar por raiz
        String trimmed = xml.stripLeading();
        return trimmed.startsWith("<Entity ")
                || trimmed.startsWith("<Entity>")
                || trimmed.startsWith("<ViewObject ")
                || trimmed.startsWith("<ViewObject>")
                || trimmed.startsWith("<ViewLink ")
                || trimmed.startsWith("<ViewLink>");
    }

    /**
     * Parsea el XML de forma tolerante: desactiva DTD validation y
     * resolucion de entidades externas (los DTDs BC4J son locales al
     * runtime de BC4J y no estan en el classpath del escaneo).
     */
    private static Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        // El DOCTYPE jbo_03_01.dtd no existe en el classpath; lo resolvemos
        // vacio para que el parser no falle. El scanner solo necesita los
        // atributos del elemento raiz, no la DTD.
        db.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

        try (InputStream in = new java.io.ByteArrayInputStream(xml.getBytes("ISO-8859-1"))) {
            return db.parse(new InputSource(in));
        }
    }

    /**
     * Extrae la primera tabla de un FromList. Formatos esperados:
     *   "TABLA alias"
     *   "TABLA1 alias1, TABLA2 alias2 ..."
     *   "SCHEMA.TABLA alias"
     */
    private static String extractFirstTableFromFromList(String fromList) {
        if (fromList == null || fromList.isBlank()) {
            return null;
        }
        // Tomamos la primera "palabra" (separada por espacios o comas).
        String first = fromList.split("[\\s,;]+")[0];
        if (first.isBlank()) {
            return null;
        }
        // Si tiene esquema "SCHEMA.TABLA" tomamos solo la tabla.
        int dot = first.lastIndexOf('.');
        return dot >= 0 ? first.substring(dot + 1) : first;
    }

    private static String extractFileName(String path) {
        if (path == null) {
            return "";
        }
        int sep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return sep >= 0 ? path.substring(sep + 1) : path;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = in.read(tmp)) > 0) {
            buf.write(tmp, 0, n);
        }
        return buf.toByteArray();
    }
}
