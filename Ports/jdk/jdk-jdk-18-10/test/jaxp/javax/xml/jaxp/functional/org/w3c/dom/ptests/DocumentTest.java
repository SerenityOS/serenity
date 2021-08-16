/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.w3c.dom.ptests;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.w3c.dom.DOMException.NAMESPACE_ERR;
import static org.w3c.dom.ptests.DOMTestUtil.DOMEXCEPTION_EXPECTED;
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;
import static org.w3c.dom.ptests.DOMTestUtil.createNewDocument;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.DocumentTest
 * @run testng/othervm org.w3c.dom.ptests.DocumentTest
 * @summary Test createAttributeNS, getElementsByTagNameNS and createElementNS method of Document
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DocumentTest {

    @DataProvider(name = "invalid-nsuri")
    public Object[][] getInvalidNamespaceURI() {
        return new Object[][] {
                { " ", "xml:novel" }, //blank
                { "hello", "xml:novel" }, //unqualified
                { null, "xml:novel" }, //null
                { "", "xmlns:novel" } };//empty
    }

    /*
     * Test for createAttributeNS method: verifies that DOMException is thrown
     * if reserved prefixes are used with an arbitrary namespace name.
     */
    @Test(dataProvider = "invalid-nsuri", expectedExceptions = DOMException.class)
    public void testCreateAttributeNSNeg(String namespaceURI, String name) throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");
        document.createAttributeNS(namespaceURI, name);
    }

    @DataProvider(name = "valid-nsuri")
    public Object[][] getValidNamespaceURI() {
        return new Object[][] {
                { XML_NS_URI, "xml:novel" },
                { XMLNS_ATTRIBUTE_NS_URI, "xmlns:novel" },
                { "urn:BooksAreUs.org:BookInfo", "attributeNew"},
                { "urn:BooksAreUs.org:BookInfonew", "attributeNew"} };
    }

    /*
     * Verify the Attr from createAttributeNS.
     */
    @Test(dataProvider = "valid-nsuri")
    public void testCreateAttributeNS(String namespaceURI, String name) throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");
        Attr attr = document.createAttributeNS(namespaceURI, name);
        assertEquals(attr.getNamespaceURI(), namespaceURI);
        assertEquals(attr.getName(), name);
    }

    @DataProvider(name = "elementName")
    public Object[][] getElementName() {
        return new Object[][] {
                { "author", 1 },
                { "b:author", 0 } };
    }

    /*
     * Verify the NodeList from getElementsByTagNameNS.
     */
    @Test(dataProvider = "elementName")
    public void testGetElementsByTagNameNS(String localName, int number) throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");
        NodeList nodeList = document.getElementsByTagNameNS("urn:BooksAreUs.org:BookInfo", localName);
        assertEquals(nodeList.getLength(), number);
    }

    /*
     * Test for createElementNS method: verifies that DOMException is thrown
     * if reserved prefixes are used with an arbitrary namespace name.
     */
    @Test(dataProvider = "invalid-nsuri")
    public void testCreateElementNSNeg(String namespaceURI, String name) throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");
        try {
            document.createElementNS(namespaceURI, name);
            fail(DOMEXCEPTION_EXPECTED);
        } catch (DOMException e) {
            assertEquals(e.code, NAMESPACE_ERR);
        }
    }

    /*
     * Test createElementNS method works as the spec.
     */
    @Test
    public void testCreateElementNS() throws Exception {
        final String nsURI = "http://www.books.com";
        final String name = "b:novel";
        final String localName = "novel";
        Document document = createDOMWithNS("DocumentTest01.xml");
        Element element = document.createElementNS(nsURI, name);
        assertEquals(element.getNamespaceURI(), nsURI);
        assertEquals(element.getNodeName(), name);
        assertEquals(element.getLocalName(), localName);
    }

    /*
     * Test createAttributeNS and then append it with setAttributeNode.
     */
    @Test
    public void testAddNewAttributeNode() throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");

        NodeList nodeList = document.getElementsByTagNameNS("http://www.w3.org/TR/REC-html40", "body");
        NodeList childList = nodeList.item(0).getChildNodes();
        Element child = (Element) childList.item(1);
        Attr a = document.createAttributeNS("urn:BooksAreUs.org:BookInfo", "attributeNew");
        child.setAttributeNode(a);
        assertNotNull(child.getAttributeNodeNS("urn:BooksAreUs.org:BookInfo", "attributeNew"));
    }

    /*
     * Test createElementNS and then append it with appendChild.
     */
    @Test
    public void testAddNewElement() throws Exception {
        Document document = createDOMWithNS("DocumentTest01.xml");

        NodeList nodeList = document.getElementsByTagNameNS("http://www.w3.org/TR/REC-html40", "body");
        NodeList childList = nodeList.item(0).getChildNodes();
        Element child = (Element) childList.item(1);
        Element elem = document.createElementNS("urn:BooksAreUs.org:BookInfonew", "newElement");
        assertNotNull(child.appendChild(elem));
    }

    /*
     * Test createElement with unqualified xml name.
     */
    @Test(expectedExceptions = DOMException.class)
    public void testCreateElementNeg() throws Exception {
        Document doc = createNewDocument();
        doc.createElement("!nc$%^*(!");
    }
}
