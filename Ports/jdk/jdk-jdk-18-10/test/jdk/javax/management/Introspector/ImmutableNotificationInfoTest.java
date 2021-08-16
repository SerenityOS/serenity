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
 * @bug 6295859
 * @summary Check that a StandardMBean has immutableInfo=true if it is
 * a NotificationBroadcasterSupport that doesn't override getNotificationInfo()
 * @author Eamonn McManus
 *
 * @run clean ImmutableNotificationInfoTest
 * @run build ImmutableNotificationInfoTest
 * @run main ImmutableNotificationInfoTest
 */
import javax.management.Descriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class ImmutableNotificationInfoTest {
    public interface UserBroadcasterMBean {}
    public interface NoOverrideNBSMBean {}
    public interface OverrideNBSMBean {}

    public static class UserBroadcaster
            implements UserBroadcasterMBean, NotificationBroadcaster {
        public void removeNotificationListener(NotificationListener listener) {
        }

        public void addNotificationListener(NotificationListener listener,
                NotificationFilter filter, Object handback) {
        }

        public MBeanNotificationInfo[] getNotificationInfo() {
            return new MBeanNotificationInfo[0];
        }
    }

    public static class NoOverrideNBS extends NotificationBroadcasterSupport
            implements NoOverrideNBSMBean {
    }

    public static class OverrideNBS extends NotificationBroadcasterSupport
            implements OverrideNBSMBean {
        public MBeanNotificationInfo[] getNotificationInfo() {
            return new MBeanNotificationInfo[0];
        }
    }

    public static void main(String[] args) throws Exception {
        boolean ok;
        ok = test(new UserBroadcaster(), false);
        ok &= test(new NoOverrideNBS(), true);
        ok &= test(new OverrideNBS(), false);
        if (!ok)
            throw new Exception("TEST FAILED: immutability incorrect");
    }

    private static boolean test(Object mbean, boolean expectImmutable)
            throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        mbs.registerMBean(mbean, on);
        MBeanInfo mbi = mbs.getMBeanInfo(on);
        Descriptor d = mbi.getDescriptor();
        String immutableValue = (String) d.getFieldValue("immutableInfo");
        boolean immutable = ("true".equals(immutableValue));
        if (immutable != expectImmutable) {
            System.out.println("FAILED: " + mbean.getClass().getName() +
                    " -> " + immutableValue);
            return false;
        } else {
            System.out.println("OK: " + mbean.getClass().getName());
            return true;
        }
    }
}
