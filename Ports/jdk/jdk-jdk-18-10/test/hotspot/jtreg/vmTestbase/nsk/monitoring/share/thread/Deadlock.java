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
import nsk.share.TestBug;
import nsk.share.TestFailure;
import nsk.share.Wicket;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

/**
 * Scenario that starts threads that use different scenarios to deadlock.
 * Several types of deadlocks are implemented:
 * Deadlock.Type.JAVA - several threads use synchronized blocks on java objects
 * Deadlock.Type.NATIVE - several threads use JNI MonitorEnter on java objects
 * Deadlock.Type.SYNCHRONIZED_METHOD - several threads use synchronized method
 * Deadlock.Type.SYNCHRONIZER - several threads use java.util.concurrent.locks locks
 * Deadlock.Type.MIXED - a mix of all above, each thread uses different combination
 * of lock types
 *
 * Note: this scenario is not reusable in sense that it cannot be run and successfully
 * checked several times, because there is no way to finish deadlocked threads.
 */
public class Deadlock extends ThreadMonitoringScenarioBase {
        public static enum Type {
                JAVA,
                NATIVE,
                SYNCHRONIZED_METHOD,
                SYNCHRONIZER,
                MIXED
        };

        private static final String[] expectedMethods = {
                "nsk.monitoring.share.thread.Deadlock$DeadlockThread.runInside",
                "nsk.monitoring.share.thread.Deadlock$DeadlockThread.javaLock",
                "nsk.monitoring.share.thread.Deadlock$DeadlockThread.nativeLock",
                "nsk.monitoring.share.thread.Deadlock$DeadlockThread.mixedLock",
                "nsk.monitoring.share.thread.Deadlock$DeadlockThread.nativeLock2"
        };
        private Type deadlockType;
        private RunType recursionType;
        private int maxDepth;
        private Wicket step1;
        private Wicket step2;
        private Wicket step3;
        private Object[] locks;
        private Locker[] lockers;
        private DeadlockThread[] threads;
        private long[] threadIds;
        private ThreadInfo[] threadInfo;
        private Deadlocker deadlocker;

        static {
                System.loadLibrary("Deadlock");
        }

        public Deadlock(Log log, RunType recursionType, int maxDepth, Type deadlockType) {
                this(log, recursionType, maxDepth, deadlockType, 3);
        }

        public Deadlock(Log log, RunType recursionType, int maxDepth, Type deadlockType, int threadCount) {
                super(log);
                this.recursionType = recursionType;
                this.maxDepth = maxDepth;
                threads = new DeadlockThread[threadCount];
                lockers = new Locker[threadCount];
                locks = new Object[threadCount];
                this.deadlockType = deadlockType;
        }

        public abstract class Locker {
                protected Locker inner;

                public Locker(Locker inner) {
                        this.inner = inner;
                }

                public abstract String getTypeName();
                public abstract void lock();
                public abstract void check(ThreadInfo info);
                public abstract Thread.State getExpectedThreadState();
        }

        private class JavaLocker extends Locker {
                private Object lock;
                private Map<String, Object[]> lockMap = new HashMap<String, Object[]>();

                public JavaLocker(Object lock, Locker inner) {
                        super(inner);
                        this.lock = lock;
                        lockMap.put("lock", new Object[] { lock });
                }

                public String getTypeName() {
                        return "synchronized block";
                }

                public void lock() {
                        synchronized (lock) {
                                if (inner != null) {
                                        step1.unlock();
                                        step2.waitFor();
                                        step3.unlock();
                                        inner.lock();
                                } else
                                        throw new TestBug("Should not reach here");
                        }
                }

                public void check(ThreadInfo info) {
                        if (inner == null) {
                                verify(info.getThreadState() == Thread.State.BLOCKED, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.BLOCKED);
                                checkLockInfo(info.getLockInfo(), lock);
                                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != info.getLockInfo().toString() = " + info.getLockInfo().toString());
                        } else {
                                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " < " + 0);
                                verify(info.getWaitedCount() >= 0, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " < " + 0);
                                checkMonitorInfo(info.getLockedMonitors(), lockMap);
                                checkSynchronizers(info.getLockedSynchronizers(), null);
                                inner.check(info);
                        }
                }

