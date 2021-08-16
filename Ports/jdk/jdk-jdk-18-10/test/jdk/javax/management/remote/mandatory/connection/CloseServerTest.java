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
 * @bug 4838640
 * @summary test server close in different conditions.
 * @author Shanliang JIANG
 *
 * @run clean CloseServerTest
 * @run build CloseServerTest
 * @run main CloseServerTest
 */

import java.net.MalformedURLException;
import java.io.IOException;

import javax.management.*;
import javax.management.remote.*;

public class CloseServerTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

    public static void main(String[] args) {
        System.out.println(">>> Tests for closing a server.");

        boolean ok = true;
        for (int i = 0; i < protocols.length; i++) {
            try {
                if (!test(protocols[i])) {
                    System.out.println(">>> Test failed for " + protocols[i]);
                    ok = false;
                } else {
                    System.out.println(">>> Test successed for " + protocols[i]);
                }
            } catch (Exception e) {
                System.out.println(">>> Test failed for " + protocols[i]);
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok) {
            System.out.println(">>> Test passed");
        } else {
            System.out.println(">>> TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto)
            throws Exception {
        System.out.println(">>> Test for protocol " + proto);
        JMXServiceURL u = new JMXServiceURL(proto, null, 0);
        JMXConnectorServer server;
        JMXServiceURL addr;
        JMXConnector client;
        MBeanServerConnection mserver;

        final ObjectName delegateName =
                    new ObjectName("JMImplementation:type=MBeanServerDelegate");
        final NotificationListener dummyListener = new NotificationListener() {
                public void handleNotification(Notification n, Object o) {
                    // do nothing
                    return;
                }
            };

        try {
            // open and close
            System.out.println(">>> Open and close a server.");

            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.stop();

            // open, start then close
            System.out.println(">>> Open, start and close a server.");

            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();
            server.stop();

            // with a client, but close the server first
            System.out.println(">>> Open, start a server, create a client, close the server then the client.");

            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();

            addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);
            client.connect(null);

            server.stop();

            try {
                client.close();
            } catch (Exception ee) {
                // OK, the server has been closed
            }

            // with a client, but close the client first
            System.out.println(">>> Open, start a server, create a client, close the client then server.");
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();

            addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);
            client.connect(null);

            client.close();

            server.stop();

            // with a client listener, but close the server first
            System.out.println(">>> Open, start a server, create a client, add a listener, close the server then the client.");
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();

            addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);
            client.connect(null);

            mserver = client.getMBeanServerConnection();
            mserver.addNotificationListener(delegateName, dummyListener, null, null);

            server.stop();

            try {
                client.close();
            } catch (Exception e) {
                // ok, it is because the server has been closed.
            }

            // with a client listener, but close the client first
            System.out.println(">>> Open, start a server, create a client, add a listener, close the client then the server.");
            server = JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();

            addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);
            client.connect(null);

            mserver = client.getMBeanServerConnection();
            mserver.addNotificationListener(delegateName, dummyListener, null, null);

            client.close();
            server.stop();
        } catch (MalformedURLException e) {
            System.out.println(">>> Skipping unsupported URL " + u);
            return true;
        }

        return true;
    }
}
