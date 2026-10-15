package org.jqassistant.plugin.bc4j.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import org.jqassistant.plugin.bc4j.api.model.Bc4jApplicationModuleConfigDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jApplicationModuleDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jAssociationEndDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jEntityAssociationDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jEntityAttributeDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jEntityDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jEntityUsageDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jPackageDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jProjectDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewAttributeDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewLinkDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewLinkEndDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewObjectDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jViewUsageDescriptor;
import org.jqassistant.plugin.bc4j.api.model.Bc4jXmlFileDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Escanea definiciones de componentes BC4J, registros de proyecto y configuración de runtime. */
public class Bc4jXmlScannerPlugin extends AbstractScannerPlugin<FileResource, Bc4jXmlFileDescriptor> {

    private static final Logger LOG = LoggerFactory.getLogger(Bc4jXmlScannerPlugin.class);

    public Bc4jXmlScannerPlugin() {
    }

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) {
        if (path == null) {
            return false;
        }
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (!lowerPath.endsWith(".xml") && !lowerPath.endsWith(".xcfg") && !lowerPath.endsWith(".jpx")) {
            return false;
        }
        try (InputStream in = item.createStream()) {
            byte[] head = new byte[512];
            int size = 0;
            int read;
            while (size < head.length && (read = in.read(head, size, head.length - size)) > 0) {
                size += read;
            }
            return size > 0 && isBc4jXml(new String(head, 0, size, StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Bc4jXmlFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner)
            throws IOException {
        return doScan(item, path, scanner);
    }

    public Bc4jXmlFileDescriptor scan(FileResource item, Bc4jXmlFileDescriptor previous, String path,
                                      Scope scope, Scanner scanner) throws IOException {
        Bc4jXmlFileDescriptor created = doScan(item, path, scanner);
        return created != null ? created : previous;
    }

    private Bc4jXmlFileDescriptor doScan(FileResource item, String path, Scanner scanner) throws IOException {
        byte[] bytes;
        try (InputStream in = item.createStream()) {
            bytes = readAllBytes(in);
        }
        String sniff = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.ISO_8859_1);
        if (!isBc4jXml(sniff)) {
            return null;
        }

        Document document;
        try {
            document = parseXml(bytes);
        } catch (Exception e) {
            LOG.warn("BC4J scanner: no se pudo parsear {}: {}", path, e.getMessage());
            return null;
        }

        Element root = document.getDocumentElement();
        String rootName = localName(root);
        if (!"Entity".equals(rootName) && !"ViewObject".equals(rootName)
                && !"ViewLink".equals(rootName) && !"AppModule".equals(rootName)
                && !"Association".equals(rootName) && !"BC4JConfig".equals(rootName)
                && !"JboProject".equals(rootName)) {
            LOG.debug("BC4J scanner: root element {} no soportado en {}", rootName, path);
            return null;
        }

        ScannerContext context = getScannerContext();
        Store store = context.getStore();
        Bc4jXmlFileDescriptor xmlFile = store.create(Bc4jXmlFileDescriptor.class);
        xmlFile.setFileName(extractFileName(path));
        xmlFile.setPathInJar(path);
        xmlFile.setBc4jRootElement(rootName);

        switch (rootName) {
            case "Entity" -> processEntity(root, path, xmlFile, store);
            case "ViewObject" -> processViewObject(root, path, xmlFile, store);
            case "ViewLink" -> processViewLink(root, path, xmlFile, store);
            case "AppModule" -> processApplicationModule(root, path, xmlFile, store);
            case "Association" -> processEntityAssociation(root, path, xmlFile, store);
            case "BC4JConfig" -> processApplicationModuleConfig(root, path, xmlFile, store);
            case "JboProject" -> processJboProject(root, path, xmlFile, store);
            default -> throw new IllegalStateException("Unexpected BC4J root element: " + rootName);
        }
        return xmlFile;
    }

    private void processEntity(Element entity, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = entity.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: Entity sin Name en {}", path);
            return;
        }

        Bc4jEntityDescriptor descriptor = store.create(Bc4jEntityDescriptor.class);
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        descriptor.setSourceFile(path);
        descriptor.setTableName(emptyToNull(entity.getAttribute("DBObjectName")));
        String dbObjectType = emptyToNull(entity.getAttribute("DBObjectType"));
        descriptor.setDbObjectType(dbObjectType == null ? "table" : dbObjectType);
        descriptor.setJavaTypeFqn(emptyToNull(entity.getAttribute("RowClass")));
        descriptor.setXmlFile(xmlFile);

        Set<String> primaryKeyAttributes = primaryKeyAttributeNames(entity);
        NodeList attributes = elementsByLocalName(entity, "Attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            String attributeName = attribute.getAttribute("Name");
            if (attributeName.isEmpty()) {
                continue;
            }
            Bc4jEntityAttributeDescriptor attributeDescriptor = store.create(Bc4jEntityAttributeDescriptor.class);
            attributeDescriptor.setName(attributeName);
            attributeDescriptor.setColumnName(emptyToNull(attribute.getAttribute("ColumnName")));
            attributeDescriptor.setJavaType(emptyToNull(attribute.getAttribute("Type")));
            attributeDescriptor.setColumnType(emptyToNull(attribute.getAttribute("ColumnType")));
            attributeDescriptor.setSqlType(emptyToNull(attribute.getAttribute("SQLType")));
            attributeDescriptor.setNotNull(Boolean.parseBoolean(attribute.getAttribute("IsNotNull")));
            attributeDescriptor.setPrimaryKey(Boolean.parseBoolean(attribute.getAttribute("PrimaryKey"))
                    || primaryKeyAttributes.contains(attributeName));
            attributeDescriptor.setTableName(emptyToNull(attribute.getAttribute("TableName")));
            attributeDescriptor.setPrecision(emptyToNull(attribute.getAttribute("Precision")));
            attributeDescriptor.setScale(emptyToNull(attribute.getAttribute("Scale")));
            String updateable = attribute.getAttribute("IsUpdateable");
            if (updateable.isEmpty()) {
                updateable = attribute.getAttribute("IsUpdatable");
            }
            attributeDescriptor.setUpdateable(Boolean.parseBoolean(updateable));
            descriptor.getAttributes().add(attributeDescriptor);
        }
    }

    private static Set<String> primaryKeyAttributeNames(Element entity) {
        Set<String> names = new LinkedHashSet<>();
        NodeList keys = elementsByLocalName(entity, "Key");
        for (int i = 0; i < keys.getLength(); i++) {
            Element key = (Element) keys.item(i);
            if (!Boolean.parseBoolean(key.getAttribute("PrimaryKey"))) {
                continue;
            }
            NodeList items = elementsByLocalName(key, "Item");
            for (int j = 0; j < items.getLength(); j++) {
                String value = ((Element) items.item(j)).getAttribute("Value");
                int separator = value.lastIndexOf('.');
                names.add(separator >= 0 ? value.substring(separator + 1) : value);
            }
        }
        return names;
    }

    private void processViewObject(Element viewObject, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = viewObject.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: ViewObject sin Name en {}", path);
            return;
        }

        Bc4jViewObjectDescriptor descriptor = store.create(Bc4jViewObjectDescriptor.class);
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        String javaTypeFqn = viewObject.getAttribute("ComponentClass");
        if (javaTypeFqn.isEmpty()) {
            javaTypeFqn = viewObject.getAttribute("RowClass");
        }
        descriptor.setJavaTypeFqn(emptyToNull(javaTypeFqn));
        descriptor.setSourceFile(path);
        descriptor.setFromList(emptyToNull(viewObject.getAttribute("FromList")));
        descriptor.setWhereClause(emptyToNull(viewObject.getAttribute("Where")));
        descriptor.setSqlQuery(textOfFirstChild(viewObject, "SQLQuery"));
        descriptor.setXmlFile(xmlFile);

        NodeList usages = elementsByLocalName(viewObject, "EntityUsage");
        for (int i = 0; i < usages.getLength(); i++) {
            Element usage = (Element) usages.item(i);
            String entityFqn = usage.getAttribute("Entity");
            if (entityFqn.isEmpty()) {
                continue;
            }
            Bc4jEntityUsageDescriptor usageDescriptor = store.create(Bc4jEntityUsageDescriptor.class);
            usageDescriptor.setName(usage.getAttribute("Name"));
            usageDescriptor.setEntityFqn(entityFqn);
            usageDescriptor.setJoinType(emptyToNull(usage.getAttribute("JoinType")));
            descriptor.getEntityUsages().add(usageDescriptor);
        }

        NodeList attributes = elementsByLocalName(viewObject, "ViewAttribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            String attributeName = attribute.getAttribute("Name");
            if (attributeName.isEmpty()) {
                continue;
            }
            Bc4jViewAttributeDescriptor attributeDescriptor = store.create(Bc4jViewAttributeDescriptor.class);
            attributeDescriptor.setName(attributeName);
            attributeDescriptor.setEntityUsageName(emptyToNull(attribute.getAttribute("EntityUsage")));
            attributeDescriptor.setEntityAttributeName(emptyToNull(attribute.getAttribute("EntityAttrName")));
            attributeDescriptor.setJavaType(emptyToNull(attribute.getAttribute("Type")));
            attributeDescriptor.setColumnName(emptyToNull(attribute.getAttribute("ColumnName")));
            attributeDescriptor.setPersistent(Boolean.parseBoolean(attribute.getAttribute("IsPersistent")));
            attributeDescriptor.setSelected(Boolean.parseBoolean(attribute.getAttribute("IsSelected")));
            String updateable = attribute.getAttribute("IsUpdateable");
            if (updateable.isEmpty()) {
                updateable = attribute.getAttribute("IsUpdatable");
            }
            attributeDescriptor.setUpdateable(Boolean.parseBoolean(updateable));
            attributeDescriptor.setAliasName(emptyToNull(attribute.getAttribute("AliasName")));
            attributeDescriptor.setPrecision(emptyToNull(attribute.getAttribute("Precision")));
            attributeDescriptor.setScale(emptyToNull(attribute.getAttribute("Scale")));
            attributeDescriptor.setNotNull(Boolean.parseBoolean(attribute.getAttribute("IsNotNull")));
            descriptor.getAttributes().add(attributeDescriptor);
        }

        if (descriptor.getEntityUsages().isEmpty()) {
            Bc4jTableSourceParser.TableMappings mappings =
                    Bc4jTableSourceParser.extract(descriptor.getFromList(), descriptor.getSqlQuery());
            descriptor.setTableNamesComplete(mappings.complete());
            if (mappings.tableNames().length > 0) {
                descriptor.setTableNames(mappings.tableNames());
                if (mappings.complete() && mappings.tableNames().length == 1) {
                    descriptor.setTableName(mappings.tableNames()[0]);
                }
            }
        }
    }

    private void processViewLink(Element viewLink, String path, Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = viewLink.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: ViewLink sin Name en {}", path);
            return;
        }

        Bc4jViewLinkDescriptor descriptor = store.create(Bc4jViewLinkDescriptor.class);
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        descriptor.setSourceFile(path);
        descriptor.setEntityAssociationFqn(emptyToNull(viewLink.getAttribute("EntityAssociation")));
        descriptor.setXmlFile(xmlFile);

        NodeList ends = elementsByLocalName(viewLink, "ViewLinkDefEnd");
        for (int i = 0; i < ends.getLength(); i++) {
            Element end = (Element) ends.item(i);
            Bc4jViewLinkEndDescriptor endDescriptor = store.create(Bc4jViewLinkEndDescriptor.class);
            endDescriptor.setName(end.getAttribute("Name"));
            endDescriptor.setOwnerViewObjectFqn(emptyToNull(end.getAttribute("Owner")));
            endDescriptor.setCardinality(emptyToNull(end.getAttribute("Cardinality")));
            endDescriptor.setSource(Boolean.parseBoolean(end.getAttribute("Source")));
            endDescriptor.setAttributes(attributeValues(end));
            endDescriptor.setFinderName(designTimeValue(end, "_finderName"));
            endDescriptor.setUpdateable(Boolean.parseBoolean(designTimeValue(end, "_isUpdateable")));
            descriptor.getEnds().add(endDescriptor);
        }
    }

    private void processApplicationModule(Element applicationModule, String path,
                                          Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = applicationModule.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: AppModule sin Name en {}", path);
            return;
        }

        Bc4jApplicationModuleDescriptor descriptor = store.create(Bc4jApplicationModuleDescriptor.class);
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        descriptor.setSourceFile(path);
        descriptor.setJavaTypeFqn(emptyToNull(applicationModule.getAttribute("ComponentClass")));
        descriptor.setXmlFile(xmlFile);

        NodeList usages = elementsByLocalName(applicationModule, "ViewUsage");
        for (int i = 0; i < usages.getLength(); i++) {
            Element usage = (Element) usages.item(i);
            String viewObjectFqn = usage.getAttribute("ViewObjectName");
            if (viewObjectFqn.isEmpty()) {
                continue;
            }
            Bc4jViewUsageDescriptor usageDescriptor = store.create(Bc4jViewUsageDescriptor.class);
            usageDescriptor.setName(usage.getAttribute("Name"));
            usageDescriptor.setViewObjectFqn(viewObjectFqn);
            descriptor.getViewUsages().add(usageDescriptor);
        }
    }

    private void processEntityAssociation(Element association, String path,
                                          Bc4jXmlFileDescriptor xmlFile, Store store) {
        String name = association.getAttribute("Name");
        if (name.isEmpty()) {
            LOG.warn("BC4J scanner: Association sin Name en {}", path);
            return;
        }

        Bc4jEntityAssociationDescriptor descriptor = store.create(Bc4jEntityAssociationDescriptor.class);
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        descriptor.setSourceFile(path);
        descriptor.setXmlFile(xmlFile);

        NodeList ends = elementsByLocalName(association, "AssociationEnd");
        for (int i = 0; i < ends.getLength(); i++) {
            Element end = (Element) ends.item(i);
            Bc4jAssociationEndDescriptor endDescriptor = store.create(Bc4jAssociationEndDescriptor.class);
            endDescriptor.setName(end.getAttribute("Name"));
            endDescriptor.setOwnerEntityFqn(emptyToNull(end.getAttribute("Owner")));
            endDescriptor.setCardinality(emptyToNull(end.getAttribute("Cardinality")));
            endDescriptor.setSource(Boolean.parseBoolean(end.getAttribute("Source")));
            endDescriptor.setForeignKey(designTimeValue(end, "_foreignKey"));
            endDescriptor.setAggregation(designTimeValue(end, "_aggregation"));
            endDescriptor.setUpdateable(Boolean.parseBoolean(designTimeValue(end, "_isUpdateable")));
            endDescriptor.setAttributes(attributeValues(end));
            descriptor.getEnds().add(endDescriptor);
        }
    }

    private void processApplicationModuleConfig(Element root, String path,
                                                Bc4jXmlFileDescriptor xmlFile, Store store) {
        NodeList configurations = elementsByLocalName(root, "AppModuleConfig");
        for (int i = 0; i < configurations.getLength(); i++) {
            Element configuration = (Element) configurations.item(i);
            String name = configuration.getAttribute("name");
            if (name.isEmpty()) {
                continue;
            }

            String applicationName = configuration.getAttribute("ApplicationName");
            if (applicationName.isEmpty() && configuration.getParentNode() instanceof Element bag) {
                applicationName = bag.getAttribute("ApplicationName");
            }
            Bc4jApplicationModuleConfigDescriptor descriptor =
                    store.create(Bc4jApplicationModuleConfigDescriptor.class);
            descriptor.setName(name);
            descriptor.setApplicationName(emptyToNull(applicationName));
            descriptor.setQualifiedName(applicationName.isEmpty() ? name : applicationName + "#" + name);
            descriptor.setSourceFile(path);
            descriptor.setDeployPlatform(emptyToNull(configuration.getAttribute("DeployPlatform")));
            descriptor.setJdbcName(emptyToNull(configuration.getAttribute("JDBCName")));
            descriptor.setPoolProperties(poolProperties(configuration));
            descriptor.setDatabaseProperties(databaseProperties(configuration));
            descriptor.setXmlFile(xmlFile);
        }
    }

    private void processJboProject(Element project, String path,
                                   Bc4jXmlFileDescriptor xmlFile, Store store) {
        Bc4jProjectDescriptor descriptor = store.create(Bc4jProjectDescriptor.class);
        String name = project.getAttribute("Name");
        descriptor.setName(name);
        descriptor.setQualifiedName(qualifiedName(path, name));
        descriptor.setSourceFile(path);
        descriptor.setXmlFile(xmlFile);
        descriptor.setPackageName(emptyToNull(project.getAttribute("PackageName")));

        List<String> applicationModules = new ArrayList<>();
        NodeList designAttributes = elementsByLocalName(project, "Attr");
        for (int i = 0; i < designAttributes.getLength(); i++) {
            Element attribute = (Element) designAttributes.item(i);
            if (attribute.getAttribute("Name").startsWith("_appModuleNames")) {
                String value = attribute.getAttribute("Value");
                if (!value.isEmpty()) {
                    applicationModules.add(value);
                }
            }
        }
        descriptor.setApplicationModuleNames(applicationModules.toArray(String[]::new));

        NodeList containees = elementsByLocalName(project, "Containee");
        for (int i = 0; i < containees.getLength(); i++) {
            Element containee = (Element) containees.item(i);
            String packageName = containee.getAttribute("FullName");
            if (packageName.isEmpty()) {
                packageName = containee.getAttribute("PackageName");
            }
            if (packageName.isEmpty()) {
                continue;
            }
            Bc4jPackageDescriptor packageDescriptor = store.create(Bc4jPackageDescriptor.class);
            packageDescriptor.setName(containee.getAttribute("Name"));
            packageDescriptor.setQualifiedName(packageName);
            NodeList flags = elementsByLocalName(containee, "Attr");
            for (int j = 0; j < flags.getLength(); j++) {
                Element flag = (Element) flags.item(j);
                boolean enabled = Boolean.parseBoolean(flag.getAttribute("Value"));
                switch (flag.getAttribute("Name")) {
                    case "_EO" -> packageDescriptor.setEntityPackage(enabled);
                    case "_VO" -> packageDescriptor.setViewObjectPackage(enabled);
                    case "_AM" -> packageDescriptor.setApplicationModulePackage(enabled);
                    default -> {
                    }
                }
            }
            descriptor.getPackages().add(packageDescriptor);
        }
    }

    private static String[] poolProperties(Element configuration) {
        List<String> properties = new ArrayList<>();
        NodeList poolingElements = elementsByLocalName(configuration, "AM-Pooling");
        for (int i = 0; i < poolingElements.getLength(); i++) {
            NamedNodeMap attributes = poolingElements.item(i).getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Node attribute = attributes.item(j);
                if (!isSensitiveConfigurationProperty(attribute.getNodeName())) {
                    properties.add(attribute.getNodeName() + "=" + attribute.getNodeValue());
                }
            }
        }
        return properties.toArray(String[]::new);
    }

    private static String[] databaseProperties(Element configuration) {
        List<String> properties = new ArrayList<>();
        NodeList databaseElements = elementsByLocalName(configuration, "Database");
        for (int i = 0; i < databaseElements.getLength(); i++) {
            NamedNodeMap attributes = databaseElements.item(i).getAttributes();
            for (int j = 0; j < attributes.getLength(); j++) {
                Node attribute = attributes.item(j);
                if (!isSensitiveConfigurationProperty(attribute.getNodeName())) {
                    properties.add(attribute.getNodeName() + "=" + attribute.getNodeValue());
                }
            }
        }
        return properties.toArray(String[]::new);
    }

    private static boolean isSensitiveConfigurationProperty(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("connection") || normalized.contains("user")
                || normalized.contains("password") || normalized.contains("credential")
                || normalized.contains("secret") || normalized.endsWith("url");
    }

    private static String[] attributeValues(Element definitionEnd) {
        List<String> values = new ArrayList<>();
        NodeList arrays = elementsByLocalName(definitionEnd, "AttrArray");
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            if (!"Attributes".equals(array.getAttribute("Name"))) {
                continue;
            }
            NodeList items = elementsByLocalName(array, "Item");
            for (int j = 0; j < items.getLength(); j++) {
                String value = ((Element) items.item(j)).getAttribute("Value");
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values.toArray(String[]::new);
    }

    private static String designTimeValue(Element element, String name) {
        NodeList attributes = elementsByLocalName(element, "Attr");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attribute = (Element) attributes.item(i);
            if (name.equals(attribute.getAttribute("Name"))) {
                return emptyToNull(attribute.getAttribute("Value"));
            }
        }
        return null;
    }

    private static boolean isBc4jXml(String xml) {
        if (xml.toLowerCase(Locale.ROOT).contains("jbo_")) {
            return true;
        }
        String rootName = rootElementFromHeader(xml);
        return "Entity".equals(rootName) || "ViewObject".equals(rootName)
                || "ViewLink".equals(rootName) || "Association".equals(rootName)
                || "AppModule".equals(rootName) || "BC4JConfig".equals(rootName)
                || "JboProject".equals(rootName);
    }

    private static String rootElementFromHeader(String xml) {
        String remaining = xml.startsWith("\uFEFF") ? xml.substring(1) : xml;
        while (true) {
            remaining = remaining.stripLeading();
            String terminator;
            if (remaining.startsWith("<?")) {
                terminator = "?>";
            } else if (remaining.startsWith("<!--")) {
                terminator = "-->";
            } else if (remaining.startsWith("<!DOCTYPE")) {
                terminator = ">";
            } else {
                break;
            }
            int end = remaining.indexOf(terminator);
            if (end < 0) {
                return null;
            }
            remaining = remaining.substring(end + terminator.length());
        }
        if (!remaining.startsWith("<") || remaining.startsWith("</") || remaining.startsWith("<!")) {
            return null;
        }
        int end = 1;
        while (end < remaining.length()) {
            char character = remaining.charAt(end);
            if (Character.isWhitespace(character) || character == '/' || character == '>') {
                break;
            }
            end++;
        }
        if (end == 1) {
            return null;
        }
        String name = remaining.substring(1, end);
        int colon = name.lastIndexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static Document parseXml(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder.parse(new ByteArrayInputStream(xml));
    }


    private static String qualifiedName(String path, String name) {
        if (name.indexOf('.') >= 0) {
            return name;
        }
        String normalized = path.replace('\\', '/');
        int archiveSeparator = normalized.lastIndexOf("!/");
        if (archiveSeparator >= 0) {
            normalized = normalized.substring(archiveSeparator + 2);
        }
        int separator = normalized.lastIndexOf('/');
        if (separator < 0) {
            return name;
        }
        String packagePath = normalized.substring(0, separator).replace('/', '.');
        return packagePath.isEmpty() ? name : packagePath + "." + name;
    }

    private static String textOfFirstChild(Element parent, String tagName) {
        NodeList children = elementsByLocalName(parent, tagName);
        return children.getLength() == 0 ? null : emptyToNull(children.item(0).getTextContent().strip());
    }

    private static NodeList elementsByLocalName(Element parent, String expectedName) {
        NodeList namespaced = parent.getElementsByTagNameNS("*", expectedName);
        return namespaced.getLength() > 0 ? namespaced : parent.getElementsByTagName(expectedName);
    }

    private static String localName(Element element) {
        String name = element.getLocalName();
        if (name == null) {
            name = element.getTagName();
        }
        int colon = name.lastIndexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String extractFileName(String path) {
        if (path == null) {
            return "";
        }
        int separator = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return separator >= 0 ? path.substring(separator + 1) : path;
    }

    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int count;
        while ((count = in.read(chunk)) > 0) {
            buffer.write(chunk, 0, count);
        }
        return buffer.toByteArray();
    }
}
