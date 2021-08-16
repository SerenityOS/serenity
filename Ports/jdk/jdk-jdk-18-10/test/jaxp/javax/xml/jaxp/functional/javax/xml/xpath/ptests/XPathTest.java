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

import static javax.xml.xpath.XPathConstants.BOOLEAN;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;
import static javax.xml.xpath.ptests.XPathTestConst.XML_DIR;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Class containing the test cases for XPath API.
 */
/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.xpath.ptests.XPathTest
 * @run testng/othervm javax.xml.xpath.ptests.XPathTest
 */
@Listeners({jaxp.library.FilePolicy.class})
public class XPathTest {
    /**
     * Document object for testing XML file.
     */
    private Document document;

    /**
     * A XPath for evaluation environment and expressions.
     */
    private XPath xpath;

    /**
     * A QName using default name space.
     */
    private static final QName TEST_QNAME = new QName(XMLConstants.XML_NS_URI, "");

    /**
     * XML File Path.
     */
    private static final Path XML_PATH = Paths.get(XML_DIR + "widgets.xml");

    /**
     * An expression name which locate at "/widgets/widget[@name='a']/@quantity"
     */
    private static final String EXPRESSION_NAME_A = "/widgets/widget[@name='a']/@quantity";

    /**
     * An expression name which locate at "/widgets/widget[@name='b']/@quantity"
     */
    private static final String EXPRESSION_NAME_B = "/widgets/widget[@name='b']/@quantity";

    /**
     * Create Document object and XPath object for every time
     * @throws Exception If any errors occur.
     */
    @BeforeTest
    public void setup() throws Exception {
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(XML_PATH.toFile());
        xpath = XPathFactory.newInstance().newXPath();
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType) which return type is String.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath01() throws XPathExpressionException {
        assertEquals(xpath.evaluate(EXPRESSION_NAME_A, document, STRING), "6");
    }


