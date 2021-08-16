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

package stream;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EventReaderDelegateTest
 * @run testng/othervm stream.EventReaderDelegateTest
 * @summary Test EventReaderDelegate.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class EventReaderDelegateTest {

    public EventReaderDelegateTest(String name) {
    }

    @Test
    public void testGetElementText() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            XMLEventReader reader = ifac.createXMLEventReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));
            EventReaderDelegate delegate = new EventReaderDelegate(reader);
            while (delegate.hasNext()) {
                XMLEvent event = (XMLEvent) delegate.next();
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String name = event.asStartElement().getName().toString();
                        if (name.equals("name") || name.equals("price")) {
                            System.out.println(delegate.getElementText());
                        } else {
                            try {
                                delegate.getElementText();
                            } catch (XMLStreamException e) {
                                System.out.println("Expected XMLStreamException in getElementText()");
                            }
                        }

                    }
                }
            }
            delegate.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testGetElementText()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("XMLStreamException in testGetElementText()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testGetElementText()");
        }

    }

    @Test
    public void testRemove() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            XMLEventReader reader = ifac.createXMLEventReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));
            EventReaderDelegate delegate = new EventReaderDelegate(reader);
            delegate.remove();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testRemove()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("XMLStreamException in testRemove()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testRemove()");
        } catch (UnsupportedOperationException e) {
            System.out.println("Expected exception in remove()");
        }

    }

    @Test
    public void testPeek() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            XMLEventReader reader = ifac.createXMLEventReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));
            EventReaderDelegate delegate = new EventReaderDelegate();
            delegate.setParent(reader);
            while (delegate.hasNext()) {
                XMLEvent peekevent = delegate.peek();
                XMLEvent event = (XMLEvent) delegate.next();
                if (peekevent != event) {
                    Assert.fail("peek() does not return same XMLEvent with next()");
                }
            }
            delegate.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testPeek()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("XMLStreamException in testPeek()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testPeek()");
        }
    }

    @Test
    public void testNextTag() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            ifac.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            XMLEventReader reader = ifac.createXMLEventReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));
            EventReaderDelegate delegate = new EventReaderDelegate(reader);
            if ((Boolean) (delegate.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) != Boolean.FALSE) {
                Assert.fail("getProperty() does not return correct value");
            }
            while (delegate.hasNext()) {
                XMLEvent event = delegate.peek();
                if (event.isEndElement() || event.isStartElement()) {
                    XMLEvent nextevent = delegate.nextTag();
                    if (!(nextevent.getEventType() == XMLStreamConstants.START_ELEMENT || nextevent.getEventType() == XMLStreamConstants.END_ELEMENT)) {
                        Assert.fail("nextTag() does not return correct event type");
                    }
                } else {
                    delegate.next();
                }
            }
            delegate.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testNextTag()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("XMLStreamException in testNextTag()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testNextTag()");
        }
    }

    @Test
    public void testNextEvent() {
        try {
            XMLInputFactory ifac = XMLInputFactory.newFactory();
            ifac.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
            XMLEventReader reader = ifac.createXMLEventReader(new FileInputStream(new File(getClass().getResource("toys.xml").getFile())));
            EventReaderDelegate delegate = new EventReaderDelegate();
            delegate.setParent(reader);
            if ((Boolean) (delegate.getParent().getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) != Boolean.FALSE) {
                Assert.fail("XMLEventReader.getProperty() does not return correct value");
            }
            if ((Boolean) (delegate.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) != Boolean.FALSE) {
                Assert.fail("EventReaderDelegate.getProperty() does not return correct value");
            }
            while (delegate.hasNext()) {
                XMLEvent event = delegate.nextEvent();
                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT: {
                        System.out.println(event.asStartElement().getName());
                        break;
                    }
                    case XMLStreamConstants.END_ELEMENT: {
                        System.out.println(event.asEndElement().getName());
                        break;
                    }
                    case XMLStreamConstants.END_DOCUMENT: {
                        System.out.println(event.isEndDocument());
                        break;
                    }
                    case XMLStreamConstants.START_DOCUMENT: {
                        System.out.println(event.isStartDocument());
                        break;
                    }
                    case XMLStreamConstants.CHARACTERS: {
                        System.out.println(event.asCharacters().getData());
                        break;
                    }
                    case XMLStreamConstants.COMMENT: {
                        System.out.println(event.toString());
                        break;
                    }
                }

            }
            delegate.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("FileNotFoundException in testNextEvent()");
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("XMLStreamException in testNextEvent()");
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
            Assert.fail("FactoryConfigurationError in testNextEvent()");
        }

    }
}
