/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import nsk.share.TestBug;
import nsk.share.log.Log;
import nsk.share.log.LogAware;

public class StandardThreadMonitoringScenarioFactory implements ThreadMonitoringScenarioFactory, LogAware {
        private Log log;
        private int maxDepth;
        private LockerThread lockerThread;
        private final int typeCount = 7;
        private int threadCount;

        public StandardThreadMonitoringScenarioFactory(Log log, int maxDepth, int threadCount) {
                setLog(log);
                this.maxDepth = maxDepth;
                this.threadCount = threadCount;
        }

        public int getScenarioCount(int basicThreadCount) {
                // This is chosen to have one scenario of each type and recursionType.
                basicThreadCount = Math.min(basicThreadCount, 3);
                return basicThreadCount * typeCount * 3;
        }

        public ThreadMonitoringScenario[] createScenarios(int count) {
                ThreadMonitoringScenario[] scenarios = new ThreadMonitoringScenario[count];
                for (int i = 0; i < count; ++i)
                        scenarios[i] = createScenario(i);
                return scenarios;
        }

        public ThreadMonitoringScenario createScenario(String scenarioType) {
                RunType recursionType = RunType.MIXED;
                if (scenarioType.equals("running"))
                        return new RunningThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("sleeping"))
                        return new SleepingThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("timedWaiting"))
                        return new TimedWaitingThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("waiting"))
                        return new WaitingThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("blocked"))
                        return new BlockedThread(log, recursionType, maxDepth, getLockerThread());
                else if (scenarioType.equals("nativeBlocked"))
                        return new NativeBlockedThread(log, recursionType, maxDepth, getLockerThread());
                else if (scenarioType.equals("new"))
                        return new NewThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("finished"))
                        return new FinishedThread(log, recursionType, maxDepth);
                else if (scenarioType.equals("lockingThreads"))
                        return new LockingThreads(log, recursionType, maxDepth);
                else if (scenarioType.equals("synchronizerLockingThreads"))
                        return new SynchronizerLockingThreads(log, recursionType, maxDepth);
                else if (scenarioType.equals("javaDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.Type.JAVA, threadCount);
                else if (scenarioType.equals("nativeDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.Type.NATIVE, threadCount);
                else if (scenarioType.equals("synchronizedMethodDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.Type.SYNCHRONIZED_METHOD, threadCount);
                else if (scenarioType.equals("synchronizerDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.Type.SYNCHRONIZER, threadCount);
                else if (scenarioType.equals("mixedDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.Type.MIXED, 16);
                /*
                else if (scenarioType.equals("mixedMonitorDeadlock"))
                        return new Deadlock(log, recursionType, maxDepth, Deadlock.DeadlockType.MIXED, threadCount);
                else if (scenarioType.equals("mixed2MonitorDeadlock"))
                        return new MonitorDeadlock(log, recursionType, maxDepth, MonitorDeadlock.DeadlockType.MIXED2, threadCount);
                        */
                throw new TestBug("Unknown scenario type: " + scenarioType);
        }

        protected ThreadMonitoringScenario createScenario(int i) {
                RunType recursionType;
                switch (i % 3) {
                case 0:
                        recursionType = RunType.JAVA;
                        break;
                case 1:
                        recursionType = RunType.NATIVE;
                        break;
                case 2:
                        recursionType = RunType.MIXED;
                        break;
                default:
                        throw new TestBug("Unknown recursionType.");
                }
                switch (i % typeCount) {
                case 0:
                        return new SleepingThread(log, recursionType, maxDepth);
                case 1:
                        return new TimedWaitingThread(log, recursionType, maxDepth);
                case 2:
                        return new RunningThread(log, recursionType, maxDepth);
                case 3:
                        return new WaitingThread(log, recursionType, maxDepth);
                case 4:
                        return new BlockedThread(log, recursionType, maxDepth, getLockerThread());
                case 5:
                        return new LockingThreads(log, recursionType, maxDepth);
                case 6:
                        return new SynchronizerLockingThreads(log, recursionType, maxDepth);
                default:
                        throw new TestBug("Unknown thread kind");
                }
        }

        private LockerThread getLockerThread() {
                if (lockerThread == null) {
                        lockerThread = new LockerThread();
                        lockerThread.start();
                        lockerThread.waitState();
                }
                return lockerThread;
        }

        public void finish() {
                if (lockerThread != null) {
                        lockerThread.finish();
                        try {
                                lockerThread.join();
                        } catch (InterruptedException e) {
                                log.warn(e);
                        }
                        lockerThread = null;
                }
        }

        public void setLog(Log log) {
                this.log = log;
        }
}
