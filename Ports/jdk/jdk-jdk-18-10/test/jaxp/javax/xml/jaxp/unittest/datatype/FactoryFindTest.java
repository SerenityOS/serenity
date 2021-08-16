/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package datatype;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import java.net.URL;
import java.net.URLClassLoader;

import javax.xml.datatype.DatatypeFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.FactoryFindTest
 * @run testng/othervm datatype.FactoryFindTest
 * @summary Test Classloader for DatatypeFactory.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class FactoryFindTest {

    boolean myClassLoaderUsed = false;

    @Test
    public void testFactoryFind() throws Exception {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        Assert.assertTrue(factory.getClass().getClassLoader() == null);

        runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(null));

        factory = DatatypeFactory.newInstance();
        Assert.assertTrue(factory.getClass().getClassLoader() == null);

        runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(new MyClassLoader()));
        factory = DatatypeFactory.newInstance();
        if (System.getSecurityManager() == null)
            Assert.assertTrue(myClassLoaderUsed);
        else
            Assert.assertFalse(myClassLoaderUsed);
    }

    class MyClassLoader extends URLClassLoader {

        public MyClassLoader() {
            super(new URL[0]);
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            myClassLoaderUsed = true;
            return super.loadClass(name);
        }
    }
}
