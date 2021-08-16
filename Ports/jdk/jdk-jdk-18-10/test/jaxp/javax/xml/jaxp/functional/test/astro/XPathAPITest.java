/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
package test.astro;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static javax.xml.xpath.XPathConstants.DOM_OBJECT_MODEL;
import static javax.xml.xpath.XPathConstants.NODESET;
import static jaxp.library.JAXPTestUtilities.filenameToURL;
import static org.testng.Assert.assertEquals;
import static test.astro.AstroConstants.ASTROCAT;
import static test.astro.AstroConstants.JAXP_SCHEMA_LANGUAGE;
import static test.astro.AstroConstants.JAXP_SCHEMA_SOURCE;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathVariableResolver;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.astro.XPathAPITest
 * @run testng/othervm test.astro.XPathAPITest
 * @summary test XPath API
 */
@Test(singleThreaded = true)
@Listeners({jaxp.library.FilePolicy.class})
public class XPathAPITest {
    private static final String STARDB_STAR_3_CONSTELLATION = "//astro:stardb/astro:star[3]/astro:constellation";
    private static final String STARDB_STAR = "//astro:stardb/astro:star";
    private Document doc;
    private XPathFactory xpf;
    private NamespaceContext nsContext;

    @BeforeClass
    public void setup() throws Exception {
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
        df.setNamespaceAware(true);
        df.setValidating(true);
        df.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA_NS_URI);
        df.setAttribute(JAXP_SCHEMA_SOURCE, "catalog.xsd");
        DocumentBuilder bldr = df.newDocumentBuilder();
        doc = bldr.parse(ASTROCAT);

        xpf = XPathFactory.newInstance(DOM_OBJECT_MODEL);

        nsContext = new MyNamespaceContext();
    }

    @DataProvider(name = "nodelist-evaluator")
    public Object[][] getNodeListEvaluator() throws MalformedURLException {
        return new Object[][] { { (XPathEvaluator) expression -> getXPath().evaluate(expression, doc.getDocumentElement(), NODESET) },
                { (XPathEvaluator) expression -> getXPath().evaluate(expression, createXMLInputSource(), NODESET) },
                { (XPathEvaluator) expression -> getXPathExpression(expression).evaluate(doc.getDocumentElement(), NODESET) },
                { (XPathEvaluator) expression -> getXPathExpression(expression).evaluate(createXMLInputSource(), NODESET) } };
    }

    /*
     * Test xpath expression evaluations method that returns type indicated by
     * QName
     */
    @Test(dataProvider = "nodelist-evaluator")
    public void testEvaluateNodeList(XPathEvaluator evaluator) throws Exception {
        NodeList o = (NodeList) evaluator.evaluate(STARDB_STAR);
        assertEquals(o.getLength(), 10);
    }

    @DataProvider(name = "string-evaluator")
    public Object[][] getStringEvaluator() throws MalformedURLException {
        return new Object[][] { { (XPathEvaluator) expression -> getXPath().evaluate(expression, doc.getDocumentElement()) },
                { (XPathEvaluator) expression -> getXPath().evaluate(expression, createXMLInputSource()) },
                { (XPathEvaluator) expression -> getXPathExpression(expression).evaluate(doc.getDocumentElement()) },
                { (XPathEvaluator) expression -> getXPathExpression(expression).evaluate(createXMLInputSource()) } };
    }

    /*
     * Test xpath expression evaluations method that returns String
     */
    @Test(dataProvider = "string-evaluator")
    public void testEvaluateString(XPathEvaluator evaluator) throws Exception {
        assertEquals(evaluator.evaluate(STARDB_STAR_3_CONSTELLATION), "Psc");
    }

    @Test
    public void testXPathVariableResolver() throws Exception {
        XPath xpath = getXPath();
        xpath.setXPathVariableResolver(new MyXPathVariableResolver());
        assertEquals(xpath.evaluate("//astro:stardb/astro:star[astro:hr=$id]/astro:constellation", doc.getDocumentElement()), "Peg");

    }

    private static class MyXPathVariableResolver implements XPathVariableResolver {
        public Object resolveVariable(QName vname) {
            return "4"; // resolve $id as 4, xpath will locate to star[hr=4]
        }
    }

    /*
     * Implementation of a NamespaceContext interface for the Xpath api tests.
     * Used in xpath.setNamespaceContext(...)
     */
    private static class MyNamespaceContext implements NamespaceContext {
        public String getNamespaceURI(String prefix) {
            return "astro".equals(prefix) ? "http://www.astro.com/astro" : "";
        }

        public String getPrefix(String nsURI) {
            return "http://www.astro.com/astro".equals(nsURI) ? "astro" : "";
        }

        public Iterator getPrefixes(String nsURI) {
            ArrayList list = new ArrayList();
            list.add("astro");
            return list.iterator();
        }
    }

    @FunctionalInterface
    private interface XPathEvaluator {
        Object evaluate(String expression) throws XPathExpressionException;
    }

    private XPath getXPath() {
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(nsContext);
        return xpath;
    }

    private XPathExpression getXPathExpression(String expression) throws XPathExpressionException {
        return getXPath().compile(expression);
    }

    private InputSource createXMLInputSource() {
        return new InputSource(filenameToURL(ASTROCAT));
    }
}
