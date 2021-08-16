/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 8238183 8266019
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm transform.ResultTest
 * @summary Verifies that the output of a transformation is well-formed when
 *          StAXResult is used.
 */
public class ResultTest {
    public static final String TEST_DIR = System.getProperty("test.classes", ".");

    // The XML contains a comment before the root element
    final static String XML =
            "<?xml version=\"1.0\" ?>\n"
            + "<!--comment-->\n"
            + "<root />\n";
    static Transformer T;

    @BeforeClass
    public void setup() {
        try {
            T = TransformerFactory.newInstance().newTransformer();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * @bug 8266019
     * Verifies that a StreamResult created with a File is processed correctly.
     *
     * @throws Exception if test fails
     */
    @Test
    public void testStreamResult() throws Exception {
        File f = new File(TEST_DIR + "/output/#/dom.xml");
        f.getParentFile().mkdirs();

        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        dom.appendChild(dom.createElement("root"));

        Transformer tr = TransformerFactory.newInstance().newTransformer();
        tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.transform(new DOMSource(dom), new StreamResult(f));
    }

    /**
     * Transforms the XML using a StAXResult with a StreamWriter.
     * @throws Exception if the process fails
     */
    @Test
    public void testStreamWriter() throws Exception {
        try (Reader reader = new StringReader(XML);
                OutputStream out = new ByteArrayOutputStream()) {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            StreamSource source = new StreamSource(reader);
            StAXResult result = new StAXResult(writer);
            T.transform(source, result);
            // verify the output is well-formed
            parseXML(out.toString());
        }
    }

    /**
     * Transforms the XML using a StAXResult with a EventWriter.
     * @throws Exception if the process fails
     */
    @Test
    public void testEventWriter() throws Exception {
        try (Reader reader = new StringReader(XML);
                OutputStream out = new ByteArrayOutputStream()) {
            XMLOutputFactory ofactory = XMLOutputFactory.newInstance();
            XMLEventWriter writer = ofactory.createXMLEventWriter(out);
            StAXResult result = new StAXResult(writer);
            StreamSource source = new StreamSource(reader);
            T.transform(source, result);
            parseXML(out.toString());
        }
    }

    // parses the xml to verify that it's well-formed
    private void parseXML(String xml) throws Exception {
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(new InputSource(new StringReader(xml)), new DefaultHandler());
    }
}
