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
 * @bug 4836468
 * @summary Test that the getClassLoaderRepository permission is not necessary
 * for the test codebase as it is executed by the MLet code using
 * doPrivileged.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MLetCommand
 * @run build MLetCommand
 * @run main/othervm/java.security.policy=policy MLetCommand
 */

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.io.File;

public class MLetCommand {

    public static void main(String[] args) throws Exception {
        if (System.getSecurityManager() == null)
            throw new IllegalStateException("No security manager installed!");

        System.out.println("java.security.policy=" +
                           System.getProperty("java.security.policy"));

        // Instantiate the MBean server
        //
        System.out.println("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        // Register the MLetMBean
        //
        System.out.println("Create MLet MBean");
        ObjectName mlet = new ObjectName("MLetTest:name=MLetMBean");
        mbs.createMBean("javax.management.loading.MLet", mlet);
        // Test OK!
        //
        System.out.println("Bye! Bye!");
    }
}
