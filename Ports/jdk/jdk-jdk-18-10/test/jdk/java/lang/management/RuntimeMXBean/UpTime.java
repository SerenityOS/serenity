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
 * @bug     4530538
 * @summary Basic unit test of RuntimeMXBean.getUptime()
 * @author  Alexei Guibadoulline
 */

import java.lang.management.*;

public class UpTime {
    static final long DELAY = 5; // Seconds
    static final long TIMEOUT = 30; // Minutes
    static final long MULTIPLIER = 1000; // millisecond ticks

    private static final RuntimeMXBean metrics
        = ManagementFactory.getRuntimeMXBean();

    public static void main(String argv[]) throws Exception {
        long jvmStartTime = metrics.getStartTime();
        // this will get an aproximate JVM uptime before starting this test
        long jvmUptime = System.currentTimeMillis() - jvmStartTime;
        long systemStartOuter = System_milliTime();
        long metricsStart = metrics.getUptime();
        long systemStartInner = System_milliTime();

        // This JVM might have been running for some time if this test runs
        // in samevm mode.  The sanity check should apply to the test uptime.
        long testUptime = metricsStart - jvmUptime;

        // If uptime is more than 30 minutes then it looks like a bug in
        // the method
        if (testUptime > TIMEOUT * 60 * MULTIPLIER)
            throw new RuntimeException("Uptime of the JVM is more than 30 "
                                     + "minutes ("
                                     + (metricsStart / 60 / MULTIPLIER)
                                     + " minutes).");

        // Wait for DELAY seconds
        Object o = new Object();
        while (System_milliTime() < systemStartInner + DELAY * MULTIPLIER) {
            synchronized (o) {
                try {
                    o.wait(DELAY * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }

        long systemEndInner = System_milliTime();
        long metricsEnd = metrics.getUptime();
        long systemEndOuter = System_milliTime();

        long systemDifferenceInner = systemEndInner - systemStartInner;
        long systemDifferenceOuter = systemEndOuter - systemStartOuter;
        long metricsDifference = metricsEnd - metricsStart;

        // Check the flow of time in RuntimeMXBean.getUptime(). See the
        // picture below.
        // The measured times can be off by 1 due to conversions from
        // nanoseconds to milliseconds, using different channels to read the
        // HR timer and rounding error. Bigger difference will make the test
        // fail.
        if (metricsDifference - systemDifferenceInner < -1)
            throw new RuntimeException("Flow of the time in "
                                     + "RuntimeMXBean.getUptime() ("
                                     + metricsDifference + ") is slower than "
                                     + " in system (" + systemDifferenceInner
                                     + ")");
        if (metricsDifference - systemDifferenceOuter > 1)
            throw new RuntimeException("Flow of the time in "
                                     + "RuntimeMXBean.getUptime() ("
                                     + metricsDifference + ") is faster than "
                                     + "in system (" + systemDifferenceOuter
                                     + ")");

        System.out.println("Test passed.");
    }

    private static long System_milliTime() {
        return System.nanoTime() / 1000000; // nanoseconds / milliseconds;
    }
}

/*

   A picture to describe the second testcase that checks the flow of time in
   RuntimeMXBean.getUptime()


   start
             o1  u1  i1    Sleep for DELAY minutes    i2  u2  o2
     |-------|---|---|--------------------------------|---|---|---------------> time


   The following inequality (checked by the test) must always be true:

       o2-o1 >= u2-u1 >= i2-i1

   In the test:

   i1 - systemStartInner
   i2 - systemEndInner
   o1 - systemStartOuter
   o2 - systemEndOuter
   u1 - metricsStart
   u2 - metricsEnd

*/
