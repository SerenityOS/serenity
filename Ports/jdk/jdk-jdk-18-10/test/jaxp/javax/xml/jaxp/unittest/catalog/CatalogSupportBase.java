/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package catalog;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogResolver;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

/**
 * Base class:
 * Initialized xml/xsd/xsl used for the test;
 * Handler classes
 *
 * @author huizhe.wang@oracle.com
 */
public class CatalogSupportBase {
    // the System Property for the USE_CATALOG feature
    final static String SP_USE_CATALOG = "javax.xml.useCatalog";
    final static String SP_ACCESS_EXTERNAL_DTD = "javax.xml.accessExternalDTD";

    boolean debug = false;

    String filepath;
    String slash = "";

    protected void setUp() {
        String file1 = getClass().getResource("CatalogSupport.xml").getFile();
        if (getSystemProperty("os.name").contains("Windows")) {
            filepath = file1.substring(1, file1.lastIndexOf("/") + 1);
            slash = "/";
        } else {
            filepath = file1.substring(0, file1.lastIndexOf("/") + 1);
        }

        initFiles();
    }

    String xml_catalog, xml_bogus_catalog;

    // For tests using system.xml
    String xml_system, dtd_system, dtd_systemResolved;
    final String elementInSystem = "catalogtest";
    final String expectedWCatalog = "Test system entry";
    final String expectedWResolver = "Test resolved by an EntityHandler, rather than a Catalog entry";

    // For tests using XInclude.xml
    String xml_xInclude, xml_xIncludeSimple;
    final String elementInXISimple = "blue";
    final String contentInXIutf8 = "trjsagdkasgdhasdgashgdhsadgashdg";
    final String contentInUIutf8Catalog = "usingCatalog";

    // For the xsd import and include
    String xsd_xmlSchema, dtd_xmlSchema, dtd_datatypes;
    String xsd_xmlSchema_import, xsd_xml;
    String xml_val_test, xml_val_test_id, xsd_val_test;
    String xsd_include_company, xsd_include_person, xsd_include_product;
    String xsl_include, xsl_includeDTD, xsl_import_html, xsl_include_header, xsl_include_footer;

    // For the xsl import and include
    String xml_xsl, xml_xslDTD;

    // For document function
    String xml_doc, xsl_doc, xml_doc2;

