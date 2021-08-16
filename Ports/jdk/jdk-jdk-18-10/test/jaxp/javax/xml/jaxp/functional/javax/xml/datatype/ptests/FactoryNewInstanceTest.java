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

package javax.xml.datatype.ptests;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.datatype.ptests.FactoryNewInstanceTest
 * @run testng/othervm javax.xml.datatype.ptests.FactoryNewInstanceTest
 * @summary Tests for DatatypeFactory.newInstance(factoryClassName , classLoader)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class FactoryNewInstanceTest {

    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl";
    private static final String DATATYPE_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;

    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] { { DATATYPE_FACTORY_CLASSNAME, null }, { DATATYPE_FACTORY_CLASSNAME, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultInstance() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        DatatypeFactory dtf1 = DatatypeFactory.newDefaultInstance();
        DatatypeFactory dtf2 = DatatypeFactory.newInstance();
        assertNotSame(dtf1, dtf2, "same instance returned:");
        assertSame(dtf1.getClass(), dtf2.getClass(),
                  "unexpected class mismatch for newDefaultInstance():");
        assertEquals(dtf1.getClass().getName(), DEFAULT_IMPL_CLASS);
    }

    /*
     * test for DatatypeFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName points to correct
     * implementation of javax.xml.datatype.DatatypeFactory , should return
     * newInstance of DatatypeFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewInstance(String factoryClassName, ClassLoader classLoader) throws DatatypeConfigurationException {
        DatatypeFactory dtf = DatatypeFactory.newInstance(DATATYPE_FACTORY_CLASSNAME, null);
        Duration duration = dtf.newDuration(true, 1, 1, 1, 1, 1, 1);
        assertNotNull(duration);
    }


    /*
     * test for DatatypeFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName is null , should
     * throw DatatypeConfigurationException
     */
    @Test(expectedExceptions = DatatypeConfigurationException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewInstanceNeg(String factoryClassName, ClassLoader classLoader) throws DatatypeConfigurationException {
        DatatypeFactory.newInstance(factoryClassName, classLoader);
    }

}
