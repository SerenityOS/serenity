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

package sax;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6949607
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.Bug6949607Test
 * @run testng/othervm sax.Bug6949607Test
 * @summary Test Attributes.getValue returns null when parameter uri is empty.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6949607Test {

    final String MSG = "Failed to parse XML";
    String textXML = "<prefix:rootElem xmlns:prefix=\"something\" prefix:attr=\"attrValue\" />";

    @Test
    public void testException() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(new ByteArrayInputStream(textXML.getBytes()), new TestFilter());

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    class TestFilter extends DefaultHandler {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(uri, localName, qName, atts);

            String attr_WithNs = atts.getValue("something", "attr");
            String attr_NoNs = atts.getValue("", "attr");

            System.out.println("withNs: " + attr_WithNs);
            System.out.println("NoNs: " + attr_NoNs);

            Assert.assertTrue(attr_NoNs == null, "Should return null when uri is empty.");

        }
    }

}
