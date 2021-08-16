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
 * @bug 4951414
 * @summary Try to get an IOException.
 * @author Shanliang JIANG
 *
 * @run clean GetConnectionTest
 * @run build GetConnectionTest
 * @run main GetConnectionTest
 */

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import javax.management.remote.JMXServiceURL;


public class GetConnectionTest {
    public static void main(String[] args) {
        System.out.println("Verify whether getting an IOException when "+
             "calling getMBeanServerConnection to a unconnected connector.");

        boolean ok = true;
        String[] protocols = {"rmi", "iiop", "jmxmp"};

        for (int i = 0; i < protocols.length; i++) {
            final String proto = protocols[i];
            System.out.println("Testing for protocol " + proto);
            try {
                ok &= test(proto);
            } catch (Exception e) {
                System.err.println("Unexpected exception: " + e);
                e.printStackTrace();
                ok = false;
            }
        }

        if (ok)
            System.out.println("All Tests passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto) throws Exception {
        JMXConnector client;
        try {
            JMXServiceURL url = new JMXServiceURL(proto, null, 0);
            client = JMXConnectorFactory.newJMXConnector(url, null);
        } catch (MalformedURLException e) {
            System.out.println("Protocol " + proto +
                               " not supported, ignoring");
            return true;
        }

        // IOException is expected
        try {
            MBeanServerConnection connection =
                client.getMBeanServerConnection();

            System.out.println("FAILED: Expected IOException is not thrown.");
            return false;
        } catch (IOException e) {
            System.out.println("PASSED");
            return true;
        }
    }
}
