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
 * @summary converted from VM Testbase nsk/monitoring/CompilationMXBean/comptimemon001.
 * VM Testbase keywords: [quick, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that
 *         CompilationMXBean.isCompilationTimeMonitoringSupported()
 *     method returns true. The test performs directly access to management
 *     metrics within the same JVM.
 *     Note, that the test is correct ONLY against Sun's Hotspot VM. This
 *     feature is optional and the method may return either true, or false.
 *     However, Sun's implementation must always return true.
 *     The test passes, if the JVM has no compilation system, for example the
 *     test is invoked with -Xint option.
 * COMMENT
 *     Fixed the bug:
 *     4953476 TEST_BUG: The spec is updated accoring to 4944573 and 4947536
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/timeout=300 nsk.monitoring.CompilationMXBean.comptimemon001.comptimemon001
 */

package nsk.monitoring.CompilationMXBean.comptimemon001;

import java.io.*;
import nsk.share.test.*;
import nsk.share.TestFailure;
import nsk.monitoring.share.*;
import java.lang.management.*;

public class comptimemon001 extends MonitoringTestBase implements Initializable {
        private CompilationMXBean compilation;
        private int iterations = 30;

        public void initialize() {
                if (monitoringFactory.hasCompilationMXBean())
                        compilation = monitoringFactory.getCompilationMXBean();
        }

        public boolean testCompilationTimeMonitoringSupported() {
                if (compilation == null) {
                        // The JVM has no compilation system, for example the test is
                        // invoked with -Xint option
                        log.info("The JVM has no compilation system.");
                        return false;
                } else if (!compilation.isCompilationTimeMonitoringSupported()) {
                        // Check the method is... for the specified way of access to MBeans
                        log.error("Monitoring of compilation time is not supported.\n");
                        setFailed(true);
                        return false;
                }
                return true;
        }

        private void testCompilationTimeOne() {
                long sleepTime = LocalRandom.randomPauseTime();
                long startTime = compilation.getTotalCompilationTime();
                if (startTime < 0)
                        throw new TestFailure("getTotalCompilationTime < 0: " + startTime);
                try {
                        Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                        throw new TestFailure("Sleep was interrupted.");
                }
                long finishTime = compilation.getTotalCompilationTime();
                if (finishTime < 0)
                        throw new TestFailure("getTotalCompilationTime < 0: " + finishTime);
                if (finishTime < startTime)
                        throw new TestFailure("getTotalCompilationTime before sleep: " + startTime
                                + " > getTotalCompilationTime after sleep: " + finishTime);
        }

        public void testCompilationTime() {
                for (int i = 0; i < iterations; ++i)
                        testCompilationTimeOne();
        }

        public void run() {
                if (!testCompilationTimeMonitoringSupported())
                        return;
                testCompilationTime();
        }

        public static void main(String[] args) {
                Monitoring.runTest(new comptimemon001(), args);
        }
}
