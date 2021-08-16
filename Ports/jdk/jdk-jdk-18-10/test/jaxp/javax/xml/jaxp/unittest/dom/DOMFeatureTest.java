/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 8206132
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng dom.DOMFeatureTest
 * @summary Tests DOM features.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DOMFeatureTest {

    private static final String XML1 = "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE document [\n"
            + "    <!ENTITY author \"William Shakespeare\">\n"
            + "    <!ELEMENT document (title)>\n"
            + "    <!ELEMENT title (#PCDATA)>\n"
            + "]>\n"
            + "<document>\n"
            + "    <title>&author;</title>\n"
            + "</document>";

    private static final String XML2 = "<?xml version=\"1.0\"?>\n"
            + "<!DOCTYPE document [\n"
            + "    <!ENTITY author \"William Shakespeare\">\n"
            + "    <!ELEMENT document (title)>\n"
            + "    <!ELEMENT title (#PCDATA|chapter)*>\n"
            + "    <!ELEMENT chapter (#PCDATA)>\n"
            + "]>\n"
            + "<document>\n"
            + "    <title>&author;  Hamlet<chapter>Chapter 1</chapter></title>\n"
            + "</document>";

    private static final String XML3 = "<?xml version=\"1.0\"?>\n"
        + "<!DOCTYPE document [\n"
        + "    <!ENTITY author SYSTEM \"https://openjdk_java_net/author.dtd\">"
        + "    <!ELEMENT document (title)>\n"
        + "    <!ELEMENT title (#PCDATA|chapter)*>\n"
        + "    <!ELEMENT chapter (#PCDATA)>\n"
        + "]>\n"
        + "<document>\n"
        + "    <title>&author;  Hamlet<chapter>Chapter 1</chapter></title>\n"
        + "</document>";

    /*
     * DataProvider: for testing the EntityExpansion feature
     * Data columns: case number, feature setting (true/false), xml file,
     *               number of nodes expected, text content expected, element if any
     */
    @DataProvider(name = "EntityExpansion")
    Object[][] getData() throws Exception {
        return new Object[][]{
            {1, true, XML1, 1, "William Shakespeare", null},
            {2, true, XML2, 2, "William Shakespeare  Hamlet", "chapter"},
            {3, false, XML1, 1, null, null},
            {4, false, XML2, 3, "  Hamlet", "chapter"},
            {4, false, XML3, 3, "  Hamlet", "chapter"},
        };
    }

    /*
     * DataProvider: for testing the EntityExpansion feature
     * Data columns: feature setting (true/false), xml file
     */
    @DataProvider(name = "EntityExpansion1")
    Object[][] getData1() throws Exception {
        return new Object[][]{
            {true, XML3},
        };
    }
    /**
     * Verifies the EntityExpansion feature.
     * @param caseNo the case number
     * @param feature flag indicating the setting of the feature
     * @param xml the XML string
     * @param n the number of nodes expected
     * @param expectedText expected Text string
     * @param expectedElement expected Element
     * @throws Exception
     */
    @Test(dataProvider = "EntityExpansion")
    public void testEntityExpansion(int caseNo, boolean feature, String xml,
            int n, String expectedText, String expectedElement) throws Exception {
        final Document doc = getDocument(feature, xml);
        final Element e = (Element) doc.getElementsByTagName("title").item(0);
        final NodeList nl = e.getChildNodes();

        switch (caseNo) {
            case 1:
                // The DOM tree should contain just the Text node
                Assert.assertTrue(nl.item(0) instanceof Text);
                Assert.assertEquals(nl.item(0).getNodeValue(), expectedText);
                Assert.assertEquals(nl.getLength(), n);
                break;
            case 2:
                // The DOM tree contains the Text node and an Element (chapter)
                Assert.assertTrue(nl.item(0) instanceof Text);
                Assert.assertEquals(nl.item(0).getNodeValue(), expectedText);
                Assert.assertTrue(nl.item(1) instanceof Element);
                Assert.assertEquals(nl.item(1).getNodeName(), expectedElement);
                Assert.assertEquals(nl.getLength(), n);
                break;
            case 3:
                // The DOM tree contains just the EntityReference node
                Assert.assertTrue(nl.item(0) instanceof EntityReference);
                Assert.assertEquals(nl.item(0).getNodeValue(), null);
                Assert.assertEquals(nl.getLength(), n);
                break;
            case 4:
                // The DOM tree contains a EntityReference, Text and an Element
                Assert.assertTrue(nl.item(0) instanceof EntityReference);
                Assert.assertEquals(nl.item(0).getNodeValue(), null);
                Assert.assertTrue(nl.item(1) instanceof Text);
                Assert.assertEquals(nl.item(1).getNodeValue(), expectedText);
                Assert.assertTrue(nl.item(2) instanceof Element);
                Assert.assertEquals(nl.item(2).getNodeName(), expectedElement);
                Assert.assertEquals(nl.getLength(), n);
                break;
        }
    }

    /**
     * Verifies the EntityExpansion feature. When the feature is set to true, the
     * parser will attempt to resolve the external reference, that in turn will
     * result in an Exception.
     * @param feature flag indicating the setting of the feature
     * @param xml the XML string
     * @throws Exception: when a non-existent external reference is encountered
     */
    @Test(dataProvider = "EntityExpansion1", expectedExceptions = java.net.UnknownHostException.class)
    public void testEntityExpansion1(boolean feature, String xml)
            throws Exception {
        final Document doc = getDocument(feature, xml);
        final Element e = (Element) doc.getElementsByTagName("title").item(0);
        final NodeList nl = e.getChildNodes();
    }

    private static Document getDocument(boolean expand, String xml)
            throws SAXException, IOException, ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setExpandEntityReferences(expand);

        final DocumentBuilder docBuilder = dbf.newDocumentBuilder();

        InputStream a = new ByteArrayInputStream(xml.getBytes());
        Document out = docBuilder.parse(a);
        return out;
    }
}
