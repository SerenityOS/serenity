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
 * @bug 4917237
 * @summary test that process exit immediately after stop() / close() called
 * @author Jean Francois Denise
 *
 * @run clean RMIExitTest
 * @run build RMIExitTest
 * @run main RMIExitTest
 */

import java.net.MalformedURLException;
import java.io.IOException;

import javax.management.MBeanServerFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MBeanServerConnection;
import javax.management.NotificationListener;
import javax.management.Notification;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXConnectorFactory;

/**
 * VM shutdown hook. Test that the hook is called less than 5 secs
 * after expected exit.
 */
class TimeChecker extends Thread {
    @Override
    public void run() {
        System.out.println("shutdown hook called");
        long elapsedTime =
            System.currentTimeMillis() - RMIExitTest.exitStartTime;
        if(elapsedTime >= 5000) {
            System.out.println("BUG 4917237 not Fixed.");
            // Once in hook, to provide an exit status != 0, halt must
            // be called. Hooks are not called when halt is called.
            Runtime.getRuntime().halt(1);
        } else {
            System.out.println("BUG 4917237 Fixed");
        }
    }
}

/**
 * Start a server, connect a client, add/remove listeners, close client,
 * stop server. Check that VM exits in less than 5 secs.
 *
 */
public class RMIExitTest {
    private static final MBeanServer mbs =
        MBeanServerFactory.createMBeanServer();
    public static long exitStartTime = 0;

    public static void main(String[] args) {
        System.out.println("Start test");
        Runtime.getRuntime().addShutdownHook(new TimeChecker());
        test();
        exitStartTime = System.currentTimeMillis();
        System.out.println("End test");
    }

    private static void test() {
        try {
            JMXServiceURL u = new JMXServiceURL("rmi", null, 0);
            JMXConnectorServer server;
            JMXServiceURL addr;
            JMXConnector client;
            MBeanServerConnection mserver;

            final ObjectName delegateName =
                new ObjectName("JMImplementation:type=MBeanServerDelegate");
            final NotificationListener dummyListener =
                new NotificationListener() {
                        public void handleNotification(Notification n,
                                                       Object o) {
                            // do nothing
                            return;
                        }
                    };

            server = JMXConnectorServerFactory.newJMXConnectorServer(u,
                                                                     null,
                                                                     mbs);
            server.start();

            addr = server.getAddress();
            client = JMXConnectorFactory.newJMXConnector(addr, null);
            client.connect(null);

            mserver = client.getMBeanServerConnection();
            String s1 = "1";
            String s2 = "2";
            String s3 = "3";

            mserver.addNotificationListener(delegateName,
                                            dummyListener, null, s1);
            mserver.addNotificationListener(delegateName,
                                            dummyListener, null, s2);
            mserver.addNotificationListener(delegateName,
                                            dummyListener, null, s3);

            mserver.removeNotificationListener(delegateName,
                                               dummyListener, null, s3);
            mserver.removeNotificationListener(delegateName,
                                               dummyListener, null, s2);
            mserver.removeNotificationListener(delegateName,
                                               dummyListener, null, s1);
            client.close();

            server.stop();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
