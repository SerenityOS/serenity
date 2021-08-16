/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/monitoring/RuntimeMXBean/RuntimeMXBean001.
 * VM Testbase keywords: [quick, monitoring]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.monitoring.RuntimeMXBean.RuntimeMXBean001.RuntimeMXBean001
 *      -testMode=directly
 */

package nsk.monitoring.RuntimeMXBean.RuntimeMXBean001;

import nsk.share.*;
import nsk.share.test.*;
import nsk.monitoring.share.*;
import java.lang.management.*;

/**
 * Test functions of RuntimeMXBean.
 *
 *
 * Checks that values returned by getStartTime() and getUptime()
 * measure actual uptime well enough, i.e.
 *  - start time does not change with time
 *  - start time + uptime is approximately current time
 */
public class RuntimeMXBean001 extends MonitoringTestBase implements Initializable {
        private static final long NANO_MS = 1000000;

        private RuntimeMXBean runtime;
        private int iterations = 10;
        private long maxDiff = 5000;
        private int fail_count = 0;

        public void initialize() {
                runtime = monitoringFactory.getRuntimeMXBean();
        }

        private static final long getMillis() {
            return System.nanoTime() / NANO_MS;
        }

        private void checkTimes(long startTime, long upTime, String msg) {
                if (startTime <= 0)
                        throw new TestFailure("Invalid start time: " + startTime);
                if (upTime <= 0)
                        throw new TestFailure("Invalid uptime: " + upTime);
        }

        private void testUptimeOne(long sleepTime) {
                long startTime1 = runtime.getStartTime();
                long uptime1 = runtime.getUptime();
                long time1 = getMillis();
                log.info("startTime: " + startTime1 + ", uptime1: " + uptime1 + ", time1: " + time1);
                checkTimes(startTime1, uptime1, "Before sleep");
                try {
                        Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                        throw new TestFailure("Sleep was interrupted", e);
                }
                long startTime2 = runtime.getStartTime();
                long uptime2 = runtime.getUptime();
                long time2 = getMillis();
                checkTimes(startTime2, uptime2, "After sleep");

                if (startTime1 != startTime2)
                        throw new TestFailure("Start time before sleep: " + startTime1 + " start time after sleep: " + startTime2);
                if (uptime2 < uptime1)
                        throw new TestFailure("Uptime before sleep: " + uptime1 + " uptime after sleep: " + uptime2);

                long utDiff = (uptime2 - uptime1);
                long tDiff = (time2 - time1);
                long flowDiff = Math.abs(utDiff -tDiff);
                if (flowDiff > maxDiff) {
                        throw new TestFailure("Difference between timeflow of uptime and current time is too big: " + flowDiff + ", expecting at most " + maxDiff);
                }
        }

        private void testUptime() {
                for (int i = 0; i < iterations; ++i) {
                        log.info("Iteration: " + i);
                        testUptimeOne(LocalRandom.randomPauseTime());
                }
        }

        public void run() {
                testUptime();
        }

        public static void main(String[] args) {
                Monitoring.runTest(new RuntimeMXBean001(), args);
        }
}
