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

package parsers;

import java.io.FileReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6518733
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6518733
 * @run testng/othervm parsers.Bug6518733
 * @summary Test SAX parser handles several attributes that each contain a newline within the attribute value.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6518733 {

    @Test
    public void test() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(new FileReader(getClass().getResource("Bug6518733.xml").getFile())), new Handler());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static class Handler extends org.xml.sax.helpers.DefaultHandler {
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            // Make sure that the value of attribute q7 is "7 G"
            if (qName.equals("obj")) {
                Assert.assertTrue(attrs.getValue("", "q7").equals("7 G"));
            }
        }
    }

}
