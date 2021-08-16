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
 * @bug 5093922 2120055
 * @summary Test that NotificationBroadcasterSupport can be subclassed
 * and used with synchronized(this) without causing deadlock
 * @author Eamonn McManus
 *
 * @run clean BroadcasterSupportDeadlockTest
 * @run build BroadcasterSupportDeadlockTest
 * @run main BroadcasterSupportDeadlockTest
 */

import java.lang.management.*;
import java.util.concurrent.*;
import javax.management.*;

public class BroadcasterSupportDeadlockTest {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName(ManagementFactory.class.getName());
        } catch (Throwable t) {
            System.out.println("TEST CANNOT RUN: needs JDK 5 at least");
            return;
        }

        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        final BroadcasterMBean mbean = new Broadcaster();
        final ObjectName name = new ObjectName("test:type=Broadcaster");
        mbs.registerMBean(mbean, name);

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        threads.setThreadContentionMonitoringEnabled(true);

        final Semaphore semaphore = new Semaphore(0);

        // Thread 1 - block the Broadcaster
        Thread t1 = new Thread() {
            public void run() {
                try {
                    mbs.invoke(name, "block",
                               new Object[] {semaphore},
                               new String[] {Semaphore.class.getName()});
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                } finally {
                    System.out.println("TEST INCORRECT: block returned");
                    System.exit(1);
                }
            }
        };
        t1.setDaemon(true);
        t1.start();

        /* Wait for Thread 1 to be doing Object.wait().  It's very
           difficult to synchronize properly here so we wait for the
           semaphore, then wait a little longer for the mbs.invoke to
           run, then just in case that isn't enough, we wait for the
           thread to be in WAITING state.  This isn't foolproof,
           because the machine could be very slow and the
           Thread.getState() could find the thread in WAITING state
           due to some operation it does on its way to the one we're
           interested in.  */
        semaphore.acquire();
        Thread.sleep(100);
        while (t1.getState() != Thread.State.WAITING)
            Thread.sleep(1);

        // Thread 2 - try to add a listener
        final NotificationListener listener = new NotificationListener() {
            public void handleNotification(Notification n, Object h) {}
        };
        Thread t2 = new Thread() {
            public void run() {
                try {
                    mbs.addNotificationListener(name, listener, null, null);
                } catch (Exception e) {
                    System.out.println("TEST INCORRECT: addNL failed:");
                    e.printStackTrace(System.out);
                }
            }
        };
        t2.setDaemon(true);
        t2.start();

        /* Wait for Thread 2 to be blocked on the monitor or to
           succeed.  */
        Thread.sleep(100);

        for (int i = 0; i < 1000/*ms*/; i++) {
            t2.join(1/*ms*/);
            switch (t2.getState()) {
            case TERMINATED:
                System.out.println("TEST PASSED");
                return;
            case BLOCKED:
                java.util.Map<Thread,StackTraceElement[]> traces =
                    Thread.getAllStackTraces();
                showStackTrace("Thread 1", traces.get(t1));
                showStackTrace("Thread 2", traces.get(t2));
                System.out.println("TEST FAILED: deadlock");
                System.exit(1);
                break;
            default:
                break;
            }
        }

        System.out.println("TEST FAILED BUT DID NOT NOTICE DEADLOCK");
        Thread.sleep(10000);
        System.exit(1);
    }

    private static void showStackTrace(String title,
                                       StackTraceElement[] stack) {
        System.out.println("---" + title + "---");
        if (stack == null)
            System.out.println("<no stack trace???>");
        else {
            for (StackTraceElement elmt : stack)
                System.out.println("    " + elmt);
        }
        System.out.println();
    }

    public static interface BroadcasterMBean {
        public void block(Semaphore semaphore);
    }

    public static class Broadcaster
            extends NotificationBroadcasterSupport
            implements BroadcasterMBean {
        public synchronized void block(Semaphore semaphore) {
            Object lock = new Object();
            synchronized (lock) {
                try {
                    // Let the caller know that it can now wait for us to
                    // hit the WAITING state
                    semaphore.release();
                    lock.wait(); // block forever
                } catch (InterruptedException e) {
                    System.out.println("TEST INCORRECT: lock interrupted:");
                    e.printStackTrace(System.out);
                    System.exit(1);
                }
            }
        }
    }
}
