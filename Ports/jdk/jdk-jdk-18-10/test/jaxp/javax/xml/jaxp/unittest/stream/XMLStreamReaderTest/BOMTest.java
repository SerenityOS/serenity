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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6218794
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.BOMTest
 * @run testng/othervm stream.XMLStreamReaderTest.BOMTest
 * @summary Test XMLStreamReader parses BOM UTF-8 and BOM UTF-16 big endian stream.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class BOMTest {
    // UTF-8 BOM test file
    private static final String INPUT_FILE1 = "UTF8-BOM.xml.data";
    // UTF-16 Big Endian test file
    private static final String INPUT_FILE2 = "UTF16-BE.wsdl.data";

    @Test
    public void testBOM() {
        XMLInputFactory ifac = XMLInputFactory.newInstance();
        try {
            XMLStreamReader re = ifac.createXMLStreamReader(this.getClass().getResource(INPUT_FILE1).toExternalForm(),
                        util.BOMInputStream.createStream("UTF-8", this.getClass().getResourceAsStream(INPUT_FILE1)));
            while (re.hasNext()) {
                int event = re.next();
            }
            XMLStreamReader re2 = ifac.createXMLStreamReader(this.getClass().getResource(INPUT_FILE2).toExternalForm(),
                        util.BOMInputStream.createStream("UTF-16BE", this.getClass().getResourceAsStream(INPUT_FILE2)));
            while (re2.hasNext()) {

                int event = re2.next();

            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }
}
