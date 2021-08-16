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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.Jsr173MR1Req8Test
 * @run testng/othervm stream.XMLStreamReaderTest.Jsr173MR1Req8Test
 * @summary Test XMLStreamReader parses attribute with namespace aware.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Jsr173MR1Req8Test {

    private static final String INPUT_FILE1 = "Jsr173MR1Req8.xml";

    @Test
    public void testDefaultAttrNS() {
        XMLInputFactory ifac = XMLInputFactory.newInstance();

        try {
            XMLStreamReader re = ifac.createXMLStreamReader(getClass().getResource(INPUT_FILE1).toExternalForm(),
                    this.getClass().getResourceAsStream(INPUT_FILE1));
            while (re.hasNext()) {
                int event = re.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    // System.out.println("#attrs = " + re.getAttributeCount());
                    Assert.assertTrue(re.getAttributeCount() == 2);
                    // This works if "" is replaced by null too
                    // System.out.println("attr1 = " + re.getAttributeValue("",
                    // "attr1"));
                    Assert.assertTrue(re.getAttributeValue("", "attr1").equals("pass"));
                }
            }
            re.close();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occured: " + e.getMessage());
        }
    }

}
