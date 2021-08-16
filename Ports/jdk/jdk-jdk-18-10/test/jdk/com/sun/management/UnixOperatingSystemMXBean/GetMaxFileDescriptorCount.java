/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @bug     4858522
 * @summary Basic unit test of UnixOperatingSystemMXBean.getMaxFileDescriptorCount()
 * @author  Steve Bohne
 */

/*
 * This test is just a sanity check and does not check for the correct
 * value.  The correct value should be checked manually:
 * Solaris/Linux:
 *   1. In a shell, as user who started java process, enter the command
 *      "limit -h descriptors"
 */

import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.*;

public class GetMaxFileDescriptorCount {

    private static UnixOperatingSystemMXBean mbean =
        (UnixOperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();

    // Careful with these values.
    // Min count for pass dynamically determined below.
    private static long       min_count_for_pass = 1;
    private static final long MAX_COUNT_FOR_PASS = Long.MAX_VALUE;

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        long min_count = mbean.getOpenFileDescriptorCount();
        if (min_count > 0) {
            min_count_for_pass = min_count;
        }

        long count = mbean.getMaxFileDescriptorCount();

        if (trace) {
            System.out.println("Max file descriptor count: " + count);
        }

        if (count < min_count_for_pass || count > MAX_COUNT_FOR_PASS) {
            throw new RuntimeException("Max file descriptor count " +
                                       "illegal value: " + count + " bytes " +
                                       "(MIN = " + min_count_for_pass + "; " +
                                       "MAX = " + MAX_COUNT_FOR_PASS + ")");
        }

        System.out.println("Test passed.");
    }
}
