/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6653214
 * @requires os.simpleArch=="x64"
 * @run main/othervm -Xmx3000M LargeHeapThresholdTest
 * @summary MemoryPoolMXBean.setUsageThreshold() does not support large heap sizes
 *
 * Large is >= 2 gigabytes
 * This test tries to find memory pools with maximum allowable size of at
 * least 2 gigabytes and set their usage thresholds to this value. If tested
 * Java implementation is defective
 * "java.lang.IllegalArgumentException: Invalid threshold value > max value of size_t"
 * will be thrown.
 * If no pool with maximum allowable size of at least 2 gigabytes exists
 * the test passes. There is a good chance that such pool will exist if JVM
 * is started with '-Xmx3000M' command line switch.
 */


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;


public class LargeHeapThresholdTest {

    final static long TWO_G = ((long) Integer.MAX_VALUE + 1); // 2 gigabytes

    public static void main(String[] args) {
        List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
        boolean verified = false;
        for (MemoryPoolMXBean i : pools) {
            if ((i.getUsage().getMax() >= TWO_G)
                    && i.isUsageThresholdSupported()) {
                i.setUsageThreshold(TWO_G);
                if(i.getUsageThreshold() != TWO_G)
                    throw new RuntimeException("Usage threshold for"
                            + " pool '" + i.getName() + "' is " + i.getUsageThreshold()
                            + " and not equal to 2GB");
                verified = true;
            }
        }
        System.out.println("Ability to use big heap thresholds has "
                + (verified ? "" : "NOT ") + "been verified");
    }
}
