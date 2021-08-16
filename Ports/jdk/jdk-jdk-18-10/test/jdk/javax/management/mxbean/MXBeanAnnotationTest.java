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
 * @bug 6316491
 * @summary Check that the MXBean annotation works as advertised
 * @author Eamonn McManus
 *
 * @run clean MXBeanAnnotationTest
 * @run build MXBeanAnnotationTest
 * @run main MXBeanAnnotationTest
 */

import javax.management.*;

public class MXBeanAnnotationTest {
    @MXBean
    public static interface Empty {}

    public static class EmptyImpl implements Empty {}

    @MXBean(false)
    public static interface NotMXBean {}

    public static class NotImpl implements NotMXBean {}

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on = new ObjectName("a:b=c");

        // Test empty interface

        try {
            mbs.registerMBean(new EmptyImpl(), on);
            boolean ok = checkMXBean(mbs.getMBeanInfo(on), true,
                                     "empty MXBean interface");
            mbs.unregisterMBean(on);
            if (ok)
                System.out.println("OK: empty MXBean interface");
        } catch (Exception e) {
            failure = "MXBean with empty interface got exception: " + e;
            System.out.println("FAILED: " + failure);
            e.printStackTrace(System.out);
        }

        // Test @MXBean(false)

        try {
            mbs.registerMBean(new NotImpl(), on);
            failure = "Registered a non-Standard MBean with @MXBean(false)";
            System.out.println("FAILED: " + failure);
        } catch (NotCompliantMBeanException e) {
            System.out.println("OK: non-Standard MBean with @MXBean(false) " +
                               "rejected");
        }

        if (failure == null)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static boolean checkMXBean(MBeanInfo mbi, boolean expected,
                                       String what) {
        Descriptor d = mbi.getDescriptor();
        String mxbean = (String) d.getFieldValue("mxbean");
        boolean is = (mxbean != null && mxbean.equals("true"));
        if (is == expected)
            return true;
        else {
            failure = "MBean should " + (expected ? "" : "not ") +
                "have mxbean=true: " + d;
            return false;
        }
    }

    private static String failure;
}
