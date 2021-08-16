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
 * WaitingThread is RecursiveMonitoringThread that is waiting on a lock.
 */
public class WaitingThread extends RecursiveMonitoringThread {
        private String lock = new String("a lock");
        private volatile boolean ready = false;
        private Object readyLock = new Object();
        private static final String[] expectedMethods = {
                "nsk.monitoring.share.thread.WaitingThread.runInside",
                "java.lang.Object.wait"
        };

        public WaitingThread(Log log, RunType recursionType, int maxDepth) {
                super(log, recursionType, maxDepth);
        }

        public void checkThreadInfo(ThreadInfo info) {
                super.checkThreadInfo(info);
                verify(info.getThreadState() == Thread.State.WAITING, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.WAITING);
                verify(info.getBlockedTime() == 0 || info.getBlockedTime() == -1, "ThreadInfo.getBlockedTime() == " + info.getBlockedTime());
                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " != 1");
                verify(info.getWaitedTime() > 0 || info.getWaitedTime() == -1, "ThreadInfo.getWaitedTime() == " + info.getWaitedTime());
                verify(info.getWaitedCount() == 1, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " != 1");
                checkLockInfo(info.getLockInfo());
                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != ThreadInfo.getLockInfo().toString() = " + info.getLockInfo().toString());
                verify(info.getLockOwnerId() == -1, "ThreadInfo.getLockOwnerId() = " + info.getLockOwnerId() + " != -1");
                verify(info.getLockOwnerName() == null , "ThreadInfo.getLockOwnerName() = " + info.getLockOwnerName() + " != null");
                checkMonitorInfo(info.getLockedMonitors(), null);
                checkSynchronizers(info.getLockedSynchronizers(), null);
        }

        private void checkLockInfo(LockInfo lockInfo) {
                verify(lockInfo.getClassName().equals(lock.getClass().getName()), "LockInfo.getClassName() = " + lockInfo.getClassName() + " differs from lock.getClass().getName() = " + lock.getClass().getName());
                verify(lockInfo.getIdentityHashCode() == System.identityHashCode(lock), "LockInfo.getIdentityHashCode() = " + lockInfo.getIdentityHashCode() + " differs from System.identityHashCode(lock) = " + System.identityHashCode(lock));
                String expectedToString = lock.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(lock));
                verify(lockInfo.toString().equals(expectedToString), "LockInfo.toString() = " + lockInfo.toString() + " differs from expected toString() = " + expectedToString);
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
                waitThreadState(Thread.State.WAITING);
        }

        public void finish() {
                ready = false;
                synchronized (lock) {
                        lock.notifyAll();
                }
        }

        protected void runInside() {
                synchronized (readyLock) {
                        ready = true;
                        readyLock.notifyAll();
                }
                while (ready) {
                        synchronized (lock) {
                                try {
                                        lock.wait();
                                } catch (InterruptedException e) {
                                        log.warn(e);
                                }
                        }
                }
        }

        protected boolean isStackTraceElementExpected(StackTraceElement element) {
                return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethods);
        }
}
