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
 *
 * @summary converted from VM Testbase nsk/monitoring/ThreadMXBean/ThreadInfo/RunningThread/RunningThread001.
 * VM Testbase keywords: [quick, monitoring, feature_136, vm6]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.monitoring.ThreadMXBean.ThreadInfo.RunningThread.RunningThread001.RunningThread001
 *      -scenarioType=running
 */

package nsk.monitoring.ThreadMXBean.ThreadInfo.RunningThread.RunningThread001;

import java.lang.management.*;
import nsk.share.runner.*;
import nsk.monitoring.share.*;
import nsk.monitoring.share.thread.*;
import nsk.share.TestFailure;

/**
 * This test starts huge number of threads of different types. Each
 * thread is then brought into pre-defined state and ThreadInfo
 * information obtained from ThreadMXBean is verified. This is repeated
 * several times.
 */
public class RunningThread001 extends MonitoringTestBase implements RunParamsAware, ScenarioTypeAware {
        private RunParams runParams;
        private ThreadMXBean thread;
        private int threadCount;
        private ThreadMonitoringScenarioFactory scenarioFactory;
        private String scenarioType;
        private int iterations;
        private int maxDepth = 200;

        private void runOne() {
                ThreadMonitoringScenario scenario = scenarioFactory.createScenario(scenarioType);
                try {
                        log.info("Starting: " + scenario);
                        scenario.begin();
                        scenario.waitState();
                        log.info("State reached");
                        log.info("Checking: " + scenario);
                        scenario.check(thread);
                } finally {
                        log.info("Finishing: " + scenario);
                        scenarioFactory.finish();
                        scenario.finish();
                        log.info("Ending: " + scenario);
                        scenario.end();
                }

        }

        public void run() {
                thread = monitoringFactory.getThreadMXBean();
                scenarioFactory = new StandardThreadMonitoringScenarioFactory(log, maxDepth, runParams.getMediumLoadThreadsCount());
                iterations = argHandler.getIterations();
                for (int i = 0; i < iterations; ++i)
                        runOne();
                log.info("TEST PASSED");
        }

        public void setRunParams(RunParams runParams) {
                this.runParams = runParams;
        }

        public void setScenarioType(String scenarioType) {
                this.scenarioType = scenarioType;
        }

        public static void main(String[] args) {
                Monitoring.runTest(new RunningThread001(), args);
        }
}
