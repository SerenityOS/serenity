/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6356458
 * @summary test to not lose a user classloader
 * @author Shanliang JIANG
 *
 * @run clean UserClassLoaderTest
 * @run build UserClassLoaderTest
 * @run main UserClassLoaderTest
 */

import java.util.*;
import java.net.*;
import java.io.IOException;

import javax.management.*;
import javax.management.remote.*;

public class UserClassLoaderTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
    private static ObjectName timer;
    private final static NotificationListener listener = new NotificationListener() {
            public void handleNotification(Notification notification, Object handback) {
            }
        };

    public static void main(String[] args) throws Exception {
        System.out.println("main: we should not lose client classloader.");

        timer = new ObjectName("test:name=timer");
        mbs.createMBean("javax.management.timer.Timer", timer);

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                if (!test(protocols[i])) {
                    System.out.println("main: Test failed for " + protocols[i]);
                    ok = false;
                } else {
                    System.out.println("main: Test successed for " + protocols[i]);
                }
            } catch (Exception e) {
                System.out.println("main: Test failed for " + protocols[i]);
                e.printStackTrace(System.out);
                ok = false;
            }        }

        if (ok) {
            System.out.println("main: Tests passed");
        } else {
            System.out.println("main: Tests FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto) throws Exception {
        System.out.println("\ntest: Test for protocol " + proto);

        JMXServiceURL u = null;
        JMXConnectorServer server = null;

        try {
            u = new JMXServiceURL(proto, null, 0);
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
        } catch (MalformedURLException e) {
            System.out.println("Skipping unsupported URL " + proto);
            return true;
        }

        server.start();
        u = server.getAddress();

        System.out.println("test: create a server on: "+u);

        JMXConnector client = JMXConnectorFactory.connect(u, null);
        MBeanServerConnection conn = client.getMBeanServerConnection();

        final ClassLoader orgCL = Thread.currentThread().getContextClassLoader();
        System.out.println("test: the orginal classloader is "+orgCL);

        final URL url = new URL("file:/xxx");
        final ClassLoader newCL = new URLClassLoader(new URL[]{url}, orgCL);

        try {
            System.out.println("test: set classloader to "+newCL);
            Thread.currentThread().setContextClassLoader(newCL);

            // reproduce the bug
            conn.addNotificationListener(timer, listener, null, null);

            client.close();
            server.stop();

            if (Thread.currentThread().getContextClassLoader() != newCL) {
                System.out.println("ERROR: The client class loader is lost.");

                return false;
            } else {
                System.out.println("test: Bye bye.");

                return true;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(orgCL);
        }
    }
}
