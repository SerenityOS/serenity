/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jdk.internal.platform.Metrics;

public class MetricsCpuTester {
    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
        switch (args[0]) {
            case "cpusets":
                testCpuSets(args[1]);
                break;
            case "cpuquota":
                testCpuQuotaAndPeriod(Long.parseLong(args[1]), Long.parseLong(args[2]));
                break;
            case "cpushares":
                testCpuShares(Long.parseLong(args[1]));
                break;
            case "cpus":
                testCpuThrottling();
                break;
            case "cpumems":
                testCpuSetMemNodes(args[1]);
                break;
            case "combo":
                testCombo(args[1], Long.parseLong(args[2]), Long.parseLong(args[3]), Long.parseLong(args[4]));
                break;
        }
    }

    private static void testCpuQuotaAndPeriod(long quota, long period) {
        Metrics metrics = Metrics.systemMetrics();
        long newQuota = metrics.getCpuQuota();
        long newPeriod = metrics.getCpuPeriod();
        if (quota != newQuota || period != newPeriod) {
            throw new RuntimeException("CPU quota or period not equal, expected : ["
                    + quota + "," + period + "]" + ", got : " + "[" + newQuota
                    + "," + newPeriod + "]");
        }

        long cpuNumPeriods = metrics.getCpuNumPeriods();
        long current = System.currentTimeMillis();
        while (System.currentTimeMillis() - current < 1000) ;    // 1sec
        long newCpuNumPeriods = metrics.getCpuNumPeriods();
        if (newCpuNumPeriods <= cpuNumPeriods) {
            throw new RuntimeException("CPU shares failed, expected : ["
                    + cpuNumPeriods + "]" + ", got : " + "["
                    + newCpuNumPeriods + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testCpuSets(String cpuset) {
        int[] ipCpuSet;
        String[] tokens = cpuset.split("-");
        if (tokens.length > 1) { // we are given range of CPUs
            ipCpuSet = IntStream.rangeClosed(Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[1])).toArray();
        } else if (cpuset.split(",").length > 1) {   // list of cpus
            ipCpuSet = Stream.of(cpuset.split(",")).mapToInt(Integer::parseInt).toArray();
        } else { // just a single cpu
            ipCpuSet = new int[]{Integer.parseInt(cpuset)};
        }

        Metrics metrics = Metrics.systemMetrics();
        int[] cpuSets = metrics.getCpuSetCpus();

        int[] effectiveCpus = metrics.getEffectiveCpuSetCpus();

        if (!Arrays.equals(ipCpuSet, cpuSets)) {
            throw new RuntimeException("Cpusets not equal, expected : "
                    + Arrays.toString(ipCpuSet) + ", got : " + Arrays.toString(cpuSets));
        }

        // Check to see if this metric is supported on this platform
        if (effectiveCpus != null) {
            if (!Arrays.equals(ipCpuSet, effectiveCpus)) {
                throw new RuntimeException("Effective Cpusets not equal, expected : "
                        + Arrays.toString(ipCpuSet) + ", got : "
                        + Arrays.toString(effectiveCpus));
            }
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testCpuSetMemNodes(String cpusetMems) {
        Metrics metrics = Metrics.systemMetrics();
        int[] cpuSets = metrics.getCpuSetMems();

        int[] ipCpuSet;
        String[] tokens = cpusetMems.split("-");
        if (tokens.length > 1) { // we are given range of CPUs
            ipCpuSet = IntStream.rangeClosed(Integer.parseInt(tokens[0]),
                    Integer.parseInt(tokens[1])).toArray();
        } else if (cpusetMems.split(",").length > 1) {   // list of cpus
            ipCpuSet = Stream.of(cpusetMems.split(",")).mapToInt(Integer::parseInt).toArray();
        } else { // just a single cpu
            ipCpuSet = new int[]{Integer.parseInt(cpusetMems)};
        }

        int[] effectiveMems = metrics.getEffectiveCpuSetMems();


        if (!Arrays.equals(ipCpuSet, cpuSets)) {
            throw new RuntimeException("Cpuset.mems not equal, expected : "
                    + Arrays.toString(ipCpuSet) + ", got : "
                    + Arrays.toString(cpuSets));
        }

        // Check to see if this metric is supported on this platform
        if (effectiveMems != null) {
            if (!Arrays.equals(ipCpuSet, effectiveMems)) {
                throw new RuntimeException("Effective mem nodes not equal, expected : "
                        + Arrays.toString(ipCpuSet) + ", got : "
                        + Arrays.toString(effectiveMems));
            }
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testCpuShares(long shares) {
        Metrics metrics = Metrics.systemMetrics();
        if ("cgroupv2".equals(metrics.getProvider()) && shares < 1024) {
            // Adjust input shares for < 1024 cpu shares as the
            // impl. rounds up to the next multiple of 1024
            shares = 1024;
        }
        long newShares = metrics.getCpuShares();
        if (newShares != shares) {
            throw new RuntimeException("CPU shares not equal, expected : ["
                    + shares + "]" + ", got : " + "[" + newShares + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testCpuThrottling() {
        Metrics metrics = Metrics.systemMetrics();
        long throttledTime = metrics.getCpuThrottledTime();
        long numThrottled = metrics.getCpuNumThrottled();

        long current = System.currentTimeMillis();

        while (System.currentTimeMillis() - current < 2000) ;  // 2 sec

        long newthrottledTime = metrics.getCpuThrottledTime();
        long newnumThrottled = metrics.getCpuNumThrottled();
        if (newthrottledTime <= throttledTime) {
            throw new RuntimeException("CPU throttle failed, expected : ["
                    + newthrottledTime + "]" + ", got : "
                    + "[" + throttledTime + "]");
        }

        if (newnumThrottled <= numThrottled) {
            throw new RuntimeException("CPU num throttle failed, expected : ["
                    + newnumThrottled + "]" + ", got : " + "["
                    + numThrottled + "]");
        }
        System.out.println("TEST PASSED!!!");
    }

    private static void testCombo(String cpuset, long quota, long period, long shares) {
        testCpuSets(cpuset);
        testCpuQuotaAndPeriod(quota, period);
        testCpuShares(shares);
    }
}
