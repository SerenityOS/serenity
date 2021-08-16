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
 * @bug 6199899
 * @summary Tests reconnection done by a fetching notif thread.
 * @author Shanliang JIANG
 *
 * @run clean NotifReconnectDeadlockTest
 * @run build NotifReconnectDeadlockTest
 * @run main NotifReconnectDeadlockTest
 */

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * "This test checks for a bug whereby reconnection did not work if (a) it was
 * initiated by the fetchNotifications thread and (b) it succeeded. These conditions
 * are not usual, since connection failure is usually caused by either idle timeout
 * (which doesn't usually happen if fetchNotifications is running) or communication
 * problems (which are usually permanent so reconnection fails).  But they can happen,
 * so we test for them here.
 * The test sets a very short idle timeout, and effectively suspends the
 * fetchNotifications thread by having it invoke a listener with a delay in it.
 * This means that the idle timeout happens.  When the delayed listener returns,
 * the fetchNotifications thread will attempt to reconnect, and this attempt should
 * succeed, so we meet the two conditions above.
 * The test succeeds if there is indeed a reconnection, detected by the connection
 * listener seeing an OPENED notification.  The connection listener should not see
 * a CLOSED or FAILED notification."
 */
public class NotifReconnectDeadlockTest {

    public static void main(String[] args) throws Exception {
        System.out.println(
           ">>> Tests reconnection done by a fetching notif thread.");

        ObjectName oname = new ObjectName ("Default:name=NotificationEmitter");
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        Map env = new HashMap(2);
        env.put("jmx.remote.x.server.connection.timeout", new Long(serverTimeout));
        env.put("jmx.remote.x.client.connection.check.period", new Long(Long.MAX_VALUE));

        final MBeanServer mbs = MBeanServerFactory.newMBeanServer();

        mbs.registerMBean(new NotificationEmitter(), oname);
        JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(
                                                                               url,
                                                                               env,
                                                                               mbs);
        server.start();

        JMXServiceURL addr = server.getAddress();
        JMXConnector client = JMXConnectorFactory.connect(addr, env);

        Thread.sleep(100); // let pass the first client open notif if there is
        client.getMBeanServerConnection().addNotificationListener(oname,
                                                                  listener,
                                                                  null,
                                                                  null);

        client.addConnectionNotificationListener(listener, null, null);

        // max test time: 2 minutes
        final long end = System.currentTimeMillis()+120000;

        synchronized(lock) {
            while(clientState == null && System.currentTimeMillis() < end) {
                mbs.invoke(oname, "sendNotifications",
                           new Object[] {new Notification("MyType", "", 0)},
                           new String[] {"javax.management.Notification"});

                try {
                    lock.wait(10);
                } catch (Exception e) {}
            }
        }

        if (clientState == null) {
            throw new RuntimeException(
                  "No reconnection happened, need to reconfigure the test.");
        } else if (JMXConnectionNotification.FAILED.equals(clientState) ||
                   JMXConnectionNotification.CLOSED.equals(clientState)) {
            throw new RuntimeException("Failed to reconnect.");
        }

        System.out.println(">>> Passed!");

        client.removeConnectionNotificationListener(listener);
        client.close();
        server.stop();
    }

//--------------------------
// private classes
//--------------------------
    public static class NotificationEmitter extends NotificationBroadcasterSupport
        implements NotificationEmitterMBean {

        public void sendNotifications(Notification n) {
            sendNotification(n);
        }
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Notification n);
    }

    private final static NotificationListener listener = new NotificationListener() {
            public void handleNotification(Notification n, Object hb) {

                // treat the client notif to know the end
                if (n instanceof JMXConnectionNotification) {
                    if (!JMXConnectionNotification.NOTIFS_LOST.equals(n.getType())) {

                        clientState = n.getType();
                        System.out.println(
                           ">>> The client state has been changed to: "+clientState);

                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    }

                    return;
                }

                System.out.println(">>> Do sleep to make reconnection.");
                synchronized(lock) {
                    try {
                        lock.wait(listenerSleep);
                    } catch (Exception e) {
                        // OK
                    }
                }
            }
        };

    private static final long serverTimeout = 1000;
    private static final long listenerSleep = serverTimeout*6;

    private static String clientState = null;
    private static final int[] lock = new int[0];
}
