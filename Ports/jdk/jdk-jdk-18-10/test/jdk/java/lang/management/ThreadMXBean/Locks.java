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
 * @bug     4530538
 * @summary Basic unit test of ThreadInfo.getLockName()
 *          and ThreadInfo.getLockOwnerName()
 * @author  Mandy Chung
 * @author  Jaroslav Bachorik
 *
 * @library /test/lib
 *
 * @run main/othervm Locks
 */
import java.lang.management.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Phaser;
import java.util.function.Predicate;
import jdk.test.lib.LockFreeLogger;

public class Locks {

    private static final Object OBJA = new Object();
    private static final Object OBJB = new Object();
    private static final EnhancedWaiter OBJC = new EnhancedWaiter();
    private static final ThreadMXBean TM = ManagementFactory.getThreadMXBean();
    private static final LockFreeLogger LOGGER = new LockFreeLogger();

    private static String getLockName(Object lock) {
        if (lock == null) return null;

        return lock.getClass().getName() + '@' +
                Integer.toHexString(System.identityHashCode(lock));
    }

    private static void assertNoLock(Thread t) {
        if (t == null) {
            return;
        }
        String name = t.getName();
        Optional<ThreadInfo> result = Arrays.stream(
                TM.getThreadInfo(TM.getAllThreadIds(), true, true))
                                            .filter(Objects::nonNull)
                                            .filter(i -> name.equals(i.getLockOwnerName()))
                                            .findAny();
        if (result.isPresent()) {
            throw new RuntimeException("Thread " + t.getName() + " is not "
                    + "supposed to be hold any lock. Currently owning lock : "
                    + result.get().getLockName());
        }
    }

   /*
    * Handy debug function to check if error condition is because of test code or not.
    */
    private static void printStackTrace(Thread thread) {
        if (thread == null) {
            return;
        }
        StackTraceElement[] stackTrace = thread.getStackTrace();
        log("Stack dump : Thread -> " + thread.getName());
        for (StackTraceElement stackTraceEl : stackTrace) {
            log("\t" + stackTraceEl.toString());
        }
    }

    private static void assertThreadState(Thread t, Thread.State expectedState) {
        long tid = t.getId();
        Thread.State actualState = TM.getThreadInfo(tid).getThreadState();
        if (!actualState.equals(expectedState)) {
            if (expectedState.equals(Thread.State.BLOCKED) ||
                expectedState.equals(Thread.State.WAITING))
            {
                int retryCount = 0;
                printStackTrace(t);
                do {
                    goSleep(100);
                    actualState = TM.getThreadInfo(tid).getThreadState();
                } while (!actualState.equals(expectedState) && retryCount++ <= 500);
            }
            if (!actualState.equals(expectedState)) {
                printStackTrace(t);
                throw new RuntimeException("Thread " + t.getName() + " is at "
                        + actualState + " state but is expected to "
                        + "be in Thread.State = " + expectedState);
            }
        }
    }

   /*
    * Do slow check if thread is blocked on a lock. It is possible that last thread
    * to come out of Phaser might still be in Phaser call stack (Unsafe.park) and
    * hence might eventually acquire expected lock.
    */
    private static void checkBlockedObject(Thread t, Object lock, Thread owner) {
        long tid = t.getId();
        String result = TM.getThreadInfo(tid).getLockName();
        final String expectedLock = (lock != null ? getLockName(lock) : null);
        Predicate<String> p = (res) -> ((res != null && !res.equals(expectedLock))
                || (res == null && expectedLock != null));

        if (p.test(result)) {
            printStackTrace(t);
            int retryCount = 0;
            while (p.test(result)) {
                if (retryCount++ > 500) {
                    printStackTrace(t);
                    throw new RuntimeException("Thread " + t.getName() + " is blocked on "
                            + expectedLock + " but got " + result);
                }
                goSleep(100);
                result = TM.getThreadInfo(tid).getLockName();
            }
        }

        result = TM.getThreadInfo(tid).getLockOwnerName();
        final String expectedOwner = (owner != null ? owner.getName() : null);

        p = (res) -> ((res != null && !res.equals(expectedOwner))
                || (res == null && expectedOwner != null));
        if (p.test(result)) {
            printStackTrace(t);
            throw new RuntimeException("Owner of " + lock + " should be "
                    + expectedOwner + " but got " + result);
        }
    }

