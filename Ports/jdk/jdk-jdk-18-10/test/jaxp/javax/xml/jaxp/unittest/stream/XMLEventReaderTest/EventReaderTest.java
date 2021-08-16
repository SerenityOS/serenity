/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.NoSuchElementException;
import javax.xml.stream.XMLEventReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 8204329 8256515
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng stream.XMLEventReaderTest.EventReaderTest
 * @summary Tests XMLEventReader
 */
@Listeners({jaxp.library.BasePolicy.class})
public class EventReaderTest {
    @Test(expectedExceptions = NoSuchElementException.class)
    public void testNextEvent() throws Exception {
        XMLEventReader eventReader = XMLInputFactory.newFactory().createXMLEventReader(
                new StringReader("<?xml version='1.0'?><foo/>"));

        while (eventReader.hasNext()) {
            eventReader.nextEvent();
        }
        // no more event
        eventReader.nextEvent();
    }

    @DataProvider
    Object[][] standaloneSetTestData() {
        return new Object[][]{
                {"<?xml version=\"1.0\"?>", false, false},
                {"<?xml version=\"1.0\" standalone=\"no\"?>", false, true},
                {"<?xml version=\"1.0\" standalone=\"yes\"?>", true, true}
        };
    }

    @Test(dataProvider = "standaloneSetTestData")
    void testStandaloneSet(String xml, boolean standalone, boolean standaloneSet) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = factory.createXMLEventReader(new StringReader(xml));
        StartDocument startDocumentEvent = (StartDocument) reader.nextEvent();

        assertEquals(startDocumentEvent.isStandalone(), standalone);
        assertEquals(startDocumentEvent.standaloneSet(), standaloneSet);
    }
}
