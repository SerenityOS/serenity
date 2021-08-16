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

package stream.XMLStreamReaderTest;

import java.util.Iterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.DefaultAttributeTest
 * @run testng/othervm stream.XMLStreamReaderTest.DefaultAttributeTest
 * @summary Test StAX parses namespace and attribute.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class DefaultAttributeTest {

    private static final String INPUT_FILE = "ExternalDTD.xml";

    @Test
    public void testStreamReader() {
        XMLInputFactory ifac = XMLInputFactory.newInstance();
        XMLOutputFactory ofac = XMLOutputFactory.newInstance();

        try {
            ifac.setProperty(ifac.IS_REPLACING_ENTITY_REFERENCES, new Boolean(false));

            XMLStreamReader re = ifac.createXMLStreamReader(this.getClass().getResource(INPUT_FILE).toExternalForm(),
                    this.getClass().getResourceAsStream(INPUT_FILE));

            while (re.hasNext()) {
                int event = re.next();
                if (event == XMLStreamConstants.START_ELEMENT && re.getLocalName().equals("bookurn")) {
                    Assert.assertTrue(re.getAttributeCount() == 0, "No attributes are expected for <bookurn> ");
                    Assert.assertTrue(re.getNamespaceCount() == 2, "Two namespaces are expected for <bookurn> ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testEventReader() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newInstance();
            XMLEventReader read = ifac.createXMLEventReader(this.getClass().getResource(INPUT_FILE).toExternalForm(),
                    this.getClass().getResourceAsStream(INPUT_FILE));
            while (read.hasNext()) {
                XMLEvent event = read.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    if (startElement.getName().getLocalPart().equals("bookurn")) {
                        Iterator iterator = startElement.getNamespaces();
                        int count = 0;
                        while (iterator.hasNext()) {
                            iterator.next();
                            count++;
                        }
                        Assert.assertTrue(count == 2, "Two namespaces are expected for <bookurn> ");

                        Iterator attributes = startElement.getAttributes();
                        count = 0;
                        while (attributes.hasNext()) {
                            iterator.next();
                            count++;
                        }
                        Assert.assertTrue(count == 0, "Zero attributes are expected for <bookurn> ");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }
}
