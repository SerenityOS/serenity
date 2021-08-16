/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathNodes;
import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/*
 * @test
 * @bug 8054196
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow xpath.XPathAnyTypeTest
 * @run testng/othervm xpath.XPathAnyTypeTest
 * @summary Test for the project XPath: support any type. This test covers the new
 * evaluateExpression methods of XPath, as well as XPathNodes and XPathEvaluationResult.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XPathAnyTypeTest extends XPathTestBase {
    /*
     Test for resolveFunction(QName functionName,int arity); evaluate throws
     NPE if functionName  is null.
     */

    @Test(dataProvider = "xpath", expectedExceptions = NullPointerException.class)
    public void testCheckXPathFunctionResolver02(XPath xpath) throws XPathExpressionException {
        xpath.setXPathFunctionResolver((functionName, arity) -> null);
        assertEquals(xpath.evaluate(null, "5"), "2");
    }
    /*
     Check that NPE is thrown when expression is null.
     */

    @Test(dataProvider = "xpath", expectedExceptions = NullPointerException.class)
    public void test01(XPath xpath) throws XPathExpressionException {
        double result = xpath.evaluateExpression(null, (Object) null, Double.class);
    }

    /*
     Check that NPE is thrown when the class type is null.
     */
    @Test(dataProvider = "xpath", expectedExceptions = NullPointerException.class)
    public void test02(XPath xpath) throws XPathExpressionException {
        double result = xpath.evaluateExpression("1+1", (Object) null, null);
    }

    /*
     Parameter item can be null when the expression does not depends on the
     context.
     */
    @Test(dataProvider = "xpath")
    public void test03(XPath xpath) throws XPathExpressionException {
        int result = xpath.evaluateExpression("1+1", (Object) null, Integer.class);
        assertTrue(result == 2);
    }

    /*
     * Test return type: boolean.
     */
    @Test(dataProvider = "document")
    public void test04(XPath xpath, Document doc) throws XPathExpressionException {
        boolean result1 = xpath.evaluateExpression("boolean(/Customers/Customer[@id=3])", doc, Boolean.class);
        assertTrue(result1);
    }

    /*
     * Test return type: numeric. Subtypes supported: Double, Integer and Long
     */
    @Test(dataProvider = "document")
    public void test05(XPath xpath, Document doc) throws XPathExpressionException {
        double result1 = xpath.evaluateExpression("count(/Customers/Customer)", doc, Double.class);
        assertTrue(result1 == 3.0);
        int result2 = xpath.evaluateExpression("count(/Customers/Customer)", doc, Integer.class);
        assertTrue(result2 == 3);
        long result3 = xpath.evaluateExpression("count(/Customers/Customer)", doc, Long.class);
        assertTrue(result3 == 3);
    }

    /*
     * Test return type: numeric.  Of the subtypes of Number, only Double,
     * Integer and Long are required.
     */
    @Test(dataProvider = "invalidNumericTypes", expectedExceptions = IllegalArgumentException.class)
    public void test06(XPath xpath, Class<Number> type) throws XPathExpressionException {
        xpath.evaluateExpression("1+1", (Object) null, type);
    }

    /*
     * Test return type: String.
     */
    @Test(dataProvider = "document")
    public void test07(XPath xpath, Document doc) throws XPathExpressionException {
        String result1 = xpath.evaluateExpression("string(/Customers/Customer[@id=3]/Phone/text())", doc, String.class);
        assertTrue(result1.equals("3333333333"));
    }

    /*
     * Test return type: NodeSet.
     */
    @Test(dataProvider = "document")
    public void test08(XPath xpath, Document doc) throws XPathExpressionException {
        XPathNodes nodes = xpath.evaluateExpression("/Customers/Customer", doc, XPathNodes.class);
        assertTrue(nodes.size() == 3);
        for (Node n : nodes) {
            assertEquals(n.getLocalName(), "Customer");
        }
    }

    /*
     * Test return type: Node.
     */
    @Test(dataProvider = "document")
    public void test09(XPath xpath, Document doc) throws XPathExpressionException {
        Node n = xpath.evaluateExpression("/Customers/Customer[@id=3]", doc, Node.class);
        assertEquals(n.getLocalName(), "Customer");
    }

    /*
     * Test return type: Unsupported type.
     */
    @Test(dataProvider = "document", expectedExceptions = IllegalArgumentException.class)
    public void test10(XPath xpath, Document doc) throws XPathExpressionException {
        File n = xpath.evaluateExpression("/Customers/Customer[@id=3]", doc, File.class);
    }

    /*
     * Test return type: Any::Boolean.
     */
    @Test(dataProvider = "document")
    public void test11(XPath xpath, Document doc) throws XPathExpressionException {
        XPathEvaluationResult<?> result = xpath.evaluateExpression("boolean(/Customers/Customer[@id=3])", doc);
        verifyResult(result, true);
    }

    /*
     * Test return type: Any::Number.
     */
    @Test(dataProvider = "document")
    public void test12(XPath xpath, Document doc) throws XPathExpressionException {
        XPathEvaluationResult<?> result = xpath.evaluateExpression("count(/Customers/Customer)", doc);
        verifyResult(result, 3.0);
    }

    /*
     * Test return type: Any::String.
     */
    @Test(dataProvider = "document")
    public void test13(XPath xpath, Document doc) throws XPathExpressionException {
        XPathEvaluationResult<?> result = xpath.evaluateExpression(
                "string(/Customers/Customer[@id=3]/Phone/text())", doc, XPathEvaluationResult.class);
        verifyResult(result, "3333333333");
    }

    /*
     * Test return type: Any::Nodeset.
     */
    @Test(dataProvider = "document")
    public void test14(XPath xpath, Document doc) throws XPathExpressionException {
        XPathEvaluationResult<?> result = xpath.evaluateExpression("/Customers/Customer", doc);
        verifyResult(result, "Customer");
    }

    /*
     * Test return type: Any::Node.
     */
    @Test(dataProvider = "document")
    public void test15(XPath xpath, Document doc) throws XPathExpressionException {
        XPathEvaluationResult<?> result = xpath.evaluateExpression("/Customers/Customer[@id=3]", doc);
        verifyResult(result, "Customer");
    }
}
