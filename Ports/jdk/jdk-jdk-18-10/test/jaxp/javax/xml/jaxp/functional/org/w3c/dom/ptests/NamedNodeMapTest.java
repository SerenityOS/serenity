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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.NamedNodeMapTest
 * @run testng/othervm org.w3c.dom.ptests.NamedNodeMapTest
 * @summary Test for the methods of NamedNodeMap Interface
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NamedNodeMapTest {
    /*
     * Test setNamedItemNS method with a node having the same namespaceURI and
     * qualified name as an existing one, and then test with a non-existing node.
     */
    @Test
    public void testSetNamedItemNS() throws Exception {
        final String nsURI = "urn:BooksAreUs.org:BookInfo";
        Document document = createDOMWithNS("NamedNodeMap01.xml");
        NodeList nodeList = document.getElementsByTagName("body");
        nodeList = nodeList.item(0).getChildNodes();
        Node n = nodeList.item(3);

        NamedNodeMap namedNodeMap = n.getAttributes();

        // creating an Attribute using createAttributeNS
        // method having the same namespaceURI
        // and the same qualified name as the existing one in the xml file
        Attr attr = document.createAttributeNS(nsURI, "b:style");
        // setting to a new Value
        attr.setValue("newValue");
        Node replacedAttr = namedNodeMap.setNamedItemNS(attr); // return the replaced attr
        assertEquals(replacedAttr.getNodeValue(), "font-family");
        Node updatedAttr = namedNodeMap.getNamedItemNS(nsURI, "style");
        assertEquals(updatedAttr.getNodeValue(), "newValue");


        // creating a non existing attribute node
        attr = document.createAttributeNS(nsURI, "b:newNode");
        attr.setValue("newValue");

        assertNull(namedNodeMap.setNamedItemNS(attr)); // return null

        // checking if the node could be accessed
        // using the getNamedItemNS method
        Node newAttr = namedNodeMap.getNamedItemNS(nsURI, "newNode");
        assertEquals(newAttr.getNodeValue(), "newValue");
    }

    /*
     * Verify getNamedItemNS works as the spec
     */
    @Test
    public void testGetNamedItemNS() throws Exception {
        Document document = createDOMWithNS("NamedNodeMap03.xml");
        NodeList nodeList = document.getElementsByTagName("body");
        nodeList = nodeList.item(0).getChildNodes();
        Node n = nodeList.item(7);
        NamedNodeMap namedNodeMap = n.getAttributes();
        Node node = namedNodeMap.getNamedItemNS("urn:BooksAreUs.org:BookInfo", "aaa");
        assertEquals(node.getNodeValue(), "value");

    }

    /*
     * Test setNamedItem method with a node having the same name as an existing
     * one, and then test with a non-existing node.
     */
    @Test
    public void testSetNamedItem() throws Exception {
        Document document = createDOMWithNS("NamedNodeMap03.xml");
        NodeList nodeList = document.getElementsByTagName("body");
        nodeList = nodeList.item(0).getChildNodes();
        Node n = nodeList.item(1);

        NamedNodeMap namedNodeMap = n.getAttributes();
        Attr attr = document.createAttribute("name");
        Node replacedAttr = namedNodeMap.setNamedItem(attr);
        assertEquals(replacedAttr.getNodeValue(), "attributeValue");
        Node updatedAttrNode = namedNodeMap.getNamedItem("name");
        assertEquals(updatedAttrNode.getNodeValue(), "");

        Attr newAttr = document.createAttribute("nonExistingName");
        assertNull(namedNodeMap.setNamedItem(newAttr));
        Node newAttrNode = namedNodeMap.getNamedItem("nonExistingName");
        assertEquals(newAttrNode.getNodeValue(), "");
    }

}
