/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLStreamWriterTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

/*
 * @test
 * @bug 8145974
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamWriterTest.SurrogatesTest
 * @run testng/othervm stream.XMLStreamWriterTest.SurrogatesTest
 * @summary Check that XMLStreamWriter generates valid xml with surrogate pair
 *  used within element text
 */

@Listeners({jaxp.library.BasePolicy.class})
public class SurrogatesTest {

    // Test that valid surrogate characters can be written/readen by xml stream
    // reader/writer
    @Test(dataProvider = "validData")
    public void xmlWithValidSurrogatesTest(String content)
            throws Exception {
        generateAndReadXml(content);
    }

    // Test that unbalanced surrogate character will
    @Test(dataProvider = "invalidData",
            expectedExceptions = XMLStreamException.class)
    public void xmlWithUnbalancedSurrogatesTest(String content)
            throws Exception {
        generateAndReadXml(content);
    }

    // Generates xml content with XMLStreamWriter and read it to check
    // for correctness of xml and generated data
    void generateAndReadXml(String content) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
        XMLStreamWriter writer = factory.createXMLStreamWriter(streamWriter);

        // Generate xml with selected stream writer type
        generateXML(writer, content);
        String output = stream.toString();
        System.out.println("Generated xml: " + output);
        // Read generated xml with StAX parser
        readXML(output.getBytes(), content);
    }

    // Generates XML with provided xml stream writer. Provided string
    // is inserted into xml twice: with usage of writeCharacters( String )
    // and writeCharacters( char [], int , int )
    private void generateXML(XMLStreamWriter writer, String sequence)
            throws XMLStreamException {
        char[] seqArr = sequence.toCharArray();
        writer.writeStartDocument();
        writer.writeStartElement("root");

        // Use writeCharacters( String ) to write characters
        writer.writeStartElement("writeCharactersWithString");
        writer.writeCharacters(sequence);
        writer.writeEndElement();

        // Use writeCharacters( char [], int , int ) to write characters
        writer.writeStartElement("writeCharactersWithArray");
        writer.writeCharacters(seqArr, 0, seqArr.length);
        writer.writeEndElement();

        // Close root element and document
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    // Reads generated XML data and check if it contains expected
    // text in writeCharactersWithString and writeCharactersWithArray
    // elements
    private void readXML(byte[] xmlData, String expectedContent)
            throws Exception {
        InputStream stream = new ByteArrayInputStream(xmlData);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader xmlReader
                = factory.createXMLStreamReader(stream);
        boolean inTestElement = false;
        StringBuilder sb = new StringBuilder();
        while (xmlReader.hasNext()) {
            String ename;
            switch (xmlReader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    ename = xmlReader.getLocalName();
                    if (ename.equals("writeCharactersWithString")
                            || ename.equals("writeCharactersWithArray")) {
                        inTestElement = true;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    ename = xmlReader.getLocalName();
                    if (ename.equals("writeCharactersWithString")
                            || ename.equals("writeCharactersWithArray")) {
                        inTestElement = false;
                        String content = sb.toString();
                        System.out.println(ename + " text:'" + content + "' expected:'" + expectedContent+"'");
                        Assert.assertEquals(content, expectedContent);
                        sb.setLength(0);
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (inTestElement) {
                        sb.append(xmlReader.getText());
                    }
                    break;
            }
            xmlReader.next();
        }
    }

    @DataProvider(name = "validData")
    public Object[][] getValidData() {
        return new Object[][] {
            {"Don't Worry Be \uD83D\uDE0A"},
            {"BMP characters \uE000\uFFFD"},
            {"Simple text"},
        };
    }

    @DataProvider(name = "invalidData")
    public Object[][] getInvalidData() {
        return new Object[][] {
            {"Unbalanced surrogate \uD83D"},
            {"Unbalanced surrogate \uD83Dis here"},
            {"Surrogate with followup BMP\uD83D\uFFF9"},
        };
    }
}
