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

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static jaxp.library.JAXPTestUtilities.tryRunWithTmpPermission;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.w3c.dom.ptests.DOMTestUtil.GOLDEN_DIR;
import static org.w3c.dom.ptests.DOMTestUtil.createDOM;
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;
import static org.w3c.dom.ptests.DOMTestUtil.createNewDocument;

import java.io.File;
import java.util.PropertyPermission;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.NodeTest
 * @run testng/othervm org.w3c.dom.ptests.NodeTest
 * @summary Test Node interface
 */
@Listeners({jaxp.library.FilePolicy.class})
public class NodeTest {
    @DataProvider(name = "feature-supported")
    public Object[][] getFeatureSupportedList() throws Exception {
        Document document = createDOMWithNS("Node01.xml");
        Node node = document.getElementsByTagName("body").item(0);
        return new Object[][] {
                { node, "XML", "2.0", true },
                { node, "HTML", "2.0", false },
                { node, "Views", "2.0", false },
                { node, "StyleSheets", "2.0", false },
                { node, "CSS", "2.0", false },
                { node, "CSS2", "2.0", false },
                { node, "Events", "2.0", true },
                { node, "UIEvents", "2.0", false },
                { node, "MouseEvents", "2.0", false },
                { node, "HTMLEvents", "2.0", false },
                { node, "Traversal", "2.0", true },
                { node, "Range", "2.0", true } };
    }

    /*
     * Verify Node for feature supporting.
     */
    @Test(dataProvider = "feature-supported")
    public void testHasFeature(Node node, String feature, String version, boolean supported) {
        assertEquals(node.isSupported(feature, version), supported);
    }

    /*
     * Test normalize method will merge adjacent Text nodes.
     */
    @Test
    public void testNormalize() throws Exception {
        Document document = createDOM("Node05.xml");

        Element root = document.getDocumentElement();

        Node node =  document.getElementsByTagName("title").item(0);
        node.appendChild(document.createTextNode("test"));
        root.normalize();
        assertEquals(node.getChildNodes().item(0).getNodeValue(), "Typographytest");
    }

    /*
     * Test cloneNode deeply, and the clone node can be appended on the same document.
     */
    @Test
    public void testCloneNode() throws Exception {
        Document document = createDOMWithNS("Node02.xml");

        NodeList nodeList = document.getElementsByTagName("body");
        Node node = nodeList.item(0);
        Node cloneNode = node.cloneNode(true);

        assertTrue(node.isEqualNode(cloneNode));
        assertNotEquals(node, cloneNode);

        nodeList = document.getElementsByTagName("html");
        Node node2 = nodeList.item(0);
        node2.appendChild(cloneNode);
    }

    /*
     * Test importing node from one document to another.
     */
    @Test
    public void testImportNode() throws Exception {
        Document document = createDOMWithNS("Node02.xml");
        Document otherDocument = createDOMWithNS("ElementSample01.xml");

        NodeList otherNodeList = otherDocument.getElementsByTagName("body");
        Node importedNode = otherNodeList.item(0);
        Node clone = importedNode.cloneNode(true);

        Node retNode = document.importNode(importedNode, true);
        assertTrue(clone.isEqualNode(importedNode)); //verify importedNode is not changed
        assertNotEquals(retNode, importedNode);
        assertTrue(importedNode.isEqualNode(retNode));

        retNode = document.importNode(importedNode, false);
        assertTrue(clone.isEqualNode(importedNode)); //verify importedNode is not changed
        assertEquals(retNode.getNodeName(), importedNode.getNodeName());
        assertFalse(importedNode.isEqualNode(retNode));
    }

    /*
     * Test inserting a document fragment before a particular node.
     */
    @Test
    public void testInsertBefore() throws Exception {
        Document document = createDOM("Node04.xml");

        Element parentElement = (Element) document.getElementsByTagName("to").item(0);
        Element element = (Element) document.getElementsByTagName("sender").item(0);
        parentElement.insertBefore(createTestDocumentFragment(document), element);

        String outputfile = USER_DIR + "InsertBefore.out";
        String goldfile = GOLDEN_DIR + "InsertBeforeGF.out";
        tryRunWithTmpPermission(() -> outputXml(document, outputfile), new PropertyPermission("user.dir", "read"));
        assertTrue(compareWithGold(goldfile, outputfile));
    }


    /*
     * Test replacing a particular node with a document fragment.
     */
    @Test
    public void testReplaceChild() throws Exception {
        Document document = createDOM("Node04.xml");

        Element parentElement = (Element) document.getElementsByTagName("to").item(0);
        Element element = (Element) document.getElementsByTagName("sender").item(0);
        parentElement.replaceChild(createTestDocumentFragment(document), element);

        String outputfile = USER_DIR + "ReplaceChild3.out";
        String goldfile = GOLDEN_DIR + "ReplaceChild3GF.out";
        tryRunWithTmpPermission(() -> outputXml(document, outputfile), new PropertyPermission("user.dir", "read"));
        assertTrue(compareWithGold(goldfile, outputfile));
    }

    /*
     * This test case checks for the replaceChild replacing a particular node
     * with a node which was created from a different document than the one
     * which is trying to use this method. It should throw a DOMException.
     */
    @Test(expectedExceptions = DOMException.class)
    public void testReplaceChildNeg() throws Exception {
        Document document = createDOM("Node04.xml");
        Document doc2 = createNewDocument();

        Element parentElement = (Element) document.getElementsByTagName("to").item(0);
        Element element = (Element) document.getElementsByTagName("sender").item(0);
        parentElement.replaceChild(createTestDocumentFragment(doc2), element);
    }

    private DocumentFragment createTestDocumentFragment(Document document) {
        DocumentFragment docFragment = document.createDocumentFragment();
        Element elem = document.createElement("dfElement");
        elem.appendChild(document.createTextNode("Text in it"));
        docFragment.appendChild(elem);
        return docFragment;
    }

    private void outputXml(Document document, String outputFileName) throws TransformerFactoryConfigurationError, TransformerException {
        DOMSource domSource = new DOMSource(document);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StreamResult streamResult = new StreamResult(new File(outputFileName));
        transformer.transform(domSource, streamResult);
    }
}