    void initFiles() {
        xml_system = filepath + "system.xml";
        dtd_system = filepath + "system.dtd";
        dtd_systemResolved = "<!ENTITY system \"resolved by an EntityHandler, rather than a Catalog\">";

        xml_catalog = Paths.get(filepath + "CatalogSupport.xml").toUri().toASCIIString();
        xml_bogus_catalog = Paths.get(filepath + "CatalogSupport_bogus.xml").toUri().toASCIIString();

        xml_xInclude = "<?xml version=\"1.0\"?>\n" +
            "<xinclude:include xmlns:xinclude=\"http://www.w3.org/2001/XInclude\"\n" +
            "  href=\"XI_simple.xml\"/>\n";
        xml_xIncludeSimple = filepath + "XI_simple.xml";

        xsd_xmlSchema = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE xs:schema PUBLIC \"-//W3C//DTD XMLSCHEMA 200102//EN\" \"pathto/XMLSchema.dtd\" >"
                + "<xs:schema targetNamespace=\"http://www.w3.org/2001/XMLSchema\" blockDefault=\"#all\" "
                + "           elementFormDefault=\"qualified\" version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                + "           xml:lang=\"EN\" xmlns:hfp=\"http://www.w3.org/2001/XMLSchema-hasFacetAndProperty\">"
                + " <xs:annotation>"
                + "  <xs:documentation>"
                + "    Part 1 version: Id: structures.xsd,v 1.2 2004/01/15 11:34:25 ht Exp "
                + "    Part 2 version: Id: datatypes.xsd,v 1.3 2004/01/23 18:11:13 ht Exp "
                + "  </xs:documentation>"
                + " </xs:annotation>"
                + "</xs:schema>";
        dtd_xmlSchema = filepath + "XMLSchema.dtd";
        dtd_datatypes = filepath + "datatypes.dtd";

        xsd_xmlSchema_import = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>"
                + "<xs:schema targetNamespace=\"http://www.w3.org/2001/XMLSchema\" "
                + "blockDefault=\"#all\" elementFormDefault=\"qualified\" version=\"1.0\" "
                + "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xml:lang=\"EN\" "
                + "xmlns:hfp=\"http://www.w3.org/2001/XMLSchema-hasFacetAndProperty\">"
                + " <xs:annotation>"
                + "  <xs:documentation>"
                + "    Part 1 version: Id: structures.xsd,v 1.2 2004/01/15 11:34:25 ht Exp "
                + "    Part 2 version: Id: datatypes.xsd,v 1.3 2004/01/23 18:11:13 ht Exp "
                + "  </xs:documentation>"
                + " </xs:annotation>"
                + ""
                + " <xs:import namespace=\"http://www.w3.org/XML/1998/namespace\" "
                + "schemaLocation=\"http://www.w3.org/2001/pathto/xml.xsd\">"
                + "   <xs:annotation>"
                + "     <xs:documentation>"
                + "       Get access to the xml: attribute groups for xml:lang"
                + "       as declared on 'schema' and 'documentation' below"
                + "     </xs:documentation>"
                + "   </xs:annotation>"
                + " </xs:import>"
                + " <xs:element name=\"schema\" id=\"schema\">"
                + "  <xs:complexType>"
                + "   <xs:simpleContent>"
                + "    <xs:extension base=\"xs:integer\">"
                + "     <xs:attribute ref=\"xml:lang\"/>"
                + "    </xs:extension>"
                + "   </xs:simpleContent>"
                + "  </xs:complexType>"
                + " </xs:element>"
                + "</xs:schema>";

        xsd_xml = filepath + "xml.xsd";

        xsd_include_company = "<?xml version=\"1.1\"?>"
                + "<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + "            targetNamespace=\"http://www.company.org\""
                + "            xmlns=\"http://www.company.org\""
                + "            elementFormDefault=\"qualified\">"
                + "    <xsd:include schemaLocation=\"pathto/XSDInclude_person.xsd\"/>"
                + "    <xsd:include schemaLocation=\"pathto/XSDInclude_product.xsd\"/>"
                + "    <xsd:element name=\"Company\">"
                + "        <xsd:complexType>"
                + "            <xsd:sequence>"
                + "                <xsd:element name=\"Person\" type=\"PersonType\""
                + "                             maxOccurs=\"unbounded\"/>"
                + "                <xsd:element name=\"Product\" type=\"ProductType\""
                + "                             maxOccurs=\"unbounded\"/>"
                + "            </xsd:sequence>"
                + "        </xsd:complexType>"
                + "    </xsd:element>"
                + "</xsd:schema>";
        xsd_include_person = filepath + "XSDInclude_person.xsd";
        xsd_include_product = filepath + "XSDInclude_product.xsd";

        xsl_include = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
                + "  <xsl:import href=\"pathto/XSLImport_html.xsl\"/>"
                + "  <xsl:include href=\"pathto/XSLInclude_header.xsl\"/>"
                + "  "
                + ""
                + "  <xsl:template match=\"content/title\">"
                + "   <h1><xsl:apply-templates/></h1>"
                + "  </xsl:template>"
                + "  "
                + "  <xsl:include href=\"pathto/XSLInclude_footer.xsl\"/>"
                + ""
                + "</xsl:stylesheet>";
        xsl_includeDTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<!DOCTYPE HTMLlat1 SYSTEM \"http://openjdk_java_net/xml/catalog/dtd/XSLDTD.dtd\">"
                + "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
                + "  <xsl:import href=\"pathto/XSLImport_html.xsl\"/>"
                + "  <xsl:include href=\"pathto/XSLInclude_header.xsl\"/>"
                + "  "
                + ""
                + "  <xsl:template match=\"content/title\">"
                + "   <h1><xsl:apply-templates/></h1>"
                + "  </xsl:template>"
                + "  "
                + "  <xsl:include href=\"pathto/XSLInclude_footer.xsl\"/>"
                + ""
                + "</xsl:stylesheet>";

        xsl_import_html = filepath + "XSLImport_html.xsl";
        xsl_include_header = filepath + "XSLInclude_header.xsl";
        xsl_include_footer = filepath + "XSLInclude_footer.xsl";

        xml_val_test = filepath + "/val_test.xml";
        xml_val_test_id = "file://" + slash + xml_val_test;
        xsd_val_test = filepath + "/val_test.xsd";

        xml_xsl = "<?xml version=\"1.0\"?>\n" +
                "<content>\n" +
                "    <header>This is the header</header>\n" +
                "    Some content\n" +
                "    <footer>footer</footer>\n" +
                "</content>";

        xml_xslDTD = "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE content SYSTEM \"http://openjdk_java_net/xml/catalog/dtd/include.dtd\">" +
                "<content>\n" +
                "    <header>This is the header</header>\n" +
                "    Some content\n" +
                "    <footer>footer</footer>\n" +
                "</content>";

        xml_doc = filepath + "/DocFunc.xml";
        xsl_doc = filepath + "/DocFunc.xsl";
        xml_doc2 = filepath + "/DocFunc2.xml";
    }


