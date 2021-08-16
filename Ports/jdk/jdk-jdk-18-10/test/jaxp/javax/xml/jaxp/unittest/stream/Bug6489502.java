/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6489502
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.Bug6489502
 * @run testng/othervm stream.Bug6489502
 * @summary Test XMLInputFactory works correctly in case it repeats to create reader.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6489502 {

    public java.io.File input;
    public final String filesDir = "./";
    protected XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    protected XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

    private static String xml = "<?xml version=\"1.0\"?><PLAY><TITLE>The Tragedy of Hamlet, Prince of Denmark</TITLE></PLAY>";

    @Test
    public void testEventReader1() {
        try {
            // Check if event reader returns the correct event
            XMLEventReader e1 = inputFactory.createXMLEventReader(inputFactory.createXMLStreamReader(new java.io.StringReader(xml)));
            Assert.assertEquals(e1.peek().getEventType(), XMLStreamConstants.START_DOCUMENT);

            // Repeat same steps to test factory state
            XMLEventReader e2 = inputFactory.createXMLEventReader(inputFactory.createXMLStreamReader(new java.io.StringReader(xml)));
            Assert.assertEquals(e2.peek().getEventType(), XMLStreamConstants.START_DOCUMENT);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEventReader2() {
        try {
            // Now advance underlying reader and then call peek on event reader
            XMLStreamReader s1 = inputFactory.createXMLStreamReader(new java.io.StringReader(xml));
            Assert.assertEquals(s1.getEventType(), XMLStreamConstants.START_DOCUMENT);
            s1.next();
            s1.next(); // advance to <TITLE>
            Assert.assertTrue(s1.getLocalName().equals("TITLE"));

            XMLEventReader e3 = inputFactory.createXMLEventReader(s1);
            Assert.assertEquals(e3.peek().getEventType(), XMLStreamConstants.START_ELEMENT);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
