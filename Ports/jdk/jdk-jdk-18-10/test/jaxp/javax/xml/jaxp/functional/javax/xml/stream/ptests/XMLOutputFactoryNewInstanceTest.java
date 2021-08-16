/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.xml.stream.XMLOutputFactory;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.stream.ptests.XMLOutputFactoryNewInstanceTest
 * @run testng/othervm javax.xml.stream.ptests.XMLOutputFactoryNewInstanceTest
 * @summary Tests for XMLOutputFactory.newFactory(factoryId , classLoader)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLOutputFactoryNewInstanceTest {

    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.xml.internal.stream.XMLOutputFactoryImpl";
    private static final String XMLOUTPUT_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;
    private static final String XMLOUTPUT_FACTORY_ID = "javax.xml.stream.XMLOutputFactory";

    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] {
            { XMLOUTPUT_FACTORY_ID, null },
            { XMLOUTPUT_FACTORY_ID, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultFactory() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        XMLOutputFactory of1 = XMLOutputFactory.newDefaultFactory();
        XMLOutputFactory of2 = XMLOutputFactory.newFactory();
        assertNotSame(of1, of2, "same instance returned:");
        assertSame(of1.getClass(), of2.getClass(),
                  "unexpected class mismatch for newDefaultFactory():");
        assertEquals(of1.getClass().getName(), DEFAULT_IMPL_CLASS);
    }

    /*
     * test for XMLOutputFactory.newFactory(java.lang.String factoryId,
     * java.lang.ClassLoader classLoader) factoryClassName points to correct
     * implementation of javax.xml.stream.XMLOutputFactory , should return
     * newInstance of XMLOutputFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewFactory(String factoryId, ClassLoader classLoader) {
        setSystemProperty(XMLOUTPUT_FACTORY_ID, XMLOUTPUT_FACTORY_CLASSNAME);
        try {
            XMLOutputFactory xif = XMLOutputFactory.newFactory(factoryId, classLoader);
            assertNotNull(xif);
        } finally {
            clearSystemProperty(XMLOUTPUT_FACTORY_ID);
        }
    }

    /*
     * test for XMLOutputFactory.newFactory(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName is null , should
     * throw NullPointerException
     */
    @Test(expectedExceptions = NullPointerException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewFactoryNeg(String factoryId, ClassLoader classLoader) {
        XMLOutputFactory.newFactory(factoryId, classLoader);
    }

}