    /*
       Verifies the Catalog support on SAXParser.
    */
    public void testSAX(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        SAXParser parser = getSAXParser(setUseCatalog, useCatalog, catalog);

        parser.parse(xml, handler);
        Assert.assertEquals(handler.getResult().trim(), expected);
    }

    /*
       Verifies the Catalog support on XMLReader.
    */
    public void testXMLReader(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        XMLReader reader = getXMLReader(setUseCatalog, useCatalog, catalog);

        reader.setContentHandler(handler);
        reader.setEntityResolver(handler);
        reader.parse(xml);
        Assert.assertEquals(handler.getResult().trim(), expected);
    }

    /*
       Verifies the Catalog support on XInclude.
    */
    public void testXInclude(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        SAXParser parser = getSAXParser(setUseCatalog, useCatalog, catalog);

        parser.parse(new InputSource(new StringReader(xml)), handler);
        debugPrint("handler.result:" + handler.getResult());
        Assert.assertEquals(handler.getResult().trim(), expected);
    }

    /*
       Verifies the Catalog support on DOM parser.
    */
    public void testDOM(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        DocumentBuilder docBuilder = getDomBuilder(setUseCatalog, useCatalog, catalog);
        docBuilder.setEntityResolver(handler);
        Document doc = docBuilder.parse(xml);

        Node node = doc.getElementsByTagName(elementInSystem).item(0);
        String result = node.getFirstChild().getTextContent();
        Assert.assertEquals(result.trim(), expected);
    }

    /*
       Verifies the Catalog support on StAX parser.
    */
    public void testStAX(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, XMLResolver resolver, String expected) throws Exception {

            XMLStreamReader streamReader = getStreamReader(
                    setUseCatalog, useCatalog, catalog, xml, resolver);
            String text = getText(streamReader, XMLStreamConstants.CHARACTERS);
            Assert.assertEquals(text.trim(), expected);
    }

    /*
       Verifies that the Catalog support for StAX parser is disabled when
       USE_CATALOG == false.
    */
    public void testStAXNegative(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, XMLResolver resolver, String expected) throws Exception {

            XMLStreamReader streamReader = getStreamReader(
                    setUseCatalog, useCatalog, catalog, xml, resolver);
            String text = getText(streamReader, XMLStreamConstants.ENTITY_REFERENCE);
            Assert.assertEquals(text.trim(), expected);
    }

