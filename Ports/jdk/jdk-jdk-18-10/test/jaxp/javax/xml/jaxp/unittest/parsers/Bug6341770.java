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

import static jaxp.library.JAXPTestUtilities.USER_DIR;
import static jaxp.library.JAXPTestUtilities.tryRunWithTmpPermission;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.PropertyPermission;

import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/*
 * @test
 * @bug 6341770
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6341770
 * @run testng/othervm parsers.Bug6341770
 * @summary Test external entity linked to non-ASCII base URL.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6341770 {

    // naming a file "aux" would fail on windows.
    @Test
    public void testNonAsciiURI() {
        if (!isNonAsciiSupported()) {
            // @bug 8167478
            // if it doesn't support non-ascii, the following test is invalid even if test is passed.
            System.out.println("Current environment doesn't support non-ascii, exit the test.");
            return;
        }
        try {
            File dir = new File(USER_DIR + ALPHA);
            dir.delete();
            dir.mkdir();
            File main = new File(dir, "main.xml");
            PrintWriter w = new PrintWriter(new FileWriter(main));
            w.println("<!DOCTYPE r [<!ENTITY aux SYSTEM \"aux1.xml\">]>");
            w.println("<r>&aux;</r>");
            w.flush();
            w.close();
            File aux = new File(dir, "aux1.xml");
            w = new PrintWriter(new FileWriter(aux));
            w.println("<x/>");
            w.flush();
            w.close();
            System.out.println("Parsing: " + main);
            tryRunWithTmpPermission(
                    () -> SAXParserFactory.newInstance().newSAXParser().parse(main, new DefaultHandler() {
                        public void startElement(String uri, String localname, String qname, Attributes attr)
                                throws SAXException {
                            System.out.println("encountered <" + qname + ">");
                        }
                    }), new PropertyPermission("user.dir", "read"));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception: " + e.getMessage());
        }
        System.out.println("OK.");
    }

    private boolean isNonAsciiSupported() {
        // Use Paths.get method to test if the path is valid in current environment
        try {
            Paths.get(ALPHA);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Select alpha because it's a very common non-ascii character in different charsets.
    // That this test can run in as many as possible environments if it's possible.
    private static final String ALPHA = "\u03b1";
}
