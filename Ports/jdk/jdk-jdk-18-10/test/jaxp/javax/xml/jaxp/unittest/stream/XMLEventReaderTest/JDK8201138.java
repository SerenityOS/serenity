/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

import static org.testng.Assert.assertTrue;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8201138
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.JDK8201138
 * @run testng/othervm stream.XMLEventReaderTest.JDK8201138
 * @summary Verifies a fix that set the type and data properly in the loop
 */
@Listeners({jaxp.library.BasePolicy.class})
public class JDK8201138 {

    @Test
    public void testTypeReset() throws XMLStreamException, FactoryConfigurationError {

        String xmlData = "<?xml version=\"1.0\"?><nextEvent><!-- peeked -->aaa<![CDATA[bbb]]>ccc</nextEvent>";

        XMLEventReader eventReader = XMLInputFactory.newFactory().createXMLEventReader(new StringReader(xmlData));
        assertTrue(eventReader.nextEvent() instanceof StartDocument, "shall be StartDocument");
        assertTrue(eventReader.nextEvent() instanceof StartElement, "shall be StartElement");
        assertTrue(eventReader.peek() instanceof Comment, "shall be Comment");
        // the following returns empty string before the fix
        assertTrue(eventReader.getElementText().equals("aaabbbccc"), "The text shall be \"aaabbbccc\"");

        eventReader.close();
    }

    @Test
    public void testTypeResetAndBufferClear() throws XMLStreamException, FactoryConfigurationError {

        String xmlData = "<?xml version=\"1.0\"?><nextEvent>aaa<!-- comment --></nextEvent>";

        XMLEventReader eventReader = XMLInputFactory.newFactory().createXMLEventReader(new StringReader(xmlData));
        assertTrue(eventReader.nextEvent() instanceof StartDocument, "shall be StartDocument");
        assertTrue(eventReader.nextEvent() instanceof StartElement, "shall be StartElement");
        assertTrue(eventReader.peek() instanceof Characters, "shall be Characters");
        // the following throws ClassCastException before the fix
        assertTrue(eventReader.getElementText().equals("aaa"), "The text shall be \"aaa\"");

        eventReader.close();
    }

}
