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

package transform;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/*
 * @test
 * @bug 6465722
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6465722
 * @run testng/othervm transform.Bug6465722
 * @summary Test Transformer can transform the node attribute prefixed with a namespace.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6465722 {

    public Bug6465722(String name) {
    }

    private static final String IDENTITY_XSLT = "<xsl:stylesheet version='1.0' " + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + "<xsl:template match='@*|node()'>" + "<xsl:copy>" + "<xsl:apply-templates select='@*|node()'/>" + "</xsl:copy>" + "</xsl:template>"
            + "</xsl:stylesheet>";

    @Test
    public void test() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document d = dbf.newDocumentBuilder().getDOMImplementation().createDocument(null, "r", null);
            d.getDocumentElement().setAttributeNS("http://nowhere.net/", "id", "1");

            Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(new StringReader(IDENTITY_XSLT)));
            t.transform(new DOMSource(d), new StreamResult(new StringWriter()));
        } catch (Throwable ex) {
            Assert.fail("Exception: " + ex.getMessage());
        }
    }

}
