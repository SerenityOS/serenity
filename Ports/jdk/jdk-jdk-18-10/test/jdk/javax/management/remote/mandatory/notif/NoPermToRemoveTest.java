/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6515161
 * @summary checks the behaviour of  mbeanServerConnection.removeNotificationListener
 * operation when there is a exception thrown during removal
 * @modules java.management
 * @run main/othervm -Djava.security.manager=allow NoPermToRemoveTest
 */

import java.lang.management.ManagementFactory;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanPermission;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class NoPermToRemoveTest {
    public static void main(String[] args) throws Exception {
        Policy.setPolicy(new NoRemovePolicy());
        System.setSecurityManager(new SecurityManager());

        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("foo:type=Sender");
        mbs.registerMBean(new Sender(), name);
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(
                url, null, mbs);
        cs.start();
        try {
            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            SnoopListener listener = new SnoopListener();
            mbsc.addNotificationListener(name, listener, null, null);
            mbsc.invoke(name, "send", null, null);
            if (!listener.waitForNotification(60))
                throw new Exception("Did not receive expected notification");

            try {
                mbsc.removeNotificationListener(name, listener);
                throw new Exception("RemoveNL did not get SecurityException");
            } catch (SecurityException e) {
                System.out.println("removeNL got expected exception: " + e);
            }
            mbsc.invoke(name, "send", null, null);
            if (!listener.waitForNotification(60)) {
                int listenerCount =
                        (Integer) mbsc.getAttribute(name, "ListenerCount");
                System.out.println("Listener count: " + listenerCount);
                if (listenerCount != 0)
                    throw new Exception("TEST FAILED");
                    /* We did not receive the notification, but the MBean still
                     * has a listener coming from the connector server, which
                     * means the connector server still thinks there is a
                     * listener.  If we retained the listener after the failing
                     * removeNL that would be OK, and if the listener were
                     * dropped by both client and server that would be OK too,
                     * but the inconsistency is not OK.
                     */
            }
            cc.close();
        } finally {
            cs.stop();
        }
    }

    private static class SnoopListener implements NotificationListener {
        private Semaphore sema = new Semaphore(0);

        public void handleNotification(Notification notification, Object handback) {
            System.out.println("Listener got: " + notification);
            sema.release();
        }

        boolean waitForNotification(int seconds) throws InterruptedException {
            return sema.tryAcquire(seconds, TimeUnit.SECONDS);
        }
    }

    private static class NoRemovePolicy extends Policy {
        public PermissionCollection getPermissions(CodeSource codesource) {
            PermissionCollection pc = new Permissions();
            pc.add(new AllPermission());
            return pc;
        }

        public void refresh() {
        }

        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (!(permission instanceof MBeanPermission))
                return true;
            MBeanPermission jmxp = (MBeanPermission) permission;
            if (jmxp.getActions().contains("removeNotificationListener")) {
                System.out.println("DENIED");
                return false;
            }
            return true;
        }
    }

    public static interface SenderMBean {
        public void send();
        public int getListenerCount();
    }

    public static class Sender extends NotificationBroadcasterSupport
            implements SenderMBean {
        private AtomicInteger listenerCount = new AtomicInteger();

        public void send() {
            System.out.println("Sending notif");
            sendNotification(new Notification("type", this, 0L));
        }

        public synchronized int getListenerCount() {
            return listenerCount.get();
        }

        public void removeNotificationListener(
                NotificationListener listener,
                NotificationFilter filter,
                Object handback) throws ListenerNotFoundException {
            System.out.println("Sender.removeNL(3)");
            super.removeNotificationListener(listener, filter, handback);
            listenerCount.decrementAndGet();
        }

        public void addNotificationListener(
                NotificationListener listener,
                NotificationFilter filter,
                Object handback) {
            System.out.println("Sender.addNL(3)");
            super.addNotificationListener(listener, filter, handback);
            listenerCount.incrementAndGet();
        }

        public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {
            System.out.println("Sender.removeNL(1)");
            super.removeNotificationListener(listener);
            listenerCount.decrementAndGet();
        }
    }
}