    /*
       Verifies the Catalog support on resolving DTD, xsd import and include in
    Schema files.
    */
    public void testValidation(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xsd, LSResourceResolver resolver)
            throws Exception {

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        // use resolver or catalog if resolver = null
        if (resolver != null) {
            factory.setResourceResolver(resolver);
        }
        if (setUseCatalog) {
            factory.setFeature(XMLConstants.USE_CATALOG, useCatalog);
        }
        factory.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);

        Schema schema = factory.newSchema(new StreamSource(new StringReader(xsd)));
        success("XMLSchema.dtd and datatypes.dtd are resolved.");
    }

    /**
     * Verifies Catalog Support for the Validator.
     * @param setUseCatalog1 a flag to indicate whether USE_CATALOG shall be set
     * on the factory.
     * @param setUseCatalog2 a flag to indicate whether USE_CATALOG shall be set
     * on the Validator.
     * @param source  the XML source
     * @param resolver1 a resolver to be set on the factory if specified
     * @param resolver2 a resolver to be set on the Validator if specified
     * @param catalog1 a catalog to be set on the factory if specified
     * @param catalog2 a catalog to be set on the Validator if specified
     */
    public void testValidator(boolean setUseCatalog1, boolean setUseCatalog2, boolean useCatalog,
            Source source, LSResourceResolver resolver1, LSResourceResolver resolver2,
            String catalog1, String catalog2)
            throws Exception {

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            if (setUseCatalog1) {
                schemaFactory.setFeature(XMLConstants.USE_CATALOG, useCatalog);
            }
            if (catalog1 != null) {
                schemaFactory.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog1);
            }
            if (resolver1 != null) {
                schemaFactory.setResourceResolver(resolver1);
            }

            Schema schema = schemaFactory.newSchema();
            Validator validator = schema.newValidator();
            if (setUseCatalog2) {
                validator.setFeature(XMLConstants.USE_CATALOG, useCatalog);
            }
            if (catalog2 != null) {
                validator.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog2);
            }
            if (resolver2 != null) {
                validator.setResourceResolver(resolver2);
            }
            validator.validate(source);
    }

    /*
       Verifies the Catalog support on resolving DTD, xsl import and include in
    XSL files.
    */
    public void testXSLImport(boolean setUseCatalog, boolean useCatalog, String catalog,
            SAXSource xsl, StreamSource xml,
        URIResolver resolver, String expected) throws Exception {

        TransformerFactory factory = getTransformerFactory(setUseCatalog, useCatalog, catalog, resolver);
        Transformer transformer = factory.newTransformer(xsl);

        StringWriter out = new StringWriter();
        transformer.transform(xml, new StreamResult(out));
        debugPrint("out:\n" + out.toString());
        Assert.assertTrue(out.toString().contains(expected), "testXSLImport");
    }

    /*
       Verifies the Catalog support on resolving DTD, xsl import and include in
    XSL files.
    */
    public void testXSLImportWTemplates(boolean setUseCatalog, boolean useCatalog,
            String catalog, SAXSource xsl, StreamSource xml,
        URIResolver resolver, String expected) throws Exception {

        TransformerFactory factory = getTransformerFactory(setUseCatalog, useCatalog, catalog, resolver);
        Transformer transformer = factory.newTemplates(xsl).newTransformer();
        StringWriter out = new StringWriter();
        transformer.transform(xml, new StreamResult(out));
        Assert.assertTrue(out.toString().contains(expected), "testXSLImportWTemplates");
    }

    /**
     * Returns an instance of SAXParser with a catalog if one is provided.
     *
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @return an instance of SAXParser
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    SAXParser getSAXParser(boolean setUseCatalog, boolean useCatalog, String catalog)
            throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        spf.setXIncludeAware(true);
        if (setUseCatalog) {
            spf.setFeature(XMLConstants.USE_CATALOG, useCatalog);
        }

        SAXParser parser = spf.newSAXParser();
        parser.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
        return parser;
    }

    /**
     * Returns an instance of XMLReader with a catalog if one is provided.
     *
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @return an instance of XMLReader
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    XMLReader getXMLReader(boolean setUseCatalog, boolean useCatalog, String catalog)
            throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        XMLReader reader = spf.newSAXParser().getXMLReader();
        if (setUseCatalog) {
            reader.setFeature(XMLConstants.USE_CATALOG, useCatalog);
        }
        reader.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
        return reader;
    }

    /**
     * Returns an instance of DocumentBuilder that may have set a Catalog.
     *
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @return an instance of DocumentBuilder
     * @throws ParserConfigurationException
     */
    DocumentBuilder getDomBuilder(boolean setUseCatalog, boolean useCatalog, String catalog)
            throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        if (setUseCatalog) {
            dbf.setFeature(XMLConstants.USE_CATALOG, useCatalog);
        }
        dbf.setAttribute(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        return docBuilder;
    }

    /**
     * Creates a DOMSource.
     *
     * @param uri the URI to the XML source file
     * @param systemId the systemId of the source
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @return a DOMSource
     * @throws Exception
     */
    DOMSource getDOMSource(String uri, String systemId, boolean setUseCatalog,
            boolean useCatalog, String catalog) {
        DOMSource ds = null;
        try {
            DocumentBuilder builder = getDomBuilder(setUseCatalog, useCatalog, catalog);
            Document doc = builder.parse(new File(uri));
            ds = new DOMSource(doc, systemId);
        } catch (Exception e) {}

        return ds;
    }

    /**
     * Creates a StAXSource.
     *
     * @param xmlFile the XML source file
     * @param xmlFileId the systemId of the source
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @return a StAXSource
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    StAXSource getStaxSource(String xmlFile, String xmlFileId, boolean setUseCatalog,
            boolean useCatalog, String catalog) {
        StAXSource ss = null;
        try {
            XMLInputFactory xif = XMLInputFactory.newFactory();
            if (setUseCatalog) {
                xif.setProperty(XMLConstants.USE_CATALOG, useCatalog);
            }
            xif.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
            ss = new StAXSource(xif.createXMLEventReader(
                        xmlFileId, new FileInputStream(xmlFile)));
        } catch (Exception e) {}

        return ss;
    }

    /**
     * Creates an XMLStreamReader.
     *
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog the path to a catalog
     * @param xml the xml to be parsed
     * @param resolver a resolver to be set on the reader
     * @return an instance of the XMLStreamReader
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    XMLStreamReader getStreamReader(boolean setUseCatalog, boolean useCatalog,
            String catalog, String xml, XMLResolver resolver)
            throws FileNotFoundException, XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        if (catalog != null) {
            factory.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
        }

        factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);

        if (resolver != null) {
            factory.setProperty(XMLInputFactory.RESOLVER, resolver);
        }

        if (setUseCatalog) {
            factory.setProperty(XMLConstants.USE_CATALOG, useCatalog);
        }

        InputStream entityxml = new FileInputStream(xml);
        XMLStreamReader streamReader = factory.createXMLStreamReader(xml, entityxml);
        return streamReader;
    }

    /**
     * Returns the accumulated text of an event type.
     *
     * @param streamReader the XMLStreamReader
     * @param type the type of event requested
     * @return the text of the accumulated text for the request type
     * @throws XMLStreamException
     */
    String getText(XMLStreamReader streamReader, int type) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        StringBuilder entityRef = new StringBuilder();

        while(streamReader.hasNext()){
            int eventType = streamReader.next();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    text.append(streamReader.getText());
                    break;
                case XMLStreamConstants.ENTITY_REFERENCE:
                    entityRef.append(streamReader.getText());
                    break;
            }
        }
        if (type == XMLStreamConstants.CHARACTERS) {
            return text.toString();
        } else {
            return entityRef.toString();
        }
    }

    /**
     * Returns an instance of TransformerFactory with either a custom URIResolver
     * or Catalog.
     *
     * @param setUseCatalog a flag indicates whether USE_CATALOG shall be set
     * through the factory
     * @param useCatalog the value of USE_CATALOG
     * @param catalog a catalog
     * @param resolver a custom resolver
     * @return an instance of TransformerFactory
     * @throws Exception
     */
    TransformerFactory getTransformerFactory(boolean setUseCatalog, boolean useCatalog,
            String catalog, URIResolver resolver)
            throws Exception {

        TransformerFactory factory = TransformerFactory.newInstance();
        if (setUseCatalog) {
            factory.setFeature(XMLConstants.USE_CATALOG, useCatalog);
        }
        if (catalog != null) {
            factory.setAttribute(CatalogFeatures.Feature.FILES.getPropertyName(), catalog);
        }

        // use resolver or catalog if resolver = null
        if (resolver != null) {
            factory.setURIResolver(resolver);
        }

        return factory;
    }

    void fail(String msg) {
        System.out.println("Test failed:");
        System.out.println(msg);
    }

    void success(String msg) {
        System.out.println("Test succeded:");
        System.out.println(msg);
    }

    void debugPrint(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    /**
     * Extends MyStaxResolver to override resolveEntity
     */
    class MyStaxEntityResolver implements XMLResolver {

        public MyStaxEntityResolver() {

        }

        public Object resolveEntity(String publicId, String systemId, String baseURI,
                String namespace)
                throws javax.xml.stream.XMLStreamException {
            try {
                return new ByteArrayInputStream(
                        "<!ENTITY system \"resolved by an EntityHandler, rather than a Catalog\">".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException ex) {
                return null;
            }
        }

    }

    /**
     * A custom XMLResolver
     */
    class MyStaxResolver implements XMLResolver {

        public MyStaxResolver() {
        }

        public Object resolveEntity(String publicId, String systemId, String baseURI,
                String namespace) throws javax.xml.stream.XMLStreamException {
            return null;
        }

    }


    /**
     * Extends MyHandler and overrides resolveEntity with a CatalogResolver
     */
    class MyCatalogHandler extends MyHandler {
        CatalogResolver cr;

        public MyCatalogHandler(CatalogResolver cr, String elementName) {
            super(elementName);
            this.cr = cr;
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return cr.resolveEntity(publicId, systemId);
        }
        @Override
        public InputSource resolveEntity(String name, String publicId,
                String baseURI, String systemId) {
            return cr.resolveEntity(publicId, systemId);
        }
    }

    /**
     * Extends MyHandler and overrides resolveEntity
     */
    class MyEntityHandler extends MyHandler {
        String[] systemIds;
        InputSource[] returnValues;
        public MyEntityHandler(String[] systemIds, InputSource[] returnValues, String elementName) {
            super(elementName);
            this.systemIds = systemIds;
            this.returnValues = returnValues;
        }

        @Override
        public InputSource resolveEntity(String name, String publicId,
                String baseURI, String systemId) {
            for (int i = 0; i < systemIds.length; i++) {
                if (systemId.endsWith(systemIds[i])) {
                    return returnValues[i];
                }
            }

            return null;
        }
    }

    /**
     * SAX handler
     */
    public class MyHandler extends DefaultHandler2 implements ErrorHandler {

        String elementName, currentElementName, result;
        StringBuilder textContent = new StringBuilder();

        /**
         *
         * @param elementName the name of the element from which the content
         * is to be captured
         */
        MyHandler(String elementName) {
            textContent.setLength(0);
            this.elementName = elementName;
        }

        String getResult() {
            return result.trim();
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            currentElementName = localName;
            textContent.delete(0, textContent.length());
            try {
                debugPrint("Element: " + uri + ":" + localName + " " + qName);
            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            debugPrint("Text: " + textContent.toString() + "");
            debugPrint("End Element: " + uri + ":" + localName + " " + qName);
            if (currentElementName.equals(elementName)) {
                result = textContent.toString();
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (currentElementName.equals(elementName)) {
                textContent.append(ch, start, length);
            }
        }

        public void internalEntityDecl(String name, String value) throws SAXException {
            super.internalEntityDecl(name, value);
            debugPrint("internalEntityDecl() is invoked for entity : " + name);
        }

        public void externalEntityDecl(String name, String publicId, String systemId)
                throws SAXException {
            super.externalEntityDecl(name, publicId, systemId);
            debugPrint("externalEntityDecl() is invoked for entity : " + name);
        }

        public void startEntity(String name) throws SAXException {
            super.startEntity(name);
//              debugPrint("startEntity() is invoked for entity : " + name) ;
        }

        public void endEntity(String name) throws SAXException {
            super.endEntity(name);
//              debugPrint("endEntity() is invoked for entity : " + name) ;
        }

        public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
            debugPrint("resolveEntity(publicId, systemId) is invoked");
            return super.resolveEntity(publicId, systemId);
        }

        /**
         * public InputSource resolveEntity(String name, String publicId, String
         * baseURI, String systemId) throws SAXException, IOException {
         * System.out.println("resolveEntity(name, publicId, baseURI, systemId)
         * is invoked"); return super.resolveEntity(name, publicId, baseURI,
         * systemId); }
         */
        public InputSource getExternalSubset(String name, String baseURI)
                throws SAXException, IOException {
            debugPrint("getExternalSubset() is invoked");
            return super.getExternalSubset(name, baseURI);
        }
    }

    /**
     * The purpose of this class, vs an anonymous class, is to show clearly what
     * we're testing by passing the parameters to the constructor.
     */
    class SourceResolver implements LSResourceResolver {

        String publicId;
        String[] systemIds;
        XmlInput[] returnValues;

        public SourceResolver(String publicId, String[] systemIds, XmlInput[] returnValues) {
            this.publicId = publicId;
            this.systemIds = systemIds;
            this.returnValues = returnValues;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                String systemId, String baseURI) {
            for (int i = 0; i < systemIds.length; i++) {
                if (systemId.endsWith(systemIds[i])) {
                    return returnValues[i];
                }
            }

            return null;
        }
    }

    class XmlInput implements LSInput {

        private InputStream inputStream;
        private String systemId;
        private String baseUri;

        public XmlInput(InputStream inputStream, String systemId, String baseUri) {
            this.inputStream = inputStream;
            this.systemId = systemId;
            this.baseUri = baseUri;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
        }

        @Override
        public InputStream getByteStream() {
            return inputStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            this.inputStream = byteStream;
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public void setPublicId(String publicId) {
        }

        @Override
        public String getBaseURI() {
            return baseUri;
        }

        @Override
        public void setBaseURI(String baseURI) {
            this.baseUri = baseURI;
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
        }
    }

    class XslResolver implements URIResolver {

        String[] hrefs;
        Source[] returnValues;

        public XslResolver(String[] href, Source[] returnValues) {
            this.hrefs = href;
            this.returnValues = returnValues;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            for (int i = 0; i < hrefs.length; i++) {
                if (href.endsWith(hrefs[i])) {
                    return returnValues[i];
                }
            }
            return null;
        }
    }

    /**
     * Simple policy implementation that grants a set of permissions to all code
     * sources and protection domains.
     */
    static class SimplePolicy extends Policy {

        private final Permissions perms;

        public SimplePolicy(Permission... permissions) {
            perms = new Permissions();
            for (Permission permission : permissions) {
                perms.add(permission);
            }
        }

        @Override
        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }

        @Override
        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p);
        }
    }
}
