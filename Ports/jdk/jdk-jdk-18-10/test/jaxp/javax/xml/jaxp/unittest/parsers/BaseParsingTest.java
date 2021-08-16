/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
package parsers;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @test
 * @bug 8169450 8222415 8219692
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.BaseParsingTest
 * @run testng/othervm parsers.BaseParsingTest
 * @summary Tests that verify base parsing
 */
@Listeners({jaxp.library.BasePolicy.class})
public class BaseParsingTest {
    private static final String DOM_IMPL =
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
    private static final String SAX_IMPL =
            "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl";

    String xml_8219692 = "<a "
            + "xmlns=\"http://openjdk_java_net/xml/defaultNS\" "
            + "xmlns:p1=\"http://openjdk_java_net/xml/serializer/\">"
            + "<b>in default namespace</b></a>";

    /**
     * Creates NamespaceAware parsers using old and new factory methods.
     * @return NamespaceAware parsers
     * @throws ParserConfigurationException
     */
    @DataProvider(name = "NSAwareDOMFactory")
    public static Object[][] getNSDOMFactory() throws Exception {
        boolean isNSAware = true;

        return new Object[][]{
            {getDOMParser(DocumentBuilderFactory.newDefaultInstance(), isNSAware)},
            {getDOMParser(DocumentBuilderFactory.newInstance(), isNSAware)},
            {getDOMParser(DocumentBuilderFactory.newInstance(DOM_IMPL, null), isNSAware)},
            // using the new methods
            {DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder()},
            {DocumentBuilderFactory.newNSInstance().newDocumentBuilder()},
            {DocumentBuilderFactory.newNSInstance(DOM_IMPL, null).newDocumentBuilder()}
        };
    }

    /**
     * Creates parsers using the old instance methods. By default, they are
     * not Namespace Aware.
     * @return non-NamespaceAware parsers
     * @throws ParserConfigurationException
     */
    @DataProvider(name = "DOMFactory")
    public static Object[][] getDOMFactory() throws Exception {
        boolean isNSAware = false;

        return new Object[][]{
            {getDOMParser(DocumentBuilderFactory.newDefaultInstance(), isNSAware)},
            {getDOMParser(DocumentBuilderFactory.newInstance(), isNSAware)},
            {getDOMParser(DocumentBuilderFactory.newInstance(DOM_IMPL, null), isNSAware)}
        };
    }


    /**
     * Creates NamespaceAware parsers using old and new factory methods.
     * @return NamespaceAware parsers
     * @throws ParserConfigurationException
     */
    @DataProvider(name = "NSAwareSAXFactory")
    public static Object[][] getNSSAXFactory() throws Exception {
        boolean isNSAware = true;

        return new Object[][]{
            {getSAXParser(SAXParserFactory.newDefaultInstance(), isNSAware)},
            {getSAXParser(SAXParserFactory.newInstance(), isNSAware)},
            {getSAXParser(SAXParserFactory.newInstance(SAX_IMPL, null), isNSAware)},
            // using the new methods
            {SAXParserFactory.newDefaultNSInstance().newSAXParser()},
            {SAXParserFactory.newNSInstance().newSAXParser()},
            {SAXParserFactory.newNSInstance(SAX_IMPL, null).newSAXParser()},
        };
    }

    @DataProvider(name = "SAXFactory")
    public static Object[][] getSAXFactory() throws Exception {
        boolean isNSAware = false;

        return new Object[][]{
            {getSAXParser(SAXParserFactory.newDefaultInstance(), isNSAware)},
            {getSAXParser(SAXParserFactory.newInstance(), isNSAware)},
            {getSAXParser(SAXParserFactory.newInstance(SAX_IMPL, null), isNSAware)},
        };
    }

    @DataProvider(name = "xmlDeclarations")
    public static Object[][] xmlDeclarations() {
        return new Object[][]{
            {"<?xml version=\"1.0\"?><root><test>t</test></root>"},
            {"<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><test>t</test></root>"},
            {"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone='yes'?><root><test>t</test></root>"},
            {"<?xml\n"
                + " version=\"1.0\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version=\"1.0\"\n"
                + " encoding=\"UTF-8\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version=\"1.0\"\n"
                + " encoding=\"UTF-8\"\n"
                + " standalone=\"yes\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version\n"
                + "=\n"
                + "\"1.0\"\n"
                + " encoding\n"
                + "=\n"
                + "\"UTF-8\"\n"
                + " standalone\n"
                + "=\n"
                + "\"yes\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml version=\"1.1\"?><root><test>t</test></root>"},
            {"<?xml version=\"1.1\" encoding=\"UTF-8\"?><root><test>t</test></root>"},
            {"<?xml version=\"1.1\" encoding=\"UTF-8\" standalone='yes'?><root><test>t</test></root>"},
            {"<?xml\n"
                + " version=\"1.1\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version=\"1.1\"\n"
                + " encoding=\"UTF-8\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version=\"1.1\"\n"
                + " encoding=\"UTF-8\"\n"
                + " standalone=\"yes\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"},
            {"<?xml\n"
                + " version\n"
                + "=\n"
                + "\"1.1\"\n"
                + " encoding\n"
                + "=\n"
                + "\"UTF-8\"\n"
                + " standalone\n"
                + "=\n"
                + "\"yes\"?>\n"
                + "<root>\n"
                + " <test>t</test>\n"
                + "</root>"}
        };
    }