    private static void goSleep(long ms){
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static volatile int dummyCounter = 0;

    static class LockAThread extends Thread {
        private final Phaser p;
        public LockAThread(Phaser p) {
            super("LockAThread");
            this.p = p;
            setDaemon(true);
        }
        @Override
        public void run() {
            synchronized(OBJA) {
                // block here while LockBThread holds OBJB
                log("LockAThread about to block on OBJB");
                p.arriveAndAwaitAdvance(); // Phase 1 (blocking)
                synchronized(OBJB) {
                    dummyCounter++;
                }
            }
            p.arriveAndAwaitAdvance(); // Phase 2 (blocking)
            log("LockAThread about to exit");
            // Make sure the current thread is not holding any lock
            assertNoLock(this);
        }
    }

    static class LockBThread extends Thread {
        private final Phaser p;
        public LockBThread(Phaser p) {
            super("LockBThread");
            this.p = p;
            setDaemon(true);
        }
        @Override
        public void run() {
            synchronized(OBJB) {
                log("LockBThread about to block on OBJC");
                p.arriveAndAwaitAdvance(); // Phase 1 (blocking)
                // Signal main thread about to block on OBJC
                synchronized(OBJC) {
                    dummyCounter++;
                }
            }
            p.arriveAndAwaitAdvance(); // Phase 2 (blocking)
            log("LockBThread about to exit");
            // Make sure the current thread is not holding any lock
            assertNoLock(this);
        }
    }

   /*
    * Must be invoked from within a synchronized context
    */
    private static class EnhancedWaiter {

        boolean isNotified = false;

        public void doWait() throws InterruptedException {
            while (!isNotified) {
                wait();
            }
            isNotified = false;
        }

        public void doNotify() {
            isNotified = true;
            notify();
        }
    }

    private static WaitingThread waiter;
    private static final Object ready = new Object();
    private static CheckerThread checker;

    static class WaitingThread extends Thread {
        private final Phaser p;

        volatile boolean waiting = false;

        public WaitingThread(Phaser p) {
            super("WaitingThread");
            this.p = p;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                synchronized (OBJC) {
                    log("WaitingThread about to wait on OBJC");
                    // Signal checker thread, about to wait on OBJC.
                    waiting = false;
                    p.arriveAndAwaitAdvance(); // Phase 1 (waiting)
                    waiting = true;
                    OBJC.doWait();

                    // block until CheckerThread finishes checking
                    log("WaitingThread about to block on ready");
                    // signal checker thread that it is about acquire
                    // object ready.
                    p.arriveAndAwaitAdvance(); // Phase 2 (waiting)
                    synchronized (ready) {
                        dummyCounter++;
                    }
                }
                synchronized (OBJC) {
                    // signal checker thread, about to wait on OBJC
                    waiting = false;
                    p.arriveAndAwaitAdvance(); // Phase 3 (waiting)
                    waiting = true;
                    OBJC.doWait();
                }
                log("WaitingThread about to exit waiting on OBJC 2");
            } catch (InterruptedException e) {
                // test failed and this thread was interrupted
            }
        }

        public void waitForWaiting() {
            p.arriveAndAwaitAdvance();
            while (!waiting) {
                goSleep(10);
            }
            waitForState(State.WAITING);
        }

        public void waitForBlocked() {
            p.arriveAndAwaitAdvance();
            waitForState(State.BLOCKED);
        }

        private void waitForState(Thread.State state) {
            while (!waiter.isInterrupted() && waiter.getState() != state) {
                Thread.yield();
            }
        }
    }

    static class CheckerThread extends Thread {
        private Exception result = null;

        public CheckerThread() {
            super("CheckerThread");
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                synchronized (ready) {
                    // wait until WaitingThread about to wait for OBJC
                    waiter.waitForWaiting(); // Phase 1 (waiting)
                    assertThreadState(waiter, Thread.State.WAITING);
                    checkBlockedObject(waiter, OBJC, null);
                    synchronized (OBJC) {
                        OBJC.doNotify();
                    }
                    // wait for waiter thread to about to enter
                    // synchronized object ready.
                    waiter.waitForBlocked(); // Phase 2 (waiting)
                    assertThreadState(waiter, Thread.State.BLOCKED);
                    checkBlockedObject(waiter, ready, this);
                }

                // wait for signal from waiting thread that it is about
                // wait for OBJC.
                waiter.waitForWaiting(); // Phase 3 (waiting)
                synchronized (OBJC) {
                    assertThreadState(waiter, Thread.State.WAITING);
                    checkBlockedObject(waiter, OBJC, Thread.currentThread());
                    OBJC.doNotify();
                }
            } catch (Exception e) {
                waiter.interrupt();
                result = e;
            }
        }

