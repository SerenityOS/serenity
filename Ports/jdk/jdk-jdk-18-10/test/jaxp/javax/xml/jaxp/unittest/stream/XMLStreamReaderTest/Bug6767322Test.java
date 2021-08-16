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

package stream.XMLStreamReaderTest;

import java.io.ByteArrayInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6767322
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Bug6767322Test
 * @run testng/othervm stream.XMLStreamReaderTest.Bug6767322Test
 * @summary Test XMLStreamReader.getVersion() returns null if a version isn't declared.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6767322Test {
    private static final String INPUT_FILE = "Bug6767322.xml";

    @Test
    public void testVersionSet() {
        try {
            XMLStreamReader r = XMLInputFactory.newInstance().createXMLStreamReader(this.getClass().getResource(INPUT_FILE).toExternalForm(),
                    this.getClass().getResourceAsStream(INPUT_FILE));

            String version = r.getVersion();
            System.out.println("Bug6767322.xml: " + version);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

    @Test
    public void testVersionNotSet() {
        try {
            String xmlText = "Version not declared";
            XMLStreamReader r = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(xmlText.getBytes()));
            String version = r.getVersion();
            System.out.println("Version for text \"" + xmlText + "\": " + version);
            if (version != null) {
                Assert.fail("getVersion should return null");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }
}
