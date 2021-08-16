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

import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLStreamReaderTest.DoubleXmlnsTest
 * @run testng/othervm stream.XMLStreamReaderTest.DoubleXmlnsTest
 * @summary Test double namespaces and nested namespaces.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DoubleXmlnsTest {

    @Test
    public void testDoubleNS() throws Exception {

        final String INVALID_XML = "<foo xmlns:xmli='http://www.w3.org/XML/1998/namespacei' xmlns:xmli='http://www.w3.org/XML/1998/namespacei' />";

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(INVALID_XML));

            while (xsr.hasNext()) {
                xsr.next();
            }

            Assert.fail("Wellformedness error expected: " + INVALID_XML);
        } catch (XMLStreamException e) {
            ; // this is expected
        }
    }

    @Test
    public void testNestedNS() throws Exception {

        final String VALID_XML = "<foo xmlns:xmli='http://www.w3.org/XML/1998/namespacei'><bar xmlns:xmli='http://www.w3.org/XML/1998/namespaceii'></bar></foo>";

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(VALID_XML));

            while (xsr.hasNext()) {
                xsr.next();
            }

            // expected success
        } catch (XMLStreamException e) {
            e.printStackTrace();

            Assert.fail("Wellformedness error is not expected: " + VALID_XML + ", " + e.getMessage());
        }
    }

    @Test
    public void testDoubleXmlns() throws Exception {

        final String INVALID_XML = "<foo xmlns:xml='http://www.w3.org/XML/1998/namespace' xmlns:xml='http://www.w3.org/XML/1998/namespace' ></foo>";

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(INVALID_XML));

            while (xsr.hasNext()) {
                xsr.next();
            }

            Assert.fail("Wellformedness error expected :" + INVALID_XML);
        } catch (XMLStreamException e) {
            ; // this is expected
        }
    }

    @Test
    public void testNestedXmlns() throws Exception {

        final String VALID_XML = "<foo xmlns:xml='http://www.w3.org/XML/1998/namespace'><bar xmlns:xml='http://www.w3.org/XML/1998/namespace'></bar></foo>";

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(VALID_XML));

            while (xsr.hasNext()) {
                xsr.next();
            }

            // expected success
        } catch (XMLStreamException e) {
            e.printStackTrace();
            Assert.fail("Wellformedness error is not expected: " + VALID_XML + ", " + e.getMessage());
        }
    }
}
