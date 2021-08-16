/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

import transform.util.TransformerTestTemplate;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.TransformerTest
 * @run testng/othervm transform.TransformerTest
 * @summary Transformer Tests
 * @bug 6272879 6305029 6505031 8150704 8162598 8169112 8169631 8169772
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TransformerTest {

    // some global constants
    private static final String LINE_SEPARATOR =
        getSystemProperty("line.separator");

    private static final String NAMESPACES =
        "http://xml.org/sax/features/namespaces";

    private static final String NAMESPACE_PREFIXES =
        "http://xml.org/sax/features/namespace-prefixes";

    public static class Test6272879 extends TransformerTestTemplate {

        private static String XSL_INPUT =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + LINE_SEPARATOR +
            "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" + LINE_SEPARATOR +
            "<xsl:output method=\"xml\" indent=\"no\" encoding=\"ISO-8859-1\"/>" + LINE_SEPARATOR +
            "<xsl:template match=\"/\">" + LINE_SEPARATOR +
            "<xsl:element name=\"TransformateurXML\">" + LINE_SEPARATOR +
            "  <xsl:for-each select=\"XMLUtils/test\">" + LINE_SEPARATOR +
            "  <xsl:element name=\"test2\">" + LINE_SEPARATOR +
            "    <xsl:element name=\"valeur2\">" + LINE_SEPARATOR +
            "      <xsl:attribute name=\"attribut2\">" + LINE_SEPARATOR +
            "        <xsl:value-of select=\"valeur/@attribut\"/>" + LINE_SEPARATOR +
            "      </xsl:attribute>" + LINE_SEPARATOR +
            "      <xsl:value-of select=\"valeur\"/>" + LINE_SEPARATOR +
            "    </xsl:element>" + LINE_SEPARATOR +
            "  </xsl:element>" + LINE_SEPARATOR +
            "  </xsl:for-each>" + LINE_SEPARATOR +
            "</xsl:element>" + LINE_SEPARATOR +
            "</xsl:template>" + LINE_SEPARATOR +
            "</xsl:stylesheet>";

        private static String XML_INPUT =
            "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + LINE_SEPARATOR +
            // "<!DOCTYPE XMLUtils [" + LINE_SEPARATOR +
            // "<!ELEMENT XMLUtils (test*)>" + LINE_SEPARATOR +
            // "<!ELEMENT test (valeur*)>" + LINE_SEPARATOR +
            // "<!ELEMENT valeur (#PCDATA)>" + LINE_SEPARATOR +
            // "<!ATTLIST valeur attribut CDATA #REQUIRED>]>" +
            // LINE_SEPARATOR +
            "<XMLUtils>" + LINE_SEPARATOR +
            "  <test>" + LINE_SEPARATOR +
            "    <valeur attribut=\"Attribut 1\">Valeur 1</valeur>" + LINE_SEPARATOR +
            "  </test>" + LINE_SEPARATOR +
            "  <test>" + LINE_SEPARATOR +
            "    <valeur attribut=\"Attribut 2\">Valeur 2</valeur>" + LINE_SEPARATOR +
            "  </test>" + LINE_SEPARATOR +
            "</XMLUtils>";

        public Test6272879() {
            super(XSL_INPUT, XML_INPUT);
        }

        /*
         * @bug 6272879
         * @summary Test for JDK-6272879
         *          DomResult had truncated Strings in some places
         */
        @Test
        public void run() throws TransformerException, ClassNotFoundException, InstantiationException,
            IllegalAccessException, ClassCastException
        {
            // print input
            printSnippet("Stylesheet:", getXsl());
            printSnippet("Source before transformation:", getSourceXml());

            // transform to DOM result
            Transformer t = getTransformer();
            DOMResult result = new DOMResult();
            t.transform(getStreamSource(), result);

            // print output
            printSnippet("Result after transformation:", prettyPrintDOMResult(result));

            // do some assertions
            Document document = (Document)result.getNode();
            NodeList nodes = document.getElementsByTagName("valeur2");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                AssertJUnit.assertEquals("Node value mismatch",
                                         "Valeur " + (i + 1),
                                         node.getFirstChild().getNodeValue());
                AssertJUnit.assertEquals("Node attribute mismatch",
                                         "Attribut " + (i + 1),
                                         node.getAttributes().item(0).getNodeValue());
            }
        }
    }

    public static class Test6305029 extends TransformerTestTemplate {

        private static String XML_INPUT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<prefix:localName xmlns:prefix=\"namespaceUri\"/>";

        // custom XMLReader representing XML_INPUT
        private class MyXMLReader implements XMLReader {
            private boolean namespaces = true;
            private boolean namespacePrefixes = false;
            private EntityResolver resolver;
            private DTDHandler dtdHandler;
            private ContentHandler contentHandler;
            private ErrorHandler errorHandler;

            public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
                if (name.equals(NAMESPACES)) {
                    return namespaces;
                } else if (name.equals(NAMESPACE_PREFIXES)) {
                    return namespacePrefixes;
                } else {
                    throw new SAXNotRecognizedException();
                }
            }

            public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
                if (name.equals(NAMESPACES)) {
                    namespaces = value;
                } else if (name.equals(NAMESPACE_PREFIXES)) {
                    namespacePrefixes = value;
                } else {
                    throw new SAXNotRecognizedException();
                }
            }

            public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
                return null;
            }

            public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
            }

            public void setEntityResolver(final EntityResolver theResolver) {
                this.resolver = theResolver;
            }

            public EntityResolver getEntityResolver() {
                return resolver;
            }

            public void setDTDHandler(final DTDHandler theHandler) {
                dtdHandler = theHandler;
            }

            public DTDHandler getDTDHandler() {
                return dtdHandler;
            }

            public void setContentHandler(final ContentHandler handler) {
                contentHandler = handler;
            }

            public ContentHandler getContentHandler() {
                return contentHandler;
            }

            public void setErrorHandler(final ErrorHandler handler) {
                errorHandler = handler;
            }

            public ErrorHandler getErrorHandler() {
                return errorHandler;
            }

            public void parse(final InputSource input) throws IOException, SAXException {
                parse();
            }

            public void parse(final String systemId) throws IOException, SAXException {
                parse();
            }

            private void parse() throws SAXException {
                contentHandler.startDocument();
                contentHandler.startPrefixMapping("prefix", "namespaceUri");

                AttributesImpl atts = new AttributesImpl();
                if (namespacePrefixes) {
                    atts.addAttribute("", "xmlns:prefix", "xmlns:prefix", "CDATA", "namespaceUri");
                }

                contentHandler.startElement("namespaceUri", "localName", namespacePrefixes ? "prefix:localName" : "", atts);
                contentHandler.endElement("namespaceUri", "localName", namespacePrefixes ? "prefix:localName" : "");
                contentHandler.endPrefixMapping("prefix");
                contentHandler.endDocument();
            }
        }

        public Test6305029() {
            super(null, XML_INPUT);
        }

        /*
         * @bug 6305029
         * @summary Test for JDK-6305029
         *          Test identity transformation
         */
        @Test
        public void run() throws TransformerFactoryConfigurationError, TransformerException {
            // get Identity transformer
            Transformer t = getTransformer();

            // test SAXSource from custom XMLReader
            SAXSource saxSource = new SAXSource(new MyXMLReader(), new InputSource());
            StringWriter resultWriter = new StringWriter();
            t.transform(saxSource, new StreamResult(resultWriter));
            String resultString = resultWriter.toString();
            printSnippet("Result after transformation from custom SAXSource:", resultString);
            AssertJUnit.assertEquals("Identity transform of SAXSource", getSourceXml(), resultString);

            // test StreamSource
            printSnippet("Source before transformation of StreamSource:", getSourceXml());
            resultWriter = new StringWriter();
            t.transform(getStreamSource(), new StreamResult(resultWriter));
            resultString = resultWriter.toString();
            printSnippet("Result after transformation of StreamSource:", resultString);
            AssertJUnit.assertEquals("Identity transform of StreamSource", getSourceXml(), resultString);
        }
    }

    public static class Test6505031 extends TransformerTestTemplate {

        public Test6505031() throws IOException {
            super();
            setXsl(fromInputStream(getClass().getResourceAsStream("transform.xsl")));
            setSourceXml(fromInputStream(getClass().getResourceAsStream("template.xml")));
        }

        /*
         * @bug 6505031
         * @summary Test transformer parses keys and their values coming from different xml documents.
         */
        @Test
        public void run() throws TransformerFactoryConfigurationError, TransformerException {
            Transformer t = getTransformer();
            t.setParameter("config", getClass().getResource("config.xml").toString());
            t.setParameter("mapsFile", getClass().getResource("maps.xml").toString());
            StringWriter resultWriter = new StringWriter();
            t.transform(getStreamSource(), new StreamResult(resultWriter));
            String resultString = resultWriter.toString();
            Assert.assertTrue(resultString.contains("map1key1value") && resultString.contains("map2key1value"));
        }
    }

    public static class Test8169631 extends TransformerTestTemplate {

        private static String XSL_INPUT =
            "<?xml version=\"1.0\"?>" + LINE_SEPARATOR +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" + LINE_SEPARATOR +
            "  <xsl:template match=\"/\">" + LINE_SEPARATOR +
            "    <xsl:variable name=\"Counter\" select=\"count(//row)\"/>" + LINE_SEPARATOR +
            "    <xsl:variable name=\"AttribCounter\" select=\"count(//@attrib)\"/>" + LINE_SEPARATOR +
            "    <Counter><xsl:value-of select=\"$Counter\"/></Counter>" + LINE_SEPARATOR +
            "    <AttribCounter><xsl:value-of select=\"$AttribCounter\"/></AttribCounter>" + LINE_SEPARATOR +
            "  </xsl:template>" + LINE_SEPARATOR +
            "</xsl:stylesheet>" + LINE_SEPARATOR;

        private static String XML_INPUT =
            "<?xml version=\"1.0\"?>" + LINE_SEPARATOR +
            "<envelope xmlns=\"http://www.sap.com/myns\" xmlns:sap=\"http://www.sap.com/myns\">" + LINE_SEPARATOR +
            "  <sap:row sap:attrib=\"a\">1</sap:row>" + LINE_SEPARATOR +
            "  <row attrib=\"b\">2</row>" + LINE_SEPARATOR +
            "  <row sap:attrib=\"c\">3</row>" + LINE_SEPARATOR +
            "</envelope>" + LINE_SEPARATOR;

        public Test8169631() {
            super(XSL_INPUT, XML_INPUT);
        }

        /**
         * Utility method to print out transformation result and check values.
         *
         * @param type
         * Text describing type of transformation
         * @param result
         * Resulting output of transformation
         * @param elementCount
         * Counter of elements to check
         * @param attribCount
         * Counter of attributes to check
         */
        private void verifyResult(String type, String result, int elementCount,
                                  int attribCount)
        {
            printSnippet("Result of transformation from " + type + ":",
                         result);
            Assert.assertEquals(
                result.contains("<Counter>" + elementCount + "</Counter>"),
                true, "Result of transformation from " + type +
                " should have count of " + elementCount + " elements.");
            Assert.assertEquals(
                result.contains("<AttribCounter>" + attribCount +
                "</AttribCounter>"), true, "Result of transformation from " +
                type + " should have count of "+ attribCount + " attributes.");
        }

        @DataProvider(name = "testdata8169631")
        public Object[][] testData()
            throws TransformerConfigurationException, SAXException, IOException,
            ParserConfigurationException, XMLStreamException
        {
            // get Transformers
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = getTransformer(tf);
            Transformer tFromTemplates = getTemplates(tf).newTransformer();

            // get DOMSource objects
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DOMSource domSourceWithoutNS = getDOMSource(dbf);
            dbf.setNamespaceAware(true);
            DOMSource domSourceWithNS = getDOMSource(dbf);

            // get SAXSource objects
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXSource saxSourceWithoutNS = getSAXSource(spf);
            spf.setNamespaceAware(true);
            SAXSource saxSourceWithNS = getSAXSource(spf);

            // get StAXSource objects
            XMLInputFactory xif = XMLInputFactory.newInstance();
            StAXSource staxSourceWithNS = getStAXSource(xif);

            // print XML/XSL snippets to ease understanding of result
            printSnippet("Source:", getSourceXml());
            printSnippet("Stylesheet:", getXsl());

            return new Object[][] {
                // test StreamSource input with all transformers
                // namespace awareness is set by transformer
                {t, getStreamSource(), "StreamSource with namespace support", 0, 1},
                {tFromTemplates, getStreamSource(), "StreamSource with namespace support using templates", 0, 1},
                // now test DOMSource, SAXSource and StAXSource
                // with rotating use of created transformers
                // namespace awareness is set by source objects
                {t, domSourceWithNS, "DOMSource with namespace support", 0, 1},
                {t, domSourceWithoutNS, "DOMSource without namespace support", 3, 3},
                {tFromTemplates, saxSourceWithNS, "SAXSource with namespace support", 0, 1},
                {tFromTemplates, saxSourceWithoutNS, "SAXSource without namespace support", 3, 3},
                {t, staxSourceWithNS, "StAXSource with namespace support", 0, 1}
            };
        }

        /*
         * @bug 8169631
         * @summary Test combinations of namespace awareness settings on
         *          XSL transformations
         */
        @Test(dataProvider = "testdata8169631")
        public void run(Transformer t, Source s, String label, int elementcount, int attributecount)
            throws TransformerException
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.transform(s, new StreamResult(baos));
            verifyResult(label, baos.toString(), elementcount, attributecount);
        }
    }

    public static class Test8150704 extends TransformerTestTemplate {

        public Test8150704() {
            super();
        }

        @DataProvider(name = "testdata8150704")
        public Object[][] testData() {
            return new Object[][] {
                {"Bug8150704-1.xsl", "Bug8150704-1.xml", "Bug8150704-1.ref"},
                {"Bug8150704-2.xsl", "Bug8150704-2.xml", "Bug8150704-2.ref"}
            };
        }

        /*
         * @bug 8150704
         * @summary Test that XSL transformation with lots of temporary result
         *          trees will not run out of DTM IDs.
         */
        @Test(dataProvider = "testdata8150704")
        public void run(String xsl, String xml, String ref) throws IOException, TransformerException {
            System.out.println("Testing transformation of " + xml + "...");
            setXsl(fromInputStream(getClass().getResourceAsStream(xsl)));
            setSourceXml(fromInputStream(getClass().getResourceAsStream(xml)));
            Transformer t = getTransformer();
            StringWriter resultWriter = new StringWriter();
            t.transform(getStreamSource(), new StreamResult(resultWriter));
            String resultString = resultWriter.toString().replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n").trim();
            String reference = fromInputStream(getClass().getResourceAsStream(ref)).trim();
            Assert.assertEquals(resultString, reference, "Output of transformation of " + xml + " does not match reference");
            System.out.println("Passed.");
        }
    }

    public static class Test8162598 extends TransformerTestTemplate {

        private static String XSL_INPUT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + LINE_SEPARATOR +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" + LINE_SEPARATOR +
            "    <xsl:template match=\"/\">" + LINE_SEPARATOR +
            "        <root xmlns=\"ns1\">" + LINE_SEPARATOR +
            "            <xsl:call-template name=\"transform\"/>" + LINE_SEPARATOR +
            "        </root>" + LINE_SEPARATOR +
            "    </xsl:template>" + LINE_SEPARATOR +
            "    <xsl:template name=\"transform\">" + LINE_SEPARATOR +
            "        <test1 xmlns=\"ns2\"><b xmlns=\"ns2\"><c xmlns=\"\"></c></b></test1>" + LINE_SEPARATOR +
            "        <test2 xmlns=\"ns1\"><b xmlns=\"ns2\"><c xmlns=\"\"></c></b></test2>" + LINE_SEPARATOR +
            "        <test3><b><c xmlns=\"\"></c></b></test3>" + LINE_SEPARATOR +
            "        <test4 xmlns=\"\"><b><c xmlns=\"\"></c></b></test4>" + LINE_SEPARATOR +
            "        <test5 xmlns=\"ns1\"><b><c xmlns=\"\"></c></b></test5>" + LINE_SEPARATOR +
            "        <test6 xmlns=\"\"/>" + LINE_SEPARATOR +
            "    </xsl:template>" + LINE_SEPARATOR +
            "</xsl:stylesheet>";

        private static String XML_INPUT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aaa></aaa>" + LINE_SEPARATOR;

        public Test8162598() {
            super(XSL_INPUT, XML_INPUT);
        }

        /**
         * Utility method for testBug8162598().
         * Provides a convenient way to check/assert the expected namespaces
         * of a Node and its siblings.
         *
         * @param test
         * The node to check
         * @param nstest
         * Expected namespace of the node
         * @param nsb
         * Expected namespace of the first sibling
         * @param nsc
         * Expected namespace of the first sibling of the first sibling
         */
        private void checkNodeNS(Node test, String nstest, String nsb, String nsc) {
            String testNodeName = test.getNodeName();
            if (nstest == null) {
                Assert.assertNull(test.getNamespaceURI(), "unexpected namespace for " + testNodeName);
            } else {
                Assert.assertEquals(test.getNamespaceURI(), nstest, "unexpected namespace for " + testNodeName);
            }
            Node b = test.getChildNodes().item(0);
            if (nsb == null) {
                Assert.assertNull(b.getNamespaceURI(), "unexpected namespace for " + testNodeName + "->b");
            } else {
                Assert.assertEquals(b.getNamespaceURI(), nsb, "unexpected namespace for " + testNodeName + "->b");
            }
            Node c = b.getChildNodes().item(0);
            if (nsc == null) {
                Assert.assertNull(c.getNamespaceURI(), "unexpected namespace for " + testNodeName + "->b->c");
            } else {
                Assert.assertEquals(c.getNamespaceURI(), nsc, "unexpected namespace for " + testNodeName + "->b->c");
            }
        }

        /*
         * @bug 8162598
         * @summary Test XSLTC handling of namespaces, especially empty namespace
         *          definitions to reset the default namespace
         */
        @Test
        public void run()  throws Exception {
            // print input
            printSnippet("Source:", getSourceXml());
            printSnippet("Stylesheet:", getXsl());

            // transform to DOM result
            Transformer t = getTransformer();
            DOMResult result = new DOMResult();
            t.transform(getStreamSource(), result);

            // print output
            printSnippet("Result after transformation:", prettyPrintDOMResult(result));

            // do some verifications
            Document document = (Document)result.getNode();
            checkNodeNS(document.getElementsByTagName("test1").item(0), "ns2", "ns2", null);
            checkNodeNS(document.getElementsByTagName("test2").item(0), "ns1", "ns2", null);
            checkNodeNS(document.getElementsByTagName("test3").item(0), null, null, null);
            checkNodeNS(document.getElementsByTagName("test4").item(0), null, null, null);
            checkNodeNS(document.getElementsByTagName("test5").item(0), "ns1", "ns1", null);
            Assert.assertNull(document.getElementsByTagName("test6").item(0).getNamespaceURI(),
                "unexpected namespace for test6");
        }
    }

    public static class Test8169112 extends TransformerTestTemplate{

        public static String XML_INPUT =
            "<?xml version=\"1.0\"?><DOCROOT/>";

        public Test8169112() throws IOException {
            super();
            setXsl(fromInputStream(getClass().getResourceAsStream("Bug8169112.xsl")));
            setSourceXml(XML_INPUT);
        }

        /**
         * @throws TransformerException
         * @bug 8169112
         * @summary Test compilation of large xsl file with outlining.
         *
         * This test merely compiles a large xsl file and tests if its bytecode
         * passes verification by invoking the transform() method for
         * dummy content. The test succeeds if no Exception is thrown
         */
        @Test
        public void run() throws TransformerException {
            Transformer t = getTransformer();
            t.transform(getStreamSource(), new StreamResult(new ByteArrayOutputStream()));
        }
    }

    public static class Test8169772 extends TransformerTestTemplate {

        public Test8169772() {
            super();
        }

        private Document getDOMWithBadElement() throws SAXException, IOException, ParserConfigurationException {
            // create a small DOM
            Document doc = DocumentBuilderFactory.newInstance().
                newDocumentBuilder().parse(
                    new ByteArrayInputStream(
                        "<?xml version=\"1.0\"?><DOCROOT/>".getBytes()
                    )
                );

            // insert a bad element
            Element e = doc.createElement("ERROR");
            e.appendChild(doc.createTextNode(null));
            doc.getDocumentElement().appendChild(e);

            return doc;
        }

        /**
         * @throws ParserConfigurationException
         * @throws IOException
         * @throws SAXException
         * @throws TransformerException
         * @bug 8169772
         * @summary Test transformation of DOM with null valued text node
         *
         * This test would throw a NullPointerException during transform when the
         * fix was not present.
         */
        @Test
        public void run() throws SAXException, IOException, ParserConfigurationException, TransformerException {
            Transformer t = getTransformer();
            StringWriter resultWriter = new StringWriter();
            DOMSource d = new DOMSource(getDOMWithBadElement().getDocumentElement());
            t.transform(d, new StreamResult(resultWriter));
            printSnippet("Transformation result (DOM with null text node):", resultWriter.toString());
        }
    }
}
