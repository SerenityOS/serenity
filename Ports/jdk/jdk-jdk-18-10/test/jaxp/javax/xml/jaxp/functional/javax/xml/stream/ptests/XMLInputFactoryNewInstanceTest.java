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

package javax.xml.stream.ptests;

import static jaxp.library.JAXPTestUtilities.setSystemProperty;
import static jaxp.library.JAXPTestUtilities.clearSystemProperty;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;

import javax.xml.stream.XMLInputFactory;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.stream.ptests.XMLInputFactoryNewInstanceTest
 * @run testng/othervm javax.xml.stream.ptests.XMLInputFactoryNewInstanceTest
 * @summary Tests for XMLInputFactory.newFactory(factoryId , classLoader)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLInputFactoryNewInstanceTest {

    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.xml.internal.stream.XMLInputFactoryImpl";
    private static final String XMLINPUT_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;
    private static final String XMLINPUT_FACTORY_ID = "javax.xml.stream.XMLInputFactory";

    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] {
            { XMLINPUT_FACTORY_ID, null },
            { XMLINPUT_FACTORY_ID, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultFactory() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        XMLInputFactory if1 = XMLInputFactory.newDefaultFactory();
        XMLInputFactory if2 = XMLInputFactory.newFactory();
        assertNotSame(if1, if2, "same instance returned:");
        assertSame(if1.getClass(), if2.getClass(),
                  "unexpected class mismatch for newDefaultFactory():");
        assertEquals(if1.getClass().getName(), DEFAULT_IMPL_CLASS);
    }

    /*
     * test for XMLInputFactory.newFactory(java.lang.String factoryId,
     * java.lang.ClassLoader classLoader) factoryClassName points to correct
     * implementation of javax.xml.stream.XMLInputFactory , should return
     * newInstance of XMLInputFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewFactory(String factoryId, ClassLoader classLoader) {
        setSystemProperty(XMLINPUT_FACTORY_ID, XMLINPUT_FACTORY_CLASSNAME);
        try {
            XMLInputFactory xif = XMLInputFactory.newFactory(factoryId, classLoader);
            assertNotNull(xif);
        } finally {
            clearSystemProperty(XMLINPUT_FACTORY_ID);
        }
    }

    /*
     * test for XMLInputFactory.newFactory(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName is null , should
     * throw NullPointerException
     */
    @Test(expectedExceptions = NullPointerException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewFactoryNeg(String factoryId, ClassLoader classLoader) {
        XMLInputFactory.newFactory(factoryId, classLoader);
    }

}
