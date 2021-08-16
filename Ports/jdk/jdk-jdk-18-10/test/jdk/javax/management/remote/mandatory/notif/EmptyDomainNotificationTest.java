/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6238731
 * @summary Check that the expected notification is received by the JMX
 *          client even when the domain in the ObjectName is not specified
 * @author Shanliang JIANG
 *
 * @run clean EmptyDomainNotificationTest
 * @run build EmptyDomainNotificationTest
 * @run main EmptyDomainNotificationTest
 */

import java.util.ArrayList;
import java.util.List;
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

public class EmptyDomainNotificationTest {

    public static interface SimpleMBean {
        public void emitNotification();
    }

    public static class Simple
        extends NotificationBroadcasterSupport
        implements SimpleMBean {
        public void emitNotification() {
            sendNotification(new Notification("simple", this, 0));
        }
    }

    public static class Listener implements NotificationListener {
        public void handleNotification(Notification n, Object h) {
        System.out.println(
          "EmptyDomainNotificationTest-Listener-handleNotification: receives:" + n);

            if (n.getType().equals("simple")) {
                synchronized(this) {
                    received++;

                    this.notifyAll();
                }
            }
        }

        public int received;
    }

    public static void main(String[] args) throws Exception {

        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");

        JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        server.start();

        JMXConnector client = JMXConnectorFactory.connect(server.getAddress(), null);

        final MBeanServerConnection mbsc = client.getMBeanServerConnection();

        final ObjectName mbean = ObjectName.getInstance(":type=Simple");
        mbsc.createMBean(Simple.class.getName(), mbean);

        System.out.println("EmptyDomainNotificationTest-main: add a listener ...");
        final Listener li = new Listener();
        mbsc.addNotificationListener(mbean, li, null, null);

        System.out.println("EmptyDomainNotificationTest-main: ask to send a notif ...");
        mbsc.invoke(mbean, "emitNotification", null, null);

        System.out.println("EmptyDomainNotificationTest-main: waiting notif...");
        synchronized(li) {
            while (li.received < 1) {
                li.wait();
            }
        }

        if (li.received != 1) {
            throw new RuntimeException("Wait one notif but got: "+li.received);
        }

        System.out.println("EmptyDomainNotificationTest-main: Got the expected notif!");

        System.out.println("EmptyDomainNotificationTest-main: remove the listener.");
        mbsc.removeNotificationListener(mbean, li);

        // clean
        client.close();
        server.stop();

        System.out.println("EmptyDomainNotificationTest-main: Bye.");
    }
}
