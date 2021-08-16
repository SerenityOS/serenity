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
 * @bug 4934236
 * @summary Tests that NotificationBuffer is created when used.
 * @author jfd@...
 *
 * @run clean NotificationBufferCreationTest NotificationSender
 * @run build NotificationBufferCreationTest
 * @run main NotificationBufferCreationTest
 */
import java.net.MalformedURLException;

import javax.management.MBeanServerFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.Notification;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class NotificationBufferCreationTest {
    private static final MBeanServer mbs =
        MBeanServerFactory.createMBeanServer();
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    public static void main(String[] args) {
        try {
            boolean error = false;
            ObjectName notifierName =
                new ObjectName("TestDomain:type=NotificationSender");

            NotificationSender s = new NotificationSender();
            mbs.registerMBean(s, notifierName);

            for(int i = 0; i < protocols.length; i++) {
                try {
                    System.out.println("dotest for " + protocols[i]);
                    dotest(protocols[i], s, notifierName);
                }catch(Exception e) {
                    e.printStackTrace();
                    error = true;
                }
            }

            if(error)
                System.exit(1);

            System.out.println("Test OK");

        }catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    private static void dotest(String protocol,
                               NotificationSender s,
                               ObjectName notifierName) throws Exception {
        JMXConnector client = null;
        JMXConnectorServer server = null;
        JMXServiceURL u = null;
        try {
            u = new JMXServiceURL(protocol, null, 0);
            server =
                JMXConnectorServerFactory.newJMXConnectorServer(u,
                                                                null,
                                                                mbs);
            checkNotifier(s, 0, "new ConnectorServer");

            server.start();

            checkNotifier(s, 0, "ConnectorServer start");

            JMXServiceURL addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);

            checkNotifier(s, 0, "new Connector");

            client.connect(null);

            checkNotifier(s, 0, "Connector connect");

            MBeanServerConnection mbsc = client.getMBeanServerConnection();

            final NotificationListener dummyListener =
                new NotificationListener() {
                        public void handleNotification(Notification n,
                                                       Object o) {
                            // do nothing
                            return;
                        }
                    };

            mbsc.addNotificationListener(notifierName,
                                         dummyListener,
                                         null,
                                         null);

            // 1 Listener is expected to be added by the ServerNotifForwader
            checkNotifier(s, 1, "addNotificationListener");

            mbsc.removeNotificationListener(notifierName,
                                            dummyListener);
            System.out.println("Test OK for " + protocol);
        }catch(MalformedURLException e) {
            System.out.println("Skipping URL " + u);
        }
        finally {
            if(client != null)
                client.close();
            if(server != null)
                server.stop();
        }
    }

    private static void checkNotifier(NotificationSender s,
                                      int expectedListenerCount,
                                      String msg) throws Exception {
        int count = s.getListenerCount();
        if(count != expectedListenerCount) {
            String errorMsg = "Invalid expected listener count [" +
                expectedListenerCount + "], real [" +  count +"] for " + msg;
            System.out.println(errorMsg);
            throw new Exception(errorMsg);
        }

    }
}