    /**
     * Test for XPath.compile(java.lang.String expression) and then
     * evaluate(java.lang.Object item, QName returnType).
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath02() throws XPathExpressionException {
        assertEquals(xpath.compile(EXPRESSION_NAME_A).evaluate(document, STRING), "6");
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item) when the third argument is left off of the XPath.evaluate method,
     * all expressions are evaluated to a String value.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath03() throws XPathExpressionException {
        assertEquals(xpath.evaluate(EXPRESSION_NAME_A, document), "6");
    }

    /**
     * Test for XPath.compile(java.lang.String expression). If expression is
     * null, should throw NPE.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath04() throws XPathExpressionException {
        xpath.compile(null);
    }

    /**
     * Test for XPath.compile(java.lang.String expression). If expression cannot
     * be compiled junk characters, should throw XPathExpressionException.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath05() throws XPathExpressionException {
        xpath.compile("-*&");
    }

    /**
     * Test for XPath.compile(java.lang.String expression). If expression is
     * blank, should throw XPathExpressionException
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath06() throws XPathExpressionException {
        xpath.compile(" ");
    }

    /**
     * Test for XPath.compile(java.lang.String expression). The expression
     * cannot be evaluated as this does not exist.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath07() throws XPathExpressionException {
        assertEquals(xpath.compile(EXPRESSION_NAME_B).evaluate(document, STRING), "");
    }


    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType). If String expression is null, should throw NPE.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath08() throws XPathExpressionException {
        xpath.evaluate(null, document, STRING);
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType). If item is null, should throw NPE.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath09() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, null, STRING);
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType). If returnType is null, should throw NPE.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath10() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, document, null);
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType). If a request is made to evaluate the expression
     * in the absence of a context item, simple expressions, such as "1+1", can
     * be evaluated.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath11() throws XPathExpressionException {
        assertEquals(xpath.evaluate("1+1", document, STRING), "2");
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) throws XPathExpressionException if expression is a empty
     * string "".
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath12() throws XPathExpressionException {
        xpath.evaluate("", document, STRING);
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) throws IllegalArgumentException if returnType is not one of
     * the types defined in XPathConstants.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCheckXPath13() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, document, TEST_QNAME);
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) returns correct boolean value if returnType is Boolean.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath14() throws XPathExpressionException {
        assertEquals(xpath.evaluate(EXPRESSION_NAME_A, document, BOOLEAN), true);
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) returns false as  expression is not successful in evaluating
     * to any result if returnType is Boolean.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath15() throws XPathExpressionException {
        assertEquals(xpath.evaluate(EXPRESSION_NAME_B, document, BOOLEAN), false);
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) returns correct number value if return type is Number.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath16() throws XPathExpressionException {
        assertEquals(xpath.evaluate(EXPRESSION_NAME_A, document, NUMBER), 6d);
    }


    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item, QName
     * returnType) returns correct string value if return type is Node.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath17() throws XPathExpressionException {
        assertEquals(((Attr)xpath.evaluate(EXPRESSION_NAME_A, document, NODE)).getValue(), "6");
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item, QName returnType). If return type is NodeList,the evaluated value
     * equals to "6" as expected.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath18() throws XPathExpressionException {
        NodeList nodeList = (NodeList)xpath.evaluate(EXPRESSION_NAME_A, document, NODESET);
        assertEquals(((Attr) nodeList.item(0)).getValue(), "6");
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item). If expression is null, should throw NPE.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath19() throws XPathExpressionException {
        xpath.evaluate(null, document);
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, java.lang.Object
     * item). If a request is made to evaluate the expression in the absence of
     * a context item, simple expressions, such as "1+1", can be evaluated.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test
    public void testCheckXPath20() throws XPathExpressionException {
        assertEquals(xpath.evaluate("1+1", document), "2");
    }

    /**
     * XPath.evaluate(java.lang.String expression, java.lang.Object item) throws
     * NPE if InputSource is null.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath21() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, null);
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source) return
     * correct value by looking for Node.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath22() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is)), "6");
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source) throws
     * NPE if InputSource is null.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath23() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, null);
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source) throws
     * NPE if String expression is null.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath24() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(null, new InputSource(is));
        }
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, InputSource source).
     * If expression is junk characters, expression cannot be evaluated, should
     * throw XPathExpressionException.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath25() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate("-*&", new InputSource(is));
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source) throws
     * XPathExpressionException if expression is blank " ".
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath26() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(" ", new InputSource(is));
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source, QName
     * returnType) returns correct string value which return type is String.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath27() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is), STRING), "6");
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source, QName
     * returnType) throws NPE if source is null.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath28() throws XPathExpressionException {
        xpath.evaluate(EXPRESSION_NAME_A, null, STRING);
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source, QName
     * returnType) throws NPE if expression is null.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath29() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(null, new InputSource(is), STRING);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) throws NPE if returnType is null.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath30() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is), null);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source, QName
     * returnType) throws XPathExpressionException if expression is junk characters.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath31() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate("-*&", new InputSource(is), STRING);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source, QName
     * returnType) throws XPathExpressionException if expression is blank " ".
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = XPathExpressionException.class)
    public void testCheckXPath32() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(" ", new InputSource(is), STRING);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) throws IllegalArgumentException if returnType is not
     * one of the types defined in XPathConstants.
     *
     * @throws Exception If any errors occur.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCheckXPath33() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is), TEST_QNAME);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) return correct boolean value if return type is Boolean.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath34() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is),
                BOOLEAN), true);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) return correct boolean value if return type is Boolean.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath35() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_B, new InputSource(is),
                BOOLEAN), false);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) return correct number value if return type is Number.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath36() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is),
                NUMBER), 6d);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) return correct string value if return type is Node.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath37() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(((Attr)xpath.evaluate(EXPRESSION_NAME_A,
                new InputSource(is), NODE)).getValue(), "6");
        }
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, InputSource source,
     * QName returnType) which return type is NodeList.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath38() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            NodeList nodeList = (NodeList)xpath.evaluate(EXPRESSION_NAME_A,
                new InputSource(is), NODESET);
            assertEquals(((Attr) nodeList.item(0)).getValue(), "6");
        }
    }

    /**
     * Test for XPath.evaluate(java.lang.String expression, InputSource iSource,
     * QName returnType). If return type is Boolean, should return false as
     * expression is not successful in evaluating to any result.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath52() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_B, new InputSource(is),
                BOOLEAN), false);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource iSource, QName
     * returnType) returns correct number value which return type is Number.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath53() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(xpath.evaluate(EXPRESSION_NAME_A, new InputSource(is),
                NUMBER), 6d);
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource iSource, QName
     * returnType) returns a node value if returnType is Node.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath54() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            assertEquals(((Attr)xpath.evaluate(EXPRESSION_NAME_A,
                new InputSource(is), NODE)).getValue(), "6");
        }
    }

    /**
     * XPath.evaluate(java.lang.String expression, InputSource iSource, QName
     * returnType) returns a node list if returnType is NodeList.
     *
     * @throws Exception If any errors occur.
     */
    @Test
    public void testCheckXPath55() throws Exception {
        try (InputStream is = Files.newInputStream(XML_PATH)) {
            NodeList nodeList = (NodeList)xpath.evaluate(EXPRESSION_NAME_A,
                new InputSource(is), NODESET);
            assertEquals(((Attr) nodeList.item(0)).getValue(), "6");
        }
    }

