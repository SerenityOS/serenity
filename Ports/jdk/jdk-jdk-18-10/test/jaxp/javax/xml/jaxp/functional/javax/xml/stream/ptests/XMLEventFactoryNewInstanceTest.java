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

import javax.xml.stream.XMLEventFactory;

import jaxp.library.JAXPDataProvider;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8169778
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.stream.ptests.XMLEventFactoryNewInstanceTest
 * @run testng/othervm javax.xml.stream.ptests.XMLEventFactoryNewInstanceTest
 * @summary Tests for XMLEventFactory.newFactory(factoryId , classLoader)
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLEventFactoryNewInstanceTest {

    private static final String DEFAULT_IMPL_CLASS =
        "com.sun.xml.internal.stream.events.XMLEventFactoryImpl";
    private static final String XMLEVENT_FACTORY_CLASSNAME = DEFAULT_IMPL_CLASS;
    private static final String XMLEVENT_FACTORY_ID = "javax.xml.stream.XMLEventFactory";

    @DataProvider(name = "parameters")
    public Object[][] getValidateParameters() {
        return new Object[][] {
            { XMLEVENT_FACTORY_ID, null },
            { XMLEVENT_FACTORY_ID, this.getClass().getClassLoader() } };
    }

    /**
     * Test if newDefaultFactory() method returns an instance
     * of the expected factory.
     * @throws Exception If any errors occur.
     */
    @Test
    public void testDefaultInstance() throws Exception {
        XMLEventFactory ef1 = XMLEventFactory.newDefaultFactory();
        XMLEventFactory ef2 = XMLEventFactory.newFactory();
        assertNotSame(ef1, ef2, "same instance returned:");
        assertSame(ef1.getClass(), ef2.getClass(),
                  "unexpected class mismatch for newDefaultFactory():");
        assertEquals(ef1.getClass().getName(), DEFAULT_IMPL_CLASS);
    }

    /*
     * test for XMLEventFactory.newFactory(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName points to correct
     * implementation of javax.xml.stream.XMLEventFactory , should return
     * newInstance of XMLEventFactory
     */
    @Test(dataProvider = "parameters")
    public void testNewFactory(String factoryId, ClassLoader classLoader) {
        setSystemProperty(XMLEVENT_FACTORY_ID, XMLEVENT_FACTORY_CLASSNAME);
        try {
            XMLEventFactory xef = XMLEventFactory.newFactory(factoryId, classLoader);
            assertNotNull(xef);
        } finally {
            clearSystemProperty(XMLEVENT_FACTORY_ID);
        }
    }

    /*
     * test for XMLEventFactory.newFactory(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) factoryClassName is null , should
     * throw NullPointerException
     */
    @Test(expectedExceptions = NullPointerException.class, dataProvider = "new-instance-neg", dataProviderClass = JAXPDataProvider.class)
    public void testNewFactoryNeg(String factoryId, ClassLoader classLoader) {
        XMLEventFactory.newFactory(null, null);
    }

}
