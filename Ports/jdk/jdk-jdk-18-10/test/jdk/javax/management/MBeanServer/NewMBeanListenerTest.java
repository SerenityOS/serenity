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
 * @bug 4757273
 * @summary Test that registered notification is sent early enough
 * @author Eamonn McManus
 *
 * @run clean NewMBeanListenerTest
 * @run build NewMBeanListenerTest
 * @run main NewMBeanListenerTest
 */

import javax.management.*;

/* Tests that you can write a listener for MBean registrations, that
 * registers another listener on every newly-registered MBean (that is
 * a NotificationBroadcaster).  Provided the newly-registered MBean
 * waits until its postRegister is called, no notifications will be lost.
 */
public class NewMBeanListenerTest {
    public static void main(String[] args) throws Exception {
        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        final ObjectName delegateName =
            new ObjectName("JMImplementation:type=MBeanServerDelegate");
        final CountListener countListener = new CountListener();
        final NotificationListener addListener = new NotificationListener() {
            public void handleNotification(Notification n, Object h) {
                if (!(n instanceof MBeanServerNotification)) {
                    System.out.println("Ignoring delegate notif: " +
                                       n.getClass().getName());
                    return;
                }
                MBeanServerNotification mbsn = (MBeanServerNotification) n;
                if (!(mbsn.getType()
                      .equals(MBeanServerNotification
                              .REGISTRATION_NOTIFICATION))) {
                    System.out.println("Ignoring MBeanServer notif: " +
                                       mbsn.getType());
                    return;
                }
                System.out.println("Got registration notif for " +
                                   mbsn.getMBeanName());
                try {
                    mbs.addNotificationListener(mbsn.getMBeanName(),
                                                countListener, null, null);
                } catch (Exception e) {
                    System.out.println("TEST INCORRECT: addNL failed:");
                    e.printStackTrace(System.out);
                    System.exit(1);
                }
                System.out.println("Added notif listener for " +
                                   mbsn.getMBeanName());
            }
        };
        System.out.println("Adding registration listener");
        mbs.addNotificationListener(delegateName, addListener, null, null);
        final ObjectName broadcasterName = new ObjectName(":type=Broadcaster");
        System.out.println("Creating Broadcaster MBean");
        mbs.createMBean(Broadcaster.class.getName(), broadcasterName);
        if (countListener.getCount() == 1)
            System.out.println("Got notif as expected");
        else {
            System.out.println("TEST FAILED: added listener not called");
            System.exit(1);
        }
        mbs.unregisterMBean(broadcasterName);
        Broadcaster b = new Broadcaster();
        System.out.println("Registering Broadcaster MBean");
        mbs.registerMBean(b, broadcasterName);
        if (countListener.getCount() == 2)
            System.out.println("Got notif as expected");
        else {
            System.out.println("TEST FAILED: added listener not called");
            System.exit(1);
        }
        System.out.println("Test passed");
    }

    public static interface BroadcasterMBean {}

    public static class Broadcaster
            extends NotificationBroadcasterSupport
            implements BroadcasterMBean, MBeanRegistration {

        public ObjectName preRegister(MBeanServer mbs, ObjectName name) {
            return name;
        }

        public void postRegister(Boolean registrationDone) {
            System.out.println("Broadcaster.postRegister: sending notif");
            sendNotification(new Notification("x", this, 0L));
        }

        public void preDeregister() {
        }

        public void postDeregister() {
        }
    }

    private static class CountListener implements NotificationListener {
        private int count;

        public synchronized void handleNotification(Notification n, Object h) {
            if (!n.getType().equals("x")) {
                System.out.println("TEST FAILED: received bogus notif: " + n +
                                   " (type=" + n.getType() + ")");
                System.exit(1);
            }
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }
}
