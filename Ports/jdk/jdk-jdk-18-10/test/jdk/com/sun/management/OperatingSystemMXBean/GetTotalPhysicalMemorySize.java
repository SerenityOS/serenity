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
 * @summary Basic unit test of OperatingSystemMXBean.getTotalPhysicalMemorySize()
 * @author  Steve Bohne
 */

/*
 * This test is just a sanity check and does not check for the correct
 * value.  The correct value should be checked manually:
 * Solaris:
 *   1. In a shell, enter the command: "prtconf"
 *   2. The value (reported in MBytes) is in the "Memory size" entry.
 * Linux:
 *   1. In a shell, enter the command: "cat /proc/meminfo"
 *   2. The value (reported in bytes) is in "Mem" entry, "total" column.
 * Windows NT/XP/2000:
 *   1. Hit Ctrl-Alt-Delete, select Task Manager, select Performance tab
 *   2. The value (reported in Kbytes) is in the "Physical Memory" box,
 *      in the "Total" entry.
 * Windows 98/ME:
 *   1. Right click on My Computer, select Properties.  Go to Performance tab.
 *   2. Total memory should be listed in the panel.
 */

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

public class GetTotalPhysicalMemorySize {

    private static OperatingSystemMXBean mbean =
        (com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();

    // Careful with these values.
    // Min size for pass dynamically determined below.
    private static long       min_size_for_pass = 1;
    private static final long MAX_SIZE_FOR_PASS = Long.MAX_VALUE;

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        long min_size = mbean.getFreePhysicalMemorySize();
        if (min_size > 0) {
            min_size_for_pass = min_size;
        }

        long size = mbean.getTotalPhysicalMemorySize();

        if (trace) {
            System.out.println("Total physical memory size in bytes: " + size);
        }

        if (size < min_size_for_pass || size > MAX_SIZE_FOR_PASS) {
            throw new RuntimeException("Total physical memory size " +
                                       "illegal value: " + size + " bytes " +
                                       "(MIN = " + min_size_for_pass + "; " +
                                       "MAX = " + MAX_SIZE_FOR_PASS + ")");
        }

        System.out.println("Test passed.");
    }
}
