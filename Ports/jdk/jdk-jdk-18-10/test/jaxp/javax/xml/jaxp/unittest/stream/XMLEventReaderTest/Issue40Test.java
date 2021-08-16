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

import java.io.File;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Issue40Test
 * @run testng/othervm stream.XMLEventReaderTest.Issue40Test
 * @summary Test XMLEventReader.getElementText() works after calling peek().
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Issue40Test {

    public java.io.File input;
    public final String filesDir = "./";
    protected XMLInputFactory inputFactory;
    protected XMLOutputFactory outputFactory;

    /**
     * test without peek
     */
    @Test
    public void testWOPeek() {
        try {
            XMLEventReader er = getReader();
            XMLEvent e = er.nextEvent();
            Assert.assertEquals(e.getEventType(), XMLStreamConstants.START_DOCUMENT);
            // we have two start elements in this file
            Assert.assertEquals(er.nextEvent().getEventType(), XMLStreamConstants.START_ELEMENT);
            Assert.assertEquals(er.nextEvent().getEventType(), XMLStreamConstants.START_ELEMENT);
            System.out.println(er.getElementText());

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * test with peek
     */
    @Test
    public void testWPeek() {
        try {
            XMLEventReader er = getReader();
            XMLEvent e = er.nextEvent();
            Assert.assertEquals(e.getEventType(), XMLStreamConstants.START_DOCUMENT);
            // we have two start elements in this file
            while (er.peek().getEventType() == XMLStreamConstants.START_ELEMENT) {
                e = er.nextEvent();
            }
            Assert.assertEquals(e.getEventType(), XMLStreamConstants.START_ELEMENT);
            System.out.println(er.getElementText());

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private XMLEventReader getReader() throws Exception {
        inputFactory = XMLInputFactory.newInstance();
        input = new File(getClass().getResource("play.xml").getFile());

        // Check if event reader returns the correct event
        XMLEventReader er = inputFactory.createXMLEventReader(inputFactory.createXMLStreamReader(new java.io.FileInputStream(input), "UTF-8"));
        return er;
    }

}
