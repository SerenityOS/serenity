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

package validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/*
 * @test
 * @bug 6946312
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug6946312Test
 * @run testng/othervm validation.Bug6946312Test
 * @summary Test XML parser shall callback to ContentHandler when receiving characters data.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6946312Test {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
    String xmlSchema = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" + "<xs:element name=\"root\">\n" + "<xs:complexType>\n"
            + "<xs:sequence>\n" + "<xs:any namespace=\"##any\"  processContents=\"skip\"/>\n" + "</xs:sequence>\n" + "</xs:complexType>\n" + "</xs:element>\n"
            + "</xs:schema>";

    boolean charEvent = false;

    @Test
    public void test() throws SAXException, ParserConfigurationException, IOException {
        Schema schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema").newSchema(new StreamSource(new StringReader(xmlSchema)));

        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        saxParserFactory.setSchema(schema);
        // saxParserFactory.setFeature("http://java.sun.com/xml/schema/features/report-ignored-element-content-whitespace",
        // true);

        SAXParser saxParser = saxParserFactory.newSAXParser();

        XMLReader xmlReader = saxParser.getXMLReader();

        xmlReader.setContentHandler(new MyContentHandler());

        // InputStream input =
        // ClassLoader.getSystemClassLoader().getResourceAsStream("test/test.xml");

        InputStream input = getClass().getResourceAsStream("Bug6946312.xml");
        System.out.println("Parse InputStream:");
        xmlReader.parse(new InputSource(input));
        if (!charEvent) {
            Assert.fail("missing character event");
        }
    }

    public class MyContentHandler implements ContentHandler {
        public void characters(char[] ch, int start, int length) {
            charEvent = true;
            System.out.println("Characters called: " + new String(ch, start, length));
        }

        public void endDocument() throws SAXException {
        }

        public void endElement(String arg0, String arg1, String arg2) throws SAXException {
        }

        public void endPrefixMapping(String arg0) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            System.out.println("ignorableWhitespace called: " + new String(ch, start, length));
        }

        public void processingInstruction(String arg0, String arg1) throws SAXException {
        }

        public void setDocumentLocator(Locator arg0) {
        }

        public void skippedEntity(String arg0) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startElement(String arg0, String arg1, String arg2, Attributes arg3) throws SAXException {
        }

        public void startPrefixMapping(String arg0, String arg1) throws SAXException {
        }
    }

}
