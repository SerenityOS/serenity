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

package stream.EventsTest;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6631268
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EventsTest.Issue41Test
 * @run testng/othervm stream.EventsTest.Issue41Test
 * @summary Test XMLEvent.writeAsEncodedUnicode can output the event content.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Issue41Test {

    public java.io.File input;
    public final String filesDir = "./";
    protected XMLInputFactory inputFactory;
    protected XMLOutputFactory outputFactory;

    @Test
    public void testEvents() {
        XMLEventFactory f = XMLEventFactory.newInstance();
        final String contents = "test <some> text & more! [[]] --";
        final String prefix = "prefix";
        final String uri = "http://foo";
        final String localName = "elem";

        try {
            StartDocument sd = f.createStartDocument();
            writeAsEncodedUnicode(sd);

            Comment c = f.createComment("some comments");
            writeAsEncodedUnicode(c);

            StartElement se = f.createStartElement(prefix, uri, localName);

            ProcessingInstruction pi = f.createProcessingInstruction("target", "data");
            writeAsEncodedUnicode(pi);

            Namespace ns = f.createNamespace(prefix, uri);
            writeAsEncodedUnicode(ns);

            Characters characters = f.createCharacters(contents);
            writeAsEncodedUnicode(characters);
            // CData
            Characters cdata = f.createCData(contents);
            writeAsEncodedUnicode(cdata);

            // Attribute
            QName attrName = new QName("http://test.com", "attr", "ns");
            Attribute attr = f.createAttribute(attrName, "value");
            writeAsEncodedUnicode(attr);

            // prefix, uri, localName
            EndElement ee = f.createEndElement(prefix, uri, localName);
            writeAsEncodedUnicode(ee);

            EndDocument ed = f.createEndDocument();
            writeAsEncodedUnicode(ed);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     * DTDEvent instances constructed via event reader are missing the notation
     * and entity declaration information
     */
    @Test
    public void testDTDEvent() {
        String XML = "<?xml version='1.0' ?>" + "<!DOCTYPE root [\n" + "<!ENTITY intEnt 'internal'>\n" + "<!ENTITY extParsedEnt SYSTEM 'url:dummy'>\n"
                + "<!NOTATION notation PUBLIC 'notation-public-id'>\n" + "<!NOTATION notation2 SYSTEM 'url:dummy'>\n"
                + "<!ENTITY extUnparsedEnt SYSTEM 'url:dummy2' NDATA notation>\n" + "]>" + "<root />";

        try {
            XMLEventReader er = getReader(XML);
            XMLEvent evt = er.nextEvent(); // StartDocument
            evt = er.nextEvent(); // DTD
            if (evt.getEventType() != XMLStreamConstants.DTD) {
                Assert.fail("Expected DTD event");
            }
            DTD dtd = (DTD) evt;
            writeAsEncodedUnicode(dtd);
            List entities = dtd.getEntities();
            if (entities == null) {
                Assert.fail("No entity found. Expected 3.");
            } else {
                writeAsEncodedUnicode((XMLEvent) entities.get(0));
                writeAsEncodedUnicode((XMLEvent) entities.get(1));
                writeAsEncodedUnicode((XMLEvent) entities.get(2));
            }

            List notations = dtd.getNotations();
            if (notations == null) {
                Assert.fail("No notation found. Expected 2.");
            } else {
                writeAsEncodedUnicode((XMLEvent) notations.get(0));
                writeAsEncodedUnicode((XMLEvent) notations.get(1));
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private XMLEventReader getReader(String XML) throws Exception {
        inputFactory = XMLInputFactory.newInstance();

        // Check if event reader returns the correct event
        XMLEventReader er = inputFactory.createXMLEventReader(new StringReader(XML));
        return er;
    }



    /**
     * The return of XMLEvent writeAsEncodedUnicode method is not defined This
     * method merely tests that the output exists
     */
    public void writeAsEncodedUnicode(XMLEvent evt) throws XMLStreamException {
        if (evt.getEventType() == XMLStreamConstants.END_DOCUMENT) {
            return;
        }
        StringWriter sw = new StringWriter();
        evt.writeAsEncodedUnicode(sw);

        Assert.assertTrue(sw.toString().length() > 0);
        System.out.println(sw.toString());
    }
}
