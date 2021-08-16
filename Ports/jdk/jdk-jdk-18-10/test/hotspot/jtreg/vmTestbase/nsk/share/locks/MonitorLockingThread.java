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
package nsk.share.locks;

/*
 *  Thread intended to hold given lock until method releaseLock() not called
 *
 *   Example of usage:
 *
 *   Object lockToHold = new Object();
 *   MonitorLockingThread lockingThread = new MonitorLockingThread(lockToHold);
 *
 *   // after calling this method lock 'lockToHold' is acquired by lockingThread
 *   lockingThread.acquireLock();
 *
 *   // after calling this method lockingThread releases 'lockToHold' and finishes execution
 *   lockingThread.releaseLock();
 */
public class MonitorLockingThread extends Thread {
    /*
     *   Class MonitorLockingThread is written for usage in tests provoking monitor contention.
     *   Typically in these tests exists thread holding lock (MonitorLockingThread) and another
     *   thread trying to acquire the same lock. But this scenario also requires one more thread
     *   which will force MonitorLockingThread to release lock when contention occurred, for this purpose
     *   auxiliary thread class LockFreeThread is written.
     *
     *   Example of usage of MonitorLockingThread and LockFreeThread:
     *
     *   Object lock = new Object();
     *   MonitorLockingThread monitorLockingThread = new MonitorLockingThread(lock);
     *
     *   MonitorLockingThread.LockFreeThread lockFreeThread =
     *       new MonitorLockingThread.LockFreeThread(Thread.currentThread(), monitorLockingThread);
     *
     *   monitorLockingThread.acquireLock();
     *
     *   lockFreeThread.start();
     *
     *   // try to acquire lock which is already held by MonitorLockingThread (here monitor contention should occur),
     *   // when LockFreeThread finds that contention occurred it forces MonitorLockingThread to release lock
     *   // and current thread is able to continue execution
     *   synchronized (lock) {
     *   }
     */
    public static class LockFreeThread extends Thread {
        private Thread blockedThread;

        private MonitorLockingThread lockingThread;

        public LockFreeThread(Thread blockedThread, MonitorLockingThread lockingThread) {
            this.blockedThread = blockedThread;
            this.lockingThread = lockingThread;
        }

        public void run() {
            /*
             * Wait when blockedThread's state will switch to 'BLOCKED' (at that moment monitor contention
             * should already occur) and then force MonitorLockingThread to release lock
             */
            while (blockedThread.getState() != Thread.State.BLOCKED)
                Thread.yield();

            lockingThread.releaseLock();
        }
    }

    private volatile boolean isRunning = true;

    private volatile boolean holdsLock;

    private Object lockToHold;

    public MonitorLockingThread(Object lockToHold) {
        this.lockToHold = lockToHold;
    }

    public void run() {
        synchronized (lockToHold) {
            holdsLock = true;
            while (isRunning)
                Thread.yield();
        }
        holdsLock = false;
    }

    public void releaseLock() {
        isRunning = false;
        while (holdsLock)
            Thread.yield();
    }

    public void acquireLock() {
        start();
        while (!holdsLock)
            Thread.yield();
    }
}
