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
 * @bug 6175517
 * @summary Check that Standard MBeans can change their MBeanNotificationInfo[]
 * and MXBeans cannot
 * @author Eamonn McManus
 *
 * @run clean ChangingNotifsTest
 * @run build ChangingNotifsTest
 * @run main ChangingNotifsTest
 */

import javax.management.*;
import java.util.Arrays;

public class ChangingNotifsTest {
    public static interface EmptyMBean {}

    public static interface EmptyMXBean {}

    public static class Base extends NotificationBroadcasterSupport {
        @Override
        public MBeanNotificationInfo[] getNotificationInfo() {
            MBeanNotificationInfo inf =
                new MBeanNotificationInfo(new String[0],
                                          Integer.toString(++called),
                                          "description");
            return new MBeanNotificationInfo[] {inf};
        }

        private static int called;
    }

    public static class Empty extends Base implements EmptyMBean {}

    public static class EmptyMX extends Base implements EmptyMXBean {}

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        for (boolean mx : new boolean[] {false, true})
            test(mbs, name, mx);
        if (failure != null)
            throw new Exception(failure);
        System.out.println("TEST PASSED");
    }

    private static void test(MBeanServer mbs, ObjectName name, boolean mx)
            throws Exception {
        Object mbean = mx ? new EmptyMX() : new Empty();
        String what = mx ? "MXBean" : "Standard MBean";
        mbs.registerMBean(mbean, name);
        try {
            MBeanInfo mbi = mbs.getMBeanInfo(name);
            Descriptor d = mbi.getDescriptor();
            String immut = (String) d.getFieldValue("immutableInfo");
            boolean immutable = (immut != null && immut.equals("true"));
            if (immutable != mx) {
                fail(what + " has immutableInfo=" + immut + ", should be " +
                     mx);
                return;
            }
            MBeanNotificationInfo[] n1 = mbi.getNotifications().clone();
            mbi = mbs.getMBeanInfo(name);
            boolean unchanged = Arrays.deepEquals(mbi.getNotifications(), n1);
            if (unchanged != mx) {
                fail(what + " notif info " +
                     (unchanged ? "did not change" : "changed"));
                return;
            }
            System.out.println("OK: " + what);
        } finally {
            mbs.unregisterMBean(name);
        }
    }

    private static void fail(String why) {
        failure = "FAILED: " + why;
        System.out.println(failure);
    }

    private static String failure;
}
