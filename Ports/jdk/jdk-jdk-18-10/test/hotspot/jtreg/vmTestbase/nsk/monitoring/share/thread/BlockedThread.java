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
package nsk.monitoring.share.thread;

import nsk.share.log.Log;
import java.lang.management.ThreadInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import nsk.share.TestBug;
import nsk.monitoring.share.Monitoring;

/**
 * BlockedThread is RecursiveMonitoringThread that blocks on entering
 * synchronized section.
 */
public class BlockedThread extends RecursiveMonitoringThread {
        private LockerThread lockerThread;
        protected Object lock;
        protected Object readyLock = new Object();
        protected volatile boolean ready = false;
        private static final String[] expectedMethods = {
                "nsk.monitoring.share.thread.BlockedThread.runInside"
        };

        public BlockedThread(Log log, RunType recursionType, int maxDepth, LockerThread lockerThread) {
                super(log, recursionType, maxDepth);
                this.lockerThread = lockerThread;
                this.lock = lockerThread.getLock();
        }

        public void checkThreadInfo(ThreadInfo info) {
                super.checkThreadInfo(info);
                verify(info.getThreadState() == Thread.State.BLOCKED, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.BLOCKED);
                verify(info.getBlockedTime() == 0 || info.getBlockedTime() == -1, "ThreadInfo.getBlockedTime() == " + info.getBlockedTime());
                verify(info.getBlockedCount() >= 1, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " != " + 1);
                verify(info.getWaitedTime() == 0 || info.getWaitedTime() == -1, "ThreadInfo.getWaitedTime() == " + info.getWaitedTime());
                verify(info.getWaitedCount() == 0, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " != " + 0);
                checkLockInfo(info.getLockInfo(), lock);
                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != lock.toString() = " + lock.toString());
                verify(info.getLockOwnerId() == lockerThread.getId(), "ThreadInfo.getLockOwnerId() = " + info.getLockOwnerId() + " != this.getId() = " + this.getId());
                verify(info.getLockOwnerName().equals(lockerThread.getName()), "ThreadInfo.getLockOwnerName() = " + info.getLockOwnerName() + " != lockerThread.getName() = " + lockerThread.getName());
                checkMonitorInfo(info.getLockedMonitors(), null);
                checkSynchronizers(info.getLockedSynchronizers(), null);
        }

        private void checkStackTrace(MonitorInfo[] minfo, StackTraceElement[] elements) {
                verify(minfo.length == 1, "ThreadInfo.getLockedMonitors() is of length " + minfo.length);
                MonitorInfo minfo1 = minfo[0];
                verify(minfo1.getLockedStackDepth() == elements.length, "MonitorInfo.getLockedStackDepth() = " + minfo1.getLockedStackDepth() + " != elements.length = " + elements.length);
                verify(minfo1.getLockedStackFrame().equals(elements[elements.length - 1]), "MonitorInfo.getLockedStackFrame() = " + minfo1.getLockedStackFrame() + " is different from last element " + elements[elements.length - 1]);
                checkLockInfo(minfo1, lock);
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
                waitThreadState(Thread.State.BLOCKED);
        }

        public void finish() {
                ready = false;
                synchronized (lock) {
                        lock.notify();
                }
        }

        protected void runInside() {
                synchronized (readyLock) {
                        ready = true;
                        readyLock.notifyAll();
                }
                do {
                        synchronized (lock) {
                                System.out.println("BlockedThread acquired lock");

                                // Actually, this thread will reach here after LockerThread.finish() is called,
                                // but before finish() is called. But it does not matter, because check(ThreadInfo)
                                // was already called.
                        }

                        System.out.println("BlockedThread relinquished lock");

                } while (ready);
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethods);
        }
}
