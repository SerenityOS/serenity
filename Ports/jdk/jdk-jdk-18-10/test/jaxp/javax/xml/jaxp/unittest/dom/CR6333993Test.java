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

package dom;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/*
 * @test
 * @bug 6333993
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.CR6333993Test
 * @run testng/othervm dom.CR6333993Test
 * @summary Test NodeList.item(valid index) returns value after NodeList.item(NodeList.getLength()).
 */
@Listeners({jaxp.library.BasePolicy.class})
public class CR6333993Test {

    @Test
    public void testNodeList() {
        int n = 5;
        while (0 != (n--))
            ;
        System.out.println("n=" + n);
        try {
            String testXML = "<root>" + "  <node/>" + "  <node/>" + "  <node/>" + "  <node/>" + "</root>\n";
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // dbf.setNamespaceAware(true);
            DocumentBuilder builder = dbf.newDocumentBuilder();
            ByteArrayInputStream bis = new ByteArrayInputStream(testXML.getBytes());
            Document testDoc = builder.parse(bis);
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            XPathExpression expr = xpath.compile("/root/node");
            NodeList testNodes = (NodeList) expr.evaluate(testDoc, XPathConstants.NODESET);
            // Node list appears to work correctly
            System.out.println("testNodes.getLength() = " + testNodes.getLength());
            System.out.println("testNodes[0] = " + testNodes.item(0));
            System.out.println("testNodes[0] = " + testNodes.item(0));
            System.out.println("testNodes.getLength() = " + testNodes.getLength());
            // Access past the end of the NodeList correctly returns null
            System.out.println("testNodes[testNodes.getLength()] = " + testNodes.item(testNodes.getLength()));
            // BUG! First access of valid node after accessing past the end
            // incorrectly returns null
            if (testNodes.item(0) == null) {
                System.out.println("testNodes[0] = null");
                Assert.fail("First access of valid node after accessing past the end incorrectly returns null");
            }
            // Subsequent access of valid node correctly returns the node
            System.out.println("testNodes[0] = " + testNodes.item(0));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