                public Thread.State getExpectedThreadState() {
                        if (inner != null)
                                return inner.getExpectedThreadState();
                        else
                                return Thread.State.BLOCKED;
                }
        }

        private class NativeLocker extends Locker {
                private Object lock;
                private Wicket step1;
                private Wicket step2;
                private Wicket step3;
                private Map<String, Object[]> lockMap = new HashMap<String, Object[]>();

                public NativeLocker(Object lock, Locker inner) {
                        super(inner);
                        this.lock = lock;
                        this.step1 = Deadlock.this.step1;
                        this.step2 = Deadlock.this.step2;
                        this.step3 = Deadlock.this.step3;
                        lockMap.put("lock", new Object[] { lock });
                }

                public String getTypeName() {
                        return "JNI MonitorEnter";
                }

                public native void lock();

                public void check(ThreadInfo info) {
                        if (inner != null) {
                                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != info.getLockInfo().toString() = " + info.getLockInfo().toString());
                                checkMonitorInfo(info.getLockedMonitors(), lockMap);
                                checkSynchronizers(info.getLockedSynchronizers(), null);
                                inner.check(info);
                        } else {
                                verify(info.getThreadState() == Thread.State.BLOCKED, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.BLOCKED);
                                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " < " + 0);
                                verify(info.getWaitedCount() >= 0, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " < " + 0);
                                checkLockInfo(info.getLockInfo(), lock);
                        }
                }

                public Thread.State getExpectedThreadState() {
                        if (inner != null)
                                return inner.getExpectedThreadState();
                        else
                                return Thread.State.BLOCKED;
                }
        }

        private class SynchronizedMethod {
                public synchronized void synchronizedMethod(Locker inner) {
                        if (inner != null) {
                                step1.unlock();
                                step2.waitFor();
                                step3.unlock();
                                inner.lock();
                        } else
                                throw new TestBug("Should not reach here");
                }
        }

        private class SynchronizedMethodLocker extends Locker {
                private SynchronizedMethod lock;
                private Map<String, Object[]> lockMap = new HashMap<String, Object[]>();

                public SynchronizedMethodLocker(SynchronizedMethod lock, Locker inner) {
                        super(inner);
                        this.lock = lock;
                        lockMap.put("synchronizedMethod", new Object[] { lock });
                }

                public String getTypeName() {
                        return "SynchronizedMethod";
                }

                public void lock() {
                        lock.synchronizedMethod(inner);
                }

                public void check(ThreadInfo info) {
                        if (inner != null) {
                                checkMonitorInfo(info.getLockedMonitors(), lockMap);
                                checkSynchronizers(info.getLockedSynchronizers(), null);
                                inner.check(info);
                        } else {
                                verify(info.getThreadState() == Thread.State.BLOCKED, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.BLOCKED);
                                verify(info.getBlockedCount() >= 0, "ThreadInfo.getBlockedCount() = " + info.getBlockedCount() + " < " + 0);
                                verify(info.getWaitedCount() >= 0, "ThreadInfo.getWaitedCount() = " + info.getWaitedCount() + " < " + 0);
                                checkLockInfo(info.getLockInfo(), lock);
                                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != info.getLockInfo().toString() = " + info.getLockInfo().toString());
                        }
                }

                public Thread.State getExpectedThreadState() {
                        if (inner != null)
                                return inner.getExpectedThreadState();
                        else
                                return Thread.State.BLOCKED;
                }
        }

        private class SynchronizerLocker extends Locker {
                private Lock lock;
                private Map<String, Lock[]> lockMap = new HashMap<String, Lock[]>();

                public SynchronizerLocker(Lock lock, Locker inner) {
                        super(inner);
                        this.lock = lock;
                        lockMap.put("lock", new Lock[] { lock });
                }

                public String getTypeName() {
                        return "java.util.concurrent.locks synchronizer";
                }

                public void lock() {
                        try {
                                lock.tryLock(10000000, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                                log.warn(e);
                        }
                        try {
                                if (inner != null) {
                                        step1.unlock();
                                        step2.waitFor();
                                        step3.unlock();
                                        inner.lock();
                                } else
                                        throw new TestBug("Should not reach here");
                        } finally {
                                lock.unlock();
                        }
                }

                public void check(ThreadInfo info) {
                        if (inner != null) {
                                checkMonitorInfo(info.getLockedMonitors(), null);
                                checkSynchronizers(info.getLockedSynchronizers(), lockMap);
                                inner.check(info);
                        } else {
                                verify(info.getThreadState() == Thread.State.TIMED_WAITING, "ThreadInfo.getThreadState() = " + info.getThreadState() + " != " + Thread.State.TIMED_WAITING);
                                //checkLockInfo(info.getLockInfo(), lock2); // Do not check this because actual lock is instance of inner class of ReentrantLock
                                verify(info.getLockName().equals(info.getLockInfo().toString()), "ThreadInfo.getLockName() = " + info.getLockName() + " != info.getLockInfo().toString() = " + info.getLockInfo().toString());
                        }
                }

                public Thread.State getExpectedThreadState() {
                        if (inner != null)
                                return inner.getExpectedThreadState();
                        else
                                return Thread.State.TIMED_WAITING;
                }
        }


        private class DeadlockThread extends RecursiveMonitoringThread {
                private boolean ready = false;
                private Object readyLock = new Object();
                private Locker locker;

                public DeadlockThread(Locker locker) {
                        super(Deadlock.this.log, Deadlock.this.recursionType, Deadlock.this.maxDepth);
                        this.locker = locker;
                }

                public void runInside() {
                        synchronized (readyLock) {
                                ready = true;
                                readyLock.notifyAll();
                        }
                        locker.lock();
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
                        waitThreadState(locker.getExpectedThreadState());
                }

                public void checkThreadInfo(ThreadInfo info) {
                        super.checkThreadInfo(info);
                        locker.check(info);
                }

                public void finish() {
                        throw new UnsupportedOperationException("Can't finish deadlocked thread");
                }

                public void end() {
                        throw new UnsupportedOperationException("Can't end deadlocked thread");
                }

                protected boolean isStackTraceElementExpected(StackTraceElement element) {
                        return super.isStackTraceElementExpected(element) ||
                               element.getClassName().startsWith("nsk.monitoring.share.thread.Deadlock") ||
                               element.getClassName().startsWith("java.util.concurrent.locks.") ||
                               element.getClassName().startsWith("jdk.internal.misc.");
                }
        }

        private interface Deadlocker {
                public void createLockers();
                public void check(ThreadMXBean threadMXBean);
        }

        private class JavaDeadlocker implements Deadlocker {
                public void createLockers() {
                        for (int i = 0; i < locks.length; ++i)
                                locks[i] = new String(this + "lock " + i);
                        for (int i = 0; i < locks.length; ++i)
                                lockers[i] = new JavaLocker(locks[i], new JavaLocker(locks[(i + 1) % locks.length], null));
                }

                public void check(ThreadMXBean threadMXBean) {
                        checkDeadlocks(threadMXBean, threadMXBean.findMonitorDeadlockedThreads());
                        checkDeadlocks(threadMXBean, threadMXBean.findDeadlockedThreads());
                        for (DeadlockThread thread : threads)
                                thread.check(threadMXBean);
                }
        }

        private class SynchronizedMethodDeadlocker implements Deadlocker {
                public void createLockers() {
                        for (int i = 0; i < locks.length; ++i)
                                locks[i] = new SynchronizedMethod();
                        for (int i = 0; i < locks.length; ++i)
                                lockers[i] = new SynchronizedMethodLocker((SynchronizedMethod) locks[i], new SynchronizedMethodLocker((SynchronizedMethod) locks[(i + 1) % locks.length], null));
                }

                public void check(ThreadMXBean threadMXBean) {
                        checkDeadlocks(threadMXBean, threadMXBean.findMonitorDeadlockedThreads());
                        checkDeadlocks(threadMXBean, threadMXBean.findDeadlockedThreads());
                        for (DeadlockThread thread : threads)
                                thread.check(threadMXBean);
                }
        }


        private class NativeDeadlocker implements Deadlocker {
                public void createLockers() {
                        for (int i = 0; i < locks.length; ++i)
                                locks[i] = new String(this + "lock " + i);
                        for (int i = 0; i < locks.length; ++i)
                                lockers[i] = new NativeLocker(locks[i], new NativeLocker(locks[(i + 1) % locks.length], null));
                }

                public void check(ThreadMXBean threadMXBean) {
                        checkDeadlocks(threadMXBean, threadMXBean.findMonitorDeadlockedThreads());
                        checkDeadlocks(threadMXBean, threadMXBean.findDeadlockedThreads());
                        for (DeadlockThread thread : threads)
                                thread.check(threadMXBean);
                }
        }

        private class SynchronizerDeadlocker implements Deadlocker {
                public void createLockers() {
                        for (int i = 0; i < locks.length; ++i)
                                locks[i] = new ReentrantLock();
                        for (int i = 0; i < locks.length; ++i)
                                lockers[i] = new SynchronizerLocker((Lock) locks[i], new SynchronizerLocker((Lock) locks[(i + 1) % locks.length], null));
                }

                public void check(ThreadMXBean threadMXBean) {
                        checkDeadlocks(threadMXBean, threadMXBean.findDeadlockedThreads());
                        for (DeadlockThread thread : threads)
                                thread.check(threadMXBean);
                }
        }

        private class MixedDeadlocker implements Deadlocker {
                private int getCount() {
                        return 4;
                }

                private Object createLock(int type, int i) {
                        switch (type) {
                        case 0:
                                return new String("lock " + i);
                        case 1:
                                return new String("lock " + i);
                        case 2:
                                return new SynchronizedMethod();
                        case 3:
                                return new ReentrantLock();
                        default:
                                throw new TestBug("Should not reach here");
                        }
                }

                private Locker createLocker(int type, Object lock, Locker inner) {
                        switch (type) {
                        case 0:
                                return new JavaLocker(lock, inner);
                        case 1:
                                return new NativeLocker(lock, inner);
                        case 2:
                                return new SynchronizedMethodLocker((SynchronizedMethod) lock, inner);
                        case 3:
                                return new SynchronizerLocker((Lock) lock, inner);
                        default:
                                throw new TestBug("Should not reach here");
                        }
                }

                public void createLockers() {
                        int n = getCount();
                        int threadCount = lockers.length;
                        if (threadCount != n * n)
                                throw new TestBug("Thread count is expected to be " + n * n + ", actual: " + threadCount);
                        int[] types = new int[n];
                        for (int i = 0; i < n; ++i)
                                types[i] = i;
                        int type = 0;
                        locks[0] = createLock(0, 0);
                        log.info("Creating lockers");
                        /*
                         * This will ensure that we will have each combination
                         * of lock type and inner lock type in some thread. Together
                         * all these threads deadlock.
                         */
                        for (int i = 0; i < threadCount; ++i) {
                                int newtype = types[type];
                                if (i < threadCount - 1)
                                        locks[i + 1] = createLock(newtype, i);
                                Locker inner = createLocker(newtype, locks[(i + 1) % threadCount], null);
                                lockers[i] = createLocker(type, locks[i], inner);
                                log.info("Locker " + i + " will lock " + locks[i] + " (" + lockers[i].getTypeName() + ") and will wait for " + locks[(i + 1) % threadCount] + " (" + inner.getTypeName() + ")");
                                types[type] = (types[type] + 1) % n;
                                type = newtype;
                        }
                }

                public void check(ThreadMXBean threadMXBean) {
                        checkDeadlocks(threadMXBean, threadMXBean.findDeadlockedThreads());
                        for (DeadlockThread thread : threads)
                                thread.check(threadMXBean);
                }
        }


        protected Deadlocker createDeadlocker() {
                switch (deadlockType) {
                case JAVA:
                        return new JavaDeadlocker();
                case NATIVE:
                        return new NativeDeadlocker();
                case SYNCHRONIZED_METHOD:
                        return new SynchronizedMethodDeadlocker();
                case SYNCHRONIZER:
                        return new SynchronizerDeadlocker();
                case MIXED:
                        return new MixedDeadlocker();
                default:
                        throw new TestBug("Unknown deadlockType: " + deadlockType);
                }
        }

        public void begin() {
                deadlocker = createDeadlocker();
                step1 = new Wicket(lockers.length);
                step2 = new Wicket();
                step3 = new Wicket(lockers.length);
                deadlocker.createLockers();
                threads = new DeadlockThread[lockers.length];
                for (int i = 0; i < threads.length; ++i)
                        threads[i] = new DeadlockThread(lockers[i]);
                for (DeadlockThread thread : threads)
                        thread.begin();
        }

        public void waitState() {
                step1.waitFor();
                while (step2.getWaiters() != threads.length)
                        Thread.yield();
                step2.unlock();
                step3.waitFor();
                for (DeadlockThread thread : threads)
                        thread.waitState();
        }

        private void obtainThreadDump(ThreadMXBean threadMXBean) {
                threadIds = new long[threads.length];
                for (int i = 0; i < threads.length; ++i)
                        threadIds[i] = threads[i].getId();
                threadInfo = threadMXBean.getThreadInfo(threadIds, true, true);
        }

        private void checkDeadlocks(ThreadMXBean threadMXBean, long[] ids) {
                try {
                        ThreadUtils.verify(ids != null, "Deadlocked thread ids array is null");
                        ThreadUtils.verify(ids.length == threads.length, "Wrong length of ThreadMXBean.findMonitorDeadlockedThreads(): " + ids.length + " expected: " + threads.length);
                        for (long id : ids) {
                                boolean found = false;
                                for (MonitoringThread thread : threads)
                                        if (thread.getId() == id)
                                                found = true;
                                ThreadUtils.verify(found, "Unexpected thread id found in ThreadMXBean.findMonitorDeadlockedThreads(): " + id);
                        }
                        for (DeadlockThread thread : threads) {
                                boolean found = false;
                                for (long id : ids)
                                        if (thread.getId() == id)
                                                found = true;
                                ThreadUtils.verify(found, "Expected thread id not found in ThreadMXBean.findMonitorDeadlockedThreads(): " + thread.getId());
                        }
                        log.info("Expected deadlock thread ids found: " + ThreadUtils.strIds(ids));
                } catch (TestFailure t) {
                        log.info("Thread dump for verified threads (before the check):");
                        ThreadUtils.threadDump(log, threadInfo);
                        log.info("Expected thread ids (total " + threads.length + "): " + ThreadUtils.strIds(threadIds));
                        if (ids == null)
                                log.info("Obtained ids array is null");
                        else {
                                log.info("Obtained thread ids (total " + ids.length + "): " + ThreadUtils.strIds(ids));
                                log.info("Thread dump for obtained threads:");
                                ThreadUtils.threadDump(log, threadMXBean.getThreadInfo(ids));
                        }
                        throw t;
                }

        }

        public void check(ThreadMXBean threadMXBean) {
                obtainThreadDump(threadMXBean);
                deadlocker.check(threadMXBean);
        }

        public void finish() {
                // Unfortunately, in deadlock situation we cannot terminate started threads
        }

        public void end() {
                // Unfortunately, in deadlock situation the threads will not ever end
        }
}
