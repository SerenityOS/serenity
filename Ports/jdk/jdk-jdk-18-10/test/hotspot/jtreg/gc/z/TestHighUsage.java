/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.z;

/*
 * @test TestHighUsage
 * @requires vm.gc.Z
 * @summary Test ZGC "High Usage" rule
 * @library /test/lib
 * @run main/othervm gc.z.TestHighUsage
 */

import java.util.LinkedList;
import jdk.test.lib.process.ProcessTools;

public class TestHighUsage {
    static class Test {
        private static final int K = 1024;
        private static final int M = K * K;
        private static final long maxCapacity = Runtime.getRuntime().maxMemory();
        private static final long slowAllocationThreshold = 16 * M;
        private static final long highUsageThreshold = maxCapacity / 20; // 5%
        private static volatile LinkedList<byte[]> keepAlive;
        private static volatile Object dummy;

        public static void main(String[] args) throws Exception {
            System.out.println("Max capacity: " + (maxCapacity / M) + "M");
            System.out.println("High usage threshold: " + (highUsageThreshold / M) + "M");
            System.out.println("Allocating live-set");

            // Allocate live-set
            keepAlive = new LinkedList<>();
            while (Runtime.getRuntime().freeMemory() > slowAllocationThreshold) {
                while (Runtime.getRuntime().freeMemory() > slowAllocationThreshold) {
                    keepAlive.add(new byte[128 * K]);
                }

                // Compact live-set and let allocation rate settle down
                System.gc();
                Thread.sleep(2000);
            }

            System.out.println("Allocating garbage slowly");

            // Allocate garbage slowly, so that the sampled allocation rate on average
            // becomes zero MB/s for the last 1 second windows. Once we reach the high
            // usage threshold we idle to allow for a "High Usage" GC cycle to happen.
            // We need to allocate slowly to avoid an "Allocation Rate" GC cycle.
            for (int i = 0; i < 300; i++) {
                if (Runtime.getRuntime().freeMemory() > highUsageThreshold) {
                    // Allocate
                    dummy = new byte[128 * K];
                    System.out.println("Free: " + (Runtime.getRuntime().freeMemory() / M) + "M (Allocating)");
                } else {
                    // Idle
                    System.out.println("Free: " + (Runtime.getRuntime().freeMemory() / M) + "M (Idling)");
                }

                Thread.sleep(250);
            }

            System.out.println("Done");
        }
    }

    public static void main(String[] args) throws Exception {
        ProcessTools.executeTestJvm("-XX:+UseZGC",
                                    "-XX:-ZProactive",
                                    "-Xms128M",
                                    "-Xmx128M",
                                    "-XX:ParallelGCThreads=1",
                                    "-XX:ConcGCThreads=1",
                                    "-Xlog:gc,gc+start",
                                    Test.class.getName())
                    .shouldNotContain("Allocation Stall")
                    .shouldContain("High Usage")
                    .shouldHaveExitValue(0);
    }
}
