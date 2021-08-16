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

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 7166896
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug7166896Test
 * @run testng/othervm parsers.Bug7166896Test
 * @summary Test DocumentBuilder.parse(String uri) supports IPv6 format.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug7166896Test {

    @Test
    public void test() throws Exception {
        final String url = "http://[fe80::la03:73ff:fead:f7b0]/note.xml";
        final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder;
        Document doc = null;
        System.out.println("URL is " + url);
        try {
            builder = domFactory.newDocumentBuilder();
            // here comes the MalformedURLException. With Java6 / 7 it looks
            // like this:
            // java.net.MalformedURLException: For input string:
            // ":la03:73ff:fead:f7b0%5D"
            // which is not fine.
            // with xerces 2.11.0 it complains about a non-existing host, which
            // is fine
            System.out.println("passing URL to DocumentBuilder.parse()");
            doc = builder.parse(url);

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            String em = e.getMessage();
            System.err.println("Error message: " + em);
            if (em.contains("For input string: \":la03:73ff:fead:f7b0%5D\"")) {
                Assert.fail("failed to accept IPv6 address");
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

    }
}
