/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathNodes;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8183266
 * @summary verifies the specification of the XPathEvaluationResult API
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.xpath.ptests.XPathEvaluationResultTest
 * @run testng/othervm javax.xml.xpath.ptests.XPathEvaluationResultTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XPathEvaluationResultTest {

    /*
     * Test getQNameType returns QName type for supported types and Number subtypes
     */
    @Test(dataProvider = "supportedTypes")
    public void testQNameTypeSupportedTypes(QName expectedQName, Class<?> type) {
        QName qName = XPathEvaluationResult.XPathResultType.getQNameType(type);
        assertNotNull(qName);
        assertEquals(expectedQName, qName);
    }

    /*
     * Test getQNameType returns null when type is not supported
     */
    @Test(dataProvider = "unsupportedTypes")
    public void testQNameTypeUnsupportedTypes(Class<?> type) {
        QName qName = XPathEvaluationResult.XPathResultType.getQNameType(type);
        assertNull(qName);
    }

    /*
     * Test getQNameType is null safe
     */
    @Test
    public void testQNameTypeNullType() {
        QName qName = XPathEvaluationResult.XPathResultType.getQNameType(null);
        assertNull(qName);
    }

    /*
     * DataProvider: Class types supported
     */
    @DataProvider(name = "supportedTypes")
    public Object[][] getSupportedTypes() {
        return new Object[][]{
                {XPathConstants.STRING, String.class},
                {XPathConstants.BOOLEAN, Boolean.class},
                {XPathConstants.NODESET, XPathNodes.class},
                {XPathConstants.NODE, Node.class},
                {XPathConstants.NUMBER, Long.class},
                {XPathConstants.NUMBER, Integer.class},
                {XPathConstants.NUMBER, Double.class},
                {XPathConstants.NUMBER, Number.class}
        };
    }

    /*
     * DataProvider: Class types not supported
     */
    @DataProvider(name = "unsupportedTypes")
    public Object[][] getUnsupportedTypes() {
        return new Object[][]{
                new Object[]{AtomicInteger.class},
                new Object[]{AtomicLong.class},
                new Object[]{BigDecimal.class},
                new Object[]{BigInteger.class},
                new Object[]{Byte.class},
                new Object[]{Float.class},
                new Object[]{Short.class},
                new Object[]{Character.class},
                new Object[]{StringBuilder.class},
        };
    }
}
