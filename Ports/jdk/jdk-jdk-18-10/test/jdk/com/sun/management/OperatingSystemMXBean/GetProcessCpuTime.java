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
 * @summary Basic unit test of OperatingSystemMXBean.getProcessCpuTime()
 * @author  Steve Bohne
 */

/*
 * This test is just a sanity check and does not check for the correct
 * value.  The correct value should be checked manually:
 * Solaris/Linux:
 *   1. In a shell, enter the command: "ps -ef"
 *   2. The value (reported in seconds) is in the "TIME" column for the process
 * Windows NT/XP/2000:
 *   1. Hit Ctrl-Alt-Delete, select Task Manager, select Processes tab
 *   2. The value (reported in seconds) is in the "CPU Time" column for the
 *      process.  You may have to add this column via View menu-> Select
 *      Columns... item.
 * Windows 98/ME:
 *   Not supported.
 */

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.*;

public class GetProcessCpuTime {

    private static OperatingSystemMXBean mbean =
        (com.sun.management.OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();


    // Careful with these values.
    private static final long MIN_TIME_FOR_PASS = 1;
    private static final long MAX_TIME_FOR_PASS = Long.MAX_VALUE;

    // No max time.

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        // Consume cpu time
        double sum=0;
        for (int i=0; i <=2000000; i++) {
          sum += i;
          sum /= i;
        }

        long ns = mbean.getProcessCpuTime();
        if (ns == -1) {
            System.out.println("getProcessCpuTime() is not supported");
            return;
        }

        if (trace) {
            System.out.println("Process CPU time in ns: " + ns);
        }

        if (ns < MIN_TIME_FOR_PASS || ns > MAX_TIME_FOR_PASS) {
            throw new RuntimeException("Process CPU time " +
                                       "illegal value: " + ns + " ns " +
                                       "(MIN = " + MIN_TIME_FOR_PASS + "; " +
                                       "MAX = " + MAX_TIME_FOR_PASS + ")");
        }

        System.out.println("Test passed.");
    }
}
