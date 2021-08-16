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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6489890
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug6489890
 * @run testng/othervm stream.XMLEventReaderTest.Bug6489890
 * @summary Test XMLEventReader's initial state is an undefined state, and nextEvent() is START_DOCUMENT.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6489890 {

    @Test
    public void test0() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();

            XMLStreamReader xsr = xif.createXMLStreamReader(getClass().getResource("sgml.xml").toString(), getClass().getResourceAsStream("sgml.xml"));

            XMLEventReader xer = xif.createXMLEventReader(xsr);

            Assert.assertTrue(xer.peek().getEventType() == XMLEvent.START_DOCUMENT);
            Assert.assertTrue(xer.peek() == xer.nextEvent());
            xsr.close();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void test1() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();

            XMLStreamReader xsr = xif.createXMLStreamReader(getClass().getResource("sgml.xml").toString(), getClass().getResourceAsStream("sgml.xml"));

            XMLEventReader xer = xif.createXMLEventReader(xsr);

            Assert.assertTrue(xer.nextEvent().getEventType() == XMLEvent.START_DOCUMENT);
            xsr.close();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
