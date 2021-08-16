/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4839389
 * @summary Test that a class can load MBeans from its class loader
 * (at least if it is the system class loader)
 * @author Eamonn McManus
 *
 * @run clean SystemClassLoaderTest
 * @run build SystemClassLoaderTest
 * @run main SystemClassLoaderTest
 */

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/* Test that we can load an MBean using createMBean(className, objectName)
   even if the class of the MBean is not known to the JMX class loader.  */
public class SystemClassLoaderTest {

    public static void main(String[] args) throws Exception {
        // Instantiate the MBean server
        //
        System.out.println("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        ClassLoader mbsClassLoader = mbs.getClass().getClassLoader();

        String testClassName = Test.class.getName();

        // Check that the MBeanServer class loader does not know our test class
        try {
            Class.forName(testClassName, true, mbsClassLoader);
            System.out.println("TEST IS INVALID: MBEANSERVER'S CLASS LOADER " +
                               "KNOWS OUR TEST CLASS");
            System.exit(1);
        } catch (ClassNotFoundException e) {
            // As required
        }

        // Register the MBean
        //
        System.out.println("Create MBean from this class");
        ObjectName objectName = new ObjectName("whatever:type=whatever");
        mbs.createMBean(testClassName, objectName);
        // Test OK!
        //
        System.out.println("Bye! Bye!");
    }

    public static class Test implements TestMBean {
        public Test() {}
    }

    public static interface TestMBean {}
}
