/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @bug     5086470 6358247
 * @summary SynchronizersLockingThread is used by LockedSynchronizers.
 *          It will create threads that acquire ReentrantLock and also object
 *          monitors.
 * @author  Mandy Chung
 *
 * @build ThreadDump Utils
 */

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class SynchronizerLockingThread extends Thread {
    static ReentrantLock lock1 = new ReentrantLock();
    static ReentrantLock lock2 = new ReentrantLock();
    static ReentrantLock lock3 = new ReentrantLock();
    static ReentrantLock lock4 = new ReentrantLock();
    static Lock lock5 = new Lock("lock5");
    static Lock lock6 = new Lock("lock6");
    static Lock lock7 = new Lock("lock7");
    static ReentrantLock lock8 = new ReentrantLock();

    static SynchronizerLockingThread t1 = new Thread1();
    static SynchronizerLockingThread t2 = new Thread2();
    static int count = 2;
    static void startLockingThreads() {
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // wait until t1 and t2 waits
        while (count != 0) {
           try {
               Thread.sleep(100);
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }
        }

        Utils.waitForBlockWaitingState(t1);
        Utils.waitForBlockWaitingState(t2);
    }

    static long[] getThreadIds() {
        return new long[] {t1.getId(), t2.getId()};
    }

    static void checkLocks(ThreadInfo[] tinfos) throws Exception {
        int matches = 0;
        for (ThreadInfo info : tinfos) {
            if (info.getThreadId() == t1.getId()) {
                t1.checkLocks(info);
                matches++;
            }
            if (info.getThreadId() == t2.getId()) {
                t2.checkLocks(info);
                matches++;
            }
        }
        if (matches != 2) {
            throw new RuntimeException("MonitorInfo missing");
        }
    }

    static class Lock {
        String name;
        Lock(String name) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    final String threadName;
    Lock   waitingLock;
    int    numOwnedMonitors;
    Map<String, Lock[]> ownedMonitors;
    Condition       waitingSync;
    int             numOwnedSyncs;
    Map<String, ReentrantLock[]> ownedSyncs;
    public SynchronizerLockingThread(String name) {
        this.threadName = name;
    }

    protected void setExpectedResult(Lock waitingLock,
                                     int numOwnedMonitors,
                                     Map<String, Lock[]> ownedMonitors,
                                     Condition waitingSync,
                                     int numOwnedSyncs,
                                     Map<String, ReentrantLock[]> ownedSyncs) {
        this.waitingLock = waitingLock;
        this.numOwnedMonitors = numOwnedMonitors;
        this.ownedMonitors = ownedMonitors;
        this.waitingSync = waitingSync;
        this.numOwnedSyncs = numOwnedSyncs;
        this.ownedSyncs = ownedSyncs;
    }

    void checkLocks(ThreadInfo info) throws Exception {
        checkThreadInfo(info);
        MonitorInfo[] monitors = info.getLockedMonitors();
        if (monitors.length != numOwnedMonitors) {
            ThreadDump.threadDump();
            throw new RuntimeException("Number of locked monitors = " +
                monitors.length +
                " not matched. Expected: " + numOwnedMonitors);
        }
        // check if each monitor returned in the list is the expected
        // one
        for (MonitorInfo m : monitors) {
            StackTraceElement ste = m.getLockedStackFrame();
            int depth = m.getLockedStackDepth();
            checkStackFrame(info, ste, depth);
            checkMonitor(m, ste.getMethodName());
        }
        // check if each expected monitor is included in the returned
        // list
        for (Map.Entry<String, Lock[]> e : ownedMonitors.entrySet()) {
            for (Lock l : e.getValue()) {
                checkMonitor(e.getKey(), l, monitors);
            }
        }

        // We can only check if the length matches since we have no
        // way to get the AbstractOwnableSynchronizer in ReentrantLock
        LockInfo[] syncs = info.getLockedSynchronizers();
        if (syncs.length != numOwnedSyncs) {
            ThreadDump.threadDump();
            throw new RuntimeException("Number of locked syncs = " +
                syncs.length +
                " not matched. Expected: " + numOwnedSyncs);
        }
    }

    void checkThreadInfo(ThreadInfo info) throws Exception {
        if (!getName().equals(info.getThreadName())) {
            throw new RuntimeException("Name: " + info.getThreadName() +
                " not matched. Expected: " + getName());
        }
        LockInfo l = info.getLockInfo();
        if ((waitingLock != null || waitingSync != null) && l == null) {
            throw new RuntimeException("LockInfo: " + l +
                " not matched. Expected: non-null");
        }
        if (waitingLock == null && waitingSync == null && l != null) {
            throw new RuntimeException("LockInfo: " + l +
                " not matched. Expected: null");
        }

        String waitingLockName;
        int hcode;
        if (waitingLock != null) {
            waitingLockName = waitingLock.getClass().getName();
            hcode = System.identityHashCode(waitingLock);
        } else {
            waitingLockName = waitingSync.getClass().getName();
            hcode = System.identityHashCode(waitingSync);
        }
        if (!waitingLockName.equals(l.getClassName())) {
            throw new RuntimeException("LockInfo : " + l +
                " class name not matched. Expected: " + waitingLockName);
        }
        if (hcode != l.getIdentityHashCode()) {
            throw new RuntimeException("LockInfo: " + l +
                " IdentityHashCode not matched. Expected: " + hcode);
        }

        String lockName = info.getLockName();
        String[] s = lockName.split("@");
        if (!waitingLockName.equals(s[0])) {
            throw new RuntimeException("LockName: " + lockName +
                " class name not matched. Expected: " + waitingLockName);
        }
        int i = Integer.parseInt(s[1], 16);
        if (hcode != i) {
            throw new RuntimeException("LockName: " + lockName +
                " IdentityHashCode not matched. Expected: " + hcode);
        }
    }

    void checkStackFrame(ThreadInfo info, StackTraceElement ste, int depth) {
        StackTraceElement[] stacktrace = info.getStackTrace();
        if (!ste.equals(stacktrace[depth])) {
            System.out.println("LockedStackFrame:- " + ste);
            System.out.println("StackTrace at " + depth + " :-" +
                stacktrace[depth]);
            throw new RuntimeException("LockedStackFrame does not match " +
                "stack frame in ThreadInfo.getStackTrace");
        }
    }
    void checkMonitor(MonitorInfo m, String methodName) {
        for (Map.Entry<String, Lock[]> e : ownedMonitors.entrySet()) {
            if (methodName.equals(e.getKey())) {
                for (Lock l : e.getValue()) {
                    String className = l.getClass().getName();
                    int hcode = System.identityHashCode(l);
                    if (className.equals(m.getClassName()) &&
                        hcode == m.getIdentityHashCode()) {
                        // monitor matched the expected
                        return;
                    }
                }
            }
        }
        throw new RuntimeException("Monitor not expected" + m);
    }
    void checkMonitor(String methodName, Lock l, MonitorInfo[] monitors) {
        String className = l.getClass().getName();
        int hcode = System.identityHashCode(l);
        for (MonitorInfo m : monitors) {
            if (className.equals(m.getClassName()) &&
                hcode == m.getIdentityHashCode() &&
                methodName.equals(m.getLockedStackFrame().getMethodName())) {
                return;
            }
        }
        throw new RuntimeException("Monitor not found in the returned list" +
            " Method: " + methodName + " Lock: " + l);

    }

    static class Thread1 extends SynchronizerLockingThread {
        public Thread1() {
            super("t1");
            initExpectedResult();
        }
        public void run() {
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
                    // signal to about to wait
                    count--;
                    lock7.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Map<String, Lock[]> LOCKED_MONITORS;
        Map<String, ReentrantLock[]> LOCKED_SYNCS;
        Lock WAITING_LOCK = lock7;
        int OWNED_MONITORS = 2;
        int OWNED_SYNCS = 4;
        void initExpectedResult() {
            LOCKED_MONITORS = new HashMap<String, Lock[]>();
            LOCKED_MONITORS.put("D", new Lock[0]); // no monitored locked
            LOCKED_MONITORS.put("C", new Lock[] {lock6});
            LOCKED_MONITORS.put("B", new Lock[] {lock5});
            LOCKED_MONITORS.put("A", new Lock[0]);

            LOCKED_SYNCS = new HashMap<String, ReentrantLock[]>();
            LOCKED_SYNCS.put("D", new ReentrantLock[0]); // no sync locked
            LOCKED_SYNCS.put("C", new ReentrantLock[0]); // no sync locked
            LOCKED_SYNCS.put("B", new ReentrantLock[] {lock4});
            LOCKED_SYNCS.put("A", new ReentrantLock[] {lock3, lock2, lock1});
            this.setExpectedResult(WAITING_LOCK,
                                   OWNED_MONITORS, LOCKED_MONITORS,
                                   null,
                                   OWNED_SYNCS, LOCKED_SYNCS);
        }

    }

    static class Thread2 extends SynchronizerLockingThread {
        Map<String, Lock[]> LOCKED_MONITORS = new HashMap<String, Lock[]>();
        Map<String, ReentrantLock[]> LOCKED_SYNCS = new HashMap<String, ReentrantLock[]>();
        Condition c = lock8.newCondition();
        Condition WAITING_LOCK = c;
        int OWNED_MONITORS = 0;
        int OWNED_SYNCS = 0;
        public Thread2() {
            super("t2");
            this.setExpectedResult(null,
                                   OWNED_MONITORS, LOCKED_MONITORS,
                                   WAITING_LOCK,
                                   OWNED_SYNCS, LOCKED_SYNCS);
        }
        public void run() {
            lock8.lock();
            try {
                synchronized(lock7) {
                    count--;
                }
                c.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock8.unlock();
            }
            throw new RuntimeException("should not reach here");
        }
    }

}