    /**
     * @bug 8169450
     * Verifies that the parser successfully parses the declarations provided in
     * xmlDeclarations. Exception would otherwise be thrown as reported in 8169450.
     *
     * XML Declaration according to https://www.w3.org/TR/REC-xml/#NT-XMLDecl
     * [23] XMLDecl     ::= '<?xml' VersionInfo EncodingDecl? SDDecl? S? '?>'
     * [24] VersionInfo ::= S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
     * [25] Eq          ::= S? '=' S? [26] VersionNum ::= '1.' [0-9]+
     *
     * @param xml the test xml
     * @throws Exception if the parser fails to parse the xml
     */
    @Test(dataProvider = "xmlDeclarations")
    public void test(String xml) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newDefaultFactory();
        XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xml));
        while (xsr.hasNext()) {
            xsr.next();
        }
    }

    /**
     * @bug 8169450
     * This particular issue does not appear in DOM parsing since the spaces are
     * normalized during version detection. This test case then serves as a guard
     * against such an issue from occurring in the version detection.
     *
     * @param xml the test xml
     * @throws Exception if the parser fails to parse the xml
     */
    @Test(dataProvider = "xmlDeclarations")
    public void testWithDOM(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * @bug 8222415
     * Verifies that the parser is configured properly for UTF-16BE or LE.
     * @throws Exception
     */
    @Test
    public void testEncoding() throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(
                "<?xml version=\"1.0\" encoding=\"UTF-16\"?> <a/>".getBytes("UnicodeLittle"));
        InputSource is = new InputSource(bis);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(is);
        assertEquals("UTF-16LE", doc.getInputEncoding());
    }

    /**
     * @bug 8219692
     * Verifies that the default namespace declaration is preserved when
     * NamespaceAware is set on the parser.
     * @throws Exception
     */
    @Test(dataProvider = "NSAwareDOMFactory")
    public void testNSAwareDOMFactory(DocumentBuilder db) throws Exception {
        LSSerializer ls = getSerializer(db);
        String out = ls.writeToString(getDoc(db, xml_8219692));
        System.out.println(out);
        Assert.assertTrue(out.contains("http://openjdk_java_net/xml/defaultNS"));
    }

    /**
     * @bug 8219692
     * Verifies that the default namespace declaration is missing when the
     * old factory methods are used.
     * @throws Exception
     */
    @Test(dataProvider = "DOMFactory")
    public void testDOMFactory(DocumentBuilder db) throws Exception {
        LSSerializer ls = getSerializer(db);
        String out = ls.writeToString(getDoc(db, xml_8219692));
        System.out.println(out);
        Assert.assertFalse(out.contains("http://openjdk_java_net/xml/defaultNS"));
    }

    /**
     * @bug 8219692
     * Verifies that the default namespace declaration is preserved when
     * NamespaceAware is set on the parser.
     * @throws Exception
     */
    @Test(dataProvider = "NSAwareSAXFactory")
    public void testNSAwareSAXFactory(SAXParser sp) throws Exception {
        MyHandler h = new MyHandler();
        sp.parse(new InputSource(new StringReader(xml_8219692)), h);

        Assert.assertTrue(h.isNSAware);
    }

    /**
     * @bug 8219692
     * Verifies that the default namespace declaration is missing when the
     * old factory methods are used.
     * @throws Exception
     */
    @Test(dataProvider = "SAXFactory")
    public void testSAXFactory(SAXParser sp) throws Exception {
        MyHandler h = new MyHandler();
        sp.parse(new InputSource(new StringReader(xml_8219692)), h);

        Assert.assertFalse(h.isNSAware);
    }

    private static DocumentBuilder getDOMParser(DocumentBuilderFactory dbf, boolean isNSAware)
            throws Exception {
        dbf.setNamespaceAware(isNSAware);
        return dbf.newDocumentBuilder();
    }

    private static SAXParser getSAXParser(SAXParserFactory spf, boolean isNSAware)
            throws Exception {
        spf.setNamespaceAware(isNSAware);
        return spf.newSAXParser();
    }

    private LSSerializer getSerializer(DocumentBuilder db) throws Exception {
        DOMImplementationLS di = (DOMImplementationLS) db.getDOMImplementation();
        return di.createLSSerializer();
    }

    private Document getDoc(DocumentBuilder db, String xml) throws Exception {
        InputSource is = new InputSource(new StringReader(xml));
        return db.parse(is);
    }

    /**
     * SAX Handler
     */
    class MyHandler extends DefaultHandler {
        boolean isNSAware = false;

        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
            isNSAware = "http://openjdk_java_net/xml/defaultNS".equals(uri)
                    && ("a".equals(localName) || "b".equals(localName));
        }
    }
}
