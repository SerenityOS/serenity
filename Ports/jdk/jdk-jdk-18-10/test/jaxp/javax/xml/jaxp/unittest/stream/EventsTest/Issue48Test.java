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
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6620632
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.EventsTest.Issue48Test
 * @run testng/othervm stream.EventsTest.Issue48Test
 * @summary Test XMLEventReader can parse notation and entity information from DTD Event.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Issue48Test {

    public java.io.File input;
    public final String filesDir = "./";
    protected XMLInputFactory inputFactory;
    protected XMLOutputFactory outputFactory;

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
            List entities = dtd.getEntities();
            if (entities == null) {
                Assert.fail("No entity found. Expected 3.");
            } else {
                Assert.assertEquals(entities.size(), 3);
            }
            // Let's also verify they are all of right type...
            testListElems(entities, EntityDeclaration.class);

            List notations = dtd.getNotations();
            if (notations == null) {
                Assert.fail("No notation found. Expected 2.");
            } else {
                Assert.assertEquals(notations.size(), 2);
            }
            // Let's also verify they are all of right type...
            testListElems(notations, NotationDeclaration.class);
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


    private void testListElems(List l, Class expType) {
        Iterator it = l.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            Assert.assertNotNull(o);
            Assert.assertTrue(expType.isAssignableFrom(o.getClass()));
        }
    }

}
