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
 * @summary Tests to receive notifications for opened and closed connections
 * @author sjiang
 *
 * @run clean RMINotifTest
 * @run build RMINotifTest
 * @run main RMINotifTest
 * @run main RMINotifTest event
 */

// java imports
//

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class RMINotifTest {

    public static void main(String[] args) {
        try {
            // create a rmi registry
            Registry reg = null;
            int port = 6666;
            final Random r = new Random();

            while(port++<7000) {
                try {
                    reg = LocateRegistry.createRegistry(++port);
                    System.out.println("Creation of rmi registry succeeded. Running on port " + port);
                    break;
                } catch (RemoteException re) {
                // no problem
                }
            }

            if (reg == null) {
                System.out.println("Failed to create a RMI registry, "+
                                   "the ports from 6666 to 6999 are all occupied.");
                System.exit(1);
            }

            // create a MBeanServer
            MBeanServer server = MBeanServerFactory.createMBeanServer();

            // create a notif emitter mbean
            ObjectName mbean = new ObjectName ("Default:name=NotificationEmitter");

            server.registerMBean(new NotificationEmitter(), mbean);

            // create a rmi server
            JMXServiceURL url =
                new JMXServiceURL("rmi", null, port,
                                  "/jndi/rmi://:" + port + "/server" + port);
            System.out.println("RMIConnectorServer address " + url);

            JMXConnectorServer sServer =
                JMXConnectorServerFactory.newJMXConnectorServer(url, null,
                                                                null);

            ObjectInstance ss = server.registerMBean(sServer, new ObjectName("Default:name=RmiConnectorServer"));

            sServer.start();

            // create a rmi client
            JMXConnector rmiConnection =
                JMXConnectorFactory.newJMXConnector(url, null);
            rmiConnection.connect(null);
            MBeanServerConnection client = rmiConnection.getMBeanServerConnection();

            // add listener at the client side
            client.addNotificationListener(mbean, listener, null, null);

            //ask to send notifs
            Object[] params = new Object[1];
            String[] signatures = new String[1];

            params[0] = new Integer(nb);
            signatures[0] = "java.lang.Integer";

            client.invoke(mbean, "sendNotifications", params, signatures);

            // waiting ...
            synchronized (lock) {
                if (receivedNotifs != nb) {
                    lock.wait(10000);
                    System.out.println(">>> Received notifications..."+receivedNotifs);

                }
            }

            // check
            if (receivedNotifs != nb) {
                System.exit(1);
            } else {
                System.out.println("The client received all notifications.");
            }

            // remove listener
            client.removeNotificationListener(mbean, listener);

            // more test
            NotificationFilterSupport filter = new NotificationFilterSupport();
            Object o = new Object();
            client.addNotificationListener(mbean, listener, filter, o);
            client.removeNotificationListener(mbean, listener, filter, o);

            sServer.stop();

//          // clean
//          client.unregisterMBean(mbean);
//          rmiConnection.close();

//          Thread.sleep(2000);



        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

//--------------------------
// private classes
//--------------------------

    private static class Listener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            if(++receivedNotifs == nb) {
                synchronized(lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    public static class NotificationEmitter extends NotificationBroadcasterSupport implements NotificationEmitterMBean {
//      public NotificationEmitter() {
//              super();
// System.out.println("===NotificationEmitter: new instance.");
//      }

        /**
         * Returns a NotificationInfo object containing the name of the Java class of the notification
         * and the notification types sent by this notification broadcaster.
         */
        public MBeanNotificationInfo[] getNotificationInfo() {

            MBeanNotificationInfo[] ntfInfoArray  = new MBeanNotificationInfo[1];

            String[] ntfTypes = new String[1];
            ntfTypes[0] = myType;

            ntfInfoArray[0] = new MBeanNotificationInfo(ntfTypes,
                                                        "javax.management.Notification",
                                                        "Notifications sent by the NotificationEmitter");
            return ntfInfoArray;
        }

//      public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
//          super.addNotificationListener(listener, filter, handback);

//          System.out.println("============NotificationEmitter: add new listener");
//      }

        /**
         * Send a Notification object with the specified times.
         * The sequence number will be from zero to times-1.
         *
         * @param nb The number of notifications to send
         */
        public void sendNotifications(Integer nb) {
            System.out.println("===NotificationEmitter: be asked to send notifications: "+nb);

            Notification notif;
            for (int i=1; i<=nb.intValue(); i++) {
                notif = new Notification(myType, this, i);
                sendNotification(notif);
            }
        }

        private String myType = "notification.my_notification";
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Integer nb);
    }

    private static NotificationListener listener = new Listener();

    private static int nb = 10;
    private static int receivedNotifs = 0;
    private static int[] lock = new int[0];
}
