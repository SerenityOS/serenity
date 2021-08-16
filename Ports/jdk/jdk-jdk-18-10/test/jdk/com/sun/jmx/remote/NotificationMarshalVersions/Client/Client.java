/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class Client {
    public static void run(String url) throws Exception {
        final int notifEmittedCnt = 10;
        final CountDownLatch counter = new CountDownLatch(notifEmittedCnt);
        final Set<Long> seqSet = Collections.synchronizedSet(new HashSet<Long>());
        final AtomicBoolean duplNotification = new AtomicBoolean();

        JMXServiceURL serverUrl = new JMXServiceURL(url);

        ObjectName name = new ObjectName("test", "foo", "bar");
        JMXConnector jmxConnector = JMXConnectorFactory.connect(serverUrl);
        System.out.println("client connected");
        jmxConnector.addConnectionNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                System.out.println("connection notification: " + notification);
                if (!seqSet.add(notification.getSequenceNumber())) {
                    duplNotification.set(true);
                }
                if (notification.getType().equals(JMXConnectionNotification.NOTIFS_LOST)) {
                    long lostNotifs = ((Long)((JMXConnectionNotification)notification).getUserData()).longValue();
                    for(int i=0;i<lostNotifs;i++) {
                        counter.countDown();
                    }
                }
            }
        }, null, null);
        MBeanServerConnection jmxServer = jmxConnector.getMBeanServerConnection();

        jmxServer.addNotificationListener(name, new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                System.out.println("client got: " + notification);
                if (!seqSet.add(notification.getSequenceNumber())) {
                    duplNotification.set(true);
                }
                counter.countDown();
            }
        }, null, null);

        System.out.println("client invoking foo (" + notifEmittedCnt + " times)");
        for(int i=0;i<notifEmittedCnt;i++) {
            System.out.print(".");
            jmxServer.invoke(name, "foo", new Object[]{}, new String[]{});
        }
        System.out.println();
        try {
            System.out.println("waiting for " + notifEmittedCnt + " notifications to arrive");
            if (!counter.await(30, TimeUnit.SECONDS)) {
                throw new InterruptedException();
            }
            if (duplNotification.get()) {
                System.out.println("ERROR: received duplicated notifications");
                throw new Error("received duplicated notifications");
            }
            System.out.println("\nshutting down client");
        } catch (InterruptedException e) {
            System.out.println("ERROR: notification processing thread interrupted");
            throw new Error("notification thread interrupted unexpectedly");
        }
    }
}
