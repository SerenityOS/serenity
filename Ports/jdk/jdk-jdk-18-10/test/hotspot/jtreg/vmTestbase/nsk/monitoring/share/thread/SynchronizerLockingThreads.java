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
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Scenario that starts two threads that use locks * to synchronize.
 * The code is based on tests/java/lang/management/ThreadMXBean/LockingThread.java
 */
public class SynchronizerLockingThreads implements ThreadMonitoringScenario, LogAware {
        private static final String[] expectedMethodsThread1 = {
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread1.runInside",
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread1.A",
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread1.B",
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread1.C",
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread1.D",
                "java.lang.Object.wait"
        };
        private static final String[] expectedMethodsThread2 = {
                "nsk.monitoring.share.thread.SynchronizerLockingThreads$Thread2.runInside"
        };
        private ReentrantLock lock1 = new ReentrantLock();
        private ReentrantLock lock2 = new ReentrantLock();
        private ReentrantLock lock3 = new ReentrantLock();
        private ReentrantLock lock4 = new ReentrantLock();
        private CustomLock lock5 = new CustomLock("lock5");
        private CustomLock lock6 = new CustomLock("lock6");
        private CustomLock lock7 = new CustomLock("lock7");
        private ReentrantLock lock8 = new ReentrantLock();
        private MonitoringThread thread1;
        private MonitoringThread thread2;
        private Log log;
        private RunType recursionType;
        private int maxDepth;

        public SynchronizerLockingThreads(Log log, RunType recursionType, int maxDepth) {
                setLog(log);
                this.recursionType = recursionType;
                this.maxDepth = maxDepth;
                thread1 = new Thread1(log, recursionType, maxDepth);
                thread2 = new Thread2(log, recursionType, maxDepth);
        }

        static class CustomLock {
                private String name;

                public CustomLock(String name) {
                        this.name = name;
                }

                public String toString() {
                        return name;
                }
        }

        private class Thread1 extends RecursiveMonitoringThread {
                private volatile boolean ready = false;
                private Object readyLock = new Object();
                private Map<String, Object[]> lockedMonitors = new HashMap<String, Object[]>();
                private Map<String, Lock[]> lockedSynchronizers = new HashMap<String, Lock[]>();

                public Thread1(Log log, RunType recursionType, int maxDepth) {
                        super(log, recursionType, maxDepth);
                        lockedMonitors.put("D", new Object[] {});
                        lockedMonitors.put("C", new Object[] { lock6 });
                        lockedMonitors.put("B", new Object[] { lock5 });
                        lockedMonitors.put("A", new Object[] { });
                        lockedSynchronizers.put("D", new ReentrantLock[0]); // no sync locked
                        lockedSynchronizers.put("C", new ReentrantLock[0]); // no sync locked
                        lockedSynchronizers.put("B", new Lock[] {lock4});
                        lockedSynchronizers.put("A", new Lock[] {lock3, lock2, lock1});
                }

                public void checkThreadInfo(ThreadInfo info) {
                        super.checkThreadInfo(info);
                        checkLockInfo(info.getLockInfo(), lock7);
                        checkMonitorInfo(info.getLockedMonitors(), lockedMonitors);
                        checkSynchronizers(info.getLockedSynchronizers(), lockedSynchronizers);
                }

                protected void runInside() {
                        A();
                }

                void A() {
                        lock1.lock();
                        try {
                                lock2.lock();
                                try {
                                        lock3.lock();
                                        try {
                                                B();
                                        } finally {
                                                lock3.unlock();
                                        }
                                } finally {
                                        lock2.unlock();
                                }
                        } finally {
                                lock1.unlock();
                        }
                }

                void B() {
                        lock4.lock();
                        try {
                                synchronized(lock5) {
                                        C();
                                }
                        } finally {
                                lock4.unlock();
                        }
                }

                void C() {
                        synchronized(lock6) {
                                D();
                        }
                }

                void D() {
                        synchronized(lock7) {
                                try {
                                        synchronized (readyLock) {
                                                ready = true;
                                                readyLock.notifyAll();
                                        }
                                        lock7.wait();
                                } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                }
                        }
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
                        synchronized (lock7) {
                                lock7.notifyAll();
                        }
                }

                protected boolean isStackTraceElementExpected(StackTraceElement element) {
                        return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethodsThread1);
                }
        }

        private class Thread2 extends RecursiveMonitoringThread {
                private boolean ready = false;
                private Object readyLock = new Object();
                private Map<String, Object[]> lockedMonitors = new HashMap<String, Object[]>();
                private Map<String, Lock[]> lockedSynchronizers = new HashMap<String, Lock[]>();
                private Condition c = lock8.newCondition();

                public Thread2(Log log, RunType recursionType, int maxDepth) {
                        super(log, recursionType, maxDepth);
                }

                public void checkThreadInfo(ThreadInfo info) {
                        super.checkThreadInfo(info);
                        checkLockInfo(info.getLockInfo(), c);
                        checkMonitorInfo(info.getLockedMonitors(), lockedMonitors);
                        checkSynchronizers(info.getLockedSynchronizers(), lockedSynchronizers);
                }

                protected void runInside() {
                        lock8.lock();
                        try {
                                synchronized (readyLock) {
                                        ready = true;
                                        readyLock.notifyAll();
                                }
                                c.await();
                        } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                        } finally {
                                lock8.unlock();
                        }
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
                        lock8.lock();
                        try {
                                c.signalAll();
                        } finally {
                                lock8.unlock();
                        }
                }

                protected boolean isStackTraceElementExpected(StackTraceElement element) {
                        return super.isStackTraceElementExpected(element) ||
                                checkStackTraceElement(element, expectedMethodsThread2) ||
                                element.getClassName().startsWith("java.util.concurrent.") ||
                                element.getClassName().startsWith("jdk.internal.misc.");
                }
        }


        public void begin() {
                thread1.begin();
                thread2.begin();
        }

        public void waitState() {
                thread1.waitState();
                thread2.waitState();
        }

        public void check(ThreadMXBean threadMXBean) {
                long[] ids = new long[] { thread1.getId(), thread2.getId() };
                ThreadInfo[] info = threadMXBean.getThreadInfo(ids, true, true);
                thread1.checkThreadInfo(info[0]);
                thread2.checkThreadInfo(info[1]);
        }

        public void finish() {
                thread1.finish();
                thread2.finish();
        }

        public void end() {
                thread1.end();
                thread2.end();
        }

        public void setLog(Log log) {
                this.log = log;
        }
}
