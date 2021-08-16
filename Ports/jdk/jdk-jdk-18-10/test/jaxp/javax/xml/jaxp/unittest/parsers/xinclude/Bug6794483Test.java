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

package parsers.xinclude;

import static java.lang.System.lineSeparator;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/*
 * @test
 * @bug 6794483 8080908
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.xinclude.Bug6794483Test
 * @run testng/othervm parsers.xinclude.Bug6794483Test
 * @summary Test JAXP parser can resolve the included content properly if the
 * included xml contains an empty tag that ends with "/>", refer to XERCESJ-1134.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6794483Test {

    @Test
    public final void test() throws Exception {
        Document doc = parseXmlFile(getClass().getResource("test1.xml").getPath());

        // check node4
        NodeList nodeList = doc.getElementsByTagName("node4");
        assertEquals(nodeList.getLength(), 1);
        assertEquals(nodeList.item(0).getTextContent(), "Node4 Value", "The data of node4 is missed in parsing: " + lineSeparator() + printXmlDoc(doc));

        // check node6
        nodeList = doc.getElementsByTagName("node6");
        assertEquals(nodeList.getLength(), 1);
        assertEquals(nodeList.item(0).getTextContent(), "Node6 Value", "The data of node6 is missed in parsing: " + lineSeparator() + printXmlDoc(doc));
    }

    public String printXmlDoc(Document doc) throws Exception {
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);

        TransformerFactory transformerFact = TransformerFactory.newInstance();
        transformerFact.setAttribute("indent-number", new Integer(4));
        Transformer transformer;

        transformer = transformerFact.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");

        transformer.transform(new DOMSource(doc), result);
        return sw.toString();
    }

    public Document parseXmlFile(String fileName) throws Exception {
        System.out.println("Parsing XML file... " + fileName);
        DocumentBuilder docBuilder = null;
        Document doc = null;
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setXIncludeAware(true);
        System.out.println("Include: " + docBuilderFactory.isXIncludeAware());
        docBuilderFactory.setNamespaceAware(true);
        docBuilderFactory.setExpandEntityReferences(true);

        docBuilder = docBuilderFactory.newDocumentBuilder();

        File sourceFile = new File(fileName);
        doc = docBuilder.parse(sourceFile);

        System.out.println("XML file parsed");
        return doc;

    }
}
