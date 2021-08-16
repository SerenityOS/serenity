/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7120365
 * @summary test on Concurrent Modification
 * @author Shanliang JIANG
 *
 * @run main ConcurrentModificationTest
 */

import java.net.MalformedURLException;
import java.util.ConcurrentModificationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 */
public class ConcurrentModificationTest {
    private static final String[] protocols = {"rmi", "iiop", "jmxmp"};
    private static int number = 100;

    private static final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
    private static ObjectName delegateName;
    private static ObjectName[] timerNames = new ObjectName[number];
    private static NotificationListener[] listeners = new NotificationListener[number];

    private static Throwable uncaughtException = null;

    public static void main(String[] args) throws Exception {
        System.out.println(">>> test on Concurrent Modification.");

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                if (e instanceof ConcurrentModificationException) {
                    uncaughtException = e;
                }
            }
        });

        delegateName = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        for (int i=0; i<number; i++) {
            timerNames[i] = new ObjectName("MBean:name=Timer"+i);
            listeners[i] = new NotificationListener() {
                @Override
                public void handleNotification(Notification notification, Object handback) {
                    // nothing
                }
            };
        }
        String errors = "";

        for (int i = 0; i < protocols.length; i++) {
            uncaughtException = null;
            System.out.println(">>> Test for protocol " + protocols[i]);
            test(protocols[i]);
            if (uncaughtException != null) {
                if ("".equals(errors)) {
                    errors = "Failed to " + protocols[i] + ": "+uncaughtException;
                } else {
                    errors = errors+", failed to " + protocols[i] + ": "+uncaughtException;
                }
                System.out.println(">>> FAILED for protocol " + protocols[i]);
            } else {
                System.out.println(">>> PASSED for protocol " + protocols[i]);
            }
        }

        if ("".equals(errors)) {
            System.out.println("All Passed!");
        } else {
            System.out.println("!!!!!! Failed.");

            throw new RuntimeException(errors);
        }
    }

    private static void test(String proto) throws Exception {
        JMXServiceURL u = new JMXServiceURL(proto, null, 0);
        JMXConnectorServer server;
        JMXConnector client;

        try {
            server =
                    JMXConnectorServerFactory.newJMXConnectorServer(u, null, mbs);
            server.start();
            JMXServiceURL addr = server.getAddress();
            client = JMXConnectorFactory.connect(addr, null);
        } catch (MalformedURLException e) {
            System.out.println(">>> not support: " + proto);
            return;
        }

        final MBeanServerConnection mserver = client.getMBeanServerConnection();

        int count = 0;
        boolean adding = true;
        while (uncaughtException == null && count++ < 10) {
            for (int i = 0; i < number; i++) {
                listenerOp(mserver, listeners[i], adding);
                mbeanOp(mserver, timerNames[i], adding);
            }
            adding = !adding;
        }

        if (uncaughtException != null) { // clean
            for (int i = 0; i < number; i++) {
                try {
                    mbeanOp(mserver, timerNames[i], false);
                } catch (Exception e) {
                }
            }
        }
        client.close();
        server.stop();
    }

    private static void mbeanOp(MBeanServerConnection mserver, ObjectName name, boolean adding)
            throws Exception {
        if (adding) {
            mserver.createMBean("javax.management.timer.Timer", name);
        } else {
            mserver.unregisterMBean(name);
        }
    }

    private static void listenerOp(MBeanServerConnection mserver, NotificationListener listener, boolean adding)
            throws Exception {
        if (adding) {
            mserver.addNotificationListener(delegateName, listener, null, null);
        } else {
            mserver.removeNotificationListener(delegateName, listener);
        }
    }
}
