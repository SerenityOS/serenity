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

package xpath;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;


/*
 * @test
 * @bug 4991857
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow xpath.Bug4991857
 * @run testng/othervm xpath.Bug4991857
 * @summary XPath.evaluate(...) throws XPathExpressionException when context is null and expression refers to the context.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4991857 {

    Document d = null;

    XPathFactory xpathFactory = XPathFactory.newInstance();

    @Test
    public void testXPath09() throws Exception {
        try {
            XPath xpath = xpathFactory.newXPath();
            Assert.assertNotNull(xpath);

            Double result = (Double) xpath.evaluate("1+2", d, XPathConstants.NUMBER);
        } catch (XPathExpressionException unused) {
            Assert.fail("Unexpected XPathExpressionException thrown");
        }
    }

    @Test
    public void testXPath10() throws Exception {
        try {
            XPath xpath = xpathFactory.newXPath();
            Assert.assertNotNull(xpath);

            xpath.evaluate(".", d, XPathConstants.STRING);
            Assert.fail("XPathExpressionException not thrown");
        } catch (XPathExpressionException e) {
            // Expected exception as context node is null
        }
    }

    @Test
    public void testXPath11() throws Exception {
        try {
            Document d = null;

            XPathFactory xpathFactory = XPathFactory.newInstance();
            Assert.assertNotNull(xpathFactory);

            XPath xpath = xpathFactory.newXPath();
            Assert.assertNotNull(xpath);

            String quantity = (String) xpath.evaluate("/widgets/widget[@name='a']/@quantity", d, XPathConstants.STRING);
            Assert.fail("XPathExpressionException not thrown");
        } catch (XPathExpressionException e) {
            // Expected exception as context node is null
        }
    }
}
