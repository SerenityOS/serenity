/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

package gc.epsilon;

/**
 * @test TestMemoryMXBeans
 * @requires vm.gc.Epsilon
 * @summary Test JMX memory beans
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC          -Xmx1g gc.epsilon.TestMemoryMXBeans   -1 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -Xms1g   -Xmx1g gc.epsilon.TestMemoryMXBeans 1024 1024
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC -Xms128m -Xmx1g gc.epsilon.TestMemoryMXBeans  128 1024
 */

import java.lang.management.*;

public class TestMemoryMXBeans {

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalStateException("Should provide expected heap sizes");
        }

        long initSize = 1L * Integer.parseInt(args[0]) * 1024 * 1024;
        long maxSize =  1L * Integer.parseInt(args[1]) * 1024 * 1024;

        testMemoryBean(initSize, maxSize);
        testAllocs();
    }

    public static void testMemoryBean(long initSize, long maxSize) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long heapInit = memoryMXBean.getHeapMemoryUsage().getInit();
        long heapMax = memoryMXBean.getHeapMemoryUsage().getMax();
        memoryMXBean.getNonHeapMemoryUsage().getInit(); // value not used
        memoryMXBean.getNonHeapMemoryUsage().getMax();  // value not used

        if (initSize > 0 && heapInit != initSize) {
            throw new IllegalStateException("Init heap size is wrong: " + heapInit + " vs " + initSize);
        }
        if (maxSize > 0 && heapMax != maxSize) {
            throw new IllegalStateException("Max heap size is wrong: " + heapMax + " vs " + maxSize);
        }
    }

    public static void testAllocs() throws Exception {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // Try multiple times, to capture either APIs we call allocate lazily, or the background threads allocating
        int maxTries = 10;
        int tries = 0;

        while (true) {
            // Compute how much we waste during the calls themselves:
            long heapUsed1 = memoryMXBean.getHeapMemoryUsage().getUsed();
            long heapUsed2 = memoryMXBean.getHeapMemoryUsage().getUsed();
            long adj = heapUsed2 - heapUsed1;

            heapUsed1 = memoryMXBean.getHeapMemoryUsage().getUsed();
            sink = new int[1024*1024];
            heapUsed2 = memoryMXBean.getHeapMemoryUsage().getUsed();

            long diff = heapUsed2 - heapUsed1 - adj;
            long min =  8 + 4*1024*1024;
            long max = 16 + 4*1024*1024;
            if ((min <= diff && diff <= max)) {
              // Success
              return;
            }

            if (tries++ > maxTries) {
              throw new IllegalStateException("Allocation did not change used space right: " + diff + " should be in [" + min + ", " + max + "]");
            }

            // Wait and try again
            Thread.sleep(1000);
        }
    }

}