    /**
     * Test for XPath.getNamespaceContext() returns the current namespace
     * context, null is returned if no namespace context is in effect.
     */
    @Test
    public void testCheckXPath56() {
        // CR 6376058 says that an impl will be provided, but by
        // default we still return null here
        assertNull(xpath.getNamespaceContext());
    }

    /**
     * Test for XPath.setNamespaceContext(NamespaceContext nsContext) Establish
     * a namespace context. Set a valid nsContext and retrieve it using
     * getNamespaceContext(), should return the same.
     */
    @Test
    public void testCheckXPath57() {
        MyNamespaceContext myNamespaceContext = new MyNamespaceContext();
        xpath.setNamespaceContext(myNamespaceContext);
        assertEquals(xpath.getNamespaceContext(), myNamespaceContext);
    }

    /**
     * Test for XPath.setNamespaceContext(NamespaceContext nsContext) Establish
     * a namespace context. NullPointerException is thrown if nsContext is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath58() {
        xpath.setNamespaceContext(null);
    }

    /**
     * Test for XPath.getXPathFunctionResolver() Return the current function
     * resolver. Null is returned if no function resolver is in effect.
     */
    @Test
    public void testCheckXPath59() {
        assertNull(xpath.getXPathFunctionResolver());
    }

    /**
     * Test for XPath.setXPathFunctionResolver(XPathFunctionResolver resolver).
     * Set a valid resolver and retrieve it using getXPathFunctionResolver(),
     * should return the same.
     */
    @Test
    public void testCheckXPath60() {
        xpath.setXPathFunctionResolver((functionName, arity) -> null);
        assertNotNull(xpath.getXPathFunctionResolver());
    }

    /**
     * Test for XPath.setXPathFunctionResolver(XPathFunctionResolver resolver).
     * set resolver as null, should throw NPE.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath61() {
        xpath.setXPathFunctionResolver(null);
    }

    /**
     * Test for XPath.getXPathVariableResolver() Return the current variable
     * resolver. null is returned if no variable resolver is in effect.
     */
    @Test
    public void testCheckXPath62() {
        assertNull(xpath.getXPathVariableResolver());
    }

    /**
     * Test for XPath.setXPathVariableResolver(XPathVariableResolver resolver).
     * Set a valid resolver and retrieve it using getXPathVariableResolver(),
     * should return the same.
     */
    @Test
    public void testCheckXPath63() {
        xpath.setXPathVariableResolver(qname -> null);
        assertNotNull(xpath.getXPathVariableResolver());
    }

    /**
     * Test for XPath.setXPathVariableResolver(XPathVariableResolver resolver).
     * Set resolver as null, should throw NPE.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPath64() {
        xpath.setXPathVariableResolver(null);
    }

    /**
     * Customized NamespaceContext used for test
     */
    private class MyNamespaceContext implements NamespaceContext {
        /**
         * et Namespace URI bound to a prefix in the current scope.
         * @param prefix prefix to look up
         * @return a Namespace URI identical to prefix
         */
        @Override
        public String getNamespaceURI(String prefix) {
            return prefix;
        }

        /**
         * Get prefix bound to Namespace URI in the current scope.
         * @param namespaceURI URI of Namespace to lookup
         * @return prefix identical to URI of Namespace
         */
        @Override
        public String getPrefix(String namespaceURI) {
            return namespaceURI;
        }

        /**
         * Get all prefixes bound to a Namespace URI in the current scope.
         * @param namespaceURI URI of Namespace to lookup
         * @return null
         */
        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    }
}
