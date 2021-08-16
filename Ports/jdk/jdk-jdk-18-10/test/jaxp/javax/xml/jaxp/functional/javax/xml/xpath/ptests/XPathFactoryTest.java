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

import static javax.xml.xpath.XPathConstants.DOM_OBJECT_MODEL;
import static javax.xml.xpath.XPathFactory.DEFAULT_OBJECT_MODEL_URI;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Class containing the test cases for XPathFactory API.
 */
/*
 * @test
 * @bug 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.xpath.ptests.XPathFactoryTest
 * @run testng/othervm javax.xml.xpath.ptests.XPathFactoryTest
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XPathFactoryTest {
    /**
     * Valid URL for creating a XPath factory.
     */
    private static final String VALID_URL = "http://java.sun.com/jaxp/xpath/dom";

    /**
     * Invalid URL not able to create a XPath factory.
     */
    private static final String INVALID_URL = "http://java.sun.com/jaxp/xpath/dom1";

    /**
     * XPathFactory builtin system-default implementation class name.
     */
    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl";

    /**
     * XPathFactory implementation class name.
     */
    private static final String XPATH_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;


    /**
     * Provide valid XPathFactory instantiation parameters.
     *
     * @return a data provider contains XPathFactory instantiation parameters.
     */
    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] { { VALID_URL, XPATH_FACTORY_CLASSNAME, null }, { VALID_URL, XPATH_FACTORY_CLASSNAME, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultInstance() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        XPathFactory xpf1 = XPathFactory.newDefaultInstance();
        XPathFactory xpf2 = XPathFactory.newInstance(DEFAULT_OBJECT_MODEL_URI);
        assertNotSame(xpf1, xpf2, "same instance returned:");
        assertSame(xpf1.getClass(), xpf2.getClass(),
                  "unexpected class mismatch for newDefaultInstance():");
        assertEquals(xpf1.getClass().getName(), DEFAULT_IMPL_CLASS);
        assertTrue(xpf1.isObjectModelSupported(DEFAULT_OBJECT_MODEL_URI),
                   "isObjectModelSupported(DEFAULT_OBJECT_MODEL_URI):");
        assertFalse(xpf1.isObjectModelSupported(INVALID_URL),
                   "isObjectModelSupported(INVALID_URL):");
    }

    /**
     * Test for XPathFactory.newInstance(java.lang.String uri, java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader) factoryClassName
     * points to correct implementation of javax.xml.xpath.XPathFactory , should
     * return newInstance of XPathFactory
     *
     * @param uri
     * @param factoryClassName
     * @param classLoader
     * @throws XPathFactoryConfigurationException
     */
    @Test(dataProvider = "parameters")
    public void testNewInstance(String uri, String factoryClassName, ClassLoader classLoader) throws XPathFactoryConfigurationException {
        XPathFactory xpf = XPathFactory.newInstance(uri, factoryClassName, classLoader);
        XPath xpath = xpf.newXPath();
        assertNotNull(xpath);
    }

    /**
     * Test for XPathFactory.newInstance(java.lang.String uri, java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader)
     *
     * @param factoryClassName
     * @param classLoader
     * @throws XPathFactoryConfigurationException
     *             is expected when factoryClassName is null
     */
    @Test(expectedExceptions = XPathFactoryConfigurationException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewInstanceWithNullFactoryClassName(String factoryClassName, ClassLoader classLoader) throws XPathFactoryConfigurationException {
        XPathFactory.newInstance(VALID_URL, factoryClassName, classLoader);
    }

    /**
     * Test for XPathFactory.newInstance(java.lang.String uri, java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader) uri is null , should
     * throw NPE
     *
     * @throws XPathFactoryConfigurationException
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testNewInstanceWithNullUri() throws XPathFactoryConfigurationException {
        XPathFactory.newInstance(null, XPATH_FACTORY_CLASSNAME, this.getClass().getClassLoader());
    }

    /**
     * Test for XPathFactory.newInstance(java.lang.String uri, java.lang.String
     * factoryClassName, java.lang.ClassLoader classLoader)
     *
     * @throws IllegalArgumentException
     *             is expected when uri is empty
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewInstanceWithEmptyUri() throws XPathFactoryConfigurationException {
        XPathFactory.newInstance("", XPATH_FACTORY_CLASSNAME, this.getClass().getClassLoader());
    }

    /**
     * Test for constructor - XPathFactory.newInstance().
     */
    @Test
    public void testCheckXPathFactory01() {
        assertNotNull(XPathFactory.newInstance());
    }

    /**
     * XPathFactory.newInstance(String uri) throws NPE if uri is null.
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void testCheckXPathFactory02() throws XPathFactoryConfigurationException {
        XPathFactory.newInstance(null);
    }

    /**
     * XPathFactory.newInstance(String uri) throws XPFCE if uri is just a blank
     * string.
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test(expectedExceptions = XPathFactoryConfigurationException.class)
    public void testCheckXPathFactory03() throws XPathFactoryConfigurationException {
        XPathFactory.newInstance(" ");
    }

    /**
     * Test for constructor - XPathFactory.newInstance(String uri) with valid
     * url - "http://java.sun.com/jaxp/xpath/dom".
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test
    public void testCheckXPathFactory04() throws XPathFactoryConfigurationException {
        assertNotNull(XPathFactory.newInstance(VALID_URL));
    }

    /**
     * Test for constructor - XPathFactory.newInstance(String uri) with invalid
     * url - "http://java.sun.com/jaxp/xpath/dom1".
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test(expectedExceptions = XPathFactoryConfigurationException.class)
    public void testCheckXPathFactory05() throws XPathFactoryConfigurationException {
        XPathFactory.newInstance(INVALID_URL);
    }

    /**
     * Test for constructor - XPathFactory.newInstance() and creating XPath with
     * newXPath().
     */
    @Test
    public void testCheckXPathFactory06() {
        assertNotNull(XPathFactory.newInstance().newXPath());
    }

    /**
     * Test for constructor - XPathFactory.newInstance(String uri) with valid
     * url - "http://java.sun.com/jaxp/xpath/dom" and creating XPath with
     * newXPath().
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test
    public void testCheckXPathFactory07() throws XPathFactoryConfigurationException {
        assertNotNull(XPathFactory.newInstance(VALID_URL).newXPath());
    }

    /**
     * Test for constructor - XPathFactory.newInstance(String uri) with valid
     * uri - DOM_OBJECT_MODEL.toString().
     *
     * @throws XPathFactoryConfigurationException If the specified object model
    *          is unavailable, or if there is a configuration error.
     */
    @Test
    public void testCheckXPathFactory08() throws XPathFactoryConfigurationException {
        assertNotNull(XPathFactory.newInstance(DOM_OBJECT_MODEL));
    }
}
