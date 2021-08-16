/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     5086470 6358247
 * @summary Basic Test for ThreadMXBean.dumpAllThreads(false, true)
 *          and getThreadInfo of customized JSR-166 synchronizers.
 * @author  Mandy Chung
 *
 * @build Barrier
 * @build ThreadDump
 * @run main/othervm MyOwnSynchronizer
 */

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

public class MyOwnSynchronizer {
    static ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    static Mutex mutex = new Mutex();
    static MyThread thread = new MyThread();
    public static void main(String[] argv) throws Exception {
        if (!mbean.isSynchronizerUsageSupported()) {
            System.out.println("Monitoring of synchronizer usage not supported");
            return;
        }

        thread.setDaemon(true);
        thread.start();

        // wait until myThread acquires mutex
        while (!mutex.isLocked()) {
           try {
               Thread.sleep(100);
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }
        }

        ThreadDump.threadDump();
        // Test dumpAllThreads with locked synchronizers
        ThreadInfo[] tinfos = mbean.dumpAllThreads(false, true);
        for (ThreadInfo ti : tinfos) {
           MonitorInfo[] monitors = ti.getLockedMonitors();
           if (monitors.length != 0) {
               throw new RuntimeException("Name: " + ti.getThreadName() +
                   " has non-empty locked monitors = " + monitors.length);
           }
           LockInfo[] syncs = ti.getLockedSynchronizers();
           if (ti.getThreadId() == thread.getId()) {
               thread.checkLockedSyncs(ti, syncs);
           }
        }

        // Test getThreadInfo with locked synchronizers
        tinfos = mbean.getThreadInfo(new long[] {thread.getId()}, false, true);
        if (tinfos.length != 1) {
            throw new RuntimeException("getThreadInfo() returns " +
                tinfos.length + " ThreadInfo objects. Expected 0.");
        }
        ThreadInfo ti = tinfos[0];
        if (ti.getLockedMonitors().length != 0) {
            throw new RuntimeException("Name: " + ti.getThreadName() +
               " has non-empty locked monitors = " +
               ti.getLockedMonitors().length);
        }
        thread.checkLockedSyncs(ti, ti.getLockedSynchronizers());

        System.out.println("Test passed");
    }

    static class Mutex implements Lock, java.io.Serializable {

        // Our internal helper class
        class Sync extends AbstractQueuedSynchronizer {
            // Report whether in locked state
            protected boolean isHeldExclusively() {
                return getState() == 1;
            }

            // Acquire the lock if state is zero
            public boolean tryAcquire(int acquires) {
                assert acquires == 1; // Otherwise unused
                if (compareAndSetState(0, 1)) {
                  setExclusiveOwnerThread(Thread.currentThread());
                  return true;
                }
                return false;
            }

            // Release the lock by setting state to zero
            protected boolean tryRelease(int releases) {
                assert releases == 1; // Otherwise unused
                if (getState() == 0) throw new IllegalMonitorStateException();
                setExclusiveOwnerThread(null);
                setState(0);
                return true;
            }

            // Provide a Condition
            Condition newCondition() { return new ConditionObject(); }

            // Deserialize properly
            private void readObject(ObjectInputStream s)
                throws IOException, ClassNotFoundException {
                s.defaultReadObject();
                setState(0); // reset to unlocked state
            }
        }

        // The sync object does all the hard work. We just forward to it.
        private final Sync sync = new Sync();

        public void lock()                { sync.acquire(1); }
        public boolean tryLock()          { return sync.tryAcquire(1); }
        public void unlock()              { sync.release(1); }
        public Condition newCondition()   { return sync.newCondition(); }
        public boolean isLocked()         { return sync.isHeldExclusively(); }
        public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        public AbstractOwnableSynchronizer getSync() { return sync; }
    }

    static class MyThread extends Thread {
        public MyThread() {
            super("MyThread");
        }
        public void run() {
            mutex.lock();
            Object o = new Object();
            synchronized(o) {
                try {
                    o.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        int OWNED_SYNCS = 1;
        void checkLockedSyncs(ThreadInfo info, LockInfo[] syncs) {
            if (!getName().equals(info.getThreadName())) {
                throw new RuntimeException("Name: " + info.getThreadName() +
                    " not matched. Expected: " + getName());
            }

            if (syncs.length != OWNED_SYNCS) {
                throw new RuntimeException("Number of locked syncs = " +
                    syncs.length +
                    " not matched. Expected: " + OWNED_SYNCS);
            }
            AbstractOwnableSynchronizer s = mutex.getSync();
            String lockName = s.getClass().getName();
            int hcode = System.identityHashCode(s);
            if (!lockName.equals(syncs[0].getClassName())) {
                throw new RuntimeException("LockInfo : " + syncs[0] +
                    " class name not matched. Expected: " + lockName);
            }
            if (hcode != syncs[0].getIdentityHashCode()) {
                throw new RuntimeException("LockInfo: " + syncs[0] +
                    " IdentityHashCode not matched. Expected: " + hcode);
            }

        }
    }
}
