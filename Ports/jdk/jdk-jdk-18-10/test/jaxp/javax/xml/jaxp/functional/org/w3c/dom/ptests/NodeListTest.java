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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.NodeListTest
 * @run testng/othervm org.w3c.dom.ptests.NodeListTest
 * @summary Verifies a bug found in jaxp1.0.1 and 1.1FCS. After going out of
 * bound, the last element of a NodeList returns null. The bug has been fixed
 * in jaxp 1.1.1 build.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NodeListTest {

    @DataProvider(name = "xml")
    public Object[][] getTestData() {
        return new Object[][] { { "nodelist.xml", "document" }, { "Node01.xml", "body" } };
    }

    @Test(dataProvider = "xml")
    public void lastItemTest(String xmlFileName, String nodeName) throws Exception {
        Document document = createDOM(xmlFileName);

        NodeList nl = document.getElementsByTagName(nodeName);
        int n = nl.getLength();

        Element elem1 = (Element) nl.item(n - 1);
        nl.item(n);
        Element elem3 = (Element) nl.item(n - 1);
        assertEquals(elem3, elem1);

    }

}
