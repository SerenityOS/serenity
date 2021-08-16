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
 * @summary Basic unit test of OperatingSystemMXBean.getFreePhysicalMemorySize()
 * @author  Steve Bohne
 */

/*
 * This test is just a sanity check and does not check for the correct
 * value.  The correct value should be checked manually:
 * Solaris:
 *   1. In a shell, enter the command: "sar -r 1"
 *   2. The value (reported in pages) is in the "freemem" column.
 * Linux:
 *   1. In a shell, enter the command: "cat /proc/meminfo"
 *   2. The value (reported in bytes) is in "Mem" entry, "free" column.
 * Windows NT/XP/2000:
 *   1. Hit Ctrl-Alt-Delete, select Task Manager, go to Performance tab.
 *   2. The value (reported in Kbytes) is in the entry "Available" in the
 *      "Physical Memory" box.
 * Windows 98/ME:
 *   1. Run Start->Programs->Accessories->System Tools->System Information.
 *   2. Use the "XXXMB RAM" and "XX% system resources free" to determine free
 *      physical memory.
 */

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

public class GetFreePhysicalMemorySize {

    private static OperatingSystemMXBean mbean =
        (com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();

    // Careful with these values.
    private static final long MIN_SIZE_FOR_PASS = 1;
    // Max size for pass dynamically determined below.
    private static long       max_size_for_pass = Long.MAX_VALUE;

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        long max_size = mbean.getTotalPhysicalMemorySize();
        if (max_size > 0) {
            max_size_for_pass = max_size;
        }

        long size = mbean.getFreePhysicalMemorySize();

        if (trace) {
            System.out.println("Free physical memory size in bytes: " + size);
        }

        if (size < MIN_SIZE_FOR_PASS || size > max_size_for_pass) {
            throw new RuntimeException("Free physical memory size " +
                                       "illegal value: " + size + " bytes " +
                                       "(MIN = " + MIN_SIZE_FOR_PASS + "; " +
                                       "MAX = " + max_size_for_pass + ")");
        }

        System.out.println("Test passed.");
    }
}
