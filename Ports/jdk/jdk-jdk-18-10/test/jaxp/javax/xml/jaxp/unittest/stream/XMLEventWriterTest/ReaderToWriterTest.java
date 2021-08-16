/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLEventWriterTest;

import static jaxp.library.JAXPTestUtilities.USER_DIR;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventWriterTest.ReaderToWriterTest
 * @run testng/othervm stream.XMLEventWriterTest.ReaderToWriterTest
 * @summary Test XMLEventWriter.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class ReaderToWriterTest {

    private static final XMLEventFactory XML_EVENT_FACTORY = XMLEventFactory.newInstance();
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private static final String INPUT_FILE = "W2JDLR4002TestService.wsdl.data";
    private static final String OUTPUT_FILE = USER_DIR + "Encoded.wsdl";

    /**
     * Unit test for writing namespaces when namespaceURI == null.
     */
    @Test
    public void testWriteNamespace() {

        /** Platform default encoding. */
        final String DEFAULT_CHARSET = java.nio.charset.Charset.defaultCharset().name();
        System.out.println("DEFAULT_CHARSET = " + DEFAULT_CHARSET);

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\" encoding=\"" + DEFAULT_CHARSET + "\"?><prefix:root xmlns=\"\" xmlns:null=\"\"></prefix:root>";
        final String EXPECTED_OUTPUT_NO_ENCODING = "<?xml version=\"1.0\"?><prefix:root xmlns=\"\" xmlns:null=\"\"></prefix:root>";

        // new Writer
        XMLEventWriter xmlEventWriter = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            xmlEventWriter = XML_OUTPUT_FACTORY.createXMLEventWriter(byteArrayOutputStream);
        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        }

        try {
            // start a valid event stream
            XMLEvent startDocumentEvent = XML_EVENT_FACTORY.createStartDocument(DEFAULT_CHARSET);
            XMLEvent startElementEvent = XML_EVENT_FACTORY.createStartElement("prefix", "http://example.com", "root");
            xmlEventWriter.add(startDocumentEvent);
            xmlEventWriter.add(startElementEvent);

            // try using a null default namespaceURI
            XMLEvent namespaceEvent = XML_EVENT_FACTORY.createNamespace(null);
            xmlEventWriter.add(namespaceEvent);

            // try using a null prefix'd namespaceURI
            XMLEvent namespacePrefixEvent = XML_EVENT_FACTORY.createNamespace("null", null);
            xmlEventWriter.add(namespacePrefixEvent);

            // close event stream
            XMLEvent endElementEvent = XML_EVENT_FACTORY.createEndElement("prefix", "http://example.com", "root");
            XMLEvent endDocumentEvent = XML_EVENT_FACTORY.createEndDocument();
            xmlEventWriter.add(endElementEvent);
            xmlEventWriter.add(endDocumentEvent);
            xmlEventWriter.flush();
        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        }

        // get XML document as String
        String actualOutput = byteArrayOutputStream.toString();

        // is output as expected?
        if (!actualOutput.equals(EXPECTED_OUTPUT) && !actualOutput.equals(EXPECTED_OUTPUT_NO_ENCODING)) {
            Assert.fail("Expected: " + EXPECTED_OUTPUT + ", actual: " + actualOutput);
        }
    }

    /**
     * Test: 6419687 NPE in XMLEventWriterImpl.
     */
    @Test
    public void testCR6419687() {

        try {
            InputStream in = getClass().getResourceAsStream("ReaderToWriterTest.wsdl");
            OutputStream out = new FileOutputStream(USER_DIR + "ReaderToWriterTest-out.xml");

            XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(in);
            XMLEventWriter writer = XML_OUTPUT_FACTORY.createXMLEventWriter(out, "UTF-8");
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                writer.add(event);
            }
            reader.close();
            writer.close();
        } catch (XMLStreamException xmlStreamException) {
            xmlStreamException.printStackTrace();
            Assert.fail(xmlStreamException.toString());
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            Assert.fail(fileNotFoundException.toString());
        }
    }

    /*
     * Reads UTF-16 encoding file and writes it to UTF-8 encoded format.
     */
    @Test
    public void testUTF8Encoding() {
        try {
            InputStream in = util.BOMInputStream.createStream("UTF-16BE", this.getClass().getResourceAsStream(INPUT_FILE));
            OutputStream out = new FileOutputStream(OUTPUT_FILE);

            XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(in);
            XMLEventWriter writer = XML_OUTPUT_FACTORY.createXMLEventWriter(out, "UTF-8");

            writeEvents(reader, writer);
            checkOutput(OUTPUT_FILE);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        } finally {
            File file = new File(OUTPUT_FILE);
            if (file.exists())
                file.delete();
        }
    }

    private void writeEvents(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            writer.add(event);
        }
        reader.close();
        writer.close();
    }

    private void checkOutput(String output) throws Exception {
        InputStream in = new FileInputStream(output);
        XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(in);
        while (reader.hasNext()) {
            reader.next();
        }
        reader.close();
    }

    /*
     * Reads UTF-16 encoding file and writes it with default encoding.
     */
    @Test
    public void testNoEncoding() {
        try {
            InputStream in = util.BOMInputStream.createStream("UTF-16BE", this.getClass().getResourceAsStream(INPUT_FILE));
            OutputStream out = new FileOutputStream(OUTPUT_FILE);

            XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(in);
            XMLEventWriter writer = XML_OUTPUT_FACTORY.createXMLEventWriter(out);

            writeEvents(reader, writer);
            checkOutput(OUTPUT_FILE);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        } finally {
            File file = new File(OUTPUT_FILE);
            if (file.exists())
                file.delete();
        }
    }

}
