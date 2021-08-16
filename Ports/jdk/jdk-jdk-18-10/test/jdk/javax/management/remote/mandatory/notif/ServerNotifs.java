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
 * @test ServerNotifs.java
 * @bug 7654321
 * @summary Tests the reception of the notifications for opened and closed
 * connections
 * @author sjiang
 *
 * @run clean ServerNotifs
 * @run build ServerNotifs
 * @run main ServerNotifs
 */

// JAVA
import java.io.*;
import java.net.*;
import java.util.*;

// JMX
import javax.management.*;

// RJMX
import javax.management.remote.*;

public class ServerNotifs {

    private static void echo(String msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) {

        try {
            // Create MBeanServer
            //
            echo("---Create the MBeanServer...");
            MBeanServer mbs = MBeanServerFactory.createMBeanServer();

            // Create RMIConnectorServer
            //
            echo("---Instantiate the RMIConnectorServer...");
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                null,
                                                                mbs);

            echo("---Register the RMIConnectorServer in the MBeanServer...");
            ObjectName on =
                new ObjectName("JMXConnectors:name=RMIConnectorServer");
            mbs.registerMBean(cs, on);

            echo("---Start the RMIConnectorServer...");
            cs.start();
            url = cs.getAddress();
            echo("---RMIConnectorServer address: " + url);

            echo("---Add a local listener to the RMIConnectorServer...");
            mbs.addNotificationListener(on, new MyListener(), null, null);

            // Create RMI connector
            //
            echo("---Instantiate the RMIConnector...");
            JMXConnector c = JMXConnectorFactory.newJMXConnector(url, null);

            // Expect to get a "jmx.remote.connection.opened" notification
            //
            echo("---Open connection...");
            c.connect(null);
            Thread.sleep(100);

            // Expect to get a "jmx.remote.connection.closed" notification
            //
            echo("---Close connection...");
            c.close();
            Thread.sleep(100);

            // Waiting for all notifications
            //
            synchronized(waiting) {
                if (!succeeded) {
                    final long waitingTime = 10000;
                    long remainingTime = waitingTime;
                    final long startTime = System.currentTimeMillis();
                    while (!succeeded && remainingTime > 0) {
                        waiting.wait(remainingTime);
                        remainingTime = waitingTime -
                            (System.currentTimeMillis() - startTime);
                    }
                }
            }

            // Stop the RMIConnectorServer
            //
            echo("---Stop the RMIConnectorServer...");
            cs.stop();

            if (!succeeded) {
                System.out.println("Timeout, did not get all notifications!");
                System.exit(1);
            }
        } catch (MBeanException mbe) {
            echo("---Test failed.");
            echo("---Got exception: " + mbe);
            mbe.getTargetException().printStackTrace();
            System.exit(1);
        } catch (RuntimeOperationsException roe) {
            echo("---Test failed.");
            echo("---Got exception: " + roe);
            roe.getTargetException().printStackTrace();
            System.exit(1);
        } catch (Throwable t) {
            echo("---Test failed.");
            echo("---Got throwable: " + t);
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static class MyListener implements NotificationListener {
        public void handleNotification(Notification n, Object o) {
            if (index == types.length) {
                return;
            }
            echo("---Got a notification: " + n.getType());
            echo(n.getMessage());
            if (n instanceof JMXConnectionNotification) {
                if (!n.getType().equals(types[index++])) {
                    System.out.println("Waiting to get a notification with " +
                                       "type: " + types[index-1] + ", but " +
                                       "got one with type: " + n.getType());
                    System.exit(1);
                }
                if (index == types.length) {
                    synchronized(waiting) {
                        succeeded = true;
                        waiting.notify();
                    }
                }
            }
        }
    }

    private static final String[] types =
        new String[] {JMXConnectionNotification.OPENED,
                      JMXConnectionNotification.CLOSED};
    private static int index = 0;
    private static int[] waiting = new int[0];
    private static boolean succeeded = false;
}