        Exception result() {
            return result;
        }
    }

    public static void main(String args[]) throws Exception {
        try {
            Thread mainThread = Thread.currentThread();

            // Test uncontested case
            LockAThread t1;
            LockBThread t2;

            Phaser p = new Phaser(3);
            synchronized(OBJC) {
                // Make sure the main thread is not holding any lock
                assertNoLock(mainThread);

                // Test deadlock case
                // t1 holds lockA and attempts to lock B
                // t2 holds lockB and attempts to lock C
                t1 = new LockAThread(p);
                t1.start();

                t2 = new LockBThread(p);
                t2.start();

                p.arriveAndAwaitAdvance(); // Phase 1 (blocking)
                assertThreadState(t2, Thread.State.BLOCKED);
                checkBlockedObject(t2, OBJC, mainThread);
                assertThreadState(t1, Thread.State.BLOCKED);
                checkBlockedObject(t1, OBJB, t2);

                long[] expectedThreads = new long[3];
                expectedThreads[0] = t1.getId(); // blocked on lockB
                expectedThreads[1] = t2.getId(); // owner of lockB blocking on lockC
                expectedThreads[2] = mainThread.getId(); // owner of lockC
                findThreadsBlockedOn(OBJB, expectedThreads);
            }
            p.arriveAndAwaitAdvance(); // Phase 2 (blocking)

            p = new Phaser(2);
            // Test Object.wait() case
            waiter = new WaitingThread(p);
            waiter.start();
            checker = new CheckerThread();
            checker.start();
            try {
                waiter.join();
                checker.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (checker.result() != null) {
                throw checker.result();
            }
        } finally { // log all the messages to STDOUT
            System.out.println(LOGGER.toString());
        }
        System.out.println("Test passed.");
    }

    private static ThreadInfo findOwnerInfo(ThreadInfo[] infos, String lock)
            throws Exception {
        ThreadInfo ownerInfo = null;
        for (ThreadInfo info : infos) {
            String blockedLock = info.getLockName();
            if (lock.equals(blockedLock)) {
                long threadId = info.getLockOwnerId();
                if (threadId == -1) {
                    throw new RuntimeException("TEST FAILED: " +
                            lock + " expected to have owner");
                }
                for (ThreadInfo info1 : infos) {
                    if (info1.getThreadId() == threadId) {
                        ownerInfo = info1;
                        break;
                    }
                }
            }
        }
        return ownerInfo;
    }
    private static void findThreadsBlockedOn(Object o, long[] expectedThreads)
            throws Exception {
        String lock = getLockName(o);
        // Check with ThreadInfo with no stack trace (i.e. no safepoint)
        ThreadInfo[] infos = TM.getThreadInfo(TM.getAllThreadIds());
        doCheck(infos, lock, expectedThreads);

        // Check with ThreadInfo with stack trace
        infos = TM.getThreadInfo(TM.getAllThreadIds(), 1);
        doCheck(infos, lock, expectedThreads);
    }

    private static void doCheck(ThreadInfo[] infos, String lock, long[] expectedThreads)
            throws Exception {
        ThreadInfo ownerInfo = null;
        // Find the thread who is blocking on lock
        for (ThreadInfo info : infos) {
            String blockedLock = info.getLockName();
            if (lock.equals(blockedLock)) {
                log("%s blocked on %s", info.getThreadName(), blockedLock);
                ownerInfo = info;
            }
        }
        if (ownerInfo == null) {
            throw new RuntimeException("TEST FAILED: " +
                    "Can't retrieve ThreadInfo for the blocked thread");
        }

        long[] threads = new long[10];
        int count = 0;
        threads[count++] = ownerInfo.getThreadId();
        while (ownerInfo.getThreadState() == Thread.State.BLOCKED) {
            ownerInfo = findOwnerInfo(infos, lock);
            threads[count++] = ownerInfo.getThreadId();
            log(" Owner = %s  id = %d",
                    ownerInfo.getThreadName(),
                    ownerInfo.getThreadId()
            );
            lock = ownerInfo.getLockName();
            log("%s Id = %d  blocked on %s",
                    ownerInfo.getThreadName(),
                    ownerInfo.getThreadId(),
                    lock
            );
        }
        log("");

        if (count != expectedThreads.length) {
            throw new RuntimeException("TEST FAILED: " +
                    "Expected chain of threads not matched; current count =" + count);
        }
        for (int i = 0; i < count; i++) {
            if (threads[i] != expectedThreads[i]) {
                log("TEST FAILED: Unexpected thread in the chain %s expected to be %s",
                    threads[i],
                    expectedThreads[i]
                );
            }
        }
    }

    private static void log(String format, Object ... args) {
        LOGGER.log(format + "%n", args);
    }
}
