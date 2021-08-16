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

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/*
 * @test
 * @bug 6376058
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow xpath.XPathTest
 * @run testng/othervm xpath.XPathTest
 * @summary Test XPath functions. See details for each test.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XPathTest {

    /*
      @bug 6211561
     * Verifies the specification for XPath and XPathExpression:
     * If a null value is provided for item (the context),
     * the expression must have no dependency on the context.
    */
    @Test(dataProvider = "noContextDependency")
    public void testNoContextDependency1(String expression, Object item) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.evaluate(expression, item, XPathConstants.STRING);
    }

    @Test(dataProvider = "noContextDependency")
    public void testNoContextDependency2(String expression, Object item) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.evaluateExpression(expression, item, String.class);
    }

    /*
      @bug 6211561
     * Verifies the specification for XPath and XPathExpression:
     * If a null value is provided for item (the context) that the operation
     * depends on, XPathExpressionException will be thrown
    */
    @Test(dataProvider = "hasContextDependency", expectedExceptions = XPathExpressionException.class)
    public void testHasContextDependency1(String expression, Object item) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.evaluate(expression, item, XPathConstants.STRING);
    }

    @Test(dataProvider = "hasContextDependency", expectedExceptions = XPathExpressionException.class)
    public void testHasContextDependency2(String expression, Object item) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.evaluateExpression(expression, item, String.class);
    }

    /*
      @bug 6376058
      Verifies that XPath.getNamespaceContext() is supported.
    */
    @Test
    public void testNamespaceContext() {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        NamespaceContext namespaceContext = xPath.getNamespaceContext();
    }

    /*
     * DataProvider: the expression has no dependency on the context
     */
    @DataProvider(name = "noContextDependency")
    public Object[][] getExpressionContext() throws Exception {
        return new Object[][]{
            {"1+1", (Node)null},
            {"5 mod 2", (Node)null},
            {"8 div 2", (Node)null},
            {"/node", getEmptyDocument()}
        };
    }

    /*
     * DataProvider: the expression has dependency on the context, but the context
     * is null.
     */
    @DataProvider(name = "hasContextDependency")
    public Object[][] getExpressionContext1() throws Exception {
        return new Object[][]{
            {"/node", (Node)null},
            {"//@lang", (Node)null},
            {"bookstore//book", (Node)null},
            {"/bookstore/book[last()]", (Node)null},
            {"//title[@lang='en']", (Node)null},
            {"/bookstore/book[price>9.99]", (Node)null},
            {"/bookstore/book[price>8.99 and price<9.99]", (Node)null},
            {"/bookstore/*", (Node)null},
            {"//title[@*]", (Node)null},
            {"//title | //price", (Node)null},
            {"//book/title | //book/price", (Node)null},
            {"/bookstore/book/title | //price", (Node)null},
            {"child::book", (Node)null},
            {"child::text()", (Node)null},
            {"child::*/child::price", (Node)null}
        };
    }

    /**
     * Returns an empty {@link org.w3c.dom.Document}.
     * @return a DOM Document, null in case of Exception
     */
    public Document getEmptyDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }
}
