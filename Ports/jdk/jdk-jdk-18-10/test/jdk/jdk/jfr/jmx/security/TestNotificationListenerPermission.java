/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jmx.security;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import jdk.management.jfr.FlightRecorderMXBean;
import jdk.test.lib.Asserts;

import jdk.jfr.jmx.JmxHelper;

/**
 * @test
 * @key jfr
 * @summary Test with minimal needed permissions. All functions should work.
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm/secure=java.lang.SecurityManager/java.security.policy=listener.policy jdk.jfr.jmx.security.TestNotificationListenerPermission
 */
public class TestNotificationListenerPermission {
    private static boolean gotSecurityException;

    static class TestListener implements NotificationListener {
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void handleNotification(Notification arg0, Object arg1) {
            try {
                System.getProperty("user.name");
            } catch (SecurityException se) {
                se.printStackTrace();
                gotSecurityException = true;
            }
            latch.countDown();
        }

        public void awaitNotication() throws InterruptedException {
            latch.await();
        }
    }

    public static void main(String[] args) throws Throwable {
        try {
            System.getProperty("user.name");
            Asserts.fail("Didn't get security exception. Test not configured propertly?");
        } catch (SecurityException se) {
            // as expected
        }
        FlightRecorderMXBean bean = JmxHelper.getFlighteRecorderMXBean();
        TestListener testListener = new TestListener();
        ManagementFactory.getPlatformMBeanServer().addNotificationListener(new ObjectName(FlightRecorderMXBean.MXBEAN_NAME), testListener, null, null);
        long id = bean.newRecording();
        bean.startRecording(id);
        testListener.awaitNotication();
        Asserts.assertTrue(gotSecurityException, "Should not get elevated privileges in notification handler!");
        bean.stopRecording(id);
        bean.closeRecording(id);
    }
}
