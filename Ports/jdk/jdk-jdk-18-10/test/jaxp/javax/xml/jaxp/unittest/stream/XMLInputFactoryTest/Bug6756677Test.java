/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package stream.XMLInputFactoryTest;

import static jaxp.library.JAXPTestUtilities.runWithTmpPermission;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.util.PropertyPermission;

import javax.xml.stream.XMLInputFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6756677
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @compile MyInputFactory.java
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.XMLInputFactoryTest.Bug6756677Test
 * @run testng/othervm stream.XMLInputFactoryTest.Bug6756677Test
 * @summary Test XMLInputFactory.newFactory(String factoryId, ClassLoader classLoader).
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6756677Test {

    @Test
    public void testNewInstance() {
        String myFactory = "stream.XMLInputFactoryTest.MyInputFactory";
        try {
            setSystemProperty("MyInputFactory", myFactory);
            XMLInputFactory xif = runWithTmpPermission(() -> XMLInputFactory.newInstance("MyInputFactory", null),
                    new PropertyPermission("MyInputFactory", "read"));
            System.out.println(xif.getClass().getName());
            Assert.assertTrue(xif.getClass().getName().equals(myFactory));

        } catch (UnsupportedOperationException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    // newFactory was added in StAX 1.2
    @Test
    public void testNewFactory() {
        String myFactory = "stream.XMLInputFactoryTest.MyInputFactory";
        ClassLoader cl = null;
        try {
            setSystemProperty("MyInputFactory", myFactory);
            XMLInputFactory xif = runWithTmpPermission(() -> XMLInputFactory.newFactory("MyInputFactory", cl),
                    new PropertyPermission("MyInputFactory", "read"));
            System.out.println(xif.getClass().getName());
            Assert.assertTrue(xif.getClass().getName().equals(myFactory));

        } catch (UnsupportedOperationException oe) {
            Assert.fail(oe.getMessage());
        }
    }


    String XMLInputFactoryClassName = "com.sun.xml.internal.stream.XMLInputFactoryImpl";
    String XMLInputFactoryID = "javax.xml.stream.XMLInputFactory";
    ClassLoader CL = null;

    /*
     * test for XMLInputFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) classloader is null and
     * factoryClassName points to correct implementation of
     * javax.xml.stream.XMLInputFactory , should return newInstance of
     * XMLInputFactory
     */
    @Test
    public void test29() throws Exception {
        setSystemProperty(XMLInputFactoryID, XMLInputFactoryClassName);
        XMLInputFactory xif = XMLInputFactory.newInstance(XMLInputFactoryID, CL);
        Assert.assertTrue(xif instanceof XMLInputFactory, "xif should be an instance of XMLInputFactory");
    }

    /*
     * test for XMLInputFactory.newInstance(java.lang.String factoryClassName,
     * java.lang.ClassLoader classLoader) classloader is
     * default(Class.getClassLoader()) and factoryClassName points to correct
     * implementation of javax.xml.stream.XMLInputFactory , should return
     * newInstance of XMLInputFactory
     */
    @Test
    public void test31() throws Exception {
        Bug6756677Test test3 = new Bug6756677Test();
        ClassLoader cl = (test3.getClass()).getClassLoader();
        setSystemProperty(XMLInputFactoryID, XMLInputFactoryClassName);
        XMLInputFactory xif = XMLInputFactory.newInstance(XMLInputFactoryID, cl);
        Assert.assertTrue(xif instanceof XMLInputFactory, "xif should be an instance of XMLInputFactory");
    }
}
