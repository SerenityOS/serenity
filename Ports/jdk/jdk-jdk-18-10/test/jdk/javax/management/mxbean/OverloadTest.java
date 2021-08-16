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
 * @summary Test that MXBean interfaces can contain overloaded methods
 * @author Eamonn McManus
 *
 * @run clean OverloadTest
 * @run build OverloadTest
 * @run main OverloadTest
 */

import java.lang.management.*;
import javax.management.*;

public class OverloadTest {
    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName("a:b=c");
        OverloadMXBean overloadImpl = new OverloadImpl();
        mbs.registerMBean(overloadImpl, on);
        OverloadMXBean p = JMX.newMXBeanProxy(mbs, on, OverloadMXBean.class);
        check(p.notOverloaded(5), "notOverloaded");
        check(p.overloaded(), "overloaded()");
        check(p.overloaded(5), "overloaded(int)");
        check(p.overloaded("x"), "overloaded(String)");
        check(p.overloaded(36, 64), "overloaded(int, int)");

//        ObjectName threadingName = new ObjectName("java.lang:type=Threading");
//        ThreadMXBean t =
//                JMX.newMXBeanProxy(mbs, threadingName, ThreadMXBean.class);
//        long[] ids = t.getAllThreadIds();
//        ThreadInfo ti = t.getThreadInfo(ids[0]);

        if (failure != null)
            throw new Exception(failure);

    }

    private static void check(String got, String expect) {
        if (!expect.equals(got)) {
            failure = "FAILED: got \"" + got + "\", expected \"" + expect + "\"";
            System.out.println(failure);
        }
    }

    public static interface OverloadMXBean {
        String notOverloaded(int x);
        String overloaded();
        String overloaded(int x);
        String overloaded(String x);
        String overloaded(int x, int y);
    }

    public static class OverloadImpl implements OverloadMXBean {
        public String notOverloaded(int x) {
            return "notOverloaded";
        }

        public String overloaded() {
            return "overloaded()";
        }

        public String overloaded(int x) {
            return "overloaded(int)";
        }

        public String overloaded(String x) {
            return "overloaded(String)";
        }

        public String overloaded(int x, int y) {
            return "overloaded(int, int)";
        }
    }

    private static String failure;
}
