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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow xpath.SecureProcessingTest
 * @run testng/othervm xpath.SecureProcessingTest
 * @summary Test when FEATURE_SECURE_PROCESSING is true, calling an external function will cause XPathFunctionException.
 */
@Test
@Listeners({jaxp.library.FilePolicy.class})
public class SecureProcessingTest {
    public final void testSecureProcessing() {
        boolean _isSecureMode = System.getSecurityManager() != null;

        final String XPATH_EXPRESSION = "ext:helloWorld()";

        // the xml source
        InputStream xmlStream = this.getClass().getResourceAsStream("SecureProcessingTest.xml");

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        Document document = null;

        try {
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.parse(xmlStream);
        } catch (ParserConfigurationException parserConfigurationException) {
            parserConfigurationException.printStackTrace();
            Assert.fail(parserConfigurationException.toString());
        } catch (SAXException saxException) {
            saxException.printStackTrace();
            Assert.fail(saxException.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Assert.fail(ioException.toString());
        }

        // the XPath
        XPathFactory xPathFactory = null;
        XPath xPath = null;
        String xPathResult = null;

        // SECURE_PROCESSING == false
        // evaluate an expression with a user defined function with a non-secure
        // XPath
        // expect success
        if (!_isSecureMode) { // jaxp secure feature can not be turned off when
                              // security manager is present
            try {
                xPathFactory = xPathFactory.newInstance();
                xPathFactory.setXPathFunctionResolver(new MyXPathFunctionResolver());
                xPathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);

                xPath = xPathFactory.newXPath();
                xPath.setNamespaceContext(new MyNamespaceContext());

                xPathResult = xPath.evaluate(XPATH_EXPRESSION, document);
            } catch (XPathFactoryConfigurationException xPathFactoryConfigurationException) {
                xPathFactoryConfigurationException.printStackTrace();
                Assert.fail(xPathFactoryConfigurationException.toString());
            } catch (XPathExpressionException xPathExpressionException) {
                xPathExpressionException.printStackTrace();
                Assert.fail(xPathExpressionException.toString());
            }

            // expected success
            System.out.println("XPath result (SECURE_PROCESSING == false) = \"" + xPathResult + "\"");
        }
        // now try with SECURE_PROCESSING == true
        // evaluate an expression with a user defined function with a secure
        // XPath
        // expect Exception
        boolean securityException = false;
        try {
            xPathFactory = xPathFactory.newInstance();
            xPathFactory.setXPathFunctionResolver(new MyXPathFunctionResolver());
            xPathFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext(new MyNamespaceContext());

            xPathResult = xPath.evaluate(XPATH_EXPRESSION, document);
        } catch (XPathFactoryConfigurationException xPathFactoryConfigurationException) {
            xPathFactoryConfigurationException.printStackTrace();
            Assert.fail(xPathFactoryConfigurationException.toString());
        } catch (XPathFunctionException xPathFunctionException) {
            // expected security exception
            securityException = true;
            xPathFunctionException.printStackTrace(System.out);
        } catch (XPathExpressionException xPathExpressionException) {
            xPathExpressionException.printStackTrace();
            Assert.fail(xPathExpressionException.toString());
        }

        // expected Exception
        if (!securityException) {
            Assert.fail("XPath result (SECURE_PROCESSING == true) = \"" + xPathResult + "\"");
        }
    }

    private class MyXPathFunctionResolver implements XPathFunctionResolver {

        public XPathFunction resolveFunction(QName functionName, int arity) {

            // not a real ewsolver, always return a default XPathFunction
            return new MyXPathFunction();
        }
    }

    private class MyXPathFunction implements XPathFunction {

        public Object evaluate(List list) throws XPathFunctionException {

            return "Hello World";
        }
    }

    private class MyNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("The prefix cannot be null.");
            }

            if (prefix.equals("ext")) {
                return "http://ext.com";
            } else {
                return null;
            }
        }

        public String getPrefix(String namespace) {

            if (namespace == null) {
                throw new IllegalArgumentException("The namespace uri cannot be null.");
            }

            if (namespace.equals("http://ext.com")) {
                return "ext";
            } else {
                return null;
            }
        }

        public Iterator getPrefixes(String namespace) {
            return null;
        }
    }
}
