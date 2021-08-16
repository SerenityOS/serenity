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

import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.XMLReader;

/*
 * @test
 * @bug 6506304
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6506304Test
 * @run testng/othervm parsers.Bug6506304Test
 * @summary Test MalformedURLException: unknown protocol won't be thrown when there is a space within the full path file name.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6506304Test {
    public static boolean isWindows = false;
    static {
        if (getSystemProperty("os.name").indexOf("Windows") > -1) {
            isWindows = true;
        }
    };

    @Test
    public void testPath() throws Exception {
        if (isWindows) {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setNamespaceAware(true);
                SAXParser jaxpParser = factory.newSAXParser();
                XMLReader reader = jaxpParser.getXMLReader();
                reader.parse("C:/space error/x.xml");
                System.exit(0);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (e.getMessage().equalsIgnoreCase("unknown protocol: c")) {
                    Assert.fail("jdk5 allowed the above form");
                } else if (e.getMessage().indexOf("(The system cannot find the path specified)") > 0) {
                    // expected
                }
            }
        }
    }
}
