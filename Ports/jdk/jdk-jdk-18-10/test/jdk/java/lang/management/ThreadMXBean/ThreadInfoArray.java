/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5058327 8074368
 * @summary Tests the correct behaviour of getThreadInfo(long[]) for non-existent
 *          thread IDs and the empty thread id array.
 *
 * @author  Mandy Chung
 * @author  Jaroslav Bachorik
 *
 * @modules jdk.management
 * @build ThreadInfoArray
 * @run main ThreadInfoArray
 */

import java.lang.management.*;
import javax.management.*;
import static java.lang.management.ManagementFactory.*;

public class ThreadInfoArray {
    public static void main(String[] argv) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName(THREAD_MXBEAN_NAME);

        ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
        ThreadMXBean proxy = newPlatformMXBeanProxy(mbs,
                                 on.toString(),
                                 ThreadMXBean.class);

        checkNullElement(mbean, proxy, mbs, on);
        checkEmptyArray(mbean, proxy, mbs, on);
        System.out.println("Test passed");
    }

    private static void checkNullElement(ThreadMXBean mbean, ThreadMXBean proxy,
                                         MBeanServer mbs, ObjectName on)
        throws Exception {
        System.out.println("--- Check null element");
        // ID for a new thread
        long [] ids = {new Thread().getId()};
        // direct call
        ThreadInfo[] tinfos = mbean.getThreadInfo(ids);

        if (tinfos[0] != null) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected to have a null element");
        }

        // call getThreadInfo through MBeanServer
        Object[] params = {ids};
        String[] sigs = {"[J"};
        Object[] result = (Object[]) mbs.invoke(on, "getThreadInfo", params, sigs);

        if (result[0] != null) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected to have a null element via MBeanServer");
        }

        // call getThreadInfo through proxy
        tinfos = proxy.getThreadInfo(ids);
        if (tinfos[0] != null) {
            throw new RuntimeException("TEST FAILED: " +
                "Expected to have a null element");
        }
        System.out.println("--- PASSED");
    }

    private static void checkEmptyArray(ThreadMXBean mbean, ThreadMXBean proxy,
                                        MBeanServer mbs, ObjectName on)
        throws Exception {
        System.out.println("--- Check empty TID array");

        long[] ids = new long[0];
        // direct call
        assertEmptyArray(mbean.getThreadInfo(ids), "Expected empty ThreadInfo array");
        assertEmptyArray(mbean.getThreadInfo(ids, 1), "Expected empty ThreadInfo array");
        assertEmptyArray(mbean.getThreadInfo(ids, true, true), "Expected empty ThreadInfo array");

        // call getThreadInfo through MBeanServer
        assertEmptyArray(
            (Object[]) mbs.invoke(
                on, "getThreadInfo", new Object[]{ids}, new String[]{"[J"}
            ),
            "Expected empty ThreadInfo array via MBeanServer"
        );
        assertEmptyArray(
            (Object[]) mbs.invoke(
                on, "getThreadInfo", new Object[]{ids, 1},
                new String[]{"[J", "int"}
            ),
            "Expected empty ThreadInfo array via MBeanServer"
        );
        assertEmptyArray(
            (Object[]) mbs.invoke(
                on, "getThreadInfo", new Object[]{ids, true, true},
                new String[]{"[J", "boolean", "boolean"}
            ),
            "Expected empty ThreadInfo array via MBeanServer"
        );

        // call getThreadInfo through proxy
        assertEmptyArray(proxy.getThreadInfo(ids), "Expected empty ThreadInfo array");
        assertEmptyArray(proxy.getThreadInfo(ids, 1), "Expected empty ThreadInfo array");
        assertEmptyArray(proxy.getThreadInfo(ids, true, true), "Expected empty ThreadInfo array");
        System.out.println("--- PASSED");
    }

    private static void assertEmptyArray(Object[] arr, String message) throws Exception {
        if (arr.length > 0) {
            throw new RuntimeException("TEST FAILED: " + message);
        }
    }
}
