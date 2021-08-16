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

package stream.XMLInputFactoryTest;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6909759
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLInputFactoryTest.Bug6909759Test
 * @run testng/othervm stream.XMLInputFactoryTest.Bug6909759Test
 * @summary Test createXMLStreamReader with StreamSource.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6909759Test {


    @Test
    public void testCreateXMLStreamReader() {

        try {
            StreamSource ss = new StreamSource(getClass().getResourceAsStream("play.xml"));
            XMLInputFactory xif = XMLInputFactory.newInstance();
            // File file = new File("./tests/XMLStreamReader/sgml.xml");
            // FileInputStream inputStream = new FileInputStream(file);
            XMLStreamReader xsr;
            xsr = xif.createXMLStreamReader(ss);

            while (xsr.hasNext()) {
                int eventType = xsr.next();
            }

        } catch (UnsupportedOperationException oe) {
            Assert.fail("StreamSource should be supported");
        } catch (XMLStreamException ex) {
            Assert.fail("fix the test");
        }
    }
}
