/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.xpath.ptests;

import static org.testng.Assert.assertEquals;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Class containing the test cases for XPathFunctionResolver.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.xpath.ptests.XPathFunctionResolverTest
 * @run testng/othervm javax.xml.xpath.ptests.XPathFunctionResolverTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XPathFunctionResolverTest {
    /**
     * A XPath for evaluation environment and expressions.
     */
    private XPath xpath;

    /**
     * Create XPath object before every test. Make sure function resolver has
     * been set for XPath object.
     */
    @BeforeTest
    public void setup() {
        xpath = XPathFactory.newInstance().newXPath();
        if (xpath.getXPathFunctionResolver() == null) {
            xpath.setXPathFunctionResolver((functionName,arity) -> null);
        }
    }
    /**
     * Test for resolveFunction(QName functionName,int arity). evaluate will
     * continue as long as functionName is meaningful.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPathFunctionResolver01() throws XPathExpressionException {
        assertEquals(xpath.evaluate("round(1.7)", (Object)null), "2");
    }

    /**
     * Test for resolveFunction(QName functionName,int arity); evaluate throws
     * NPE if functionName  is null.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPathFunctionResolver02() throws XPathExpressionException {
        assertEquals(xpath.evaluate(null, "5"), "2");
    }
}
