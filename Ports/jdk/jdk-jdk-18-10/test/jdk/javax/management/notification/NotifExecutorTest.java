/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4661545
 * @summary Tests to use an executor to send notifications.
 * @author Shanliang JIANG
 *
 * @run clean NotifExecutorTest
 * @run build NotifExecutorTest
 * @run main NotifExecutorTest
 */

// java imports
//
import java.io.IOException;
import java.util.concurrent.*;

// JMX imports
//
import javax.management.*;
import javax.management.remote.*;

public class NotifExecutorTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Tests to use an executor to send notifications.");

        final MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        final ObjectName mbean = new ObjectName ("Default:name=NotificationEmitter");
        final MyListener myLister = new MyListener();
        final NotificationListener nullListener = new NotificationListener() {
                public void handleNotification(Notification n, Object hb) {
                    // nothing
                }
            };
        final Object[] params = new Object[] {new Integer(nb)};
        final String[] signatures = new String[] {"java.lang.Integer"};

        // test with null executor
        System.out.println(">>> Test with a null executor.");
        mbs.registerMBean(new NotificationEmitter(null), mbean);

        mbs.addNotificationListener(mbean, myLister, null, null);
        mbs.addNotificationListener(mbean, nullListener, null, null);

        mbs.invoke(mbean, "sendNotifications", params, signatures);
        check(nb, 0);

        mbs.unregisterMBean(mbean);

        // test with an executor
        System.out.println(">>> Test with a executor.");
        mbs.registerMBean(new NotificationEmitter(
                           new NotifExecutorTest.MyExecutor()), mbean);
        mbs.addNotificationListener(mbean, myLister, null, null);
        mbs.addNotificationListener(mbean, nullListener, null, null);

        mbs.invoke(mbean, "sendNotifications", params, signatures);

        check(nb, nb*2);

        // test without listener
        System.out.println(">>> Test without listener.");

        mbs.removeNotificationListener(mbean, myLister);
        mbs.removeNotificationListener(mbean, nullListener);

        mbs.invoke(mbean, "sendNotifications", params, signatures);
        check(0, 0);
    }

    private static void check(int notifs, int called) throws Exception {
        // Waiting...
        synchronized (lock) {
            for (int i = 0; i < 10; i++) {
                if (receivedNotifs < notifs) {
                    lock.wait(1000);
                }
            }
        }

        // Waiting again to ensure no more notifs
        //
        Thread.sleep(1000);

        // checking
        synchronized (lock) {
            if (receivedNotifs != notifs) {
                throw new RuntimeException("The listener expected to receive " +
                                           notifs + " notifs, but got " + receivedNotifs);
            } else {
                System.out.println(">>> The listener recieved as expected: "+receivedNotifs);
            }

            if (calledTimes != called) {
                throw new RuntimeException("The notif executor should be called " +
                                           called + " times, but got " + calledTimes);
            } else {
                System.out.println(">>> The executor was called as expected: "+calledTimes);
            }
        }

        // clean
        receivedNotifs = 0;
        calledTimes = 0;
    }


    //--------------------------
    // private classes
    //--------------------------
    private static class MyListener implements NotificationListener {
        public void handleNotification(Notification notif, Object handback) {
            synchronized(lock) {
                if(++receivedNotifs >= nb) {
                    lock.notifyAll();
                }
            }
        }
    }

    public static class NotificationEmitter
        extends NotificationBroadcasterSupport
        implements NotificationEmitterMBean {

        public NotificationEmitter(Executor executor) {
            super(executor);
        }

        /**
         * Send a Notification object with the specified times.
         * The sequence number will be from zero to times-1.
         *
         * @param nb The number of notifications to send
         */
        public void sendNotifications(Integer nb) {
            System.out.println(">>> NotificationEmitter: asked to send " +
                               "notifications: " + nb);

            Notification notif;
            for (int i = 1; i <= nb.intValue(); i++) {
                notif = new Notification(null, this, ++seqno);
                super.sendNotification(notif);
            }
        }
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Integer nb);
    }

    public static class MyExecutor extends ThreadPoolExecutor {
        public MyExecutor() {
            super(1, 1, 1L, TimeUnit.MILLISECONDS,
                  new ArrayBlockingQueue(nb*5));
        }

        public synchronized void execute(Runnable job) {
            synchronized(lock) {
                calledTimes++;
            }

            super.execute(job);
        }
    }

    private static int nb = 10;
    private static int receivedNotifs = 0;
    private static int[] lock = new int[0];
    private static volatile long seqno;

    private static int calledTimes = 0;
}
