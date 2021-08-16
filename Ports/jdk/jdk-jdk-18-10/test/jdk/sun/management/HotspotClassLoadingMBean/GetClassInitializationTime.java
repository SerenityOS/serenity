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
 * @summary Basic unit test of HotspotClassLoadingMBean.getClassInitializationTime()
 * @author  Steve Bohne
 *
 * @run main/othervm -XX:+UsePerfData GetClassInitializationTime
 */

/*
 * This test is just a sanity check and does not check for the correct value.
 */

import sun.management.*;

public class GetClassInitializationTime {

    private static HotspotClassLoadingMBean mbean =
        (HotspotClassLoadingMBean)ManagementFactoryHelper.getHotspotClassLoadingMBean();

    // Careful with these values.
    private static final long MIN_TIME_FOR_PASS = 1;
    private static final long MAX_TIME_FOR_PASS = Long.MAX_VALUE;

    private static boolean trace = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        long time = mbean.getClassInitializationTime();

        if (trace) {
            System.out.println("Class initialization time (ms): " + time);
        }

        if (time < MIN_TIME_FOR_PASS || time > MAX_TIME_FOR_PASS) {
            throw new RuntimeException("Class initialization time " +
                                       "illegal value: " + time + " ms " +
                                       "(MIN = " + MIN_TIME_FOR_PASS + "; " +
                                       "MAX = " + MAX_TIME_FOR_PASS + ")");
        }

        // Load a class and make sure the time increases
        Class.forName("ClassToInitialize");

        long time2 = mbean.getClassInitializationTime();

        if (trace) {
            System.out.println("Class initialization time2 (ms): " + time2);
        }

        if (time2 <= time) {
            throw new RuntimeException("Class initialization time " +
                                       "did not increase when class loaded " +
                                       "(time = " + time + "; " +
                                       "time2 = " + time2 + ")");
        }

        System.out.println("Test passed.");
    }
}

// This class should just cause the initialization timer to run
class ClassToInitialize {
    static {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // do nothing
        }
    }
}
