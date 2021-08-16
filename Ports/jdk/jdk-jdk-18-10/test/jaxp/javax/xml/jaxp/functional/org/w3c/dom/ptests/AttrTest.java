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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.w3c.dom.ptests.DOMTestUtil.createDOM;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;


/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.AttrTest
 * @run testng/othervm org.w3c.dom.ptests.AttrTest
 * @summary Test for the Attr Interface
 */
@Listeners({jaxp.library.FilePolicy.class})
public class AttrTest {
    /*
     * Verify getName method against both existing Attr and new Attr.
     */
    @Test
    public void testGetName() throws Exception {
        Document document = createDOM("Attr01.xml");
        //test a new created Attr
        Attr attr = document.createAttribute("newAttribute");
        assertEquals(attr.getName(), "newAttribute");

        //test a Attr loaded from xml file
        Element elemNode = (Element) document.getElementsByTagName("book").item(1);
        Attr attr2 = (Attr) elemNode.getAttributes().item(0);
        assertEquals(attr2.getName(), "category1");
    }

    /*
     * Verify getOwnerElement method against both existing Attr and new Attr.
     */
    @Test
    public void testGetOwnerElement() throws Exception {
        Document document = createDOM("Attr01.xml");

        //test Attr loaded from xml file
        Element elemNode = (Element) document.getElementsByTagName("book").item(1);
        NamedNodeMap nnMap = elemNode.getAttributes();
        for (int i = 0; i < nnMap.getLength(); i++) {
            Attr attr = (Attr) nnMap.item(i);
            assertEquals(attr.getOwnerElement().getNodeName(), "book");
        }

        //test an Attr without owner node
        Attr attr = document.createAttribute("newAttribute");
        assertNull(attr.getOwnerElement());

    }

    /*
     * Verify getSpecified method works as the spec.
     */
    @Test
    public void testGetSpecified1() throws Exception {
        Document document = createDOM("Attr01.xml");

        Element elemNode = (Element) document.getElementsByTagName("book").item(1);
        Attr attr = elemNode.getAttributeNode("category1");
        assertTrue(attr.getSpecified());

    }

    /*
     * In this xml file, the dtd has the value for the attrribute, but the xml
     * element does not specify the value for the attrribute, as per the spec it
     * should return false.
     */
    @Test
    public void testGetSpecified2() throws Exception {

        Document document = createDOM("Attr2.xml");
        Element elemNode = (Element) document.getElementsByTagName("Name").item(0);
        Attr attr = elemNode.getAttributeNode("type");

        assertFalse(attr.getSpecified());
    }

    /*
     * Creating a new attribute, the owner element is null since the attribute
     * has just been created, getSpecified should return true.
     */
    @Test
    public void testNewCreatedAttribute() throws Exception {
        Document document = createDOM("Attr01.xml");
        Attr attr = document.createAttribute("newAttribute");
        assertTrue(attr.getSpecified());
        assertNull(attr.getOwnerElement());

    }

    /*
     * The xml file includes the dtd having the IMPLIED value for the attrribute
     * and the xml element does not specify the value. As per the spec it should
     * not be seen as a part of the structure model hence getAttributeNode
     * rerurn null if the attribute is even found.
     */
    @Test
    public void testIMPLIEDAttribute() throws Exception {
        Document document = createDOM("Attr3.xml");
        Element elemNode = (Element) document.getElementsByTagName("Name").item(0);
        Attr attr = elemNode.getAttributeNode("type");
        assertNull(attr);
    }

    /*
     * Test setValue method and verify by getValue method.
     */
    @Test
    public void testSetValue() throws Exception {
        Document document = createDOM("Attr01.xml");
        Attr attr = document.createAttribute("newAttribute");
        attr.setValue("newVal");
        assertEquals(attr.getValue(), "newVal");

    }

}
