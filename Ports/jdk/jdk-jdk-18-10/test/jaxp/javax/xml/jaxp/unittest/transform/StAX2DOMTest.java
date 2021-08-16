/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

/*
 * @test
 * @bug 8016914
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng transform.StAX2DOMTest
 * @summary Verifies transforming a StAXSource to a DOMResult.
 */
public class StAX2DOMTest {
    /**
     * Data files for test.
     * Column(s): xml file
     *
     * @return data for test
     */
    @DataProvider(name = "datafiles")
    public Object[][] getData() {
        return new Object[][] {
            { "StAX2DOMTest.xml"}, //without declaration
            { "StAX2DOMTest1.xml"}, //with declaration
        };
    }

    /**
     * Verifies that transforming a StAX source to a DOM result passes with
     * or without the XML declaration.
     *
     * @param file the XML file
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "datafiles")
    public void test(String file) throws Exception {
        final XMLInputFactory xif = XMLInputFactory.newInstance();
        final XMLStreamReader xsr = xif.createXMLStreamReader(
                this.getClass().getResourceAsStream(file));
        xsr.nextTag(); // Advance to statements element

        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer t = tf.newTransformer();
        while(xsr.nextTag() == XMLStreamConstants.START_ELEMENT) {
            final DOMResult result = new DOMResult();
            t.transform(new StAXSource(xsr), result);
            final Node domNode = result.getNode();
            System.out.println(domNode);
        }
    }
}
