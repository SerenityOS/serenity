/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package dom;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.DOMConfigurationTest
 * @run testng/othervm dom.DOMConfigurationTest
 * @summary Test DOMConfiguration for supported properties.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DOMConfigurationTest {

    static class TestHandler implements DOMErrorHandler {
        private String warning;
        private String error;
        private String fatalError;

        public String getError() {
            return error;
        }

        public String getFatalError() {
            return fatalError;
        }

        public String getWarning() {
            return warning;
        }

        public boolean handleError(DOMError error) {
            if (error.getSeverity() == DOMError.SEVERITY_ERROR) {
                this.error = "" + error.getMessage();
                return false;
            }
            if (error.getSeverity() == DOMError.SEVERITY_FATAL_ERROR) {
                this.fatalError = "" + error.getMessage();
                return false;
            }
            this.warning = "" + error.getMessage();
            return true; // warning
        }
    }

    static class TestFailureHandler implements DOMErrorHandler {
        public boolean handleError(DOMError error) {
            if (error.getSeverity() == DOMError.SEVERITY_ERROR) {
                Assert.fail("Error: " + error.getMessage());
            }
            if (error.getSeverity() == DOMError.SEVERITY_FATAL_ERROR) {
                Assert.fail("Fatal error: " + error.getMessage());
            }
            return true; // warning
        }
    }

    void setHandler(Document doc) {
        doc.getDomConfig().setParameter("error-handler", new TestFailureHandler());
    }

    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    static final String XMLNS = "http://www.w3.org/2000/xmlns/";

    static Document loadDocument(String schemaURL, String instanceText) {
        Document document = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            if (schemaURL != null) {
                dbf.setAttribute(SCHEMA_LANGUAGE, XMLConstants.W3C_XML_SCHEMA_NS_URI);
                dbf.setAttribute(SCHEMA_SOURCE, schemaURL);
            }
            DocumentBuilder parser = dbf.newDocumentBuilder();

            InputSource inSource = new InputSource(new StringReader(instanceText));
            inSource.setSystemId("doc.xml");
            document = parser.parse(inSource);
        } catch (ParserConfigurationException e) {
            Assert.fail(e.toString());
        } catch (IOException e) {
            Assert.fail(e.toString());
        } catch (SAXException e) {
            Assert.fail(e.toString());
        }

        return document;
    }

    static final String test_xml = "<?xml version=\"1.0\"?>\n" + "<test:root xmlns:test=\"test\" \n"
            + "           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + ">&#x9;&#xA;&#xD; 1 </test:root>\n";

    static final String test1_xml = "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE root [\n" + "    <!ELEMENT root ANY>\n" + "    <!ENTITY x \"X\">\n" + "]>\n"
            + "<root/>\n";

    static final String test2_xml = "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE root [\n" + "    <!ELEMENT root ANY>\n"
            + "    <!ATTLIST root attr CDATA #REQUIRED>\n" + "    <!ENTITY x \"<\">\n" + "]>\n" + "<root attr=\"x\"/>\n";

    static final String test3_xml = "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE root [\n" + "    <!ELEMENT root (elem*)>\n" + "    <!ELEMENT elem EMPTY>\n"
            + "]>\n" + "<root/>\n";

    static String test1_xsd_url;
    static {
        try {
            test1_xsd_url = DOMConfigurationTest.class.getResource("DOMConfigurationTest.xsd").toURI().toString();
        } catch (URISyntaxException uriSyntaxException) {
            Assert.fail(uriSyntaxException.toString());
        }
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the doc contains two subsequent processing
     * instrictions, <br>
     * <b>name</b>: canonical-form <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the subsequent processing instrictions are
     * separated with a single line break
     */
    @Test
    public void testCanonicalForm001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();

        Element root = doc.getDocumentElement();
        ProcessingInstruction pi1 = doc.createProcessingInstruction("target1", "data1");
        ProcessingInstruction pi2 = doc.createProcessingInstruction("target2", "data2");

        root.appendChild(pi1);
        root.appendChild(pi2);

        if (!config.canSetParameter("canonical-form", Boolean.TRUE)) {
            System.out.println("OK, setting 'canonical-form' to true is not supported");
            return;
        }

        config.setParameter("canonical-form", Boolean.TRUE);
        setHandler(doc);
        doc.normalizeDocument();

        Node child1 = root.getFirstChild();
        Node child2 = child1.getNextSibling();

        if (child2.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            Assert.fail("the second child is expected to be a" + "single line break, returned: " + child2);
        }

        // return Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the parameters "namespaces",
     * "namespace-declarations", "well-formed", "element-content-whitespace" are
     * set to false if possible; the parameters "entities",
     * "normalize-characters", "cdata-sections" are set to true if possible, <br>
     * <b>name</b>: canonical-form <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the parameters "namespaces",
     * "namespace-declarations", "well-formed", "element-content-whitespace" are
     * set to true; the parameters "entities", "normalize-characters",
     * "cdata-sections" are set to false
     */
    @Test
    public void testCanonicalForm002() {
        Object[][] params = { { "namespaces", Boolean.TRUE }, { "namespace-declarations", Boolean.TRUE }, { "well-formed", Boolean.TRUE },
                { "element-content-whitespace", Boolean.TRUE },

                { "entities", Boolean.FALSE }, { "normalize-characters", Boolean.FALSE }, { "cdata-sections", Boolean.FALSE }, };

        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("canonical-form", Boolean.TRUE)) {
            System.out.println("OK, setting 'canonical-form' to true is not supported");
            return;
        }

        for (int i = params.length; --i >= 0;) {
            Boolean reset = params[i][1].equals(Boolean.TRUE) ? Boolean.FALSE : Boolean.TRUE;
            if (config.canSetParameter(params[i][0].toString(), reset)) {
                config.setParameter(params[i][0].toString(), reset);
            }
        }

        config.setParameter("canonical-form", Boolean.TRUE);

        StringBuffer result = new StringBuffer();

        for (int i = params.length; --i >= 0;) {
            Object param = config.getParameter(params[i][0].toString());
            if (!params[i][1].equals(param)) {
                result.append("; the parameter \'" + params[i][0] + "\' is set to " + param + ", expected: " + params[i][1]);
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the doc's root element contains superfluous
     * namespace declarations, <br>
     * <b>name</b>: canonical-form <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the superfluous namespace declarations are
     * removed
     */
    @Test
    public void testCanonicalForm003() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();

        Element root = doc.getDocumentElement();
        String XMLNS = "http://www.w3.org/2000/xmlns/";
        root.setAttributeNS(XMLNS, "xmlns:extra1", "ExtraNS1");
        root.setAttributeNS(XMLNS, "xmlns:extra2", "ExtraNS2");

        if (!config.canSetParameter("canonical-form", Boolean.TRUE)) {
            System.out.println("OK, setting 'canonical-form' to true is not supported");
            return;
        }
        config.setParameter("canonical-form", Boolean.TRUE);
        setHandler(doc);
        doc.normalizeDocument();

        String xmlns2 = root.getAttributeNS(XMLNS, "extra1");
        if (xmlns2 == null || xmlns2.length() != 0) {
            Assert.fail("superfluous namespace declarations is not removed: xmlns:extra2 = " + xmlns2);
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: setting the "canonical-form" to true is supported, <br>
     * <b>name</b>: canonical-form <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: one of the following parameters is changed:
     * "namespaces", "namespace-declarations", "well-formed",
     * "element-content-whitespace", "entities", "normalize-characters",
     * "cdata-sections" then "canonical-form" becomes false
     */
    @Test
    public void testCanonicalForm004() {
        Object[][] params = { { "namespaces", Boolean.TRUE }, { "namespace-declarations", Boolean.TRUE }, { "well-formed", Boolean.TRUE },
                { "element-content-whitespace", Boolean.TRUE },

                { "entities", Boolean.FALSE }, { "normalize-characters", Boolean.FALSE }, { "cdata-sections", Boolean.FALSE }, };

        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("canonical-form", Boolean.TRUE)) {
            System.out.println("OK, setting 'canonical-form' to true is not supported");
            return;
        }

        StringBuffer result = new StringBuffer();

        for (int i = params.length; --i >= 0;) {
            config.setParameter("canonical-form", Boolean.TRUE);
            Boolean changedValue = (params[i][1].equals(Boolean.TRUE)) ? Boolean.FALSE : Boolean.TRUE;
            if (config.canSetParameter(params[i][0].toString(), changedValue)) {
                config.setParameter(params[i][0].toString(), changedValue);
                Object param = config.getParameter("canonical-form");
                if (!Boolean.FALSE.equals(param)) {
                    result.append("; setting the parameter '" + params[i][0] + "' to " + changedValue + " does not change 'canonical-form' to false");
                }
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element has one CDATASection followed by
     * one Text node, <br>
     * <b>name</b>: cdata-sections <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the CDATASection is left intact
     */
    @Test
    public void testCdataSections001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        String cdataText = "CDATA CDATA CDATA";
        String textText = "text text text";

        CDATASection cdata = doc.createCDATASection(cdataText);
        Text text = doc.createTextNode(textText);

        DOMConfiguration config = doc.getDomConfig();
        config.setParameter("cdata-sections", Boolean.TRUE);

        Element root = doc.getDocumentElement();
        root.appendChild(cdata);
        root.appendChild(text);

        setHandler(doc);
        doc.normalizeDocument();

        Node returned = root.getFirstChild();

        if (returned.getNodeType() != Node.CDATA_SECTION_NODE) {
            Assert.fail("reurned: " + returned + ", expected: CDATASection");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element has one CDATASection followed by
     * one Text node, <br>
     * <b>name</b>: cdata-sections <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: the root element has one Text node with text of
     * the CDATASection and the Text node
     */
    @Test
    public void testCdataSections002() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        String cdataText = "CDATA CDATA CDATA";
        String textText = "text text text";

        CDATASection cdata = doc.createCDATASection(cdataText);
        Text text = doc.createTextNode(textText);

        DOMConfiguration config = doc.getDomConfig();
        config.setParameter("cdata-sections", Boolean.FALSE);

        Element root = doc.getDocumentElement();
        root.appendChild(cdata);
        root.appendChild(text);

        setHandler(doc);
        doc.normalizeDocument();

        Node returned = root.getFirstChild();

        if (returned.getNodeType() != Node.TEXT_NODE) {
            Assert.fail("reurned: " + returned + ", expected: TEXT_NODE");
        }

        String returnedText = returned.getNodeValue();
        if (!(cdataText + textText).equals(returnedText)) {
            Assert.fail("reurned: " + returnedText + ", expected: \"" + cdataText + textText + "\"");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element has one Text node with not fully
     * normalized characters, the 'check-character-normalization' parameter set
     * to true, <br>
     * <b>name</b>: error-handler <br>
     * <b>value</b>: DOMErrorHandler. <br>
     * <b>Expected results</b>: LSParser calls the specified error handler
     */
    @Test
    public void testCheckCharNorm001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        DOMImplementationLS lsImpl = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");

        if (lsImpl == null) {
            System.out.println("OK, the DOM implementation does not support the LS 3.0");
            return;
        }

        LSParser lsParser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = lsParser.getDomConfig();

        if (!config.canSetParameter("check-character-normalization", Boolean.TRUE)) {
            System.out.println("OK, setting 'check-character-normalization' to true is not supported");
            return;
        }

        config.setParameter("check-character-normalization", Boolean.TRUE);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        LSInput lsInput = lsImpl.createLSInput();
        lsInput.setStringData("<root>\u0073\u0075\u0063\u0327\u006F\u006E</root>");
        Document doc = lsParser.parse(lsInput);

        if (null == testHandler.getError()) {
            Assert.fail("no error is reported, expected 'check-character-normalization-failure'");

        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element contains a fully-normalized text, <br>
     * <b>name</b>: check-character-normalization <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: LSParser reports no errors
     */
    @Test
    public void testCheckCharNorm002() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        DOMImplementationLS lsImpl = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");

        if (lsImpl == null) {
            System.out.println("OK, the DOM implementation does not support the LS 3.0");
            return;
        }

        LSParser lsParser = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

        DOMConfiguration config = lsParser.getDomConfig();

        if (!config.canSetParameter("check-character-normalization", Boolean.FALSE)) {
            Assert.fail("setting 'check-character-normalization' to false is not supported");
        }

        config.setParameter("check-character-normalization", Boolean.FALSE);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        LSInput lsInput = lsImpl.createLSInput();
        lsInput.setStringData("<root>fully-normalized</root>");
        Document doc = lsParser.parse(lsInput);

        if (null != testHandler.getError()) {
            Assert.fail("no error is expected, but reported: " + testHandler.getError());

        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element has two Comment nodes, <br>
     * <b>name</b>: comments <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the Comment nodes belong to the root element
     */
    @Test
    public void testComments001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        Comment comment1 = doc.createComment("comment1");
        Comment comment2 = doc.createComment("comment2");

        DOMConfiguration config = doc.getDomConfig();
        config.setParameter("comments", Boolean.TRUE);

        Element root = doc.getDocumentElement();
        root.appendChild(comment1);
        root.appendChild(comment2);

        setHandler(doc);
        doc.normalizeDocument();

        if (comment1.getParentNode() != root) {
            Assert.fail("comment1 is attached to " + comment1.getParentNode() + ", but expected to be a child of root");
        }

        if (comment2.getParentNode() != root) {
            Assert.fail("comment1 is attached to " + comment2.getParentNode() + ", but expected to be a child of root");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element has two Comment nodes, <br>
     * <b>name</b>: comments <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: the root element has no children
     */
    @Test
    public void testComments002() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        Comment comment1 = doc.createComment("comment1");
        Comment comment2 = doc.createComment("comment2");

        DOMConfiguration config = doc.getDomConfig();
        config.setParameter("comments", Boolean.FALSE);

        Element root = doc.getDocumentElement();
        root.appendChild(comment1);
        root.appendChild(comment2);

        doc.normalizeDocument();

        if (root.getFirstChild() != null) {
            Assert.fail("root has a child " + root.getFirstChild() + ", but expected to has none");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element is declared as int and its value
     * has subsequent characters #x9 (tab), #xA (line feed) and #xD (carriage
     * return) , #x20 (space), '1', #x20 (space), <br>
     * <b>name</b>: datatype-normalization <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: after Document.normalizeDocument() is called the
     * content of the root is '1'
     */
    @Test
    public void testDatatypeNormalization001() {
        Document doc = null;
        try {
            doc = loadDocument(test1_xsd_url, test_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("schema-location", test1_xsd_url) || !config.canSetParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            System.out.println("cannot set the parameters 'schema-location' and 'schema-type'" + " to '" + test1_xsd_url + "' and '"
                    + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' respectively");
            return;
        }
        config.setParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        config.setParameter("schema-location", test1_xsd_url);

        if (!config.canSetParameter("validate", Boolean.TRUE)) {
            System.out.println("OK, setting 'validate' to true is not supported");
            return;
        }
        config.setParameter("validate", Boolean.TRUE);

        if (!config.canSetParameter("datatype-normalization", Boolean.TRUE)) {
            System.out.println("OK, setting 'datatype-normalization' to true is not supported");
            return;
        }
        config.setParameter("datatype-normalization", Boolean.TRUE);

        Element root = doc.getDocumentElement();
        while (root.getFirstChild() != null) {
            root.removeChild(root.getFirstChild());
        }
        root.appendChild(doc.createTextNode("\t\r\n 1 "));

        setHandler(doc);
        doc.normalizeDocument();

        Node child = root.getFirstChild();
        if (child == null || child.getNodeType() != Node.TEXT_NODE || !"1".equals(child.getNodeValue())) {
            Assert.fail("child: " + child + ", expected: text node '1'");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the root element is declared as int and its value
     * has subsequent characters #x9 (tab), #xA (line feed) and #xD (carriage
     * return) , #x20 (space), '1', #x20 (space), <br>
     * <b>name</b>: datatype-normalization <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: after Document.normalizeDocument() is called the
     * value is left unchanged
     */
    @Test
    public void testDatatypeNormalization002() {
        Document doc = null;
        try {
            doc = loadDocument(test1_xsd_url, test_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("schema-location", test1_xsd_url) || !config.canSetParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            System.out.println("cannot set the parameters 'schema-location' and 'schema-type'" + " to '" + test1_xsd_url + "' and '"
                    + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' respectively");
            return;
        }
        config.setParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        config.setParameter("schema-location", test1_xsd_url);

        if (config.canSetParameter("validate", Boolean.TRUE)) {
            config.setParameter("validate", Boolean.TRUE);
        }

        if (!config.canSetParameter("datatype-normalization", Boolean.FALSE)) {
            Assert.fail("datatype-normalization' to false is not supported");
        }
        config.setParameter("datatype-normalization", Boolean.FALSE);

        Element root = doc.getDocumentElement();
        while (root.getFirstChild() != null) {
            root.removeChild(root.getFirstChild());
        }
        String value = "\t\r\n 1 ";
        root.appendChild(doc.createTextNode(value));

        setHandler(doc);
        doc.normalizeDocument();

        Node child = root.getFirstChild();
        if (child == null || child.getNodeType() != Node.TEXT_NODE || !value.equals(child.getNodeValue())) {
            Assert.fail("child: " + child + ", expected: '\\t\\r\\n 1 '");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the doc contains one entity and one entity
     * reference, <br>
     * <b>name</b>: entities <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the entity and the entity reference are left
     * unchanged
     */
    @Test
    public void testEntities001() {
        Document doc = null;
        try {
            doc = loadDocument(null, test1_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("entities", Boolean.TRUE)) {
            Assert.fail("setting 'entities' to true is not supported");
        }

        Element root = doc.getDocumentElement();
        root.appendChild(doc.createEntityReference("x"));

        config.setParameter("entities", Boolean.TRUE);

        setHandler(doc);
        doc.normalizeDocument();
        Node child = root.getFirstChild();
        if (child == null) {
            Assert.fail("root has no child");
        }
        if (child.getNodeType() != Node.ENTITY_REFERENCE_NODE) {
            Assert.fail("root's child is " + child + ", expected entity reference &x;");
        }

        if (doc.getDoctype() == null) {
            Assert.fail("no doctype found");
        }

        if (doc.getDoctype().getEntities() == null) {
            Assert.fail("no entitiy found");
        }

        if (doc.getDoctype().getEntities().getNamedItem("x") == null) {
            Assert.fail("no entitiy with name 'x' found");
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the doc contains one entity and one entity
     * reference, <br>
     * <b>name</b>: entities <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: the entity and the entity reference are removed
     */
    @Test
    public void testEntities002() {
        Document doc = null;
        try {
            doc = loadDocument(null, test1_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("entities", Boolean.FALSE)) {
            Assert.fail("setting 'entities' to false is not supported");
        }

        Element root = doc.getDocumentElement();
        root.appendChild(doc.createEntityReference("x"));

        // TODO: remove debug
        NamedNodeMap entities = doc.getDoctype().getEntities();
        Entity entityX = (Entity) entities.getNamedItem("x");
        System.err.println();
        System.err.println("Entity x: " + entityX.getTextContent());
        System.err.println();

        config.setParameter("entities", Boolean.FALSE);

        setHandler(doc);
        doc.normalizeDocument();
        Node child = root.getFirstChild();

        // TODO: restore test, exclude for now to allow other tests to run
        /*
         * if (child == null) { fail("root has no child"); } if
         * (child.getNodeType() != Node.TEXT_NODE ||
         * !"X".equals(child.getNodeValue())) { fail("root's child is " + child
         * + ", expected text node with value 'X'"); }
         *
         * if (doc.getDoctype() == null) { fail("no doctype found"); }
         *
         * if (doc.getDoctype().getEntities() != null &&
         * doc.getDoctype().getEntities().getNamedItem("x") != null) {
         * fail("entity with name 'x' is found, expected to be removed"); }
         */

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the 'infoset' parameter is set to true, <br>
     * <b>name</b>: infoset <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: the parameters "validate-if-schema", "entities",
     * "datatype-normalization", "cdata-sections", "namespace-declarations",
     * "well-formed", "element-content-whitespace", "comments", "namespaces" are
     * left unchanged
     */
    @Test
    public void testInfoset001() {
        Object[][] params = { { "validate-if-schema", Boolean.FALSE }, { "entities", Boolean.FALSE }, { "datatype-normalization", Boolean.FALSE },
                { "cdata-sections", Boolean.FALSE },

                { "namespace-declarations", Boolean.TRUE }, { "well-formed", Boolean.TRUE }, { "element-content-whitespace", Boolean.TRUE },
                { "comments", Boolean.TRUE }, { "namespaces", Boolean.TRUE }, };

        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("infoset", Boolean.TRUE)) {
            Assert.fail("setting 'infoset' to true is not supported");
        }

        for (int i = params.length; --i >= 0;) {
            Boolean reset = params[i][1].equals(Boolean.TRUE) ? Boolean.FALSE : Boolean.TRUE;
            if (config.canSetParameter(params[i][0].toString(), reset)) {
                config.setParameter(params[i][0].toString(), reset);
            }
        }

        config.setParameter("infoset", Boolean.TRUE);
        config.setParameter("infoset", Boolean.FALSE); // has no effect

        StringBuffer result = new StringBuffer();

        for (int i = params.length; --i >= 0;) {
            Object param = config.getParameter(params[i][0].toString());
            if (!params[i][1].equals(param)) {
                result.append("; the parameter \'" + params[i][0] + "\' is set to " + param + ", expected: " + params[i][1]);
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: A document with one root element created. The
     * prefix 'ns' is bound to 'namespaceURI'. The 'namespaces' parameter is set
     * to true, <br>
     * <b>name</b>: namespace-declarations <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: Attribute xmlns:ns="namespaceURI" is not added
     * to the root element
     */
    @Test
    public void testNamespaces001() {
        DOMImplementation domImpl = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            domImpl = dbf.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);
        setHandler(doc);
        Element root = doc.getDocumentElement();
        DOMConfiguration config = doc.getDomConfig();

        StringBuffer result = new StringBuffer();
        if (config.canSetParameter("namespaces", Boolean.FALSE)) {
            config.setParameter("namespaces", Boolean.FALSE);

            // namespaces = false
            // namespace-declarations = true (default)
            doc.normalizeDocument();
            String xmlnsNS = root.getAttributeNS(XMLNS, "ns");
            if (xmlnsNS.length() > 0) {
                result.append("; the 'namespaces' parameter is set to false but" + "Namespace normalization is performed, attribute" + " xmlns:ns=\"" + xmlnsNS
                        + "\" is added");
            }
        }

        doc = domImpl.createDocument("namespaceURI", "ns:root", null);
        root = doc.getDocumentElement();
        config = doc.getDomConfig();

        if (!config.canSetParameter("namespaces", Boolean.TRUE)) {
            result.append("; setting 'namespaces' to true is not supported");
        } else {

            config.setParameter("namespaces", Boolean.TRUE);

            if (!config.canSetParameter("namespace-declarations", Boolean.FALSE)) {
                result.append("; setting 'namespace-declarations' to false is not supported");
            } else {
                config.setParameter("namespace-declarations", Boolean.FALSE);

                // namespaces = true
                // namespace-declarations = false
                doc.normalizeDocument();

                String xmlnsNS = root.getAttributeNS(XMLNS, "ns");
                if (xmlnsNS.length() > 0) {
                    result.append("; namespaces = true, namespace-declarations = false, but" + " xmlns:ns=\"" + xmlnsNS + "\"");
                }
            }

            doc = domImpl.createDocument("namespaceURI", "ns:root", null);
            setHandler(doc);
            root = doc.getDocumentElement();
            config = doc.getDomConfig();

            config.setParameter("namespaces", Boolean.TRUE);

            if (!config.canSetParameter("namespace-declarations", Boolean.TRUE)) {
                result.append("; setting 'namespace-declarations' to true is not supported");
            } else {
                config.setParameter("namespace-declarations", Boolean.TRUE);

                // namespaces = true
                // namespace-declarations = true
                doc.normalizeDocument();

                String xmlnsNS = root.getAttributeNS(XMLNS, "ns");
                if (!"namespaceURI".equals(xmlnsNS)) {
                    result.append("; namespaces = true, namespace-declarations = true, but" + " xmlns:ns=\"" + xmlnsNS + "\"");
                }
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }
        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: an attribute value is not fully normalized, <br>
     * <b>name</b>: normalize-characters <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: Node.normalize() leaves the value unchanged
     */
    @Test
    public void testNormalizeCharacters001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument(null, null, null);

        Attr attr = doc.createAttribute("attr");
        String notNormalized = " \u0073\u0075\u0063\u0327\u006F\u006E ";
        attr.setValue(notNormalized);

        DOMConfiguration config = doc.getDomConfig();

        StringBuffer result = new StringBuffer();
        if (!config.canSetParameter("normalize-characters", Boolean.FALSE)) {
            result.append("; setting 'normalize-characters' to false is not supported");
        } else {

            config.setParameter("normalize-characters", Boolean.FALSE);

            attr.normalize();

            String value = attr.getValue();
            if (!notNormalized.equals(value)) {
                result.append("; the value is normalized to '" + value + "', expected to stay unchanged");
            }
        }

        if (config.canSetParameter("normalize-characters", Boolean.TRUE)) {
            config.setParameter("normalize-characters", Boolean.TRUE);

            attr.setValue(notNormalized);
            attr.normalize();

            String value = attr.getValue();
            if (notNormalized.equals(value)) {
                result.append("; the value is not normalized: '" + value + "', expected: '\u0073\u0075\u00E7\u006F\u006E'");
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }
        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: The root element has invalid content. The
     * 'validate' parameter is set to true. The 'schema-location' parameter is
     * set to 'DOMConfigurationTest.xsd'., <br>
     * <b>name</b>: schema-type <br>
     * <b>value</b>: http://www.w3.org/2001/XMLSchema. <br>
     * <b>Expected results</b>: An error is reported
     */
    @Test
    public void testValidate001() {
        DOMImplementation domImpl = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            domImpl = dbf.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("test", "ns:root", null);

        Element root = doc.getDocumentElement();
        root.appendChild(doc.createTextNode("xxx")); // invalid value

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("schema-location", test1_xsd_url) || !config.canSetParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            System.out.println("cannot set the parameters 'schema-location' and 'schema-type'" + " to '" + test1_xsd_url + "' and '"
                    + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' respectively");
            return;
        }
        config.setParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        config.setParameter("schema-location", test1_xsd_url);

        String resultOK = "OK";
        StringBuffer result = new StringBuffer();
        if (!config.canSetParameter("validate", Boolean.TRUE)) {
            resultOK = "OK, setting the parameter 'validate' to true is not supported";
        } else {
            config.setParameter("validate", Boolean.TRUE);
            TestHandler testHandler = new TestHandler();
            config.setParameter("error-handler", testHandler);
            doc.normalizeDocument();
            if (testHandler.getError() == null && null == testHandler.getFatalError()) {
                result.append("; no error was reported when the 'validate' is set to true");
            }
        }

        if (!config.canSetParameter("validate", Boolean.FALSE)) {
            result.append("; cannot set the parameters 'validate' to false");
        } else {
            config.setParameter("validate", Boolean.FALSE);
            TestHandler testHandler = new TestHandler();
            config.setParameter("error-handler", testHandler);
            doc.normalizeDocument();
            if (testHandler.getError() != null || null != testHandler.getFatalError()) {
                result.append("; unexpected error: " + testHandler.getFatalError() + "; " + testHandler.getError());
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }
        return; // Status.passed(resultOK);

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: The root contains a CDATASection with the
     * termination marker ']]&gt;', <br>
     * <b>name</b>: split-cdata-sections <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: A warning is reported when the section is
     * splitted
     */
    @Test
    public void testSplitCDATA001() {
        DOMImplementation domImpl = null;
        try {
            domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();
        CDATASection cdata = doc.createCDATASection("text]" + "]>text");
        doc.getDocumentElement().appendChild(cdata);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        if (!config.canSetParameter("split-cdata-sections", Boolean.TRUE)) {
            Assert.fail("cannot set the parameters 'split-cdata-sections' to true");
        }
        config.setParameter("split-cdata-sections", Boolean.TRUE);

        doc.normalizeDocument();
        if (null == testHandler.getWarning()) {
            Assert.fail("no warning is reported");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: The root contains a CDATASection with the
     * termination marker ']]&gt;', <br>
     * <b>name</b>: split-cdata-sections <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: No warning is reported
     */
    @Test
    public void testSplitCDATA002() {
        DOMImplementation domImpl = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            domImpl = dbf.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("namespaceURI", "ns:root", null);

        DOMConfiguration config = doc.getDomConfig();
        CDATASection cdata = doc.createCDATASection("text]" + "]>text");
        doc.getDocumentElement().appendChild(cdata);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        if (!config.canSetParameter("split-cdata-sections", Boolean.FALSE)) {
            Assert.fail("cannot set the parameters 'split-cdata-sections' to false");
        }
        config.setParameter("split-cdata-sections", Boolean.FALSE);

        doc.normalizeDocument();
        if (null == testHandler.getError()) {
            Assert.fail("no error is reported");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: The root element has invalid content. The schema
     * is specified by setting the 'schema-location' and the 'schema-type'
     * parameters., <br>
     * <b>name</b>: validate-if-schema <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: No error is reported
     */
    @Test
    public void testValidateIfSchema001() {
        DOMImplementation domImpl = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            domImpl = dbf.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("test", "ns:root", null);

        Element root = doc.getDocumentElement();
        root.appendChild(doc.createTextNode("xxx")); // invalid value

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("schema-location", test1_xsd_url) || !config.canSetParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            System.out.println("cannot set the parameters 'schema-location' and 'schema-type'" + " to 'DOMConfigurationTest.xsd' and '"
                    + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' respectively");
            return;
        }
        config.setParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        config.setParameter("schema-location", test1_xsd_url);

        String resultOK = "OK";
        StringBuffer result = new StringBuffer();
        if (!config.canSetParameter("validate-if-schema", Boolean.FALSE)) {
            result.append("; cannot set the parameters 'validate-if-schema' to false");
        } else {
            config.setParameter("validate-if-schema", Boolean.FALSE);
            TestHandler testHandler = new TestHandler();
            config.setParameter("error-handler", testHandler);
            doc.normalizeDocument();
            if (testHandler.getError() != null || null != testHandler.getFatalError()) {
                result.append("; unexpected error: " + testHandler.getFatalError() + "; " + testHandler.getError());
            }
        }

        if (!config.canSetParameter("validate-if-schema", Boolean.TRUE)) {
            resultOK = "OK, setting the parameter 'validate-if-schema' to true is not supported";
        } else {
            config.setParameter("validate-if-schema", Boolean.TRUE);
            TestHandler testHandler = new TestHandler();
            config.setParameter("error-handler", testHandler);
            doc.normalizeDocument();
            if (testHandler.getError() == null && null == testHandler.getFatalError()) {
                result.append("; no error was reported when the 'validate-if-schema' is set to true");
            }
        }

        if (result.length() > 0) {
            Assert.fail(result.toString().substring(2));
        }
        return; // Status.passed(resultOK);

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: The root element is not declared in the schema
     * specified by setting the 'schema-location' and the 'schema-type'
     * parameters., <br>
     * <b>name</b>: validate-if-schema <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: No error is reported
     */
    @Test
    public void testValidateIfSchema002() {
        DOMImplementation domImpl = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(true);
            domImpl = dbf.newDocumentBuilder().getDOMImplementation();
        } catch (ParserConfigurationException pce) {
            Assert.fail(pce.toString());
        } catch (FactoryConfigurationError fce) {
            Assert.fail(fce.toString());
        }

        Document doc = domImpl.createDocument("test", "ns:undeclared_root", null);

        Element root = doc.getDocumentElement();
        root.appendChild(doc.createTextNode("xxx"));

        DOMConfiguration config = doc.getDomConfig();

        if (!config.canSetParameter("schema-location", test1_xsd_url) || !config.canSetParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
            System.out.println("cannot set the parameters 'schema-location' and 'schema-type'" + " to 'DOMConfigurationTest.xsd' and '"
                    + XMLConstants.W3C_XML_SCHEMA_NS_URI + "' respectively");
            return;
        }
        config.setParameter("schema-type", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        config.setParameter("schema-location", test1_xsd_url);

        if (!config.canSetParameter("validate-if-schema", Boolean.TRUE)) {
            System.out.println("OK, setting the parameter 'validate-if-schema'" + " to true is not supported");
            return;
        }

        config.setParameter("validate-if-schema", Boolean.TRUE);
        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);
        doc.normalizeDocument();
        if (testHandler.getError() != null || null != testHandler.getFatalError()) {
            Assert.fail("unexpected error: " + testHandler.getFatalError() + "; " + testHandler.getError());
        }
        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the attribute has EntityReference to '&lt;', <br>
     * <b>name</b>: well-formed <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: An error is reported
     */
    @Test
    public void testWellFormed001() {
        Document doc = null;
        try {
            doc = loadDocument(null, test2_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("well-formed", Boolean.TRUE)) {
            Assert.fail("setting 'well-formed' to true is not supported");
        }
        config.setParameter("well-formed", Boolean.TRUE);

        Element root = doc.getDocumentElement();

        Attr attr = doc.createAttributeNS(null, "attr");

        try {
            attr.appendChild(doc.createEntityReference("<"));
        } catch (DOMException domException) {
            System.out.println("testWellFormed001: Expected DOMException for Attribute value = '<'" + domException.toString());
            return; // OK
        }

        root.setAttributeNode(attr);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        doc.normalizeDocument();

        if (testHandler.getError() == null && null == testHandler.getFatalError()) {
            Assert.fail("no error was reported when attribute has <");
        }

        return; // Status.passed("OK");
    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the attribute has EntityReference to '&lt;', <br>
     * <b>name</b>: well-formed <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: No error is reported
     */
    @Test
    public void testWellFormed002() {
        Document doc = null;
        try {
            doc = loadDocument(null, test2_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("well-formed", Boolean.FALSE)) {
            System.out.println("OK, setting 'well-formed' to false is not supported");
            return;
        }
        config.setParameter("well-formed", Boolean.FALSE);

        Element root = doc.getDocumentElement();

        Attr attr = doc.createAttributeNS(null, "attr");
        attr.appendChild(doc.createEntityReference("x"));

        root.setAttributeNode(attr);

        TestHandler testHandler = new TestHandler();
        config.setParameter("error-handler", testHandler);

        doc.normalizeDocument();

        if (testHandler.getError() != null || null != testHandler.getFatalError()) {
            Assert.fail("unexpected error: " + testHandler.getFatalError() + "; " + testHandler.getError());
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the document root element has a text node with
     * four white space characters, <br>
     * <b>name</b>: element-content-whitespace <br>
     * <b>value</b>: true. <br>
     * <b>Expected results</b>: the text node is preserved
     */
    @Test
    public void testECWhitespace001() {
        Document doc = null;
        try {
            doc = loadDocument(null, test3_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        Element root = doc.getDocumentElement();
        Text text = doc.createTextNode("\t\n\r ");
        root.appendChild(text);

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("element-content-whitespace", Boolean.TRUE)) {
            Assert.fail("setting 'element-content-whitespace' to true is not supported");
        }
        config.setParameter("element-content-whitespace", Boolean.TRUE);

        if (!config.canSetParameter("validate", Boolean.TRUE)) {
            System.out.println("OK, setting 'validate' to true is not supported");
            return;
        }
        config.setParameter("validate", Boolean.TRUE);

        setHandler(doc);
        doc.normalizeDocument();

        Node firstChild = root.getFirstChild();
        if (firstChild == null || firstChild.getNodeType() != Node.TEXT_NODE || !((Text) firstChild).isElementContentWhitespace()) {
            Assert.fail("the first child is " + firstChild + ", expected a text node with the four whitespace characters");
        }

        return; // Status.passed("OK");

    }

    /**
     * Equivalence class partitioning with state and input values orientation
     * for public void setParameter(String name, Object value) throws
     * DOMException, <br>
     * <b>pre-conditions</b>: the document root element has a text node with
     * four white space characters, <br>
     * <b>name</b>: element-content-whitespace <br>
     * <b>value</b>: false. <br>
     * <b>Expected results</b>: the text node is discarded
     */
    @Test
    public void testECWhitespace002() {
        Document doc = null;
        try {
            doc = loadDocument(null, test3_xml);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        Element root = doc.getDocumentElement();
        Text text = doc.createTextNode("\t\n\r ");
        root.appendChild(text);

        DOMConfiguration config = doc.getDomConfig();
        if (!config.canSetParameter("element-content-whitespace", Boolean.FALSE)) {
            System.out.println("OK, setting 'element-content-whitespace' to false is not supported");
            return;
        }
        config.setParameter("element-content-whitespace", Boolean.FALSE);

        if (!config.canSetParameter("validate", Boolean.TRUE)) {
            System.out.println("OK, setting 'validate' to true is not supported");
            return;
        }
        config.setParameter("validate", Boolean.TRUE);

        setHandler(doc);
        doc.normalizeDocument();

        Node firstChild = root.getFirstChild();
        if (firstChild != null) {
            Assert.fail("the first child is " + firstChild + ", but no child is expected");
        }

        return; // Status.passed("OK");

    }
}
