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

package stream.XMLEventReaderTest;

import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.sun.org.apache.xerces.internal.impl.XMLEntityManager;

/*
 * @test
 * @bug 8153781
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug8153781
 * @run testng/othervm stream.XMLEventReaderTest.Bug8153781
 * @summary Test if method skipDTD of class XMLDTDScannerImpl will correctly skip the DTD section,
 *          even if a call to XMLEntityScanner.scanData for skipping to the closing ']' returns true.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug8153781 {
    public static int DOCTYPE_SECTION_LENGTH = XMLEntityManager.DEFAULT_BUFFER_SIZE * 2;
    public static int DOCUMENT_LENGTH = DOCTYPE_SECTION_LENGTH + 4096;

    public String createXMLDocument(int doctypeoffset) {
        StringBuilder xmlcontentbuilder = new StringBuilder(DOCUMENT_LENGTH);
        xmlcontentbuilder.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\r\n");
        xmlcontentbuilder.append("<!DOCTYPE dummy [\r\n");
        xmlcontentbuilder.append("  <!ELEMENT dummy EMPTY>\r\n");
        xmlcontentbuilder.append("  <!--\r\n");
        int doctypelines = DOCTYPE_SECTION_LENGTH / 3;
        for (int i = 0; i < doctypeoffset; i++)
            xmlcontentbuilder.append('a');
        for (int i = 0; i < doctypelines; i++)
            xmlcontentbuilder.append("a\r\n");
        xmlcontentbuilder.append("  -->\r\n");
        xmlcontentbuilder.append("  ]\r\n");
        xmlcontentbuilder.append(">\r\n");
        xmlcontentbuilder.append("<dummy>\r\n");
        xmlcontentbuilder.append("</dummy>\r\n");
        System.out.println("Document length:" + xmlcontentbuilder.length());
        return xmlcontentbuilder.toString();
    }

    public void runReader(XMLInputFactory factory, int offset) throws XMLStreamException {
        StringReader stringReader = new StringReader(createXMLDocument(offset));
        XMLEventReader reader = factory.createXMLEventReader(stringReader);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            System.out.println("Event Type: " + event.getEventType());
        }
    }

    @Test
    public void test() {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            for (int i = 0; i < 3; i++) {
                runReader(factory, i);
            }
        } catch (XMLStreamException xe) {
            xe.printStackTrace();
            Assert.fail(xe.getMessage());
        }
    }
}
