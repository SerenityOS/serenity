/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import jdk.internal.platform.CgroupV1Metrics;
import jdk.internal.platform.Metrics;

public class MetricsMemoryTester {

    public static final long UNLIMITED = -1;

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        switch (args[0]) {
            case "memory":
                testMemoryLimit(args[1]);
                break;
            case "memoryswap":
                testMemoryAndSwapLimit(args[1], args[2]);
                break;
            case "kernelmem":
                testKernelMemoryLimit(args[1]);
                break;
            case "oomkill":
                testOomKillFlag(Boolean.parseBoolean(args[2]));
                break;
            case "failcount":
                testMemoryFailCount();
                break;
            case "softlimit":
                testMemorySoftLimit(args[1]);
                break;
        }
    }

    private static void testMemoryLimit(String value) {
        long limit = getMemoryValue(value);

        if (limit != Metrics.systemMetrics().getMemoryLimit()) {
            throw new RuntimeException("Memory limit not equal, expected : ["
                    + limit + "]" + ", got : ["
                    + Metrics.systemMetrics().getMemoryLimit() + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testMemoryFailCount() {
        long memAndSwapLimit = Metrics.systemMetrics().getMemoryAndSwapLimit();
        long memLimit = Metrics.systemMetrics().getMemoryLimit();

        // We need swap to execute this test or will SEGV
        if (memAndSwapLimit <= memLimit) {
            System.out.println("No swap memory limits, test case skipped");
        } else {
            long count = Metrics.systemMetrics().getMemoryFailCount();

            // Allocate 512M of data
            byte[][] bytes = new byte[64][];
            boolean atLeastOneAllocationWorked = false;
            for (int i = 0; i < 64; i++) {
                try {
                    bytes[i] = new byte[8 * 1024 * 1024];
                    atLeastOneAllocationWorked = true;
                    // Break out as soon as we see an increase in failcount
                    // to avoid getting killed by the OOM killer.
                    if (Metrics.systemMetrics().getMemoryFailCount() > count) {
                        break;
                    }
                } catch (Error e) { // OOM error
                    break;
                }
            }
            if (!atLeastOneAllocationWorked) {
                System.out.println("Allocation failed immediately. Ignoring test!");
                return;
            }
            // Be sure bytes allocations don't get optimized out
            System.out.println("DEBUG: Bytes allocation length 1: " + bytes[0].length);
            if (Metrics.systemMetrics().getMemoryFailCount() <= count) {
                throw new RuntimeException("Memory fail count : new : ["
                        + Metrics.systemMetrics().getMemoryFailCount() + "]"
                        + ", old : [" + count + "]");
            }
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testMemorySoftLimit(String softLimit) {

        long memorySoftLimit = Metrics.systemMetrics().getMemorySoftLimit();
        long newmemorySoftLimit = getMemoryValue(softLimit);

        if (newmemorySoftLimit != memorySoftLimit) {
            throw new RuntimeException("Memory softlimit not equal, Actual : ["
                    + newmemorySoftLimit + "]" + ", Expected : ["
                    + memorySoftLimit + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testKernelMemoryLimit(String value) {
        Metrics m = Metrics.systemMetrics();
        if (m instanceof CgroupV1Metrics) {
            CgroupV1Metrics mCgroupV1 = (CgroupV1Metrics)m;
            System.out.println("TEST PASSED!!!");
            long limit = getMemoryValue(value);
            long kmemlimit = mCgroupV1.getKernelMemoryLimit();
            if (kmemlimit != UNLIMITED && limit != kmemlimit) {
                throw new RuntimeException("Kernel Memory limit not equal, expected : ["
                        + limit + "]" + ", got : ["
                        + kmemlimit + "]");
            }
        } else {
            throw new RuntimeException("kernel memory limit test not supported for cgroups v2");
        }
    }

    private static void testMemoryAndSwapLimit(String memory, String memAndSwap) {
        long expectedMem = getMemoryValue(memory);
        long expectedMemAndSwap = getMemoryValue(memAndSwap);
        long actualMemAndSwap = Metrics.systemMetrics().getMemoryAndSwapLimit();

        if (expectedMem != Metrics.systemMetrics().getMemoryLimit()
                || (expectedMemAndSwap != actualMemAndSwap
                && expectedMem != actualMemAndSwap)) {
            throw new RuntimeException("Memory and swap limit not equal, expected : ["
                    + expectedMem + ", " + expectedMemAndSwap + "]"
                    + ", got : [" + Metrics.systemMetrics().getMemoryLimit()
                    + ", " + Metrics.systemMetrics().getMemoryAndSwapLimit() + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static long getMemoryValue(String value) {
        long result;
        if (value.endsWith("m")) {
            result = Long.parseLong(value.substring(0, value.length() - 1))
                    * 1024 * 1024;
        } else if (value.endsWith("g")) {
            result = Long.parseLong(value.substring(0, value.length() - 1))
                    * 1024 * 1024 * 1024;
        } else {
            result = Long.parseLong(value);
        }
        return result;
    }

    private static void testOomKillFlag(boolean oomKillFlag) {
        Metrics m = Metrics.systemMetrics();
        if (m instanceof CgroupV1Metrics) {
            CgroupV1Metrics mCgroupV1 = (CgroupV1Metrics)m;
            Boolean expected = Boolean.valueOf(oomKillFlag);
            Boolean actual = mCgroupV1.isMemoryOOMKillEnabled();
            if (!(expected.equals(actual))) {
                throw new RuntimeException("oomKillFlag error");
            }
            System.out.println("TEST PASSED!!!");
        } else {
            throw new RuntimeException("oomKillFlag test not supported for cgroups v2");
        }
    }
}
