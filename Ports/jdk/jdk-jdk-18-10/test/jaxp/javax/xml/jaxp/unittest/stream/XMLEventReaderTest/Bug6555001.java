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

import java.io.FilePermission;
import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.XMLEvent;

import jaxp.library.JAXPTestUtilities;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6555001
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLEventReaderTest.Bug6555001
 * @run testng/othervm stream.XMLEventReaderTest.Bug6555001
 * @summary Test StAX parser replaces the entity reference as setting.
 */
@Listeners({ jaxp.library.BasePolicy.class })
public class Bug6555001 {
    private static final String XML = ""
            + "<!DOCTYPE doc SYSTEM 'file:///tmp/this/does/not/exist/but/that/is/ok' ["
            + "<!ENTITY def '<para/>'>" + "]>" + "<doc>&def;&undef;</doc>";

    @Test
    public void testReplacing() throws Exception {
        JAXPTestUtilities.tryRunWithTmpPermission(() -> {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty("javax.xml.stream.isReplacingEntityReferences", true);

            StringReader sr = new StringReader(XML);
            XMLEventReader reader = factory.createXMLEventReader(sr);

            boolean sawUndef = false;
            boolean sawDef = false;

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                // System.out.println("Event: " + event);
                if (event.isEntityReference()) {
                    EntityReference ref = (EntityReference) event;
                    if ("def".equals(ref.getName())) {
                        sawDef = true;
                    } else if ("undef".equals(ref.getName())) {
                        sawUndef = true;
                    } else {
                        throw new IllegalArgumentException("Unexpected entity name");
                    }
                }
            }

            Assert.assertEquals(false, sawDef);
            Assert.assertEquals(true, sawUndef);
            reader.close();
        }, new FilePermission("/tmp/this/does/not/exist/but/that/is/ok", "read"));
    }

    @Test
    public void testNotReplacing() throws Exception {
        JAXPTestUtilities.tryRunWithTmpPermission(() -> {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty("javax.xml.stream.isReplacingEntityReferences", false);

            StringReader sr = new StringReader(XML);
            XMLEventReader reader = factory.createXMLEventReader(sr);

            boolean sawUndef = false;
            boolean sawDef = false;

            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                // System.out.println("Event: " + event);
                if (event.isEntityReference()) {
                    EntityReference ref = (EntityReference) event;
                    if ("def".equals(ref.getName())) {
                        sawDef = true;
                    } else if ("undef".equals(ref.getName())) {
                        sawUndef = true;
                    } else {
                        throw new IllegalArgumentException("Unexpected entity name");
                    }
                }
            }

            Assert.assertEquals(true, sawDef);
            Assert.assertEquals(true, sawUndef);
            reader.close();
        }, new FilePermission("/tmp/this/does/not/exist/but/that/is/ok", "read"));
    }
}
