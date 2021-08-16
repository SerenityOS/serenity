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

package parsers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6564400
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6564400
 * @run testng/othervm parsers.Bug6564400
 * @summary Test ignorable whitespace handling with schema validation.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6564400 {
    private boolean sawIgnorable = false;
    Schema schema = null;

    public Bug6564400(String name) {
        String xsdFile = "Bug6564400.xsd";
        File schemaFile = new File(xsdFile);

        // Now attempt to load up the schema
        try {
            SchemaFactory schFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = schFactory.newSchema(new StreamSource(getClass().getResourceAsStream(xsdFile)));
        } catch (Exception e) {
            // Nevermind, bad things will happen later
        }
    }

    @Test
    public void testDOM() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlFile = getClass().getResourceAsStream("Bug6564400.xml");

        // Set the options on the DocumentFactory to remove comments, remove
        // whitespace
        // and validate against the schema.
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringComments(true);
        docFactory.setIgnoringElementContentWhitespace(true);
        docFactory.setSchema(schema);

        DocumentBuilder parser = docFactory.newDocumentBuilder();
        Document xmlDoc = parser.parse(xmlFile);

        boolean ok = dump(xmlDoc, true);
        Assert.assertEquals(true, ok);
    }

    @Test
    public void testSAX() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlFile = getClass().getResourceAsStream("Bug6564400.xml");

        // Parse with SAX
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setSchema(schema);

        SAXParser saxparser = saxFactory.newSAXParser();

        sawIgnorable = false;
        saxparser.parse(xmlFile, new MyHandler());
        Assert.assertEquals(true, sawIgnorable);
    }

    @Test
    public void testConformantDOM() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlFile = getClass().getResourceAsStream("Bug6564400.xml");

        // Set the options on the DocumentFactory to remove comments, remove
        // whitespace
        // and validate against the schema.
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setIgnoringComments(true);
        docFactory.setIgnoringElementContentWhitespace(true);
        docFactory.setSchema(schema);
        docFactory.setFeature("http://java.sun.com/xml/schema/features/report-ignored-element-content-whitespace", true);

        DocumentBuilder parser = docFactory.newDocumentBuilder();
        Document xmlDoc = parser.parse(xmlFile);

        boolean ok = dump(xmlDoc, true);
        Assert.assertEquals(false, ok);
    }

    @Test
    public void testConformantSAX() throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlFile = getClass().getResourceAsStream("Bug6564400.xml");

        // Parse with SAX
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        saxFactory.setSchema(schema);
        saxFactory.setFeature("http://java.sun.com/xml/schema/features/report-ignored-element-content-whitespace", true);

        SAXParser saxparser = saxFactory.newSAXParser();

        sawIgnorable = false;
        saxparser.parse(xmlFile, new MyHandler());
        Assert.assertEquals(false, sawIgnorable);
    }

    private boolean dump(Node node) {
        return dump(node, false);
    }

    private boolean dump(Node node, boolean silent) {
        return dump(node, silent, 0);
    }

    private boolean dump(Node node, boolean silent, int depth) {
        boolean ok = true;
        if (!silent) {
            for (int i = 0; i < depth; i++) {
                System.out.print("  ");
            }
            System.out.println(node);
        }

        if (node.getNodeType() == Node.TEXT_NODE) {
            String text = ((Text) node).getData();
            ok = ok && text.trim().length() > 0;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE || node.getNodeType() == Node.DOCUMENT_NODE) {
            Node child = node.getFirstChild();
            while (child != null) {
                ok = ok && dump(child, silent, depth + 1);
                child = child.getNextSibling();
            }
        }
        return ok;
    }

    public class MyHandler extends DefaultHandler {
        public void ignorableWhitespace(char[] ch, int start, int length) {
            sawIgnorable = true;
        }
    }
}
