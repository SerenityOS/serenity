/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6338874
 * @summary Check that notification dispatch is not linear in number of MBeans.
 * @author Eamonn McManus
 *
 * @library /test/lib
 *
 * @run build jdk.test.lib.Platform ListenerScaleTest
 * @run main ListenerScaleTest
 */

/*
 * The notification dispatch logic in the connector server used to be
 * linear in the number of listeners there were on any MBean.  For
 * example, if there were 1000 MBeans, each with one listener, then
 * every time a notification was sent it would be compared against
 * every one of the 1000 MBeans, even though its source ObjectName was
 * known and could not possibly match the name of 999 of the MBeans.
 * This test checks that we no longer have linear behaviour.  We do
 * this by registering just one MBean and measuring the time it takes
 * to send and receive a certain number of notifications from that
 * MBean.  Then we register many other MBeans, each with a listener,
 * and we make the same measurement as before.  The presence of the
 * extra MBeans with their listeners should not impact the dispatch
 * time significantly.  If it does, the test fails.
 *
 * As usual with timing-sensitive tests, we could potentially get
 * sporadic failures.  We fail if the ratio of the time with many
 * MBeans to the time with just one MBean is more than 500.  With the
 * fix in place, it is usually less than 1, presumably because some
 * code was being interpreted during the first measurement but had
 * been compiled by the second.
 */

import java.util.concurrent.Semaphore;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import jdk.test.lib.Platform;

public class ListenerScaleTest {
    private static final int WARMUP_WITH_ONE_MBEAN = 1000;
    private static final int NOTIFS_TO_TIME = 100;
    private static final int EXTRA_MBEANS = 20000;

    private static final ObjectName testObjectName;
    static {
        try {
            testObjectName = new ObjectName("test:type=Sender,number=-1");
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    private static volatile int nnotifs;
    private static volatile long startTime;
    private static volatile long elapsed;
    private static final Semaphore sema = new Semaphore(0);

    private static final NotificationListener timingListener =
        new NotificationListener() {
            public void handleNotification(Notification n, Object h) {
                if (++nnotifs == NOTIFS_TO_TIME) {
                    elapsed = System.nanoTime() - startTime;
                    sema.release();
                }
            }
        };

    private static final long timeNotif(MBeanServer mbs) {
        try {
            startTime = System.nanoTime();
            nnotifs = 0;
            mbs.invoke(testObjectName, "send", null, null);
            sema.acquire();
            return elapsed;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static interface SenderMBean {
        public void send();
    }

    public static class Sender extends NotificationBroadcasterSupport
            implements SenderMBean {
        public void send() {
            for (int i = 0; i < NOTIFS_TO_TIME; i++)
                sendNotification(new Notification("type", this, 1L));
        }
    }

    private static final NotificationListener nullListener =
        new NotificationListener() {
            public void handleNotification(Notification n, Object h) {}
        };

    public static void main(String[] args) throws Exception {
        if (Platform.isDebugBuild()) {
            System.out.println("Running on a debug build. Performance test not applicable. Skipping.");
            return;
        }
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        Sender sender = new Sender();
        mbs.registerMBean(sender, testObjectName);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://");
        JMXConnectorServer cs =
            JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector cc = JMXConnectorFactory.connect(addr);
        try {
            test(mbs, cs, cc);
        } finally {
            cc.close();
            cs.stop();
        }
    }

    private static void test(MBeanServer mbs, JMXConnectorServer cs,
                             JMXConnector cc) throws Exception {
        MBeanServerConnection mbsc = cc.getMBeanServerConnection();
        mbsc.addNotificationListener(testObjectName, timingListener, null, null);
        long singleMBeanTime = 0;
        for (int i = 0; i < WARMUP_WITH_ONE_MBEAN; i++)
            singleMBeanTime = timeNotif(mbs);
        if (singleMBeanTime == 0)
            singleMBeanTime = 1;
        System.out.println("Time with a single MBean: " + singleMBeanTime + "ns");
        System.out.println("Now registering " + EXTRA_MBEANS + " MBeans");
        for (int i = 0; i < EXTRA_MBEANS; i++) {
            ObjectName on = new ObjectName("test:type=Sender,number=" + i);
            mbs.registerMBean(new Sender(), on);
            if (i % 1000 == 999) {
                System.out.print("..." + (i+1));
                System.out.flush();
            }
        }
        System.out.println();
        System.out.println("Now registering " + EXTRA_MBEANS + " listeners");
        for (int i = 0; i < EXTRA_MBEANS; i++) {
            ObjectName on = new ObjectName("test:type=Sender,number=" + i);
            mbsc.addNotificationListener(on, nullListener, null, null);
            if (i % 1000 == 999) {
                System.out.print("..." + (i+1));
                System.out.flush();
            }
        }
        System.out.println();
        System.out.println("Timing a notification send now");
        long manyMBeansTime = timeNotif(mbs);
        System.out.println("Time with many MBeans: " + manyMBeansTime + "ns");
        double ratio = (double) manyMBeansTime / singleMBeanTime;
        if (ratio > 500.0)
            throw new Exception("Failed: ratio=" + ratio);
        System.out.println("Test passed: ratio=" + ratio);
    }
}
