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

package stream.XMLOutputFactoryTest;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6846132
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLOutputFactoryTest.Bug6846132Test
 * @run testng/othervm stream.XMLOutputFactoryTest.Bug6846132Test
 * @summary Test createXMLStreamWriter with SAXResult won't throw a NullPointerException.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6846132Test {

    @Test
    public void testSAXResult() {
        DefaultHandler handler = new DefaultHandler();

        final String EXPECTED_OUTPUT = "<?xml version=\"1.0\"?><root></root>";
        try {
            SAXResult saxResult = new SAXResult(handler);
            // saxResult.setSystemId("jaxp-ri/unit-test/javax/xml/stream/XMLOutputFactoryTest/cr6846132.xml");
            XMLOutputFactory ofac = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = ofac.createXMLStreamWriter(saxResult);
            writer.writeStartDocument("1.0");
            writer.writeStartElement("root");
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (Exception e) {
            if (e instanceof UnsupportedOperationException) {
                // expected
            } else {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
        }
    }

    @Test
    public void testSAXResult1() {
        DefaultHandler handler = new DefaultHandler();

        try {
            SAXResult saxResult = new SAXResult(handler);
            XMLOutputFactory ofac = XMLOutputFactory.newInstance();
            XMLEventWriter writer = ofac.createXMLEventWriter(saxResult);
        } catch (Exception e) {
            if (e instanceof UnsupportedOperationException) {
                // expected
            } else {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
        }
    }

}
