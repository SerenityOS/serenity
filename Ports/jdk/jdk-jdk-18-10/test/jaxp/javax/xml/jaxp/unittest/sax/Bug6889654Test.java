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

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6889654
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow sax.Bug6889654Test
 * @run testng/othervm sax.Bug6889654Test
 * @summary Test SAXException includes whole information.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6889654Test {

    final String MSG = "Failed to parse XML";

    @Test
    public void testException() {
        try {
            parse();
        } catch (SAXException e) {
            // e.printStackTrace();
            String msg = e.toString();
            if (msg.indexOf("systemId") == -1) {
                Assert.fail("CR6889654 -- details should be returned.");
            }
            if (msg.indexOf(MSG) == -1) {
                Assert.fail("CR6889649 -- additional error message not returned.");
            }
            System.out.println("error message:\n" + msg);
        }
    }

    void parse() throws SAXException {
        String xml = "<data>\n<broken/>\u0000</data>";

        try {
            InputSource is = new InputSource(new StringReader(xml));
            is.setSystemId("file:///path/to/some.xml");
            // notice that exception thrown here doesn't include the line number
            // information when reported by JVM -- CR6889654
            SAXParserFactory.newInstance().newSAXParser().parse(is, new DefaultHandler());
        } catch (SAXException e) {
            // notice that this message isn't getting displayed -- CR6889649
            throw new SAXException(MSG, e);
        } catch (ParserConfigurationException pce) {

        } catch (IOException ioe) {

        }

    }

}
