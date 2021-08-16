/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic unit test of OperatingSystemMXBean.getFreeSwapSpaceSize()
 * @author  Steve Bohne
 */

/*
 * This test is just a sanity check and does not check for the correct
 * value.  The correct value should be checked manually:
 * Solaris:
 *   1. In a shell, enter the command: "swap -l"
 *   2. The value (reported in blocks) is in the "free" column.
 * Linux:
 *   1. In a shell, enter the command: "cat /proc/meminfo"
 *   2. The value (reported in bytes) is in "Swap" entry, "free" column.
 * Windows NT/XP/2000:
 *   Unknown.
 * Windows 98/ME:
 *   Unknown.
 */

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

public class GetFreeSwapSpaceSize {

    private static OperatingSystemMXBean mbean =
        (com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();

    // Careful with these values.
    // Min size is zero since the system can run of swap spaces
    private static final long MIN_SIZE_FOR_PASS = 0;
    // Max size for pass dynamically determined below.
    private static long       max_size_for_pass = Long.MAX_VALUE;

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        long max_size = mbean.getTotalSwapSpaceSize();
        if (max_size > 0) {
            max_size_for_pass = max_size;
        }

        long size = mbean.getFreeSwapSpaceSize();

        if (trace) {
            System.out.println("Free swap space size in bytes: " + size);
        }

        if (size < MIN_SIZE_FOR_PASS || size > max_size_for_pass) {
            throw new RuntimeException("Free swap space size " +
                                       "illegal value: " + size + " bytes " +
                                       "(MIN = " + MIN_SIZE_FOR_PASS + "; " +
                                       "MAX = " + max_size_for_pass + ")");
        }

        System.out.println("Test passed.");
    }
}
