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

import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EventsTest.Issue58Test
 * @run testng/othervm stream.EventsTest.Issue58Test
 * @summary Test XMLEvent.getLocation() returns a non-volatile Location.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Issue58Test {

    public java.io.File input;
    public final String filesDir = "./";
    protected XMLInputFactory inputFactory;
    protected XMLOutputFactory outputFactory;

    @Test
    public void testLocation() {
        String XML = "<?xml version='1.0' ?>" + "<!DOCTYPE root [\n" + "<!ENTITY intEnt 'internal'>\n" + "<!ENTITY extParsedEnt SYSTEM 'url:dummy'>\n"
                + "<!NOTATION notation PUBLIC 'notation-public-id'>\n" + "<!NOTATION notation2 SYSTEM 'url:dummy'>\n"
                + "<!ENTITY extUnparsedEnt SYSTEM 'url:dummy2' NDATA notation>\n" + "]>\n" + "<root />";

        try {
            XMLEventReader er = getReader(XML);
            XMLEvent evt = er.nextEvent(); // StartDocument
            Location loc1 = evt.getLocation();
            System.out.println("Location 1: " + loc1.getLineNumber() + "," + loc1.getColumnNumber());
            evt = er.nextEvent(); // DTD
            // loc1 should not change so its line number should still be 1
            Assert.assertTrue(loc1.getLineNumber() == 1);
            Location loc2 = evt.getLocation();
            System.out.println("Location 2: " + loc2.getLineNumber() + "," + loc2.getColumnNumber());
            evt = er.nextEvent(); // root
            System.out.println("Location 1: " + loc1.getLineNumber() + "," + loc1.getColumnNumber());
            Assert.assertTrue(loc1.getLineNumber() == 1);
            Assert.assertTrue(loc2.getLineNumber() == 7);
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

}
