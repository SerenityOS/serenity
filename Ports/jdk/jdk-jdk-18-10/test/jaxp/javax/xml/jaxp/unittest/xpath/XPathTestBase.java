/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import static javax.xml.xpath.XPathConstants.BOOLEAN;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import javax.xml.xpath.XPathNodes;
import javax.xml.xpath.XPathEvaluationResult;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/*
 * Base class for XPath test
 */
class XPathTestBase {

    static final String rawXML
            = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Customers>"
            + "    <Customer id=\"1\">"
            + "        <Name>name1</Name>"
            + "        <Phone>1111111111</Phone>"
            + "        <Email>123@xyz.com</Email>"
            + "        <Address>"
            + "            <Street>1111 111st ave</Street>"
            + "            <City>The City</City>"
            + "            <State>The State</State>"
            + "        </Address>"
            + "    </Customer>"
            + "    <Customer id=\"2\">"
            + "        <Name>name1</Name>"
            + "        <Phone>2222222222</Phone>"
            + "        <Email>123@xyz.com</Email>"
            + "        <Address>"
            + "            <Street>2222 222nd ave</Street>"
            + "            <City>The City</City>"
            + "            <State>The State</State>"
            + "        </Address>"
            + "    </Customer>"
            + "    <Customer id=\"3\">"
            + "        <Name>name1</Name>"
            + "        <Phone>3333333333</Phone>"
            + "        <Email>123@xyz.com</Email>"
            + "        <Address>"
            + "            <Street>3333 333rd ave</Street>"
            + "            <City>The City</City>"
            + "            <State>The State</State>"
            + "        </Address>"
            + "    </Customer>"
            + "</Customers>";

    void verifyResult(XPathEvaluationResult<?> result, Object expected) {
        switch (result.type()) {
            case BOOLEAN:
                assertTrue(((Boolean) result.value()).equals(expected));
                return;
            case NUMBER:
                assertTrue(((Double) result.value()).equals(expected));
                return;
            case STRING:
                assertTrue(((String) result.value()).equals(expected));
                return;
            case NODESET:
                XPathNodes nodes = (XPathNodes) result.value();
                for (Node n : nodes) {
                    assertEquals(n.getLocalName(), expected);
                }
                return;
            case NODE:
                assertTrue(((Node) result.value()).getLocalName().equals(expected));
                return;
        }
        assertFalse(true, "Unsupported type");
    }

    /*
     * DataProvider: XPath object
     */
    @DataProvider(name = "xpath")
    public Object[][] getXPath() {
        return new Object[][]{{XPathFactory.newInstance().newXPath()}};
    }

    /*
     * DataProvider: Numeric types not supported
     */
    @DataProvider(name = "invalidNumericTypes")
    public Object[][] getInvalidNumericTypes() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return new Object[][]{{xpath, AtomicInteger.class},
            {xpath, AtomicInteger.class},
            {xpath, AtomicLong.class},
            {xpath, BigDecimal.class},
            {xpath, BigInteger.class},
            {xpath, Byte.class},
            {xpath, Float.class},
            {xpath, Short.class}
        };
    }

    /*
     * DataProvider: XPath and Document objects
     */
    @DataProvider(name = "document")
    public Object[][] getDocument() throws Exception {
        DocumentBuilderFactory dBF = DocumentBuilderFactory.newInstance();
        dBF.setValidating(false);
        dBF.setNamespaceAware(true);
        Document doc = dBF.newDocumentBuilder().parse(
                new ByteArrayInputStream(rawXML.getBytes("UTF-8")));

        return new Object[][]{{XPathFactory.newInstance().newXPath(), doc}};
    }
}
