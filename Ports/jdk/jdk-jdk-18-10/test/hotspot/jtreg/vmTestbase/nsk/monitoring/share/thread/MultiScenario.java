/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.share.thread;

import java.lang.management.*;
import nsk.share.log.*;
import java.util.List;
import java.util.ArrayList;

/**
 * MultiScenario is stress scenario that creates many scenarios and runs all of them.
 */
public class MultiScenario implements ThreadMonitoringScenario {
        private ThreadMonitoringScenario[] scenarios;
        private int count;
        private ThreadMonitoringScenarioFactory scenarioFactory;

        public MultiScenario(ThreadMonitoringScenarioFactory scenarioFactory, int count) {
                this.scenarioFactory = scenarioFactory;
                this.count = count;
        }

        public void begin() {
                scenarios = scenarioFactory.createScenarios(count);
                for (ThreadMonitoringScenario scenario : scenarios)
                        scenario.begin();
        }

        public void waitState() {
                for (ThreadMonitoringScenario scenario : scenarios) {
                        System.out.println("Waiting: " + scenario);
                        scenario.waitState();
                }
        }

        public void check(ThreadMXBean threadMXBean) {
                for (ThreadMonitoringScenario scenario : scenarios) {
                        System.out.println("Checking: " + scenario);
                        scenario.check(threadMXBean);
                }
        }

        public void finish() {
                /* This is still called when ScenarioFactory.createScenarios() throws exception.. */
                if (scenarios == null)
                        return;
                for (ThreadMonitoringScenario scenario : scenarios)
                        scenario.finish();
        }

        public void end() {
                /* This is still called when ScenarioFactory.createScenarios() throws exception.. */
                if (scenarios == null)
                        return;
                for (ThreadMonitoringScenario scenario : scenarios)
                        scenario.end();
        }
}
