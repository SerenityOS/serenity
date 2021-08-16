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
import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/*
 * @test
 * @bug 6586466
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug6586466Test
 * @run testng/othervm stream.XMLEventReaderTest.Bug6586466Test
 * @summary Test XMLEventReader.nextTag() shall update internal event state.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6586466Test {

    @Test
    public void test() {
        String xmlData = "<?xml version=\"1.0\"?><Test>Hello</Test>";
        try {
            XMLEventReader xmlReader = XMLInputFactory.newInstance().createXMLEventReader(new ByteArrayInputStream(xmlData.getBytes()));

            XMLEvent event = xmlReader.nextEvent();
            System.out.println(event.getClass());

            // xmlReader.peek(); // error in both cases with/without peek()
            event = xmlReader.nextTag(); // nextEvent() would work fine
            // nextTag() forgets to set fLastEvent
            System.out.println(event.getClass());

            String text = xmlReader.getElementText();
            System.out.println(text);
        } catch (XMLStreamException e) {
            Assert.fail(e.getMessage());
        }
    }

}
