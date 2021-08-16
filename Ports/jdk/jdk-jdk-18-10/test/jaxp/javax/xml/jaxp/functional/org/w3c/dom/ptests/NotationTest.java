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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Notation;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.NotationTest
 * @run testng/othervm org.w3c.dom.ptests.NotationTest
 * @summary Test for Notation interface
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NotationTest {
    /*
     * Test getSystemId method.
     */
    @Test
    public void testGetSystemId() throws Exception {
        assertEquals(findNotation("gs").getSystemId(), "http://who.knows.where/");
    }

    /*
     * Test getPublicId method.
     */
    @Test
    public void testGetPublicId() throws Exception {
        assertEquals(findNotation("pubname").getPublicId(), "pubId");
    }

    //find notation in Notation01.xml
    private Notation findNotation(String name) throws SAXException, IOException, ParserConfigurationException {
        Document document = createDOM("Notation01.xml");
        NamedNodeMap nm = document.getDoctype().getNotations();
        for (int i = 0; i < nm.getLength(); i++) {
            if (nm.item(i).getNodeName().equals(name)) {
                return (Notation) nm.item(i);
            }
        }
        throw new RuntimeException("Notation: '" + name + "' not found.");
    }

}
