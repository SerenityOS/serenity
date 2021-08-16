/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8185003
 * @build ThreadDump
 * @run main MaxDepthForThreadInfoTest
 * @summary verifies the functionality of ThreadMXBean.dumpAllThreads
 * and ThreadMXBean.getThreadInfo with maxDepth argument
 */

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;



public class MaxDepthForThreadInfoTest {


    public static void main(String[] Args) {

        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

        long[] threadIds = tmxb.getAllThreadIds();

        ThreadInfo[] tinfos = tmxb.getThreadInfo(threadIds, true, true, 0);
        for (ThreadInfo ti : tinfos) {
            if (ti != null && ti.getStackTrace().length > 0) {
                ThreadDump.printThreadInfo(ti);
                throw new RuntimeException("more than requested " +
                        "number of frames dumped");
            }
        }

        tinfos = tmxb.getThreadInfo(threadIds, true, true, 3);
        for (ThreadInfo ti : tinfos) {
            if (ti != null && ti.getStackTrace().length > 3) {
                ThreadDump.printThreadInfo(ti);
                throw new RuntimeException("more than requested " +
                        "number of frames dumped");
            }
        }

        try {
            tmxb.getThreadInfo(threadIds, true, true, -1);
            throw new RuntimeException("Didn't throw IllegalArgumentException " +
                    "for negative maxdepth value");
        } catch (IllegalArgumentException e) {
            System.out.println("Throwed IllegalArgumentException as expected");
        }

        tinfos = tmxb.dumpAllThreads(true, true, 0);
        for (ThreadInfo ti : tinfos) {
            if (ti.getStackTrace().length > 0) {
                ThreadDump.printThreadInfo(ti);
                throw new RuntimeException("more than requested " +
                        "number of frames dumped");
            }
        }
        tinfos = tmxb.dumpAllThreads(true, true, 2);
        for (ThreadInfo ti : tinfos) {
            if (ti.getStackTrace().length > 2) {
                ThreadDump.printThreadInfo(ti);
                throw new RuntimeException("more than requested " +
                        "number of frames dumped");
            }
        }

        try {
            tmxb.dumpAllThreads(true, true, -1);
            throw new RuntimeException("Didn't throw IllegalArgumentException " +
                    "for negative maxdepth value");
        } catch (IllegalArgumentException e) {
            System.out.println("Throwed IllegalArgumentException as expected");
        }

        System.out.println("Test passed");
    }
}
