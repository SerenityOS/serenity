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
 * @summary Tests the NotificationBuffer class.
 * @author Eamonn McManus
 * @modules java.management/com.sun.jmx.remote.internal
 *          java.management/com.sun.jmx.remote.util
 * @run clean NotificationBufferTest
 * @run build NotificationBufferTest NotificationSender NotificationSenderMBean
 * @run main NotificationBufferTest
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.ObjectName;
import javax.management.loading.MLet;

import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

import com.sun.jmx.remote.internal.ArrayNotificationBuffer;
import com.sun.jmx.remote.internal.NotificationBufferFilter;
import com.sun.jmx.remote.internal.NotificationBuffer;

public class NotificationBufferTest {

    public static void main(String[] args) {
//      System.setProperty("java.util.logging.config.file",
//                         "../../../../logging.properties");
//      // we are in <workspace>/build/test/JTwork/scratch
        try {
//          java.util.logging.LogManager.getLogManager().readConfiguration();
            boolean ok = test();
            if (ok) {
                System.out.println("Test completed");
                return;
            } else {
                System.out.println("Test failed!");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Unexpected exception: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean test() throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        Integer queuesize = new Integer(10);
        HashMap env = new HashMap();
        env.put(com.sun.jmx.remote.util.EnvHelp.BUFFER_SIZE_PROPERTY, queuesize);
        final NotificationBuffer nb =
            ArrayNotificationBuffer.getNotificationBuffer(mbs, env);

        final ObjectName senderName = new ObjectName("dom:type=sender");
        final ObjectName wildcardName = new ObjectName("*:*");
        final String notifType =
            MBeanServerNotification.REGISTRATION_NOTIFICATION;

        Integer allListenerId = new Integer(99);
        NotificationBufferFilter allListenerFilter =
                makeFilter(allListenerId, wildcardName, null);
        NotificationFilterSupport regFilter = new NotificationFilterSupport();
        regFilter.enableType(notifType);

        // Get initial sequence number
        NotificationResult nr =
            nb.fetchNotifications(allListenerFilter, 0, 0L, 0);
        int nnotifs = nr.getTargetedNotifications().length;
        if (nnotifs > 0) {
            System.out.println("Expected 0 notifs for initial fetch, " +
                               "got " + nnotifs);
            return false;
        }
        System.out.println("Got 0 notifs for initial fetch, OK");

        long earliest = nr.getEarliestSequenceNumber();
        long next = nr.getNextSequenceNumber();
        if (earliest != next) {
            System.out.println("Expected earliest==next in initial fetch, " +
                               "earliest=" + earliest + "; next=" + next);
            return false;
        }
        System.out.println("Got earliest==next in initial fetch, OK");

        mbs.createMBean(MLet.class.getName(), null);
        mbs.createMBean(NotificationSender.class.getName(), senderName);

        NotificationSenderMBean sender = (NotificationSenderMBean)
            MBeanServerInvocationHandler.newProxyInstance(mbs,
                                                          senderName,
                                                          NotificationSenderMBean.class,
                                                          false);

        /* We test here that MBeans already present when the
           NotificationBuffer was created get a listener for the
           buffer, as do MBeans created later.  The
           MBeanServerDelegate was already present, while the
           NotificationSender was created later.  */

        // Check that the NotificationSender does indeed have a listener
        /* Note we are dependent on the specifics of our JMX
           implementation here.  There is no guarantee that the MBean
           creation listeners will have run to completion when
           creation of the MBean returns.  */
        int nlisteners = sender.getListenerCount();
        if (nlisteners != 1) {
            System.out.println("Notification sender should have 1 listener, " +
                               "has " + nlisteners);
            return false;
        }
        System.out.println("Notification sender has 1 listener, OK");

        // Now we should see two creation notifications
        nr = nb.fetchNotifications(allListenerFilter, next, 0L,
                                   Integer.MAX_VALUE);
        TargetedNotification[] tns = nr.getTargetedNotifications();
        if (tns.length != 2) {
            System.out.println("Expected 2 notifs, got: " +
                               Arrays.asList(tns));
            return false;
        }
        if (!(tns[0].getNotification() instanceof MBeanServerNotification)
            || !(tns[1].getNotification() instanceof MBeanServerNotification))
            {
            System.out.println("Expected 2 MBeanServerNotifications, got: " +
                               Arrays.asList(tns));
            return false;
        }
        if (!tns[0].getListenerID().equals(tns[1].getListenerID())
            || !tns[0].getListenerID().equals(allListenerId)) {
            System.out.println("Bad listener IDs: " + Arrays.asList(tns));
            return false;
        }
        System.out.println("Got 2 different MBeanServerNotifications, OK");

        // If we ask for max 1 notifs, we should only get one
        nr = nb.fetchNotifications(allListenerFilter, next, 0L, 1);
        tns = nr.getTargetedNotifications();
        if (tns.length != 1) {
            System.out.println("Expected 1 notif, got: " + Arrays.asList(tns));
            return false;
        }
        TargetedNotification tn1 = tns[0];
        System.out.println("Got 1 notif when asked for 1, OK");

        // Now we should get the other one
        nr = nb.fetchNotifications(allListenerFilter, nr.getNextSequenceNumber(),
                                   0L, 1);
        tns = nr.getTargetedNotifications();
        if (tns.length != 1) {
            System.out.println("Expected 1 notif, got: " + Arrays.asList(tns));
            return false;
        }
        TargetedNotification tn2 = tns[0];
        System.out.println("Got 1 notif when asked for 1 again, OK");

        if (tn1.getNotification() == tn2.getNotification()) {
            System.out.println("Returned same notif twice: " + tn1);
            return false;
        }
        System.out.println("2 creation notifs are different, OK");

        // Now we should get none (timeout is 0)
        long oldNext = nr.getNextSequenceNumber();
        nr = nb.fetchNotifications(allListenerFilter, oldNext, 0L,
                                   Integer.MAX_VALUE);
        tns = nr.getTargetedNotifications();
        if (tns.length != 0) {
            System.out.println("Expected 0 notifs, got: " +
                               Arrays.asList(tns));
            return false;
        }
        System.out.println("Got 0 notifs with 0 timeout, OK");
        if (nr.getNextSequenceNumber() != oldNext) {
            System.out.println("Sequence number changed: " + oldNext + " -> " +
                               nr.getNextSequenceNumber());
            return false;
        }
        System.out.println("Next seqno unchanged with 0 timeout, OK");

        // Check that timeouts work
        long startTime = System.currentTimeMillis();
        nr = nb.fetchNotifications(allListenerFilter, oldNext, 250L,
                                   Integer.MAX_VALUE);
        tns = nr.getTargetedNotifications();
        if (tns.length != 0) {
            System.out.println("Expected 0 notifs, got: " +
                               Arrays.asList(tns));
            return false;
        }
        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        if (elapsed < 250L) {
            System.out.println("Elapsed time shorter than timeout: " +
                               elapsed);
            return false;
        }
        System.out.println("Timeout worked, OK");

        // Check that notification filtering works
        NotificationFilter senderFilter = new NotificationFilter() {
            public boolean isNotificationEnabled(Notification n) {
                if (!(n instanceof MBeanServerNotification))
                    return false;
                MBeanServerNotification mbsn = (MBeanServerNotification) n;
                return (mbsn.getMBeanName().equals(senderName));
            }
        };
        Integer senderListenerId = new Integer(88);
        NotificationBufferFilter senderListenerFilter =
                makeFilter(senderListenerId, wildcardName, senderFilter);
        nr = nb.fetchNotifications(senderListenerFilter, 0, 1000L,
                                   Integer.MAX_VALUE);
        tns = nr.getTargetedNotifications();
        if (tns.length != 1) {
            System.out.println("Expected 1 notif, got: " + Arrays.asList(tns));
            return false;
        }
        MBeanServerNotification mbsn =
            (MBeanServerNotification) tns[0].getNotification();
        if (!mbsn.getMBeanName().equals(senderName)) {
            System.out.println("Expected notif with senderName, got: " +
                               mbsn + " (" + mbsn.getMBeanName() + ")");
            return false;
        }
        System.out.println("Successfully applied NotificationFilter, OK");

        // Now send 8 notifs to fill up our 10-element buffer
        sender.sendNotifs("tiddly.pom", 8);
        nr = nb.fetchNotifications(allListenerFilter, 0, 1000L,
                                   Integer.MAX_VALUE);
        tns = nr.getTargetedNotifications();
        if (tns.length != 10) {
            System.out.println("Expected 10 notifs, got: " +
                               Arrays.asList(tns));
            return false;
        }
        System.out.println("Got full buffer of 10 notifications, OK");

        // Check that the 10 notifs are the ones we expected
        for (int i = 0; i < 10; i++) {
            String expected =
                (i < 2) ? notifType : "tiddly.pom";
            String found = tns[i].getNotification().getType();
            if (!found.equals(expected)) {
                System.out.println("Notif " + i + " bad type: expected <" +
                                   expected + ">, found <" + found + ">");
                return false;
            }
        }
        System.out.println("Notifs have right types, OK");

        // Check that ObjectName filtering works
        NotificationBufferFilter senderNameFilter =
                makeFilter(new Integer(66), senderName, null);
        nr = nb.fetchNotifications(senderNameFilter, 0, 0L,
                                   Integer.MAX_VALUE);
        tns = nr.getTargetedNotifications();
        if (tns.length != 8) {
            System.out.println("Bad result from ObjectName filtering: " +
                               Arrays.asList(tns));
            return false;
        }
        System.out.println("ObjectName filtering works, OK");

        // Send one more notif, which should cause the oldest one to drop
        sender.sendNotifs("foo.bar", 1);
        nr = nb.fetchNotifications(allListenerFilter, 0, 1000L,
                                   Integer.MAX_VALUE);
        if (nr.getEarliestSequenceNumber() <= earliest) {
            System.out.println("Expected earliest to increase: " +
                               nr.getEarliestSequenceNumber() + " should be > "
                               + earliest);
            return false;
        }
        System.out.println("Earliest notif dropped, OK");

        // Check that the 10 notifs are the ones we expected
        tns = nr.getTargetedNotifications();
        for (int i = 0; i < 10; i++) {
            String expected =
                (i < 1) ? notifType
                        : (i < 9) ? "tiddly.pom" : "foo.bar";
            String found = tns[i].getNotification().getType();
            if (!found.equals(expected)) {
                System.out.println("Notif " + i + " bad type: expected <" +
                                   expected + ">, found <" + found + ">");
                return false;
            }
        }
        System.out.println("Notifs have right types, OK");

        // Apply a filter that only selects the first notif, with max notifs 1,
        // then check that it skipped past the others even though it already
        // had its 1 notif
        NotificationBufferFilter firstFilter =
                makeFilter(new Integer(55), wildcardName, regFilter);
        nr = nb.fetchNotifications(firstFilter, 0, 1000L, 1);
        tns = nr.getTargetedNotifications();
        if (tns.length != 1
            || !tns[0].getNotification().getType().equals(notifType)) {
            System.out.println("Unexpected return from filtered call: " +
                               Arrays.asList(tns));
            return false;
        }
        nr = nb.fetchNotifications(allListenerFilter, nr.getNextSequenceNumber(),
                                   0L, 1000);
        tns = nr.getTargetedNotifications();
        if (tns.length != 0) {
            System.out.println("Expected 0 notifs, got: " +
                               Arrays.asList(tns));
            return false;
        }

        // Create a second, larger buffer, which should share the same notifs
        nr = nb.fetchNotifications(allListenerFilter, 0,
                                   1000L, Integer.MAX_VALUE);
        queuesize = new Integer(20);
        env.put(com.sun.jmx.remote.util.EnvHelp.BUFFER_SIZE_PROPERTY, queuesize);
        NotificationBuffer nb2 =
            ArrayNotificationBuffer.getNotificationBuffer(mbs, env);
        NotificationResult nr2 =
            nb2.fetchNotifications(allListenerFilter, 0,
                                   1000L, Integer.MAX_VALUE);
        if (nr.getEarliestSequenceNumber() != nr2.getEarliestSequenceNumber()
            || nr.getNextSequenceNumber() != nr2.getNextSequenceNumber()
            || !sameTargetedNotifs(nr.getTargetedNotifications(),
                                   nr2.getTargetedNotifications()))
            return false;
        System.out.println("Adding second buffer preserved notif list, OK");

        // Check that the capacity is now 20
        sender.sendNotifs("propter.hoc", 10);
        nr2 = nb2.fetchNotifications(allListenerFilter, 0,
                                     1000L, Integer.MAX_VALUE);
        if (nr.getEarliestSequenceNumber() !=
            nr2.getEarliestSequenceNumber()) {
            System.out.println("Earliest seq number changed after notifs " +
                               "that should have fit");
            return false;
        }
        TargetedNotification[] tns2 = new TargetedNotification[10];
        Arrays.asList(nr2.getTargetedNotifications()).subList(0, 10).toArray(tns2);
        if (!sameTargetedNotifs(nr.getTargetedNotifications(), tns2)) {
            System.out.println("Early notifs changed after notifs " +
                               "that should have fit");
            return false;
        }
        System.out.println("New notifications fit in now-larger buffer, OK");

        // Drop the second buffer and check that the capacity shrinks
        nb2.dispose();
        NotificationResult nr3 =
            nb.fetchNotifications(allListenerFilter, 0,
                                  1000L, Integer.MAX_VALUE);
        if (nr3.getEarliestSequenceNumber() != nr.getNextSequenceNumber()) {
            System.out.println("After shrink, notifs not dropped as expected");
            return false;
        }
        if (nr3.getNextSequenceNumber() != nr2.getNextSequenceNumber()) {
            System.out.println("After shrink, next seq no does not match");
            return false;
        }
        tns2 = new TargetedNotification[10];
        Arrays.asList(nr2.getTargetedNotifications()).subList(10, 20).toArray(tns2);
        if (!sameTargetedNotifs(nr3.getTargetedNotifications(), tns2)) {
            System.out.println("Later notifs not preserved after shrink");
            return false;
        }
        System.out.println("Dropping second buffer shrank capacity, OK");

        // Final test: check that destroying the final shared buffer
        // removes its listeners
        nb.dispose();
        nlisteners = sender.getListenerCount();
        if (nlisteners != 0) {
            System.out.println("Disposing buffer should leave 0 listeners, " +
                               "but notification sender has " + nlisteners);
            return false;
        }
        System.out.println("Dropping first buffer drops listeners, OK");

        return true;
    }

    private static boolean sameTargetedNotifs(TargetedNotification[] tn1,
                                              TargetedNotification[] tn2) {
        if (tn1.length != tn2.length) {
            System.out.println("Not same length");
            return false;
        }
        for (int i = 0; i < tn1.length; i++) {
            TargetedNotification n1 = tn1[i];
            TargetedNotification n2 = tn2[i];
            if (n1.getNotification() != n2.getNotification()
                || !n1.getListenerID().equals(n2.getListenerID()))
                return false;
        }
        return true;
    }

    private static NotificationBufferFilter makeFilter(final Integer id,
                                                       final ObjectName pattern,
                                                       final NotificationFilter filter) {
        return new NotificationBufferFilter() {
            public void apply(List<TargetedNotification> notifs,
                              ObjectName source, Notification notif) {
                if (pattern.apply(source)) {
                    if (filter == null || filter.isNotificationEnabled(notif))
                        notifs.add(new TargetedNotification(notif, id));
                }
            }
        };
    };
}
