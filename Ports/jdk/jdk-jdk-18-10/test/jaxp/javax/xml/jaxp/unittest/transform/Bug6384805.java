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

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6384805
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.Bug6384805
 * @run testng/othervm transform.Bug6384805
 * @summary Test XSLTC can parse XML namespace when nodeset is created within a template.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class Bug6384805 {

    @Test
    public void test0() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();

            try {
                // tf.setAttribute("generate-translet", Boolean.TRUE);
            } catch (IllegalArgumentException e) {
                // ignore
            }

            Transformer t = tf.newTransformer(new StreamSource(getClass().getResourceAsStream("tigertest.xsl"), getClass().getResource("tigertest.xsl")
                    .toString()));

            StreamSource src = new StreamSource(getClass().getResourceAsStream("tigertest-in.xml"));
            DOMResult res = new DOMResult();
            t.transform(src, res);

            // Verify output of transformation
            XPath query = XPathFactory.newInstance().newXPath();

            query.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return prefix.equals("style") ? "http://openoffice.org/2000/style" : prefix.equals("office") ? "http://openoffice.org/2000/office" : null;
                }

                public String getPrefix(String namespaceURI) {
                    return null;
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            // Find the value of the style:family attribute
            Object o1 = query.evaluate("/test/office:document/office:styles/style:default-style/@style:family", res.getNode(), XPathConstants.STRING);

            Assert.assertTrue(o1.equals("graphics"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
