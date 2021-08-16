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

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6976938
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.Bug6976938Test
 * @run testng/othervm stream.Bug6976938Test
 * @summary Test StAX parser won't throw StackOverflowError while reading valid XML file, in case the text content of an XML element contains many lines like "&lt; ... &gt;".
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6976938Test {

    private static final String INPUT_FILE = "Bug6976938.xml";

    public static final String VF_GENERIC_TT_NAMESPACE = "http://www.vodafone.com/oss/xml/TroubleTicket";

    public static final QName ATTACHMENT_NAME = new QName(VF_GENERIC_TT_NAMESPACE, "attachment");

    @Test
    public void testEventReader() {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        eventReaderTest(xif);
    }

    @Test
    public void testEventReader1() {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        eventReaderTest(xif);
    }

    public void eventReaderTest(XMLInputFactory xif) {
        XMLEventReader eventReader = null;
        try {
            eventReader = xif.createXMLEventReader(this.getClass().getResourceAsStream(INPUT_FILE));
            XMLEventReader filteredEventReader = xif.createFilteredReader(eventReader, new EventFilter() {
                public boolean accept(XMLEvent event) {
                    if (!event.isStartElement()) {
                        return false;
                    }
                    QName elementQName = event.asStartElement().getName();
                    if ((elementQName.getLocalPart().equals(ATTACHMENT_NAME.getLocalPart()) || elementQName.getLocalPart().equals("Attachment"))
                            && elementQName.getNamespaceURI().equals(VF_GENERIC_TT_NAMESPACE)) {
                        return true;
                    }
                    return false;
                }
            });
            if (filteredEventReader.hasNext()) {
                System.out.println("containsAttachments() returns true");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());

        } finally {
            if (eventReader != null) {
                try {
                    eventReader.close();
                } catch (XMLStreamException xse) {
                    // Ignored by intention
                }
            }
        }
    }

}
