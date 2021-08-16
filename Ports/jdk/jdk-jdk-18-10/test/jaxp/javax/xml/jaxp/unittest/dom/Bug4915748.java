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

package dom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;

/*
 * @test
 * @bug 4915748
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug4915748
 * @run testng/othervm dom.Bug4915748
 * @summary Test DOMErrorHandler is called in case CDATA section is split by termination marker ']]>'.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4915748 {

    @Test
    public void testMain() throws Exception {

        final boolean[] hadError = new boolean[1];

        DocumentBuilderFactory docBF = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBF.newDocumentBuilder();

        Document doc = docBuilder.getDOMImplementation().createDocument("namespaceURI", "ns:root", null);

        CDATASection cdata = doc.createCDATASection("text1]]>text2");
        doc.getDocumentElement().appendChild(cdata);

        DOMConfiguration config = doc.getDomConfig();
        DOMErrorHandler erroHandler = new DOMErrorHandler() {
            public boolean handleError(DOMError error) {
                System.out.println(error.getMessage());
                Assert.assertEquals(error.getType(), "cdata-sections-splitted");
                Assert.assertFalse(hadError[0], "two errors were reported");
                hadError[0] = true;
                return false;
            }
        };
        config.setParameter("error-handler", erroHandler);
        doc.normalizeDocument();
        Assert.assertTrue(hadError[0]);
    }
}
