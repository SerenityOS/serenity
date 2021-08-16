/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5086470 6358247 7193302 8048215
 * @summary Test type conversion when invoking ThreadMXBean.dumpAllThreads
 *          through proxy.
 * @author  Mandy Chung
 *
 * @run main ThreadMXBeanProxy
 */

import static java.lang.management.ManagementFactory.*;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
import javax.management.*;

public class ThreadMXBeanProxy {
    private static MBeanServer server =
        ManagementFactory.getPlatformMBeanServer();
    private static ThreadMXBean mbean;
    static Mutex mutex = new Mutex();
    static Object lock = new Object();
    static Object waiter = new Object();
    static MyThread thread = new MyThread();
    public static void main(String[] argv) throws Exception {
        mbean = newPlatformMXBeanProxy(server,
                                       THREAD_MXBEAN_NAME,
                                       ThreadMXBean.class);

        if (!mbean.isSynchronizerUsageSupported()) {
            System.out.println("Monitoring of synchronizer usage not supported");
            return;
        }

        thread.setDaemon(true);
        thread.start();

        // wait until myThread acquires mutex and lock owner is set.
        while (!(mutex.isLocked() && mutex.getLockOwner() == thread)) {
           try {
               Thread.sleep(100);
           } catch (InterruptedException e) {
               throw new RuntimeException(e);
           }
        }

        // 'thread' holds the mutex, which means it must also have the monitor of
        // 'waiter' at least until it does the wait(). So we acquire the monitor of
        // 'waiter' here, which ensures that 'thread' must be in wait()
        synchronized(waiter) {
        }

        long[] ids = new long[] { thread.getId() };

        // validate the local access
        ThreadInfo[] infos = getThreadMXBean().getThreadInfo(ids, true, true);
        if (infos.length != 1) {
            throw new RuntimeException("Returned ThreadInfo[] of length=" +
                infos.length + ". Expected to be 1.");
        }
        thread.checkThreadInfo(infos[0]);

        // validate the remote access
        infos = mbean.getThreadInfo(ids, true, true);
        if (infos.length != 1) {
            throw new RuntimeException("Returned ThreadInfo[] of length=" +
                infos.length + ". Expected to be 1.");
        }
        thread.checkThreadInfo(infos[0]);

        boolean found = false;
        infos = mbean.dumpAllThreads(true, true);
        for (ThreadInfo ti : infos) {
            if (ti.getThreadId() == thread.getId()) {
                thread.checkThreadInfo(ti);
                found = true;
            }
        }

        if (!found) {
            throw new RuntimeException("No ThreadInfo found for MyThread");
        }

        System.out.println("Test passed");
    }

    static class MyThread extends Thread {
        public MyThread() {
            super("MyThread");
        }
        public void run() {
            synchronized (lock) {
                synchronized(waiter) {
                    mutex.lock();
                    try {
                        waiter.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        int OWNED_MONITORS = 1;
        int OWNED_SYNCS = 1;
        void checkThreadInfo(ThreadInfo info) {
            if (!getName().equals(info.getThreadName())) {
                throw new RuntimeException("Name: " + info.getThreadName() +
                    " not matched. Expected: " + getName());
            }

            MonitorInfo[] monitors = info.getLockedMonitors();
            if (monitors.length != OWNED_MONITORS) {
                throw new RuntimeException("Number of locked monitors = " +
                    monitors.length +
                    " not matched. Expected: " + OWNED_MONITORS);
            }
            MonitorInfo m = monitors[0];
            StackTraceElement ste = m.getLockedStackFrame();
            int depth = m.getLockedStackDepth();
            StackTraceElement[] stacktrace = info.getStackTrace();
            if (!ste.equals(stacktrace[depth])) {
                System.out.println("LockedStackFrame:- " + ste);
                System.out.println("StackTrace at " + depth + " :-" +
                    stacktrace[depth]);
                throw new RuntimeException("LockedStackFrame does not match " +
                    "stack frame in ThreadInfo.getStackTrace");
           }

           String className = lock.getClass().getName();
           int hcode = System.identityHashCode(lock);
           if (!className.equals(m.getClassName()) ||
                   hcode != m.getIdentityHashCode() ||
                   !m.getLockedStackFrame().getMethodName().equals("run")) {
                System.out.println(info);
                throw new RuntimeException("MonitorInfo " + m +
                   " doesn't match.");
            }

            LockInfo[] syncs = info.getLockedSynchronizers();
            if (syncs.length != OWNED_SYNCS) {
                throw new RuntimeException("Number of locked syncs = " +
                        syncs.length + " not matched. Expected: " + OWNED_SYNCS);
            }
            AbstractOwnableSynchronizer s = mutex.getSync();
            String lockName = s.getClass().getName();
            hcode = System.identityHashCode(s);
            if (!lockName.equals(syncs[0].getClassName())) {
                throw new RuntimeException("LockInfo : " + syncs[0] +
                    " class name not matched. Expected: " + lockName);
            }
            if (hcode != syncs[0].getIdentityHashCode()) {
                throw new RuntimeException("LockInfo: " + syncs[0] +
                    " IdentityHashCode not matched. Expected: " + hcode);
            }
            LockInfo li = info.getLockInfo();
            if (li == null) {
                throw new RuntimeException("Expected non-null LockInfo");
            }
        }
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

            protected Thread getLockOwner() {
                return getExclusiveOwnerThread();
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

        public Thread getLockOwner()     { return sync.getLockOwner(); }

        public AbstractOwnableSynchronizer getSync() { return sync; }
    }
}
