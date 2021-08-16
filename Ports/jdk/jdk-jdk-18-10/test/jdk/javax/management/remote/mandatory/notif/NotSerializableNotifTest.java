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
 * @bug 5022196 8132003
 * @summary Tests to send a not serializable notification.
 * @author Shanliang JIANG
 *
 * @run clean NotSerializableNotifTest
 * @run build NotSerializableNotifTest
 * @run main NotSerializableNotifTest
 */

// java imports
//
import java.net.MalformedURLException;
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

public class NotSerializableNotifTest {
    private static final MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();
    private static ObjectName emitter;

    private static String[] protocols = new String[] {"rmi", "iiop", "jmxmp"};

    private static final int sentNotifs = 10;

    public static void main(String[] args) throws Exception {
        System.out.println(">>> Test to send a not serializable notification");

        emitter = new ObjectName("Default:name=NotificationEmitter");
        mbeanServer.registerMBean(new NotificationEmitter(), emitter);

        for (int i = 0; i < protocols.length; i++) {
            test(protocols[i]);
        }

        System.out.println(">>> Test passed");
    }


    private static void test(String proto) throws Exception {
        System.out.println("\n>>> Test for protocol " + proto);

        JMXServiceURL url = new JMXServiceURL(proto, null, 0);

        System.out.println(">>> Create a server: "+url);

        JMXConnectorServer server = null;
        try {
            server = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbeanServer);
        } catch (MalformedURLException e) {
            System.out.println("System does not recognize URL: " + url +
                               "; ignoring");
            return;
        }

        server.start();

        url = server.getAddress();

        System.out.println(">>> Creating a client connectint to: "+url);
        JMXConnector conn = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection client = conn.getMBeanServerConnection();

        // add listener from the client side
        Listener listener = new Listener();
        client.addNotificationListener(emitter, listener, null, null);

        // ask to send one not serializable notif
        Object[] params = new Object[] {new Integer(1)};
        String[] signatures = new String[] {"java.lang.Integer"};
        client.invoke(emitter, "sendNotserializableNotifs", params, signatures);

        // listener clean
        client.removeNotificationListener(emitter, listener);
        listener = new Listener();
        client.addNotificationListener(emitter, listener, null, null);

        //ask to send serializable notifs
        params = new Object[] {new Integer(sentNotifs)};
        client.invoke(emitter, "sendNotifications", params, signatures);

        // waiting ...
        synchronized (listener) {
            while (listener.received() < sentNotifs) {
                listener.wait(); // either pass or test timeout (killed by test harness)

            }
        }

        // clean
        client.removeNotificationListener(emitter, listener);

        conn.close();
        server.stop();
    }

//--------------------------
// private classes
//--------------------------

    private static class Listener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            synchronized (this) {
                if(++receivedNotifs == sentNotifs) {
                    this.notifyAll();
                }
            }
        }

        public int received() {
            return receivedNotifs;
        }

        private int receivedNotifs = 0;
    }

    public static class NotificationEmitter extends NotificationBroadcasterSupport
        implements NotificationEmitterMBean {

        public MBeanNotificationInfo[] getNotificationInfo() {
            final String[] ntfTypes = {myType};

            final MBeanNotificationInfo[] ntfInfoArray  = {
                new MBeanNotificationInfo(ntfTypes,
                                          "javax.management.Notification",
                                          "Notifications sent by the NotificationEmitter")};

            return ntfInfoArray;
        }

        /**
         * Send not serializable Notifications.
         *
         * @param nb The number of notifications to send
         */
        public void sendNotserializableNotifs(Integer nb) {

            Notification notif;
            for (int i=1; i<=nb.intValue(); i++) {
                notif = new Notification(myType, this, i);

                notif.setUserData(new Object());
                sendNotification(notif);
            }
        }

        /**
         * Send Notification objects.
         *
         * @param nb The number of notifications to send
         */
        public void sendNotifications(Integer nb) {
            Notification notif;
            for (int i=1; i<=nb.intValue(); i++) {
                notif = new Notification(myType, this, i);

                sendNotification(notif);
            }
        }

        private final String myType = "notification.my_notification";
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Integer nb);

        public void sendNotserializableNotifs(Integer nb);
    }
}
