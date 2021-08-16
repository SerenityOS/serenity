/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/*
 * @test
 * @bug 8010285
 * @summary General MBean test.
 * @author Jaroslav Bachorik
 *
 * @run clean MBeanTest
 * @run build MBeanTest
 * @run main MBeanTest
 */
public class MBeanTest {
    private static interface PrivateMBean {
        public int[] getInts();
    }

    public static class Private implements PrivateMBean {
        public int[] getInts() {
            return new int[]{1,2,3};
        }
    }

    public static interface NonCompliantMBean {
        public boolean getInt();
        public boolean isInt();
        public void setInt(int a);
        public void setInt(long b);
    }

    public static class NonCompliant implements NonCompliantMBean {
        public boolean getInt() {
            return false;
        }

        public boolean isInt() {
            return true;
        }

        public void setInt(int a) {
        }

        public void setInt(long b) {
        }
    }

    public static interface CompliantMBean {
        public boolean isFlag();
        public int getInt();
        public void setInt(int value);
    }

    public static class Compliant implements CompliantMBean {
        public boolean isFlag() {
            return false;
        }

        public int getInt() {
            return 1;
        }

        public void setInt(int value) {
        }
    }

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        testCompliant(CompliantMBean.class, new Compliant());
        testNonCompliant(PrivateMBean.class, new Private());
        testNonCompliant(NonCompliantMBean.class, new NonCompliant());

        if (failures == 0)
            System.out.println("Test passed");
        else
            throw new Exception("TEST FAILURES: " + failures);
    }

    private static void fail(String msg) {
        failures++;
        System.out.println("FAIL: " + msg);
    }

    private static void success(String msg) {
        System.out.println("OK: " + msg);
    }

    private static void testNonCompliant(Class<?> iface, Object bean) throws Exception {
        try {
            System.out.println("Registering a non-compliant MBean " +
                                iface.getName() + " ...");

            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            ObjectName on = new ObjectName("test:type=NonCompliant");

            mbs.registerMBean(bean, on);

            fail("Registered a non-compliant MBean - " + iface.getName());
        } catch (Exception e) {
            Throwable t = e;
            while (t != null && !(t instanceof NotCompliantMBeanException)) {
                t = t.getCause();
            }
            if (t != null) {
                success("MBean not registered");
            } else {
                throw e;
            }
        }
    }
    private static void testCompliant(Class<?> iface, Object bean) throws Exception {
        try {
            System.out.println("Registering a compliant MBean " +
                                iface.getName() + " ...");

            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            ObjectName on = new ObjectName("test:type=Compliant");

            mbs.registerMBean(bean, on);
            success("Registered a compliant MBean - " + iface.getName());
        } catch (Exception e) {
            Throwable t = e;
            while (t != null && !(t instanceof NotCompliantMBeanException)) {
                t = t.getCause();
            }
            if (t != null) {
                fail("MBean not registered");
            } else {
                throw e;
            }
        }
    }
}
