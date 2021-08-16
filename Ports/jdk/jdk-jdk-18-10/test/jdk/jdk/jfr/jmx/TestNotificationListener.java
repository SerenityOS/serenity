/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.jmx;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import jdk.management.jfr.FlightRecorderMXBean;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.jmx.TestNotificationListener
 */
public class TestNotificationListener {

    private final static CountDownLatch latch = new CountDownLatch(1);

    private final static NotificationListener listener = new NotificationListener() {
        public void handleNotification(Notification notification, Object handback) {
            System.out.println("Got notification: " + notification);
            latch.countDown();
        }
    };

    public static void main(String[] args) throws Throwable {
        ObjectName objectName = new ObjectName(FlightRecorderMXBean.MXBEAN_NAME);
        ManagementFactory.getPlatformMBeanServer().addNotificationListener(objectName, listener, null, null);
        FlightRecorderMXBean bean = ManagementFactory.getPlatformMXBean(FlightRecorderMXBean.class);

        long recId = bean.newRecording();
        bean.startRecording(recId);

        latch.await();
        System.out.println("Completed successfully");
    }
}
