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

package stream.XMLEventReaderTest;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.Assert;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/*
 * @test
 * @bug 6613059
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug6613059Test
 * @run testng/othervm stream.XMLEventReaderTest.Bug6613059Test
 * @summary Test XMLEventReader.nextTag() shall update internal event state, same as 6586466.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6613059Test {

    @Test
    public void test() {
        String xmlFile = "bug6613059.xml";
        XMLEventReader xer = null;
        XMLInputFactory xif = XMLInputFactory.newInstance();
        try {
            xer = xif.createXMLEventReader(xif.createXMLStreamReader(getClass().getResource(xmlFile).getFile(), getClass().getResourceAsStream(xmlFile)));
        } catch (XMLStreamException e) {
            System.out.println("Error while reading XML: " + e.getClass().getName() + " " + e.getMessage());
        }

        try {
            while (xer.hasNext()) {
                XMLEvent event = xer.nextTag();
                if (event.isEndElement() && event.asEndElement().getName().equals(new QName("menubar"))) {
                    break;
                }

                if (event.asStartElement().getName().equals(new QName("menu"))) {
                    // nextTag should be used when processing element-only
                    // content, assuming "addMenu" in
                    // the user's code handles the menu part properly
                    addMenu(xer, event);
                }

            }
        } catch (XMLStreamException e) {
            Assert.fail("Exception while reading " + xmlFile + ": " + e.getClass().getName() + " " + e.getMessage());
        }
    }

    void addMenu(XMLEventReader xer, XMLEvent event) throws XMLStreamException {
        // user did not submit this part of code, just jump to the end of menu
        // element
        int eventType = 0;
        while (true) {
            event = xer.nextEvent();
            // System.out.println("event: " + event);
            eventType = event.getEventType();
            if (eventType == XMLStreamConstants.END_ELEMENT && event.asEndElement().getName().equals(new QName("menu"))) {
                break;
            }
        }
    }
}
