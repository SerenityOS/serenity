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
 * @bug 6335337
 * @summary Test correctness of mxbean flag in descriptor.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MXBeanFlagTest
 * @run build MXBeanFlagTest
 * @run main MXBeanFlagTest
 */

import javax.management.*;

public class MXBeanFlagTest {

    public interface Compliant1MXBean {}
    public static class Compliant1 implements Compliant1MXBean {}

    public interface Compliant2MXBean {}
    public static class Compliant2 implements Compliant2MXBean {}

    public interface Compliant3MBean {}
    public static class Compliant3 implements Compliant3MBean {}

    public interface Compliant4MBean {}
    public static class Compliant4 implements Compliant4MBean {}

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName on1 = new ObjectName(":type=Compliant1");
        ObjectName on2 = new ObjectName(":type=Compliant2");
        ObjectName on3 = new ObjectName(":type=Compliant3");
        ObjectName on4 = new ObjectName(":type=Compliant4");
        Compliant1 compliant1 = new Compliant1();
        StandardMBean compliant2 =
            new StandardMBean(new Compliant2(), Compliant2MXBean.class, true);
        Compliant3 compliant3 = new Compliant3();
        StandardMBean compliant4 =
            new StandardMBean(new Compliant4(), Compliant4MBean.class, false);
        mbs.registerMBean(compliant1, on1);
        mbs.registerMBean(compliant2, on2);
        mbs.registerMBean(compliant3, on3);
        mbs.registerMBean(compliant4, on4);
        String flag1 = (String)
            mbs.getMBeanInfo(on1).getDescriptor().getFieldValue("mxbean");
        String flag2 = (String)
            mbs.getMBeanInfo(on2).getDescriptor().getFieldValue("mxbean");
        String flag3 = (String)
            mbs.getMBeanInfo(on3).getDescriptor().getFieldValue("mxbean");
        String flag4 = (String)
            mbs.getMBeanInfo(on4).getDescriptor().getFieldValue("mxbean");
        System.out.println("MXBean compliant1:\n" +
            "\t[Expected: mxbean=true]  [Got: mxbean=" + flag1 + "]");
        System.out.println("MXBean compliant2:\n" +
            "\t[Expected: mxbean=true]  [Got: mxbean=" + flag2 + "]");
        System.out.println("Standard MBean compliant3:\n" +
            "\t[Expected: mxbean=false] [Got: mxbean=" + flag3 + "]");
        System.out.println("Standard MBean compliant4:\n" +
            "\t[Expected: mxbean=false] [Got: mxbean=" + flag4 + "]");
        if (flag1 != null && flag1.equals("true") &&
            flag2 != null && flag2.equals("true") &&
            flag3 != null && flag3.equals("false") &&
            flag4 != null && flag4.equals("false"))
            System.out.println("Test PASSED");
        else {
            System.out.println("Test FAILED");
            throw new Exception("invalid flags");
        }
    }
}
