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

import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import nsk.share.log.Log;
import nsk.share.log.LogAware;
import nsk.share.TestFailure;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;

/**
 * Base class for all threads that are used in monitoring testing.
 */
public abstract class MonitoringThread extends ThreadMonitoringScenarioBase implements Runnable {
        private final static String[] expectedMethods = {
                "java.lang.Thread.run"
        };
        protected boolean expectedJava = true;
        protected boolean expectedSuspended = false;
        protected Thread runner;

        public MonitoringThread(Log log) {
                super(log);
        }

        /**
         * Check that info for this thread is correct. This method is expected
         * to be called between calls to waitState() and finish().
         *
         * @param info thread info
         * @throws TestFailure when verification fails
         */
        public void checkThreadInfo(ThreadInfo info) {
                verify(info != null, "ThreadInfo is null");
                verify(info.getThreadId() == runner.getId(), "info.getId() = " + info.getThreadId() + " != this.getId() " + runner.getId());
                verify(info.getThreadName().equals(runner.getName()), "info.getName() = " + info.getThreadName() + " != this.getName() " + runner.getName());
                verify(info.getThreadState().equals(runner.getState()), "info.getState() = " + info.getThreadState() + " != this.getState() " + runner.getState());
                verify(checkStackTrace(info.getStackTrace()), "Verification of stack trace failed");
                verify(info.isSuspended() == expectedSuspended, "ThreadInfo.isSuspended() = " + info.isSuspended() + " != " + expectedSuspended);
                verify(info.isInNative() == !expectedJava, "ThreadInfo.isInNative() = " + info.isInNative() + " != " + !expectedJava);
        }

        public void begin() {
                runner = new Thread(this);
                runner.setName(this.getClass().getName() + '@' + System.identityHashCode(this) + "[id=" + runner.getId() + "]");
                runner.setDaemon(true);
                runner.start();
        }

        public abstract void waitState();

        private void doCheck(ThreadInfo info, String description) {
                try {
                        checkThreadInfo(info);
                } catch (TestFailure t) {
                        log.info("Failed to check: " + description + " thread: " + this);
                        printThreadInfo(info);
                        throw t;
                }
        }

        private void doCheck(ThreadInfo[] info, String description) {
                verify(info.length == 1, "Failed to check: " + description + " thread: " + this + " length of returned array is wrong: " + info.length + " expected: " + 1);
                doCheck(info[0], description);
        }

        public void check(ThreadMXBean threadMXBean) {
                lockedMonitorsAvailable = false;
                lockedSynchronizersAvailable = false;
                doCheck(threadMXBean.getThreadInfo(runner.getId(), Integer.MAX_VALUE), "ThreadMXBean.getThreadInfo(long, int)");
                doCheck(threadMXBean.getThreadInfo(runner.getId()), "ThreadMXBean.getThreadInfo(long)");
                doCheck(threadMXBean.getThreadInfo(new long[] { runner.getId() }), "ThreadMXBean.getThreadInfo(long[])");
                doCheck(threadMXBean.getThreadInfo(new long[] { runner.getId() }, Integer.MAX_VALUE), "ThreadMXBean.getThreadInfo(long[], int)");
                lockedMonitorsAvailable = true;
                lockedSynchronizersAvailable = true;
                doCheck(threadMXBean.getThreadInfo(new long[] { runner.getId() }, true, true), "ThreadMXBean.getThreadInfo(long[], boolean, boolean)");
        }

        public abstract void finish();

        public void end() {
                try {
                        if (runner != null)
                                runner.join();
                } catch (InterruptedException e) {
                        log.warn(e);
                }
        }

        /**
         * Run this thread.
         */
        public abstract void run();

        protected void waitThreadState(Thread.State state) {
                ThreadUtils.waitThreadState(runner, state);
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethods);
        }

        public final void setExpectedJava(boolean expectedJava) {
                this.expectedJava = expectedJava;
        }

        public final long getId() {
                return runner.getId();
        }

        public static void yield() {
                Thread.yield();
        }
}
