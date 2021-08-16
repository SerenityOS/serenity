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

import static javax.xml.XMLConstants.XML_NS_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.w3c.dom.DOMException.INUSE_ATTRIBUTE_ERR;
import static org.w3c.dom.ptests.DOMTestUtil.DOMEXCEPTION_EXPECTED;
import static org.w3c.dom.ptests.DOMTestUtil.createDOM;
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;
import static org.w3c.dom.ptests.DOMTestUtil.createNewDocument;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.ElementTest
 * @run testng/othervm org.w3c.dom.ptests.ElementTest
 * @summary Test for the methods of Element Interface
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ElementTest {
    @Test
    public void testGetAttributeNS() throws Exception {
        Document document = createDOMWithNS("ElementSample01.xml");
        Element elemNode = (Element) document.getElementsByTagName("book").item(0);
        String s = elemNode.getAttributeNS("urn:BooksAreUs.org:BookInfo", "category");
        assertEquals(s, "research");
    }

    @Test
    public void testGetAttributeNodeNS() throws Exception {
        Document document = createDOMWithNS("ElementSample01.xml");
        Element elemNode = (Element) document.getElementsByTagName("book").item(0);
        Attr attr = elemNode.getAttributeNodeNS("urn:BooksAreUs.org:BookInfo", "category");
        assertEquals(attr.getValue(), "research");

    }

    /*
     * Test getAttributeNode to get a Attr and then remove it successfully by
     * removeAttributeNode.
     */
    @Test
    public void testRemoveAttributeNode() throws Exception {
        Document document = createDOMWithNS("ElementSample01.xml");
        Element elemNode = (Element) document.getElementsByTagName("book").item(1);
        Attr attr = elemNode.getAttributeNode("category1");
        assertEquals(attr.getValue(), "research");

        assertEquals(elemNode.getTagName(), "book");
        elemNode.removeAttributeNode(attr);
        assertEquals(elemNode.getAttribute("category1"), "");
    }

    /*
     * Test removing an Attribute Node with removeAttributeNS(String
     * namespaceURI, String localName).
     */
    @Test
    public void testRemoveAttributeNS() throws Exception {
        final String nsURI = "urn:BooksAreUs.org:BookInfo";
        final String localName = "category";
        Document document = createDOMWithNS("ElementSample01.xml");
        Element elemNode = (Element) document.getElementsByTagName("book").item(0);
        elemNode.removeAttributeNS(nsURI, localName);

        assertNull(elemNode.getAttributeNodeNS(nsURI, localName));
    }

    /*
     * Test getFirstChild and getLastChild.
     */
    @Test
    public void testGetChild() throws Exception {
        Document document = createDOMWithNS("ElementSample01.xml");
        Element elemNode = (Element) document.getElementsByTagName("b:aaa").item(0);
        elemNode.normalize();
        Node firstChild = elemNode.getFirstChild();
        Node lastChild = elemNode.getLastChild();
        assertEquals(firstChild.getNodeValue(), "fjfjf");
        assertEquals(lastChild.getNodeValue(), "fjfjf");
    }

    /*
     * Test setAttributeNode with an Attr from createAttribute.
     */
    @Test
    public void testSetAttributeNode() throws Exception {
        final String attrName = "myAttr";
        final String attrValue = "attrValue";
        Document document = createDOM("ElementSample02.xml");
        Element elemNode = document.createElement("pricetag2");
        Attr myAttr = document.createAttribute(attrName);
        myAttr.setValue(attrValue);

        assertNull(elemNode.setAttributeNode(myAttr));
        assertEquals(elemNode.getAttribute(attrName), attrValue);
    }

    @DataProvider(name = "attribute")
    public Object[][] getAttributeData() {
        return new Object[][] {
                { "thisisname", "thisisitsvalue" },
                { "style", "font-Family" } };
    }

    @Test(dataProvider = "attribute")
    public void testSetAttribute(String name, String value) throws Exception {
        Document document = createDOM("ElementSample02.xml");
        Element elemNode = document.createElement("pricetag2");
        elemNode.setAttribute(name, value);
        assertEquals(elemNode.getAttribute(name), value);
    }

    /*
     * Negative test for setAttribute, null is not a valid name.
     */
    @Test(expectedExceptions = DOMException.class)
    public void testSetAttributeNeg() throws Exception {
        Document document = createDOM("ElementSample02.xml");
        Element elemNode = document.createElement("pricetag2");
        elemNode.setAttribute(null, null);
    }

    /*
     * Test setAttributeNode, newAttr can't be an attribute of another Element
     * object, must explicitly clone Attr nodes to re-use them in other
     * elements.
     */
    @Test
    public void testDuplicateAttributeNode() throws Exception {
        final String name = "testAttrName";
        final String value = "testAttrValue";
        Document document = createNewDocument();
        Attr attr = document.createAttribute(name);
        attr.setValue(value);

        Element element1 = document.createElement("AFirstElement");
        element1.setAttributeNode(attr);
        Element element2 = document.createElement("ASecondElement");
        Attr attr2 = (Attr) attr.cloneNode(true);
        element2.setAttributeNode(attr2);
        assertEquals(element1.getAttribute(name), element2.getAttribute(name));

        Element element3 = document.createElement("AThirdElement");
        try {
            element3.setAttributeNode(attr);
            fail(DOMEXCEPTION_EXPECTED);
        } catch (DOMException doe) {
            assertEquals(doe.code, INUSE_ATTRIBUTE_ERR);
        }
    }

    /*
     * If not setting the namsepace aware method of DocumentBuilderFactory to
     * true, can't retrieve element by namespace and local name.
     */
    @Test
    public void testNamespaceAware() throws Exception {
        Document document = createDOM("ElementSample02.xml");

        NodeList nl = document.getElementsByTagNameNS("urn:BooksAreUs.org:BookInfo", "author");
        assertNull(nl.item(0));

        nl = document.getDocumentElement().getElementsByTagNameNS("urn:BooksAreUs.org:BookInfo", "author");
        assertNull(nl.item(0));
    }

    @DataProvider(name = "nsattribute")
    public Object[][] getNSAttributeData() {
        return new Object[][] {
                { "h:html", "html", "attrValue" },
                { "b:style", "style",  "attrValue" } };
    }

    /*
     * setAttributeNodeNS and verify it with getAttributeNS.
     */
    @Test(dataProvider = "nsattribute")
    public void testSetAttributeNodeNS(String qualifiedName, String localName, String value) throws Exception {
        Document document = createDOM("ElementSample03.xml");
        Element elemNode = document.createElement("pricetag2");
        Attr myAttr = document.createAttributeNS(XML_NS_URI, qualifiedName);
        myAttr.setValue(value);
        assertNull(elemNode.setAttributeNodeNS(myAttr));
        assertEquals(elemNode.getAttributeNS(XML_NS_URI, localName), value);
    }

    @Test
    public void testHasAttributeNS() throws Exception {
        Document document = createDOMWithNS("ElementSample04.xml");
        NodeList nodeList = document.getElementsByTagName("body");
        NodeList childList = nodeList.item(0).getChildNodes();
        Element child = (Element) childList.item(7);
        assertTrue(child.hasAttributeNS("urn:BooksAreUs.org:BookInfo", "style"));
    }

    @Test
    public void testToString() throws Exception {
        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<!DOCTYPE datacenterlist>"
                + "<datacenterlist>"
                + "  <datacenterinfo"
                + "    id=\"0\""
                + "    naddrs=\"1\""
                + "    nnodes=\"1\""
                + "    ismaster=\"0\">\n"
                + "    <gateway ipaddr=\"192.168.100.27:26000\"/>"
                + "  </datacenterinfo>"
                + "</datacenterlist>";

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();

        assertEquals(root.toString(), "[datacenterlist: null]");
    }

}
