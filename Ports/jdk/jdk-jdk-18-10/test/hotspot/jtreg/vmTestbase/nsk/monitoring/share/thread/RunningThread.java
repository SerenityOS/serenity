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

import nsk.share.log.Log;
import java.lang.management.ThreadInfo;
import nsk.share.TestFailure;

/**
 * RunningThread is a RecursiveMonitoringThread that simply
 * runs a loop.
 */
public class RunningThread extends RecursiveMonitoringThread {
        private volatile boolean running = false;
        private volatile boolean ready = false;
        private Object readyLock = new Object();
        private static final String[] expectedMethods = {
                "nsk.monitoring.share.thread.RunningThread.runInside",
                "java.lang.Thread.yield"
        };

        public RunningThread(Log log, RunType recursionType, int maxDepth) {
                super(log, recursionType, maxDepth);
        }

        public void waitState() {
                synchronized (readyLock) {
                        while (!ready) {
                                try {
                                        readyLock.wait();
                                } catch (InterruptedException e) {
                                        log.warn(e);
                                }
                        }
                }
                waitThreadState(Thread.State.RUNNABLE);
        }

        public void finish() {
                running = false;
        }

        protected void runInside() {
                synchronized (readyLock) {
                        ready = true;
                        running = true;
                        readyLock.notifyAll();
                }
                while (running)
                        ;
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethods);
        }

        public void checkThreadInfo(ThreadInfo info) {
                super.checkThreadInfo(info);
                verify(info.getThreadState() == Thread.State.RUNNABLE, "ThreadInfo.getState() = " + info.getThreadState() + "!= Thread.State.RUNNABLE");
                verify(info.getBlockedTime() == 0 || info.getBlockedTime() == -1, "ThreadInfo.getBlockedTime() == " + info.getBlockedTime());
                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " < 0");
                verify(info.getWaitedTime() == 0 || info.getWaitedTime() == -1, "ThreadInfo.getWaitedTime() == " + info.getWaitedTime());
                verify(info.getWaitedCount() == 0, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " != 0");
                verify(info.getLockInfo() == null, "ThreadInfo.getLockInfo() != null");
                verify(info.getLockName() == null, "ThreadInfo.getLockName() != null");
                verify(info.getLockOwnerId() == -1, "ThreadInfo.getLockOwnerId() != -1");
                verify(info.getLockOwnerName() == null, "ThreadInfo.getLockOwnerName() != null");
                checkMonitorInfo(info.getLockedMonitors(), null);
                checkSynchronizers(info.getLockedSynchronizers(), null);
        }
}
