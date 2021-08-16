/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.parsers.ptests;

import static org.testng.Assert.assertNotNull;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.parsers.ptests.SAXFactoryNewInstanceTest
 * @run testng/othervm javax.xml.parsers.ptests.SAXFactoryNewInstanceTest
 * @summary Tests for SAXParserFactory.newInstance(factoryClassName , classLoader)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class SAXFactoryNewInstanceTest {

    private static final String SAXPARSER_FACTORY_CLASSNAME = "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl";

    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] { { SAXPARSER_FACTORY_CLASSNAME, null }, { SAXPARSER_FACTORY_CLASSNAME, this.getClass().getClassLoader() } };
    }

    /*
     * test for SAXParserFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName points to correct
     * implementation of javax.xml.parsers.SAXParserFactory , should return
     * newInstance of SAXParserFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewInstance(String factoryClassName, ClassLoader classLoader) throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance(factoryClassName, classLoader);
        SAXParser sp = spf.newSAXParser();
        assertNotNull(sp);
    }

    /*
     * test for SAXParserFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName is null , should
     * throw FactoryConfigurationError
     */
    @Test(expectedExceptions = FactoryConfigurationError.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewInstanceNeg(String factoryClassName, ClassLoader classLoader) {
        SAXParserFactory.newInstance(factoryClassName, classLoader);
    }

}
