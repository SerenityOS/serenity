/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic unit test of HotspotRuntimeMBean.getTotalSafepointTime()
 *
 * @run main/othervm -XX:+UsePerfData GetTotalSafepointTime
 */

/*
 * This test is just a sanity check and does not check for the correct value.
 */

import sun.management.*;

public class GetTotalSafepointTime {

    private static HotspotRuntimeMBean mbean =
        (HotspotRuntimeMBean)ManagementFactoryHelper.getHotspotRuntimeMBean();

    // Careful with these values.
    private static final long MIN_VALUE_FOR_PASS = 1;

    // Thread.getAllStackTraces() should cause safepoints.
    // If this test is failing because it doesn't,
    // MIN_VALUE_FOR_PASS should be reset to 0
    public static long executeThreadDumps(long initial_value) {
        long value;
        do {
            Thread.getAllStackTraces();
            value = mbean.getTotalSafepointTime();
        } while (value == initial_value);
        return value;
    }

    public static void main(String args[]) throws Exception {
        long value = executeThreadDumps(0);
        System.out.println("Total safepoint time (ms): " + value);

        if (value < MIN_VALUE_FOR_PASS) {
            throw new RuntimeException("Total safepoint time " +
                                       "illegal value: " + value + " ms " +
                                       "(MIN = " + MIN_VALUE_FOR_PASS + ")");
        }

        long value2 = executeThreadDumps(value);
        System.out.println("Total safepoint time (ms): " + value2);

        if (value2 <= value) {
            throw new RuntimeException("Total safepoint time " +
                                       "did not increase " +
                                       "(value = " + value + "; " +
                                       "value2 = " + value2 + ")");
        }

        System.out.println("Test passed.");
    }
}
