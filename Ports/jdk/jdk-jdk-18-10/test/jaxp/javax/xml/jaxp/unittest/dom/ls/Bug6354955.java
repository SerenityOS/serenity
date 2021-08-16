/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package dom.ls;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;


/*
 * @test
 * @bug 6354955
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.Bug6354955
 * @run testng/othervm dom.ls.Bug6354955
 * @summary Test LSSerializer can writeToString on DOM Text node with white space.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6354955 {

    @Test
    public void testTextNode() {
        try {
            Document xmlDocument = createNewDocument();

            String whitespace = "\r\n    ";
            Text textNode = xmlDocument.createTextNode(whitespace);

            System.out.println("original text is:\r\n\"" + whitespace + "\"");
            String outerXML = getOuterXML(textNode);
            System.out.println("OuterXML Text Node is:\r\n\"" + outerXML + "\"");

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testCommentNode() {
        try {
            Document xmlDocument = createNewDocument();
            String commentStr = "This is a comment node";
            Comment cmtNode = xmlDocument.createComment(commentStr);
            String outerXML = getOuterXML(cmtNode);
            System.out.println("OuterXML of Comment Node is:" + outerXML);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testPINode() {
        try {
            Document xmlDocument = createNewDocument();
            ProcessingInstruction piNode = xmlDocument.createProcessingInstruction("execute", "test");
            String outerXML = getOuterXML(piNode);
            System.out.println("OuterXML of Comment Node is:" + outerXML);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testCDATA() {
        try {
            Document xmlDocument = createNewDocument();
            CDATASection cdataNode = xmlDocument.createCDATASection("See Data!!");
            String outerXML = getOuterXML(cdataNode);
            System.out.println("OuterXML of Comment Node is:" + outerXML);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testEntityReference() {
        try {
            Document xmlDocument = createNewDocument();
            EntityReference erefNode = xmlDocument.createEntityReference("entityref");
            String outerXML = getOuterXML(erefNode);
            System.out.println("OuterXML of Comment Node is:" + outerXML);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    private String getOuterXML(Node node) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) node.getOwnerDocument().getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        if (!(node instanceof Document)) {
            lsSerializer.getDomConfig().setParameter("xml-declaration", false);
        }
        return lsSerializer.writeToString(node);
    }

    private Document createNewDocument() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }
}
