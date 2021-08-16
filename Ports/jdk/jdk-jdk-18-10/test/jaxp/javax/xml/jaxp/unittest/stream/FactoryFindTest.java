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

package stream;

import static jaxp.library.JAXPTestUtilities.getSystemProperty;
import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.FactoryFindTest
 * @run testng/othervm stream.FactoryFindTest
 * @summary Test SaTX factory using factory property and using ContextClassLoader.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class FactoryFindTest {

    boolean myClassLoaderUsed = false;

    final static String FACTORY_KEY = "javax.xml.stream.XMLInputFactory";

//    @BeforeClass
//    public void setup(){
//        policy.PolicyUtil.changePolicy(getClass().getResource("FactoryFindTest.policy").getFile());
//    }

    @Test(enabled=false) // due to 8156508
    public void testFactoryFindUsingStaxProperties() {
        // If property is defined, will take precendence so this test
        // is ignored :(
        if (getSystemProperty(FACTORY_KEY) != null) {
            return;
        }

        Properties props = new Properties();
        String configFile = getSystemProperty("java.home") + File.separator + "lib" + File.separator + "stax.properties";

        File f = new File(configFile);
        if (f.exists()) {
            try {
                FileInputStream fis = new FileInputStream(f);
                props.load(fis);
                fis.close();
            } catch (FileNotFoundException e) {
                return;
            } catch (IOException e) {
                return;
            }
        } else {
            props.setProperty(FACTORY_KEY, "com.sun.xml.internal.stream.XMLInputFactoryImpl");
            try {
                FileOutputStream fos = new FileOutputStream(f);
                props.store(fos, null);
                fos.close();
                f.deleteOnExit();
            } catch (FileNotFoundException e) {
                return;
            } catch (IOException e) {
                return;
            }
        }

        XMLInputFactory factory = XMLInputFactory.newInstance();
        Assert.assertTrue(factory.getClass().getName().equals(props.getProperty(FACTORY_KEY)));
    }

    @Test
    public void testFactoryFind() {
        try {
            // setSystemProperty("jaxp.debug", "true");

            XMLInputFactory factory = XMLInputFactory.newInstance();
            Assert.assertTrue(factory.getClass().getClassLoader() == null);

            runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(null));
            factory = XMLInputFactory.newInstance();
            Assert.assertTrue(factory.getClass().getClassLoader() == null);

            runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(new MyClassLoader()));
            factory = XMLInputFactory.newInstance();
            // because it's decided by having sm or not in FactoryFind code
            if (System.getSecurityManager() == null)
                Assert.assertTrue(myClassLoaderUsed);
            else
                Assert.assertFalse(myClassLoaderUsed);

            XMLOutputFactory ofactory = XMLOutputFactory.newInstance();
            Assert.assertTrue(ofactory.getClass().getClassLoader() == null);

            runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(null));
            ofactory = XMLOutputFactory.newInstance();
            Assert.assertTrue(ofactory.getClass().getClassLoader() == null);

            runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(new MyClassLoader()));
            ofactory = XMLOutputFactory.newInstance();
            if (System.getSecurityManager() == null)
                Assert.assertTrue(myClassLoaderUsed);
            else
                Assert.assertFalse(myClassLoaderUsed);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
