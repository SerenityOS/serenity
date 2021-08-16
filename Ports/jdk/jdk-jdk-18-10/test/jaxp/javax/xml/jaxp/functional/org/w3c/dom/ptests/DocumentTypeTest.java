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
import static org.w3c.dom.ptests.DOMTestUtil.createDOM;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.DocumentTypeTest
 * @run testng/othervm org.w3c.dom.ptests.DocumentTypeTest
 * @summary Test DocumentType
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DocumentTypeTest {

    /*
     * Test testGetEntities method, and verify the entity items.
     */
    @Test
    public void testGetEntities() throws Exception {
        DocumentType documentType = createDOM("DocumentType01.xml").getDoctype();
        NamedNodeMap namedNodeMap = documentType.getEntities();
        // should return both external and internal. Parameter entities are not
        // contained. Duplicates are discarded.
        assertEquals(namedNodeMap.getLength(), 3);
        assertEquals(namedNodeMap.item(0).getNodeName(), "author");
        assertEquals(namedNodeMap.item(1).getNodeName(), "test");
        assertEquals(namedNodeMap.item(2).getNodeName(), "writer");
    }

    /*
     * Test getNotations method, and verify the notation items.
     */
    @Test
    public void testGetNotations() throws Exception {
        DocumentType documentType = createDOM("DocumentType03.xml").getDoctype();
        NamedNodeMap nm = documentType.getNotations();
        assertEquals(nm.getLength(), 2); // should return 2 because the notation
                                         // name is repeated and
                                         // it considers only the first
                                         // occurence
        assertEquals(nm.item(0).getNodeName(), "gs");
        assertEquals(nm.item(1).getNodeName(), "name");
    }

    /*
     * Test getName method.
     */
    @Test
    public void testGetName() throws Exception {
        DocumentType documentType = createDOM("DocumentType03.xml").getDoctype();
        assertEquals(documentType.getName(), "note");
    }

    /*
     * Test getSystemId and getPublicId method.
     */
    @Test
    public void testGetSystemId() throws Exception {
        DocumentType documentType = createDOM("DocumentType05.xml").getDoctype();
        assertEquals(documentType.getSystemId(), "DocumentBuilderImpl02.dtd");
        Assert.assertNull(documentType.getPublicId());
    }

}
