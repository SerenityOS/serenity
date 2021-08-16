/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test deadlock in MBeanServerDelegate listeners
 * @author Eamonn McManus
 *
 * @run clean NotifDeadlockTest
 * @run build NotifDeadlockTest
 * @run main NotifDeadlockTest
 */

/*
 * Test deadlock when a listener for an MBeanServerDelegate does a
 * register or unregister of an MBean.  Since such a listener is
 * triggered by a register or unregister operation, deadlock scenarios
 * are possible if there are any locks held while the listener is
 * being dispatched.
 *
 * The flow of control looks rather like this:
 *
 * Thread 1:
 * - MBeanServer.createMBean(..., objectName1);
 * --- MBeanServerDelegate.sendNotification
 * ----- XListener.handleNotification
 * ------- create Thread 2
 * ------- wait for Thread 2 to complete
 *
 * Thread 2:
 * - MBeanServer.createMBean(..., objectName2);
 * - end Thread 2
 *
 * If any locks are held by Thread 1 within createMBean or
 * sendNotification, then Thread 2 can block waiting for them.
 * Since Thread 1 is itself waiting for Thread 2, this is a deadlock.
 *
 * We test all four combinations of:
 * (Thread1-create,Thread1-unregister) x (Thread2-create,Thread2-unregister)
 *
 * In the JMX 1.1 RI, all four tests fail.  In the JMX 1.2 RI, all four
 * tests should pass.
 */
import javax.management.*;

public class NotifDeadlockTest {
    static ObjectName on1, on2, delName;
    static {
        try {
            on1 = new ObjectName("thing:a=b");
            on2 = new ObjectName("thing:c=d");
            delName =
                new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            throw new Error();
        }
    }
    static MBeanServer mbs;

    /* This listener registers or unregisters the MBean called on2
       when triggered.  */
    private static class XListener implements NotificationListener {
        private boolean firstTime = true;
        private final boolean register;

        XListener(boolean register) {
            this.register = register;
        }

        public void handleNotification(Notification not, Object handback) {
            if (firstTime) {
                firstTime = false;
                Thread t = new Thread() {
                    public void run() {
                        try {
                            if (register) {
                                mbs.createMBean("javax.management.timer.Timer",
                                                on2);
                                System.out.println("Listener created " + on2);
                            } else {
                                mbs.unregisterMBean(on2);
                                System.out.println("Listener removed " + on2);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace(); // should not happen
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Test 1: in register notif, unregister an MBean");
        mbs = MBeanServerFactory.createMBeanServer();
        mbs.createMBean("javax.management.timer.Timer", on2);
        mbs.addNotificationListener(delName, new XListener(false), null, null);
        mbs.createMBean("javax.management.timer.Timer", on1);
        MBeanServerFactory.releaseMBeanServer(mbs);
        System.out.println("Test 1 completed");

        System.out.println("Test 2: in unregister notif, unregister an MBean");
        mbs = MBeanServerFactory.createMBeanServer();
        mbs.createMBean("javax.management.timer.Timer", on1);
        mbs.createMBean("javax.management.timer.Timer", on2);
        mbs.addNotificationListener(delName, new XListener(false), null, null);
        mbs.unregisterMBean(on1);
        MBeanServerFactory.releaseMBeanServer(mbs);
        System.out.println("Test 2 completed");

        System.out.println("Test 3: in register notif, register an MBean");
        mbs = MBeanServerFactory.createMBeanServer();
        mbs.addNotificationListener(delName, new XListener(true), null, null);
        mbs.createMBean("javax.management.timer.Timer", on1);
        MBeanServerFactory.releaseMBeanServer(mbs);
        System.out.println("Test 3 completed");

        System.out.println("Test 4: in unregister notif, register an MBean");
        mbs = MBeanServerFactory.createMBeanServer();
        mbs.createMBean("javax.management.timer.Timer", on1);
        mbs.addNotificationListener(delName, new XListener(true), null, null);
        mbs.unregisterMBean(on1);
        MBeanServerFactory.releaseMBeanServer(mbs);
        System.out.println("Test 4 completed");

        System.out.println("Test passed");
    }
}
