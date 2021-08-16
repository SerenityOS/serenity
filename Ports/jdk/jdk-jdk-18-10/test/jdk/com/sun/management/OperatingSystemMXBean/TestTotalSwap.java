/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4858522
 * @summary Basic unit test of OperatingSystemMXBean.getTotalSwapSpaceSize()
 * @author  Steve Bohne
 * @author  Jaroslav Bachorik
 *
 * @library /test/lib
 *
 * @run main TestTotalSwap
 */

/*
 * This test tests the actual swap size on linux and solaris.
 * The correct value should be checked manually:
 * Solaris:
 *   1. In a shell, enter the command: "swap -l"
 *   2. The value (reported in blocks) is in the "blocks" column.
 * Linux:
 *   1. In a shell, enter the command: "cat /proc/meminfo"
 *   2. The value (reported in bytes) is in "Swap" entry, "total" column.
 * Windows NT/XP/2000:
 *   1. Run Start->Accessories->System Tools->System Information.
 *   2. The value (reported in Kbytes) is in the "Page File Space" entry
 * Windows 98/ME:
 *   Unknown.
 *
 * Usage: GetTotalSwapSpaceSize <expected swap size | "sanity-only"> [trace]
 */

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestTotalSwap {

    private static final OperatingSystemMXBean mbean =
        (com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();

    // Careful with these values.
    // Min size for pass dynamically determined below.
    // zero if no swap space is configured.
    private static long       min_size_for_pass = 0;
    private static final long MAX_SIZE_FOR_PASS = Long.MAX_VALUE;

    public static void main(String args[]) throws Throwable {
        // yocto might ignore the request to report swap size in bytes
        boolean swapInKB = mbean.getVersion().contains("yocto");

        long expected_swap_size = getSwapSizeFromOs();

        long min_size = mbean.getFreeSwapSpaceSize();
        if (min_size > 0) {
            min_size_for_pass = min_size;
        }

        long size = mbean.getTotalSwapSpaceSize();

        System.out.println("Total swap space size in bytes: " + size);

        if (expected_swap_size > -1) {
            if (size != expected_swap_size) {
                // try the expected size in kiloBytes
                if (!(swapInKB && expected_swap_size * 1024 == size)) {
                    throw new RuntimeException("Expected total swap size      : " +
                                               expected_swap_size +
                                               " but getTotalSwapSpaceSize returned: " +
                                               size);
                }
            }
        }

        // sanity check
        if (size < min_size_for_pass || size > MAX_SIZE_FOR_PASS) {
            throw new RuntimeException("Total swap space size " +
                                       "illegal value: " + size + " bytes " +
                                       "(MIN = " + min_size_for_pass + "; " +
                                       "MAX = " + MAX_SIZE_FOR_PASS + ")");
        }

        System.out.println("Test passed.");
    }

    private static long getSwapSizeFromOs() throws Throwable {
        if (Platform.isLinux()) {
            // total       used       free     shared    buffers     cached
            // Mem:    16533540864 13638467584 2895073280  534040576 1630248960 6236909568
            // -/+ buffers/cache: 5771309056 10762231808
            // Swap:   15999168512          0 15999168512
            String swapSizeStr = ProcessTools.executeCommand("free", "-b")
                                             .firstMatch("Swap:\\s+([0-9]+)\\s+.*", 1);
            return Long.parseLong(swapSizeStr);
        } else if (Platform.isOSX()) {
            // total = 8192.00M used = 7471.11M free = 720.89M (encrypted)
            String swapSizeStr = ProcessTools.executeCommand(
                    "/usr/sbin/sysctl",
                    "-n",
                    "vm.swapusage"
            ).firstMatch("total\\s+=\\s+([0-9]+(\\.[0-9]+)?[Mm]?).*", 1);
            if (swapSizeStr.toLowerCase().endsWith("m")) {
                swapSizeStr = swapSizeStr.substring(0, swapSizeStr.length() - 1);
                return (long)(Double.parseDouble(swapSizeStr) * 1024 * 1024); // size in MB
            }
            return (long)(Double.parseDouble(swapSizeStr) * 1024 * 1024);
        } else {
            System.err.println("Unsupported operating system: " + Platform.getOsName());
        }

        return -1;
    }
}
