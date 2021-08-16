/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package common.prettyprint;

import java.io.ByteArrayInputStream;
import static jaxp.library.JAXPTestUtilities.clearSystemProperty;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/*
 * @test
 * @bug 6439439 8087303 8174025 8223291 8249867 8261209 8260858 8265073
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.prettyprint.PrettyPrintTest
 * @run testng/othervm common.prettyprint.PrettyPrintTest
 * @summary Test serializing xml and html with indentation.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PrettyPrintTest {

    private static final String DOM_FORMAT_PRETTY_PRINT = "format-pretty-print";
    private static final String JDK_IS_STANDALONE
            = "http://www.oracle.com/xml/jaxp/properties/isStandalone";
    private static final String SP_JDK_IS_STANDALONE = "jdk.xml.isStandalone";
    private static final String XSLTC_IS_STANDALONE
            = "http://www.oracle.com/xml/jaxp/properties/xsltcIsStandalone";
    private static final String SP_XSLTC_IS_STANDALONE = "jdk.xml.xsltcIsStandalone";

    // pretty-print=true, isStandalone=true, linebreak added after header
    private static final String XML_LB
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sometag/>\n";
    // pretty-print=true, isStandalone=false, no linebreak after header
    private static final String XML_PPTRUE_NOLB
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><sometag/>\n";
    // pretty-print=false, isStandalone=true, linebreak added after header
    private static final String XML_PPFALSE_LB
            = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<sometag/>";

    private static final String XSL = "<?xml version=\"1.0\"?>\n"
            + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
            + "\n"
            + "<!--Identity template, copies all content into the output -->\n"
            + "    <xsl:template match=\"@*|node()\">\n"
            + "        <xsl:copy>\n"
            + "            <xsl:apply-templates select=\"@*|node()\"/>\n"
            + "        </xsl:copy>\n"
            + "    </xsl:template>\n"
            + "\n"
            + "</xsl:stylesheet>";

    /*
     * test CDATA, elements only, text and element, xml:space property, mixed
     * node types.
     */
    @DataProvider(name = "xml-data")
    public Object[][] xmlData() throws Exception {
        return new Object[][]{
            {"xmltest1.xml", "xmltest1.out"},
            {"xmltest2.xml", "xmltest2.out"},
            {"xmltest3.xml", "xmltest3.out"},
            {"xmltest4.xml", "xmltest4.out"},
            {"xmltest6.xml", "xmltest6.out"},
            {"xmltest8.xml", "xmltest8.out"}};
    }

    /*
     * Bug: 8249867
     * DataProvider: for testing the isStandalone property
     * Data columns: pretty-print, property, system property, value, expected result
     */
    @DataProvider(name = "setting")
    Object[][] getData() throws Exception {
        return new Object[][]{
            // pretty-print = true
            {true, false, true, true, XML_LB}, //set System property = true

            {true, false, true, false, XML_PPTRUE_NOLB}, //set System property = false
            {true, true, false, true, XML_LB}, //set property = true
            {true, true, false, false, XML_PPTRUE_NOLB}, //set property = false
            {true, false, false, false, XML_PPTRUE_NOLB},//default

            // pretty-print = false
            {false, false, true, true, XML_PPFALSE_LB}, //System property = true
            {false, true, false, true, XML_PPFALSE_LB} //set property = true
        };
    }

    /*
     * Bug: 8249867 8260858
     * DataProvider: for testing the xsltcIsStandalone property
     * Data columns: xsl, pretty-print, property, system property, value, expected result
     */
    @DataProvider(name = "dataWithTemplate")
    Object[][] getDataWTemplate() throws Exception {
        return new Object[][]{
            // pretty-print = true
            {XSL, true, false, true, true, XML_LB}, //set System property = true

            {XSL, true, false, true, false, XML_PPTRUE_NOLB}, //set System property = false
            {XSL, true, true, false, true, XML_LB}, //set property = true
            {XSL, true, true, false, false, XML_PPTRUE_NOLB}, //set property = false
            {XSL, true, false, false, false, XML_PPTRUE_NOLB},//default

            // pretty-print = false
            {XSL, false, false, true, true, XML_PPFALSE_LB}, //System property = true
            {XSL, false, true, false, true, XML_PPFALSE_LB} //set property = true
        };
    }

    /*
     * Bug: 8249867
     * DataProvider: for verifying the System property
     * Data columns: value for system property, expected result
     */
    @DataProvider(name = "SP")
    Object[][] getSystemProperty() throws Exception {
        return new Object[][]{
            // the system property is true only if it is "true" and false otherwise
            {"true", true},
            {"false", false},
            {"yes", false},
            {"", false}
        };
    }

    private final String xml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<a><b>element b</b><c>element c</c><d xml:space=\"preserve\">TRUE</d><e>test</e></a>";
    private final String expected1 = "<a>\n"
            + "    <b>element b</b>\n"
            + "    <c>element c</c>\n"
            + "    <d xml:space=\"preserve\">TRUE</d>\n"
            + "    <e>test</e>\n"
            + "</a>\n";

    private final String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<l0><l1><l2 xml:space=\"preserve\">level 2</l2> level 1 <l2>level 2</l2></l1>"
            + "<l1 xml:space=\"preserve\"><l2>level 2</l2> level 1 <l2>level 2</l2></l1></l0>";
    private final String expected2 = "<l0>\n"
            + "    <l1>\n"
            + "        <l2 xml:space=\"preserve\">level 2</l2>\n"
            + "         level 1 \n"
            + "        <l2>level 2</l2>\n"
            + "    </l1>\n"
            + "    <l1 xml:space=\"preserve\"><l2>level 2</l2> level 1 <l2>level 2</l2></l1>\n"
            + "</l0>\n";

    /*
     * Bug: 8265073
     * source and expected output
     */
    @DataProvider
    public Object[][] preserveSpace() {
        return new Object[][]{
            {xml1, expected1},
            {xml2, expected2},
        };
    }

    /**
     * Bug: 8265073
     * Verifies that the scope of the preserve attribute is applied properly
     * within the relevant elements.
     * @param xml the source
     * @param expected the expected result
     * @throws Exception if the assertion fails or an error occurs in the
     * transform process
     */
    @Test(dataProvider = "preserveSpace")
    public void test(String xml, String expected) throws Exception {
        String result = transform(null, xml, true, true, false, false, false);
        Assert.assertEquals(result.replaceAll("\r\n", "\n"), expected);
    }

    /*
     * Bug: 8260858
     * Verifies the use of the new property "xsltcIsStandalone" and the
     * corresponding System property "jdk.xml.xsltcIsStandalone".
     */
    @Test(dataProvider = "setting")
    public void testIsStandalone_XSLTC(boolean pretty, boolean p, boolean sp,
            boolean val, String expected)
            throws Exception {
        String result = transform(null, expected, false, pretty, p, sp, val);
        Assert.assertEquals(result.replaceAll("\r\n", "\n"), expected);
    }

    /*
     * Bug: 8260858
     * Samiliar to testIsStandalone_XSLTC, except that the transformer is created
     * from a template.
     */
    @Test(dataProvider = "dataWithTemplate")
    public void testIsStandalone_Template(String xsl, boolean pretty, boolean p,
            boolean sp, boolean val, String expected)
            throws Exception {
        String result = transform(xsl, expected, false, pretty, p, sp, val);
        Assert.assertEquals(result.replaceAll("\r\n", "\n"), expected);
    }

    /*
     * Bug: 8249867
     * Verifies the use of the new property "isStandalone" and the
     * corresponding System property "jdk.xml.isStandalone".
     *
     * Bug: 8261209
     * Verifies that the property takes effect regardless of the settings of
     * property "pretty-print".
     */
    @Test(dataProvider = "setting")
    public void testIsStandalone_DOMLS(boolean pretty, boolean p, boolean sp,
            boolean val, String expected)
            throws Exception {
        if (sp) {
            setSystemProperty(SP_JDK_IS_STANDALONE, Boolean.toString(val));
        }
        Document document = getDocument();
        DOMImplementationLS impl = (DOMImplementationLS) document.getImplementation();
        LSSerializer ser = impl.createLSSerializer();
        DOMConfiguration config = ser.getDomConfig();
        if (pretty) {
            config.setParameter("format-pretty-print", true);
        }
        if (p && !sp) {
            config.setParameter(JDK_IS_STANDALONE, val);
        }
        if (sp) {
            clearSystemProperty(SP_JDK_IS_STANDALONE);
        }
        final StringWriter writer = new StringWriter();
        final LSOutput output = impl.createLSOutput();
        output.setCharacterStream(writer);
        ser.write(document, output);
        String result = writer.toString();

        Assert.assertEquals(result, expected);
    }

    /*
     * Bug: 8249867
     * Verifies the definition of the System property "jdk.xml.isStandalone".
     * The system property is true only if it is "true" and false otherwise.
     */
    @Test(dataProvider = "SP")
    public void testSP(String value, boolean expected) throws Exception {
        setSystemProperty(SP_JDK_IS_STANDALONE, value);
        DOMConfiguration c = getConfig();
        clearSystemProperty(SP_JDK_IS_STANDALONE);

        Assert.assertEquals(c.getParameter(JDK_IS_STANDALONE), expected);
    }

    /*
     * @bug 8087303
     * Test the xml document are serialized with pretty-print by
     * LSSerializer and transformer correctly
     *
     */
    @Test(dataProvider = "xml-data")
    public void testXMLPrettyPrint(String sourceFile, String expectedFile)
            throws Exception {
        String source = read(sourceFile);
        String expected = read(expectedFile);
        // test it's no change if no pretty-print
        String result = serializerWrite(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)),
                "The actual is: " + result);
        // test pretty-print
        assertEquals(serializerWrite(toXmlDocument(source), true), expected);
        // test it's no change if no pretty-print
        result = transform(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)),
                "The actual is: " + result);
        // test pretty-print
        assertEquals(transform(toXmlDocument(source), true).replaceAll("\r\n", "\n"),
                expected);
    }


    /*
     * @bug 8087303
     * Test a single text node is serialized with pretty-print by
     * LSSerializer and transformer correctly
     *
     */
    @Test
    public void testSingleTextNode() throws Exception {
        Node xml = newTextNode(read("nodetest1.txt"));
        String expected = read("nodetest1.out");
        assertEquals(serializerWrite(xml, true), expected);
        assertEquals(transform(xml, true).replaceAll("\r\n", "\n"), expected);
    }

    /*
     * @bug 8087303
     * Test the transformer shall keep all whitespace text node in
     * sequent text nodes
     *
     */
    @Test
    public void testSequentTextNodesWithTransformer() throws Exception {
        Node xml = createDocWithSequentTextNodes();
        String expected = read("nodetest2.out");
        assertEquals(transform(xml, true).replaceAll("\r\n", "\n"), expected);
    }

    /*
     * @bug 8087303
     * Test LSSerializer shall eliminate the whitespace text node
     * in sequent text nodes
     *
     */
    @Test
    public void testSequentTextNodesWithLSSerializer() throws Exception {
        Node xml = createDocWithSequentTextNodes();
        String expected = read("nodetest2ls.out");
        assertEquals(serializerWrite(xml, true), expected);
    }


    /*
     * test whitespace and element, nested xml:space property.
     */
    @DataProvider(name = "xml-data-whitespace-ls")
    public Object[][] whitespaceLS() throws Exception {
        return new Object[][]{
            {"xmltest5.xml", "xmltest5ls.out"},
            {"xmltest7.xml", "xmltest7ls.out"}};
    }

    /*
     * @bug 8087303
     * Test LSSerializer shall eliminate the whitespace text node
     * unless xml:space="preserve"
     *
     */
    @Test(dataProvider = "xml-data-whitespace-ls")
    public void testWhitespaceWithLSSerializer(String sourceFile, String expectedFile)
            throws Exception {
        String source = read(sourceFile);
        String expected = read(expectedFile);
        // test it's no change if no pretty-print
        String result = serializerWrite(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)),
                "The actual is: " + result);
        // test pretty-print
        assertEquals(serializerWrite(toXmlDocument(source), true), expected);
    }

    /*
     * test whitespace and element, nested xml:space property.
     */
    @DataProvider(name = "xml-data-whitespace-xslt")
    public Object[][] whitespaceXSLT() throws Exception {
        return new Object[][]{
            {"xmltest5.xml", "xmltest5xslt.out"},
            {"xmltest7.xml", "xmltest7xslt.out"}};
    }

    /*
     * @bug 8087303
     * Test the transformer shall format the output but keep all
     * whitespace text node even if xml:space="preserve"
     *
     */
    @Test(dataProvider = "xml-data-whitespace-xslt")
    public void testWhitespaceWithTransformer(String sourceFile, String expectedFile)
            throws Exception {
        String source = read(sourceFile);
        String expected = read(expectedFile);
        // test it's no change if no pretty-print
        String result = transform(toXmlDocument(source), false);
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(result)),
                "The actual is: " + result);
        // test pretty-print
        assertEquals(transform(toXmlDocument(source), true).replaceAll("\r\n", "\n"),
                expected);
    }

    /*
     * test block element, inline element, text, and mixed elements.
     */
    @DataProvider(name = "html-data")
    public Object[][] htmlData() throws Exception {
        return new Object[][]{
            {"htmltest1.xml", "htmltest1.out"},
            {"htmltest2.xml", "htmltest2.out"},
            {"htmltest3.xml", "htmltest3.out"},
            {"htmltest4.xml", "htmltest4.out"},
            {"htmltest5.xml", "htmltest5.out"},
            {"htmltest6.xml", "htmltest6.out"},
            /* @bug 8174025, test whitespace between inline elements */
            {"htmltest7.xml", "htmltest7.out"}};
    }

    /*
     * @bug 8087303
     * Transform to HTML, test Pretty Print for HTML.
     *
     */
    @Test(dataProvider = "html-data")
    public void testTransformToHTML(String sourceFile, String expectedFile)
            throws Exception {
        String source = read(sourceFile);
        String expected = read(expectedFile);
        // test it's no change if no pretty-print
        StringWriter writer = new StringWriter();
        getTransformer(true, false).transform(
                new StreamSource(new StringReader(source)), new StreamResult(writer));
        assertTrue(toXmlDocument(source).isEqualNode(toXmlDocument(writer.toString())),
                "The actual is: " + writer.toString());

        // test pretty-print
        writer = new StringWriter();
        getTransformer(true, true).transform(
                new StreamSource(new StringReader(source)), new StreamResult(writer));
        assertEquals(writer.toString().replaceAll("\r\n", "\n"), expected);
    }

    /*
     * @bug 8174025
     * Test the serializer can handle <xsl:text disable-output-escaping="yes"> correctly.
     *
     */
    @Test
    public void testDisableOutputEscaping() throws Exception {
        final String xsl = "generate-catalog.xsl";
        final String xml = "simple-entity-resolver-config.xml";
        final String expectedOutput = "simple-entity-resolver-config-transformed.xml";
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTemplates(
                new StreamSource(new StringReader(read(xsl)))).newTransformer();

        String key = "schemaBase";
        String value = "schemas";
        transformer.setParameter(key, value);
        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(read(xml))),
                new StreamResult(writer));
        assertEquals(writer.toString().replaceAll("\r\n", "\n"), read(expectedOutput));
    }

    @Test
    public void testLSSerializerFormatPrettyPrint() {

        final String XML_DOCUMENT = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n"
                + "<hello>before child element<child><children/><children/></child>"
                + "after child element</hello>";
        /**
         * JDK-8035467 no newline in default output
         */
        final String XML_DOCUMENT_DEFAULT_PRINT
                = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>"
                + "<hello>"
                + "before child element"
                + "<child><children/><children/></child>"
                + "after child element</hello>";

        final String XML_DOCUMENT_PRETTY_PRINT
                = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><hello>\n"
                + "    before child element\n"
                + "    <child>\n"
                + "        <children/>\n"
                + "        <children/>\n"
                + "    </child>\n"
                + "    after child element\n"
                + "</hello>\n";

        // it all begins with a Document
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        }
        Document document = null;

        StringReader stringReader = new StringReader(XML_DOCUMENT);
        InputSource inputSource = new InputSource(stringReader);
        try {
            document = documentBuilder.parse(inputSource);
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }

        // query DOM Interfaces to get to a LSSerializer
        DOMImplementation domImplementation = documentBuilder.getDOMImplementation();
        DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation;
        LSSerializer lsSerializer = domImplementationLS.createLSSerializer();

        System.out.println("Serializer is: " + lsSerializer.getClass().getName()
                + " " + lsSerializer);

        // get configuration
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();

        // query current configuration
        Boolean defaultFormatPrettyPrint
                = (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT);
        Boolean canSetFormatPrettyPrintFalse
                = (Boolean) domConfiguration.canSetParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.FALSE);
        Boolean canSetFormatPrettyPrintTrue
                = (Boolean) domConfiguration.canSetParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.TRUE);

        System.out.println(DOM_FORMAT_PRETTY_PRINT + " default/can set false/can set true = "
                + defaultFormatPrettyPrint + "/"
                + canSetFormatPrettyPrintFalse + "/" + canSetFormatPrettyPrintTrue);

        // test values
        assertEquals(defaultFormatPrettyPrint, Boolean.FALSE,
                "Default value of " + DOM_FORMAT_PRETTY_PRINT + " should be " + Boolean.FALSE);

        assertEquals(canSetFormatPrettyPrintFalse, Boolean.TRUE,
                "Can set " + DOM_FORMAT_PRETTY_PRINT + " to " + Boolean.FALSE + " should be "
                + Boolean.TRUE);

        assertEquals(canSetFormatPrettyPrintTrue, Boolean.TRUE,
                "Can set " + DOM_FORMAT_PRETTY_PRINT + " to " + Boolean.TRUE + " should be "
                + Boolean.TRUE);

        // get default serialization
        String prettyPrintDefault = lsSerializer.writeToString(document);
        System.out.println("(default) " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintDefault + "\"");

        assertEquals(prettyPrintDefault, XML_DOCUMENT_DEFAULT_PRINT,
                "Invalid serialization with default value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));

        // configure LSSerializer to not format-pretty-print
        domConfiguration.setParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.FALSE);
        String prettyPrintFalse = lsSerializer.writeToString(document);
        System.out.println("(FALSE) " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintFalse + "\"");

        assertEquals(prettyPrintFalse, XML_DOCUMENT_DEFAULT_PRINT,
                "Invalid serialization with FALSE value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));

        // configure LSSerializer to format-pretty-print
        domConfiguration.setParameter(DOM_FORMAT_PRETTY_PRINT, Boolean.TRUE);
        String prettyPrintTrue = lsSerializer.writeToString(document);
        System.out.println("(TRUE) " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT)
                + ": \n\"" + prettyPrintTrue + "\"");

        assertEquals(prettyPrintTrue, XML_DOCUMENT_PRETTY_PRINT,
                "Invalid serialization with TRUE value, " + DOM_FORMAT_PRETTY_PRINT + "=="
                + (Boolean) domConfiguration.getParameter(DOM_FORMAT_PRETTY_PRINT));
    }

    private String serializerWrite(Node xml, boolean pretty) throws Exception {
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        DOMImplementationLS domImplementation
                = (DOMImplementationLS) registry.getDOMImplementation("LS");
        StringWriter writer = new StringWriter();
        LSOutput formattedOutput = domImplementation.createLSOutput();
        formattedOutput.setCharacterStream(writer);
        LSSerializer domSerializer = domImplementation.createLSSerializer();
        domSerializer.getDomConfig().setParameter(DOM_FORMAT_PRETTY_PRINT, pretty);
        domSerializer.getDomConfig().setParameter("xml-declaration", false);
        domSerializer.write(xml, formattedOutput);
        return writer.toString();
    }

    private String transform(Node xml, boolean pretty) throws Exception {
        Transformer transformer = getTransformer(false, pretty);
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(xml), new StreamResult(writer));
        return writer.toString();
    }

    private String transform(String xsl, String xml, boolean omit, boolean pretty, boolean p, boolean sp, boolean standalone)
            throws Exception {
        Transformer transformer = getTransformer(xsl, false, omit, pretty, p, sp, standalone);
        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
        return writer.toString();
    }

    private Document toXmlDocument(String xmlString) throws Exception {
        InputSource xmlInputSource = new InputSource(new StringReader(xmlString));
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder xmlDocumentBuilder = dbf.newDocumentBuilder();
        Document node = xmlDocumentBuilder.parse(xmlInputSource);
        return node;
    }

    private Text newTextNode(String text) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.newDocument().createTextNode(text);
    }

    private Document createDocWithSequentTextNodes() throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = db.newDocument();
        Node root = doc.createElement("root");
        doc.appendChild(root);
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode("t"));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("t"));
        root.appendChild(doc.createTextNode("   "));
        Node child1 = doc.createElement("child1");
        root.appendChild(child1);
        child1.appendChild(doc.createTextNode(" "));
        child1.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createTextNode("t"));
        Node child2 = doc.createElement("child2");
        root.appendChild(child2);
        child2.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        Node child3 = doc.createElement("child3");
        root.appendChild(child3);
        child3.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        Node child4 = doc.createElement("child4");
        root.appendChild(child4);
        child4.appendChild(doc.createTextNode(" "));

        root.appendChild(doc.createTextNode(" "));
        Node child5 = doc.createElement("child5");
        root.appendChild(child5);
        child5.appendChild(doc.createTextNode("t"));

        Node child51 = doc.createElement("child51");
        child5.appendChild(child51);
        child51.appendChild(doc.createTextNode(" "));
        Node child511 = doc.createElement("child511");
        child51.appendChild(child511);
        child511.appendChild(doc.createTextNode("t"));
        child51.appendChild(doc.createTextNode(" "));
        child5.appendChild(doc.createTextNode("t"));

        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createComment(" test comment "));
        root.appendChild(doc.createTextNode(" \n"));
        root.appendChild(doc.createComment(" "));
        root.appendChild(doc.createTextNode("\n"));
        root.appendChild(doc.createProcessingInstruction("target1", "test"));
        root.appendChild(doc.createTextNode(" "));
        root.appendChild(doc.createTextNode(" "));
        return doc;
    }

    private Transformer getTransformer(boolean html, boolean pretty) throws Exception {
        return getTransformer(null, html, true, pretty, false, false, false);
    }

    private Transformer getTransformer(String xsl) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        if (xsl == null) {
            return tf.newTransformer();
        }

        return tf.newTemplates(
                new StreamSource(new ByteArrayInputStream(xsl.getBytes())))
                .newTransformer();
    }

    private Transformer getTransformer(String xsl, boolean html, boolean omit,
            boolean pretty, boolean p, boolean sp, boolean standalone)
            throws Exception {
        if (sp) {
            setSystemProperty(SP_XSLTC_IS_STANDALONE, standalone ? "yes" : "no");
        }
        Transformer transformer = getTransformer(xsl);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        if (omit) {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        if (html) {
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
        }
        transformer.setOutputProperty(OutputKeys.INDENT, pretty ? "yes" : "no");
        if (p && !sp) {
            transformer.setOutputProperty(XSLTC_IS_STANDALONE, standalone ? "yes" : "no");
        }
        if (sp) {
            clearSystemProperty(SP_XSLTC_IS_STANDALONE);
        }
        return transformer;
    }

    private String read(String filename) throws Exception {
        try (InputStream in = PrettyPrintTest.class.getResourceAsStream(filename)) {
            return new String(in.readAllBytes());
        }
    }

    private Document getDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element child = document.createElement("sometag");
        document.appendChild(child);
        return document;
    }

    private DOMImplementationLS getImpl() throws Exception {
        Document document = getDocument();
        return (DOMImplementationLS) document.getImplementation();
    }

    private DOMConfiguration getConfig() throws Exception {
        LSSerializer ser = getImpl().createLSSerializer();
        return ser.getDomConfig();
    }
}
