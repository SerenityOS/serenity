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

/**
 * Scenario that starts two threads that use locks * to synchronize.
 * Based on tests/java/lang/management/ThreadMXBean/LockingThread.java.
 */
public class LockingThreads implements ThreadMonitoringScenario, LogAware {
        private static final String[] expectedMethodsThread1 = {
                "nsk.monitoring.share.thread.LockingThreads$Thread1.runInside",
                "nsk.monitoring.share.thread.LockingThreads$Thread1.A",
                "nsk.monitoring.share.thread.LockingThreads$Thread1.B",
                "nsk.monitoring.share.thread.LockingThreads$Thread1.C",
                "nsk.monitoring.share.thread.LockingThreads$Thread1.D",
                "java.lang.Object.wait",
        };
        private static final String[] expectedMethodsThread2 = {
                "nsk.monitoring.share.thread.LockingThreads$Thread2.runInside",
                "java.lang.Object.wait",
        };
        private CustomLock lock1 = new CustomLock("lock1");
        private CustomLock lock2 = new CustomLock("lock2");
        private CustomLock lock3 = new CustomLock("lock3");
        private CustomLock lock4 = new CustomLock("lock4");
        private CustomLock lock5 = new CustomLock("lock5");
        private CustomLock lock6 = new CustomLock("lock6");
        private CustomLock lock7 = new CustomLock("lock7");
        private CustomLock lock8 = new CustomLock("lock8");
        private MonitoringThread thread1;
        private MonitoringThread thread2;
        private Log log;
        private RunType recursionType;
        private int maxDepth;

        static {
                System.loadLibrary("LockingThreads");
        }

        public LockingThreads(Log log, RunType recursionType, int maxDepth) {
                setLog(log);
                this.recursionType = recursionType;
                this.maxDepth = maxDepth;
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
                private CustomLock lock4;
                private CustomLock lock5;
                private Map<String, Object[]> lockedMonitors = new HashMap<String, Object[]>();

                public Thread1(Log log, RunType recursionType, int maxDepth) {
                        super(log, recursionType, maxDepth);
                        lock4 = LockingThreads.this.lock4;
                        lock5 = LockingThreads.this.lock5;
                        lockedMonitors.put("D", new Object[] {});
                        lockedMonitors.put("C", new Object[] { lock6 });
                        lockedMonitors.put("B", new Object[] { lock5, lock4 });
                        lockedMonitors.put("A", new Object[] { lock3, lock2, lock1});
                }

                public void checkThreadInfo(ThreadInfo info) {
                        super.checkThreadInfo(info);
                        checkLockInfo(info.getLockInfo(), lock7);
                        checkMonitorInfo(info.getLockedMonitors(), lockedMonitors);
                }

                protected void runInside() {
                        A();
                }

                void A() {
                        synchronized (lock1) {
                                synchronized(lock2) {
                                        synchronized(lock3) {
                                                B();
                                        }
                                }
                        }
                }

                private native void B();

                /*
                void B() {
                        synchronized (lock4) {
                                synchronized(lock5) {
                                        C();
                                }
                        }
                }
                */

                void C() {
                        synchronized (lock6) {
                                D();
                        }
                }

                void D() {
                        synchronized (lock7) {
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

                public Thread2(Log log, RunType recursionType, int maxDepth) {
                        super(log, recursionType, maxDepth);
                }

                public void checkThreadInfo(ThreadInfo info) {
                        super.checkThreadInfo(info);
                        checkLockInfo(info.getLockInfo(), lock8);
                        ThreadUtils.printMonitorInfo(log, info.getLockedMonitors());
                        checkMonitorInfo(info.getLockedMonitors(), lockedMonitors);
                }

                protected void runInside() {
                        synchronized (lock8) {
                                try {
                                        synchronized (readyLock) {
                                                ready = true;
                                                readyLock.notifyAll();
                                        }
                                        lock8.wait();
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
                        synchronized (lock8) {
                                lock8.notifyAll();
                        }
                }

                protected boolean isStackTraceElementExpected(StackTraceElement element) {
                        return super.isStackTraceElementExpected(element) || checkStackTraceElement(element, expectedMethodsThread2);
                }
        }


        public void begin() {
                thread1 = new Thread1(log, recursionType, maxDepth);
                thread1.begin();
                thread2 = new Thread2(log, recursionType, maxDepth);
                thread2.begin();
        }

        public void waitState() {
                thread1.waitState();
                thread2.waitState();
        }

        public void check(ThreadMXBean threadMXBean) {
                thread1.check(threadMXBean);
                thread2.check(threadMXBean);
        }

        public void finish() {
                thread1.finish();
                thread2.finish();
        }

        public void end() {
                thread1.end();
                thread1.end();
        }

        public void setLog(Log log) {
                this.log = log;
        }
}
