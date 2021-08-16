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

package stream.EntitiesTest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EntitiesTest.EntityTest
 * @run testng/othervm stream.EntitiesTest.EntityTest
 * @summary Test StAX parses entity.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class EntityTest {

    XMLInputFactory factory = null;
    String output = "";

    @BeforeMethod
    public void setUp() {
        try {
            factory = XMLInputFactory.newInstance();
        } catch (Exception ex) {
            Assert.fail("Could not create XMLInputFactory");
        }
    }

    @AfterMethod
    public void tearDown() {
        factory = null;
    }

    @Test
    public void testProperties() {
        Assert.assertTrue(factory.isPropertySupported("javax.xml.stream.isReplacingEntityReferences"));
    }

    @Test
    public void testCharacterReferences() {
        try {
            URL fileName = EntityTest.class.getResource("testCharRef.xml");
            URL outputFileName = EntityTest.class.getResource("testCharRef.xml.output");
            XMLStreamReader xmlr = factory.createXMLStreamReader(new InputStreamReader(fileName.openStream()));
            int eventType = 0;
            while (xmlr.hasNext()) {
                eventType = xmlr.next();
                handleEvent(xmlr, eventType);
            }
            System.out.println("Output:");
            System.out.println(output);
            Assert.assertTrue(compareOutput(new InputStreamReader(outputFileName.openStream()), new StringReader(output)));
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

    private void handleEvent(XMLStreamReader xmlr, int eventType) {
        switch (eventType) {
            case XMLEvent.START_ELEMENT:
                handleStartElement(xmlr);
                break;
            case XMLEvent.END_ELEMENT:
                handleEndElement(xmlr);
                break;
            case XMLEvent.CHARACTERS:
                handleCharacters(xmlr);
                break;
            case XMLEvent.COMMENT:
                handleComment(xmlr);
                break;
            case XMLEvent.ENTITY_REFERENCE:
                break;
            case XMLEvent.ATTRIBUTE:
                break;
            case XMLEvent.DTD:
                break;
            case XMLEvent.CDATA:
                break;
            default:
                break;
        }
    }

    private void handleStartElement(XMLStreamReader xmlr) {
        output += "<";
        output += xmlr.getLocalName();
        if (xmlr.hasText())
            output += xmlr.getText();
        printAttributes(xmlr);
        output += ">";
    }

    private void handleEndElement(XMLStreamReader xmlr) {
        output += "</";
        output += xmlr.getLocalName();
        output += ">";
    }

    private void handleComment(XMLStreamReader xmlr) {
        if (xmlr.hasText())
            output += xmlr.getText();
    }

    private void handleCharacters(XMLStreamReader xmlr) {
        if (xmlr.hasText())
            output += xmlr.getText();
    }

    private void printAttributes(XMLStreamReader xmlr) {
        if (xmlr.getAttributeCount() > 0) {
            int count = xmlr.getAttributeCount();
            for (int i = 0; i < count; i++) {
                output += xmlr.getAttributeName(i);
                output += "=";
                output += xmlr.getAttributeValue(i);
                /*
                 * String name = xmlr.getAttributeName(i) ; String value =
                 * xmlr.getAttributeValue(i) ;
                 * System.out.println(name+"="+value);
                 */
            }
        }
    }

    protected boolean compareOutput(Reader expected, Reader actual) throws IOException {
        LineNumberReader expectedOutput = new LineNumberReader(expected);
        LineNumberReader actualOutput = new LineNumberReader(actual);

        while (expectedOutput.ready() && actualOutput.ready()) {
            String expectedLine = expectedOutput.readLine();
            String actualLine = actualOutput.readLine();
            if (!expectedLine.equals(actualLine)) {
                System.out.println("Entityreference expansion failed, line no: " + expectedOutput.getLineNumber());
                System.out.println("Expected: " + expectedLine);
                System.out.println("Actual  : " + actualLine);
                return false;
            }
        }
        expectedOutput.close();
        actualOutput.close();
        return true;
    }
}
