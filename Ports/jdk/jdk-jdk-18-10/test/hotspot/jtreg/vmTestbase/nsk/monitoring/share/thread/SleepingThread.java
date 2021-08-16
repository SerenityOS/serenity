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
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;

/**
 * SleepingThread is RecursiveMonitoringThread that sleeps.
 */
public class SleepingThread extends RecursiveMonitoringThread {
        private String lock = new String("a lock");
        private volatile boolean sleeping = false;
        private Object readyLock = new Object();
        private static final String[] expectedMethods = {
                "java.lang.Thread.sleep",
                "nsk.monitoring.share.thread.SleepingThread.runInside"
        };

        public SleepingThread(Log log, RunType recursionType, int maxDepth) {
                super(log, recursionType, maxDepth);
        }

        public void checkThreadInfo(ThreadInfo info) {
                super.checkThreadInfo(info);
                verify(info.getThreadState() == Thread.State.TIMED_WAITING, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.TIMED_WAITING);
                verify(info.getBlockedTime() == 0 || info.getBlockedTime() == -1, "ThreadInfo.getBlockedTime() == " + info.getBlockedTime());
                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " != 0");
                verify(info.getWaitedTime() == 0 || info.getWaitedTime() == -1, "ThreadInfo.getWaitedTime() == " + info.getWaitedTime());
                verify(info.getWaitedCount() == 1, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " != 1");
                verify(info.getLockInfo() == null, "ThreadInfo.getLockInfo() != null ");
                verify(info.getLockName() == null, "ThreadInfo.getLockName() " + info.getLockName() + " != null");
                verify(info.getLockOwnerId() == -1, "ThreadInfo.getLockOwnerId() = " + info.getLockOwnerId() + " != null");
                verify(info.getLockOwnerName() == null, "ThreadInfo.getLockOwnerName() = " + info.getLockOwnerName() + " != null");
                checkMonitorInfo(info.getLockedMonitors(), null);
                checkSynchronizers(info.getLockedSynchronizers(), null);
        }

        public void waitState() {
                synchronized (readyLock) {
                        while (!sleeping) {
                                try {
                                        readyLock.wait();
                                } catch (InterruptedException e) {
                                        log.warn(e);
                                }
                        }
                }
                waitThreadState(Thread.State.TIMED_WAITING);
        }

        public void finish() {
                sleeping = false;
                runner.interrupt();
        }

        protected void runInside() {
                synchronized (readyLock) {
                        sleeping = true;
                        readyLock.notifyAll();
                }
                while (sleeping) {
                        try {
                                Thread.sleep(10000000);
                        } catch (InterruptedException e) {
                        }
                }
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethods);
        }
}
