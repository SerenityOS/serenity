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
 * @bug 7654321
 * @summary Tests whether a listener receives notifs emitted before the
 * listener is registered.
 * @author Shanliang JIANG
 *
 * @run clean UnexpectedNotifTest
 * @run build UnexpectedNotifTest
 * @run main UnexpectedNotifTest
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
//
import javax.management.remote.rmi.RMIConnectorServer;

public class UnexpectedNotifTest {

    public static void main(String[] args) throws Exception {
        List<String> protos = new ArrayList<String>();
        protos.add("rmi");
        try {
            Class.forName("javax.management.remote.jmxmp.JMXMPConnectorServer");
            protos.add("jmxmp");
        } catch (ClassNotFoundException e) {
            // OK: JMXMP not present so don't test it.
        }
        for (String proto : protos)
            test(proto);
    }

    private static void test(String proto) throws Exception {
        System.out.println("Unexpected notifications test for protocol " +
                           proto);
        MBeanServer mbs = null;
        try {
            // Create a MBeanServer
            //
            mbs = MBeanServerFactory.createMBeanServer();

            // Create a NotificationEmitter MBean
            //
            mbean = new ObjectName ("Default:name=NotificationEmitter");
            mbs.registerMBean(new NotificationEmitter(), mbean);

            // Create a connector server
            //
            url = new JMXServiceURL("service:jmx:" + proto + "://");

            server = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                     null,
                                                                     mbs);

            mbs.registerMBean(
                        server,
                        new ObjectName("Default:name=ConnectorServer"));

            server.start();

            url = server.getAddress();

            for (int j = 0; j < 2; j++) {
                test();
            }
        } finally {
            // Stop server
            //
            server.stop();
            // Release the MBeanServer
            //
            MBeanServerFactory.releaseMBeanServer(mbs);
        }
    }

    private static void test() throws Exception {
        // Create client
        //
        JMXConnector connector = JMXConnectorFactory.connect(url);
        MBeanServerConnection client = connector.getMBeanServerConnection();

        // Add listener at the client side
        //
        client.addNotificationListener(mbean, listener, null, null);

        // Cleanup
        //
        receivedNotifs = 0;

        // Ask to send notifs
        //
        Object[] params = new Object[] {new Integer(nb)};
        String[] signatures = new String[] {"java.lang.Integer"};

        client.invoke(mbean, "sendNotifications", params, signatures);

        // Waiting...
        //
        synchronized (lock) {
            for (int i = 0; i < 10; i++) {
                if (receivedNotifs < nb) {
                    lock.wait(1000);
                }
            }
        }

        // Waiting again to ensure no more notifs
        //
        Thread.sleep(3000);

        synchronized (lock) {
            if (receivedNotifs != nb) {
                throw new Exception("The client expected to receive " +
                                    nb + " notifs, but got " + receivedNotifs);
            }
        }

        // Remove listener
        //
        client.removeNotificationListener(mbean, listener);

        connector.close();
    }

    //--------------------------
    // private classes
    //--------------------------

    private static class Listener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            System.out.println("Received: " + notif + " (" +
                               notif.getSequenceNumber() + ")");
            synchronized(lock) {
                if(++receivedNotifs == nb) {
                    lock.notifyAll();
                } else if (receivedNotifs > nb) {
                    System.out.println("The client expected to receive " +
                                       nb + " notifs, but got at least " +
                                       receivedNotifs);
                    System.exit(1);
                }
            }
        }
    }

    public static class NotificationEmitter
        extends NotificationBroadcasterSupport
        implements NotificationEmitterMBean {

        /**
         * Returns a NotificationInfo object containing the name of the Java
         * class of the notification and the notification types sent by this
         * notification broadcaster.
         */
        public MBeanNotificationInfo[] getNotificationInfo() {

            MBeanNotificationInfo[] ntfInfoArray = new MBeanNotificationInfo[1];

            String[] ntfTypes = new String[1];
            ntfTypes[0] = myType;

            ntfInfoArray[0] = new MBeanNotificationInfo(
                              ntfTypes,
                              "javax.management.Notification",
                              "Notifications sent by the NotificationEmitter");
            return ntfInfoArray;
        }

        /**
         * Send a Notification object with the specified times.
         * The sequence number will be from zero to times-1.
         *
         * @param nb The number of notifications to send
         */
        public void sendNotifications(Integer nb) {
            System.out.println("NotificationEmitter: asked to send " +
                               "notifications: " + nb);

            Notification notif;
            for (int i = 1; i <= nb.intValue(); i++) {
                notif = new Notification(myType, this, ++seqno);
                sendNotification(notif);
            }
        }

        private String myType = "notification.my_notification";
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Integer nb);
    }

    private static JMXConnectorServer server;
    private static JMXServiceURL url;
    private static ObjectName mbean;
    private static NotificationListener listener = new Listener();

    private static int nb = 10;
    private static int receivedNotifs = 0;
    private static int[] lock = new int[0];
    private static volatile long seqno;
}
