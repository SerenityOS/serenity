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
 * @bug 5042004
 * @summary Check that MBeans with the same class have identical MBeanInfo
 * unless they are NotificationBroadcasters
 * @author Eamonn McManus
 *
 * @run clean IdenticalMBeanInfoTest
 * @run build IdenticalMBeanInfoTest
 * @run main IdenticalMBeanInfoTest
 */

import javax.management.*;

/* What we test here is not required by the spec.  It is an
   optimization that can save a considerable amount of memory when
   there are many MBeans of the same type.  There is no reason why two
   Standard MBeans of the same type should have different MBeanInfo
   objects, unless they are NotificationBroadcasters and return
   different arrays from getNotificationInfo().  Note that two MBeans
   that share the same MBean interface but are of different classes
   cannot have the same MBeanInfo because the MBeanInfo includes the
   implementation class name.  */
public class IdenticalMBeanInfoTest {
    private static String failure = null;

    public static interface WhatsitMBean {
        public int getReadOnly();
        public int getReadWrite();
        public void setReadWrite(int x);
        public int op(int x, int y);
    }

    public static class Whatsit implements WhatsitMBean {
        public Whatsit() {}
        public Whatsit(int irrelevant) {}

        public int getReadOnly() { return 0; }
        public int getReadWrite() { return 0; }
        public void setReadWrite(int x) {}
        public int op(int x, int y) { return 0; }
    }

    public static interface BroadcasterMBean extends WhatsitMBean {
    }

    public static class Broadcaster extends Whatsit
            implements BroadcasterMBean, NotificationBroadcaster {
        private static int nextId = 1;
        private int id = nextId++;

        public void addNotificationListener(NotificationListener l,
                                            NotificationFilter f,
                                            Object h) {}

        public void removeNotificationListener(NotificationListener l) {}

        public MBeanNotificationInfo[] getNotificationInfo() {
            return new MBeanNotificationInfo[] {
                new MBeanNotificationInfo(new String[] {"type" + id},
                                          "something",
                                          "a something")
            };
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        ObjectName on1 = new ObjectName("d:type=Whatsit,number=1");
        ObjectName on2 = new ObjectName("d:type=Whatsit,number=2");
        ObjectName on3 = new ObjectName("d:type=Whatsit,number=3");
        ObjectName on4 = new ObjectName("d:type=Whatsit,number=4");
        ObjectName on5 = new ObjectName("d:type=Whatsit,number=5");

        mbs.registerMBean(new Whatsit(), on1);
        mbs.createMBean(Whatsit.class.getName(), on2);
        DynamicMBean mbean =
            new StandardMBean(new Whatsit(), WhatsitMBean.class);
        mbs.registerMBean(mbean, on3);
        mbs.registerMBean(new Broadcaster(), on4);
        mbs.createMBean(Broadcaster.class.getName(), on5);
        MBeanInfo mbi1 = mbs.getMBeanInfo(on1);
        MBeanInfo mbi2 = mbs.getMBeanInfo(on2);
        MBeanInfo mbi3 = mbs.getMBeanInfo(on3);
        MBeanInfo mbi4 = mbs.getMBeanInfo(on4);
        MBeanInfo mbi5 = mbs.getMBeanInfo(on5);

        if (mbi1 != mbi2) {
            fail("Two MBeans of the same class should have identical " +
                 "MBeanInfo");
        }

        if (mbi2 != mbi3) {
            if (true)
                System.out.println("IGNORING StandardMBean(...) failure");
            else
                fail("Plain Standard MBean should have identical MBeanInfo " +
                     "to StandardMBean(...) with same class as resource");
        }

        if (mbi4 == mbi5 || mbi4.equals(mbi5)) {
            fail("Two MBeans of the same class should NOT have identical " +
                 "MBeanInfo if they are NotificationBroadcasters and "+
                 "do not return the same MBeanNotificationInfo[]");
        }

        if (failure == null) {
            System.out.println("Test passed");
            return;
        }

        throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String why) {
        System.out.println("FAILURE: " + why);
        failure = why;
    }
}
