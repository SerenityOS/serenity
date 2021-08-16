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

import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 4985486
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug4985486
 * @run testng/othervm parsers.Bug4985486
 * @summary Test SAXParser can parse large characters(more than 10000).
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug4985486 {

    @Test
    public void test1() throws Exception {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        System.out.println(spf.getClass().getName());
        spf.setNamespaceAware(true);
        spf.newSAXParser().parse(Bug4985486.class.getResourceAsStream("Bug4985486.xml"), new Handler());
    }

    private class Handler extends DefaultHandler {
        StringBuffer buf = new StringBuffer();

        public void characters(char[] ch, int start, int length) throws SAXException {
            buf.append(ch, start, length);
        }

        public void endDocument() throws SAXException {
            String contents = buf.toString();
            Assert.assertTrue(contents.endsWith("[END]"));
            while (contents.length() >= 10) {
                Assert.assertTrue(contents.startsWith("0123456789"));
                contents = contents.substring(10);
            }
        }

    }
}
