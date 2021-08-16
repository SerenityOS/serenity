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
package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames009;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import nsk.share.jdi.*;

public class ownedMonitorsAndFrames009a extends AbstractJDIDebuggee {

    public static String testThreadName = "ownedMonitorsAndFrames009a_TestThread";

    public static String COMMAND_RELEASE_ALL_LOCKS = "releaseAllLocks";

    public static String COMMAND_STOP_TEST_THREAD = "stopTestThread";

    class TestThread extends Thread {

        private volatile boolean locksAcquied;

        private volatile boolean locksReleased;

        private ReentrantLock lock1 = new ReentrantLock();

        private ReentrantReadWriteLock lock2 = new ReentrantReadWriteLock();

        private ReentrantReadWriteLock lock3 = new ReentrantReadWriteLock();

        public TestThread() {
            super(testThreadName);
        }

        public void run() {
            lock1.lock();
            lock2.readLock().lock();
            lock3.writeLock().lock();

            locksAcquied = true;

            /*
             * Methods Thread.sleep/Thread.interrupt are used for synchronization
             * with main thread because of wait/notify can't be used in this test
             * (see for example test bug 6487131).
             */

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // expected exception
            }

            lock1.unlock();
            lock2.readLock().unlock();
            lock3.writeLock().unlock();

            locksReleased = true;

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // expected exception
            }
        }

        public void startTestThread() {
            start();

            while (!locksAcquied)
                Thread.yield();
        }

        public void stopTestThread() {
            interrupt();

            try {
                join();
            } catch (InterruptedException e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(log.getOutStream());
            }
        }

        public void releaseLocks() {
            testThread.interrupt();

            while (!locksReleased)
                Thread.yield();
        }
    }

    private TestThread testThread;

    public String[] doInit(String args[]) {
        args = super.doInit(args);

        testThread = new TestThread();
        testThread.startTestThread();

        return args;
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_RELEASE_ALL_LOCKS)) {
            testThread.releaseLocks();
            return true;
        } else if (command.equals(COMMAND_STOP_TEST_THREAD)) {
            testThread.stopTestThread();
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        new ownedMonitorsAndFrames009a().doTest(args);
    }

}
