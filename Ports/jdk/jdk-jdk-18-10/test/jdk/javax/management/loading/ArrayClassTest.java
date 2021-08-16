/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4974913
 * @summary Test that array classes can be found in signatures always
 * and can be deserialized by the deprecated MBeanServer.deserialize method
 * @author Eamonn McManus
 *
 * @run clean ArrayClassTest
 * @run build ArrayClassTest
 * @run main ArrayClassTest
 */

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.Paths;
import javax.management.*;
import javax.management.loading.*;

public class ArrayClassTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        String[] cpaths = System.getProperty("test.classes", ".")
                                .split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        for (int i=0; i < cpaths.length; i++) {
            urls[i] = Paths.get(cpaths[i]).toUri().toURL();
        }

        // Create an MLet that can load the same class names but
        // will produce different results.
        ClassLoader loader = new SpyLoader(urls);
        ObjectName loaderName = new ObjectName("test:type=SpyLoader");
        mbs.registerMBean(loader, loaderName);

        ObjectName testName = new ObjectName("test:type=Test");
        mbs.createMBean(Test.class.getName(), testName, loaderName,
                        new Object[1], new String[] {X[].class.getName()});
        ClassLoader checkLoader = mbs.getClassLoaderFor(testName);
        if (checkLoader != loader)
            throw new AssertionError("Wrong loader: " + checkLoader);

        mbs.invoke(testName, "ignore", new Object[1],
                   new String[] {Y[].class.getName()});

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(new Z[0]);
        oout.close();
        byte[] bytes = bout.toByteArray();
        ObjectInputStream oin = mbs.deserialize(testName, bytes);
        Object zarray = oin.readObject();
        String failed = null;
        if (zarray instanceof Z[])
            failed = "read back a real Z[]";
        else if (!zarray.getClass().getName().equals(Z[].class.getName())) {
            failed = "returned object of wrong type: " +
                zarray.getClass().getName();
        } else if (Array.getLength(zarray) != 0)
            failed = "returned array of wrong size: " + Array.getLength(zarray);
        if (failed != null) {
            System.out.println("TEST FAILED: " + failed);
            System.exit(1);
        }

        System.out.println("Test passed");
    }

    public static interface TestMBean {
        public void ignore(Y[] ignored);
    }

    public static class Test implements TestMBean {
        public Test(X[] ignored) {}
        public void ignore(Y[] ignored) {}
    }

    public static class X {}
    public static class Y {}
    public static class Z implements Serializable {}

    public static interface SpyLoaderMBean {}

    /* We originally had this extend MLet but for some reason that
       stopped the bug from happening.  Some side-effect of registering
       the MLet in the MBean server caused it not to fail when asked
       to load Z[].  */
    public static class SpyLoader extends URLClassLoader
            implements SpyLoaderMBean, PrivateClassLoader {
        public SpyLoader(URL[] urls) {
            // important that the parent classloader be null!
            // otherwise we can pick up classes from the classpath
            super(urls, null);
        }

        /*
        public Class loadClass(String name) throws ClassNotFoundException {
            System.out.println("loadClass: " + name);
            return super.loadClass(name);
        }

        public Class loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            System.out.println("loadClass: " + name + ", " + resolve);
            return super.loadClass(name, resolve);
        }
        */

        public Class findClass(String name) throws ClassNotFoundException {
            System.out.println("findClass: " + name);
            if (false)
                new Throwable().printStackTrace(System.out);
            Class c = super.findClass(name);
            System.out.println(" -> " + name + " (" + c.getClassLoader() + ")");
            return c;
        }
    }
}
