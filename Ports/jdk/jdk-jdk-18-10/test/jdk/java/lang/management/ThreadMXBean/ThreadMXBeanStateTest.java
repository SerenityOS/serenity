/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4967283 5080203 8022208
 * @summary Basic unit test of thread states returned by
 *          ThreadMXBean.getThreadInfo.getThreadState().
 *          It also tests lock information returned by ThreadInfo.
 * @author  Mandy Chung
 *
 * @library ../../Thread
 * @library /test/lib
 *
 * @build ThreadMXBeanStateTest ThreadStateController
 * @run main ThreadMXBeanStateTest
 */

import jdk.test.lib.Utils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import static java.lang.Thread.State.*;

public class ThreadMXBeanStateTest {
    private static final ThreadMXBean tm = ManagementFactory.getThreadMXBean();

    static class Lock {
        private final String name;
        Lock(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    private static final Lock globalLock = new Lock("my lock");

    public static void main(String[] argv) throws Exception {
        // Force thread state initialization now before the test
        // verification begins.
        Thread.currentThread().getState();
        ThreadStateController thread = new ThreadStateController("StateChanger", globalLock);
        thread.setDaemon(true);
        try {
            // before myThread starts
            thread.checkThreadState(NEW);

            thread.start();
            thread.transitionTo(RUNNABLE);
            thread.checkThreadState(RUNNABLE);
            checkLockInfo(thread, RUNNABLE, null, null);

            thread.suspend();
            ThreadStateController.pause(10);
            thread.checkThreadState(RUNNABLE);
            checkSuspendedThreadState(thread, RUNNABLE);
            thread.resume();

            synchronized (globalLock) {
                thread.transitionTo(BLOCKED);
                thread.checkThreadState(BLOCKED);
                checkLockInfo(thread, BLOCKED,
                              globalLock, Thread.currentThread());
            }

            thread.transitionTo(WAITING);
            thread.checkThreadState(WAITING);
            checkLockInfo(thread, Thread.State.WAITING,
                          globalLock, null);

            thread.transitionTo(TIMED_WAITING);
            thread.checkThreadState(TIMED_WAITING);
            checkLockInfo(thread, TIMED_WAITING,
                          globalLock, null);


            thread.transitionToPark(true /* timed park */);
            thread.checkThreadState(TIMED_WAITING);
            checkLockInfo(thread, TIMED_WAITING, null, null);

            thread.transitionToPark(false /* indefinite park */);
            thread.checkThreadState(WAITING);
            checkLockInfo(thread, WAITING, null, null);

            thread.transitionToSleep();
            thread.checkThreadState(TIMED_WAITING);
            checkLockInfo(thread, TIMED_WAITING, null, null);

            thread.transitionTo(TERMINATED);
            thread.checkThreadState(TERMINATED);
        } finally {
            try {
                System.out.println(thread.getLog(Utils.adjustTimeout(60_000)));
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("TEST FAILED: Unexpected exception.");
                throw new RuntimeException(e);
            }
        }
        System.out.println("Test passed.");
    }

    private static void checkSuspendedThreadState(ThreadStateController t, Thread.State state) {
        ThreadInfo info = getThreadInfo(t, state);
        if (info == null) {
            throw new RuntimeException(t.getName() +
               " expected to have ThreadInfo " +
               " but got null.");
        }

        if (info.getThreadState() != state) {
            throw new RuntimeException(t.getName() + " expected to be in " +
                state + " state but got " + info.getThreadState());
        }

        if (!info.isSuspended()) {
            throw new RuntimeException(t.getName() + " expected to be suspended " +
                " but isSuspended() returns " + info.isSuspended());
        }
    }

    private static String getLockName(Object lock) {
        if (lock == null) return null;

        return lock.getClass().getName() + '@' +
            Integer.toHexString(System.identityHashCode(lock));
    }

    // maximum number of retries when checking for thread state.
    private static final int MAX_RETRY = 500;
    private static ThreadInfo getThreadInfo(ThreadStateController t, Thread.State expected) {
        // wait for the thread to transition to the expected state.
        // There is a small window between the thread checking the state
        // and the thread actual entering that state.
        int retryCount=0;
        ThreadInfo info = tm.getThreadInfo(t.getId());
        while (info.getThreadState() != expected && retryCount < MAX_RETRY) {
            ThreadStateController.pause(10);
            retryCount++;
            info = tm.getThreadInfo(t.getId());
        }
        return info;
    }

    private static void checkLockInfo(ThreadStateController t, Thread.State state,
                                      Object lock, Thread owner) {
        ThreadInfo info = getThreadInfo(t, state);
        if (info == null) {
            throw new RuntimeException(t.getName() +
               " expected to have ThreadInfo " +
               " but got null.");
        }

        if (info.getThreadState() != state) {
            throw new RuntimeException(t.getName() + " expected to be in " +
                state + " state but got " + info.getThreadState());
        }

        if (lock == null && info.getLockName() != null) {
            throw new RuntimeException(t.getName() +
                " expected not to be blocked on any lock" +
                " but got " + info.getLockName());
        }
        String expectedLockName = getLockName(lock);
        if (lock != null && info.getLockName() == null) {
            throw new RuntimeException(t.getName() +
                " expected to be blocked on lock [" + expectedLockName +
                "] but got null.");
        }

        if (lock != null && !expectedLockName.equals(info.getLockName())) {
            throw new RuntimeException(t.getName() +
                " expected to be blocked on lock [" + expectedLockName +
                "] but got [" + info.getLockName() + "].");
        }

        if (owner == null && info.getLockOwnerName() != null) {
            throw new RuntimeException("Lock owner is expected " +
                " to be null but got " + info.getLockOwnerName());
        }

        if (owner != null && info.getLockOwnerName() == null) {
            throw new RuntimeException("Lock owner is expected to be " +
                owner.getName() +
                " but got null.");
        }
        if (owner != null && !info.getLockOwnerName().equals(owner.getName())) {
            throw new RuntimeException("Lock owner is expected to be " +
                owner.getName() +
                " but got " + owner.getName());
        }
        if (owner == null && info.getLockOwnerId() != -1) {
            throw new RuntimeException("Lock owner is expected " +
                " to be -1 but got " + info.getLockOwnerId());
        }

        if (owner != null && info.getLockOwnerId() <= 0) {
            throw new RuntimeException("Lock owner is expected to be " +
                owner.getName() + "(id = " + owner.getId() +
                ") but got " + info.getLockOwnerId());
        }
        if (owner != null && info.getLockOwnerId() != owner.getId()) {
            throw new RuntimeException("Lock owner is expected to be " +
                owner.getName() + "(id = " + owner.getId() +
                ") but got " + info.getLockOwnerId());
        }
        if (info.isSuspended()) {
            throw new RuntimeException(t.getName() +
                " isSuspended() returns " + info.isSuspended());
        }
    }
}
