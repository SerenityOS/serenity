/*
 * Copyright (c) 2020, Red Hat Inc.
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


package jdk.test.lib.containers.cgroup;

import java.util.Objects;

import jdk.internal.platform.Metrics;

/**
 * Cgroup version agnostic metrics tester
 *
 */
public class MetricsTester {

    private static final String CGROUP_V1 = "cgroupv1";
    private static final String CGROUP_V2 = "cgroupv2";

    private static CgroupMetricsTester createInstance(Metrics m) {
        Objects.requireNonNull(m);
        if (CGROUP_V1.equals(m.getProvider())) {
            MetricsTesterCgroupV1 t = new MetricsTesterCgroupV1();
            t.setup();
            return t;
        } else if (CGROUP_V2.equals(m.getProvider())) {
            return new MetricsTesterCgroupV2();
        } else {
            System.err.println("WARNING: Metrics provider, '" + m.getProvider()
                                                              + "' is unknown!");
            return null;
        }
    }

    public void testAll(Metrics m) throws Exception {
        CgroupMetricsTester tester =  createInstance(m);
        tester.testCpuAccounting();
        tester.testCpuConsumption();
        tester.testCpuSchedulingMetrics();
        tester.testCpuSets();
        tester.testMemorySubsystem();
        tester.testMemoryUsage();
        tester.testMisc();
    }

    public static void main(String[] args) throws Exception {
        Metrics m = Metrics.systemMetrics();
        // If cgroups is not configured, report success
        if (m == null) {
            System.out.println("TEST PASSED!!!");
            return;
        }

        MetricsTester metricsTester = new MetricsTester();
        metricsTester.testAll(m);
        System.out.println("TEST PASSED!!!");
    }
}
