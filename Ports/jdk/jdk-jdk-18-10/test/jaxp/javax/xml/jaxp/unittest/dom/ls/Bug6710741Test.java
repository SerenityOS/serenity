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

package dom.ls;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSException;

/*
 * @test
 * @bug 6710741
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.ls.Bug6710741Test
 * @run testng/othervm dom.ls.Bug6710741Test
 * @summary Test there should be stack trace information if LSSerializer().writeToString reports an exception.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6710741Test {

    @Test
    public final void test() {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element el = doc.createElement("x");
            DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation().getFeature("LS", "3.0");
            System.out.println(ls.createLSSerializer().writeToString(el));
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        } catch (LSException ex) {
            ex.printStackTrace();
            System.out.println("cause: " + ex.getCause());
            if (ex.getCause() == null) {
                Assert.fail("should set cause.");
            }
        }
    }

    @Test
    public void testWorkaround() {
        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element el = doc.createElement("x");
            doc.appendChild(el);
            DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation().getFeature("LS", "3.0");
            System.out.println(ls.createLSSerializer().writeToString(doc));
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

}
