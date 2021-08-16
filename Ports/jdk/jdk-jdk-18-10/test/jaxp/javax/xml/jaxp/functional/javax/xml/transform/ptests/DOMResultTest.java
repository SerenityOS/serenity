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

package javax.xml.transform.ptests;

import static javax.xml.transform.ptests.TransformerTestConst.GOLDEN_DIR;
import static javax.xml.transform.ptests.TransformerTestConst.XML_DIR;
import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.compareWithGold;
import static org.testng.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * DOM parse on test file to be compared with golden output file. No Exception
 * is expected.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.transform.ptests.DOMResultTest
 * @run testng/othervm javax.xml.transform.ptests.DOMResultTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DOMResultTest {
    /**
     * Unit test for simple DOM parsing.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testcase01() throws Exception {
        String resultFile = USER_DIR  + "domresult01.out";
        String goldFile = GOLDEN_DIR  + "domresult01GF.out";
        String xsltFile = XML_DIR + "cities.xsl";
        String xmlFile = XML_DIR + "cities.xml";

        XMLReader reader = XMLReaderFactory.createXMLReader();
        SAXTransformerFactory saxTFactory
                = (SAXTransformerFactory) TransformerFactory.newInstance();
        SAXSource saxSource = new SAXSource(new InputSource(xsltFile));
        TransformerHandler handler
                = saxTFactory.newTransformerHandler(saxSource);

        DOMResult result = new DOMResult();

        handler.setResult(result);
        reader.setContentHandler(handler);
        reader.parse(xmlFile);

        Node node = result.getNode();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile))) {
            writeNodes(node, writer);
        }
        assertTrue(compareWithGold(goldFile, resultFile));
    }

    /**
     * Prints all node names, attributes to file
     * @param node a node that need to be recursively access.
     * @param bWriter file writer.
     * @throws IOException if writing file failed.
     */
    private void writeNodes(Node node, BufferedWriter bWriter) throws IOException {
        String str = "Node: " + node.getNodeName();
        bWriter.write( str, 0,str.length());
        bWriter.newLine();

        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null && nnm.getLength() > 0)
            for (int i=0; i<nnm.getLength(); i++) {
                str = "AttributeName:" + ((Attr) nnm.item(i)).getName() +
                      ", AttributeValue:" +((Attr) nnm.item(i)).getValue();
                bWriter.write( str, 0,str.length());
                bWriter.newLine();
            }

        NodeList kids = node.getChildNodes();
        if (kids != null)
            for (int i=0; i<kids.getLength(); i++)
                writeNodes(kids.item(i), bWriter);
    }
}
