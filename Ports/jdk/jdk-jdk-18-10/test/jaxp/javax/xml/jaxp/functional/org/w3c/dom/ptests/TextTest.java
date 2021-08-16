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
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;
import static org.w3c.dom.ptests.DOMTestUtil.createNewDocument;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/functional
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.TextTest
 * @run testng/othervm org.w3c.dom.ptests.TextTest
 * @summary Test for Text implementation returned by Document.createTextNode(String)
 */
@Listeners({jaxp.library.FilePolicy.class})
public class TextTest extends AbstractCharacterDataTest {
    /*
     * Verify splitText method works as the spec.
     */
    @Test
    public void testSplitText() throws Exception {
        Document document = createDOMWithNS("Text01.xml");

        NodeList nodeList = document.getElementsByTagName("p");
        Node node = nodeList.item(0);
        Text textNode = document.createTextNode("This is a text node");
        node.appendChild(textNode);
        int rawChildNum = node.getChildNodes().getLength();

        textNode.splitText(0);
        int increased = node.getChildNodes().getLength() - rawChildNum;
        assertEquals(increased, 1);

    }

    @Override
    protected CharacterData createCharacterData(String text) throws IOException, SAXException, ParserConfigurationException {
        Document document = createNewDocument();
        return document.createTextNode(text);
    }

}
