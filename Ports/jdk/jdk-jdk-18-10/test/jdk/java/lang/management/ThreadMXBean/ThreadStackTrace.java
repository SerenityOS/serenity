/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic unit test of ThreadInfo.getStackTrace() and
 *          ThreadInfo.getThreadState()
 * @author  Mandy Chung
 *
 * @run build Utils
 * @run main ThreadStackTrace
 */

import java.lang.management.*;
import java.util.concurrent.Phaser;

public class ThreadStackTrace {
    private static final ThreadMXBean mbean
        = ManagementFactory.getThreadMXBean();
    private static boolean notified = false;
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    private static volatile boolean testFailed = false;
    private static final String[] blockedStack = {"run", "test", "A", "B", "C", "D"};
    private static final int bsDepth = 6;
    private static final int methodB = 4;
    private static final String[] examinerStack = {"run", "examine1", "examine2"};
    private static final int esDepth = 3;
    private static final int methodExamine1= 2;

    private static void checkNullThreadInfo(Thread t) throws Exception {
        ThreadInfo ti = mbean.getThreadInfo(t.getId());
        if (ti != null) {
            ThreadInfo info =
                mbean.getThreadInfo(t.getId(), Integer.MAX_VALUE);
            System.out.println(INDENT + "TEST FAILED:");
            if (info != null) {
                printStack(t, info.getStackTrace());
                System.out.println(INDENT + "Thread state: " + info.getThreadState());
            }
            throw new RuntimeException("TEST FAILED: " +
                "getThreadInfo() is expected to return null for " + t);
        }
    }

    private static boolean trace = false;
    public static void main(String args[]) throws Exception {
        if (args.length > 0 && args[0].equals("trace")) {
            trace = true;
        }

        final Phaser p = new Phaser(2);

        Examiner examiner = new Examiner("Examiner", p);
        BlockedThread blocked = new BlockedThread("BlockedThread", p);
        examiner.setThread(blocked);

        checkNullThreadInfo(examiner);
        checkNullThreadInfo(blocked);

        // Start the threads and check them in  Blocked and Waiting states
        examiner.start();

        // #1 - block until examiner begins doing its real work
        p.arriveAndAwaitAdvance();

        System.out.println("Checking stack trace for the examiner thread " +
                           "is waiting to begin.");

        // The Examiner should be waiting to be notified by the BlockedThread
        Utils.checkThreadState(examiner, Thread.State.WAITING);

        // Check that the stack is returned correctly for a new thread
        checkStack(examiner, examinerStack, esDepth);

        System.out.println("Now starting the blocked thread");
        blocked.start();

        try {
            examiner.join();
            blocked.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Unexpected exception.");
            testFailed = true;
        }

        // Check that the stack is returned correctly for a terminated thread
        checkNullThreadInfo(examiner);
        checkNullThreadInfo(blocked);

        if (testFailed)
            throw new RuntimeException("TEST FAILED.");

        System.out.println("Test passed.");
    }

    private static String INDENT = "    ";
    private static void printStack(Thread t, StackTraceElement[] stack) {
        System.out.println(INDENT +  t +
                           " stack: (length = " + stack.length + ")");
        if (t != null) {
            for (int j = 0; j < stack.length; j++) {
                System.out.println(INDENT + stack[j]);
            }
            System.out.println();
        }
    }

    private static void checkStack(Thread t, String[] expectedStack,
                                   int depth) throws Exception {
        ThreadInfo ti = mbean.getThreadInfo(t.getId(), Integer.MAX_VALUE);
        StackTraceElement[] stack = ti.getStackTrace();

        if (trace) {
            printStack(t, stack);
        }
        int frame = stack.length - 1;
        for (int i = 0; i < depth; i++) {
            if (! stack[frame].getMethodName().equals(expectedStack[i])) {
                throw new RuntimeException("TEST FAILED: " +
                    "Expected " + expectedStack[i] + " in frame " + frame +
                    " but got " + stack[frame].getMethodName());
            }
            frame--;
        }
    }

    static class BlockedThread extends Thread {
        private final Phaser phaser;

        BlockedThread(String name, Phaser phaser) {
            super(name);
            this.phaser = phaser;
        }

        private void test() {
            A();
        }
        private void A() {
            B();
        }
        private void B() {
            C();

            // #4 - notify the examiner about to block on lockB
            phaser.arriveAndAwaitAdvance();

            synchronized (lockB) {};
        }
        private void C() {
            D();
        }
        private void D() {
            // #2 - Notify that examiner about to enter lockA
            phaser.arriveAndAwaitAdvance();

            synchronized (lockA) {
                notified = false;
                while (!notified) {
                    try {
                        // #3 - notify the examiner about to release lockA
                        phaser.arriveAndAwaitAdvance();
                        // Wait and let examiner thread check the mbean
                        lockA.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        System.out.println("Unexpected exception.");
                        testFailed = true;
                    }
                }
                System.out.println("BlockedThread notified");
            }
        }

        @Override
        public void run() {
            test();
        } // run()
    } // BlockedThread

    static class Examiner extends Thread {
        private static BlockedThread blockedThread;
        private final Phaser phaser;

        Examiner(String name, Phaser phaser) {
            super(name);
            this.phaser = phaser;
        }

        public void setThread(BlockedThread thread) {
            blockedThread = thread;
        }

        private Thread itself;
        private void examine1() {
            synchronized (lockB) {
                examine2();
                try {
                    System.out.println("Checking examiner's its own stack trace");
                    Utils.checkThreadState(itself, Thread.State.RUNNABLE);
                    checkStack(itself, examinerStack, methodExamine1);

                    // #4 - wait until blockedThread is blocked on lockB
                    phaser.arriveAndAwaitAdvance();
                    Utils.waitForThreadState(blockedThread, State.BLOCKED);

                    System.out.println("Checking stack trace for " +
                        "BlockedThread - should be blocked on lockB.");
                    Utils.checkThreadState(blockedThread, Thread.State.BLOCKED);
                    checkStack(blockedThread, blockedStack, methodB);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Unexpected exception.");
                    testFailed = true;
                }
            }
        }

        private void examine2() {
            synchronized (lockA) {
                // #1 - examiner ready to do the real work
                phaser.arriveAndAwaitAdvance();
                try {
                    // #2 - Wait until BlockedThread is about to block on lockA
                    phaser.arriveAndAwaitAdvance();
                    Utils.waitForThreadState(blockedThread, State.BLOCKED);

                    System.out.println("Checking examiner's its own stack trace");
                    Utils.checkThreadState(itself, Thread.State.RUNNABLE);
                    checkStack(itself, examinerStack, esDepth);

                    System.out.println("Checking stack trace for " +
                        "BlockedThread - should be blocked on lockA.");
                    Utils.checkThreadState(blockedThread, Thread.State.BLOCKED);
                    checkStack(blockedThread, blockedStack, bsDepth);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Unexpected exception.");
                    testFailed = true;
                }
            }

            // #3 - release lockA and let BlockedThread to get the lock
            // and wait on lockA
            phaser.arriveAndAwaitAdvance();
            Utils.waitForThreadState(blockedThread, State.WAITING);

            synchronized (lockA) {
                try {
                    System.out.println("Checking stack trace for " +
                        "BlockedThread - should be waiting on lockA.");
                    Utils.checkThreadState(blockedThread, Thread.State.WAITING);
                    checkStack(blockedThread, blockedStack, bsDepth);

                    // Let the blocked thread go
                    notified = true;
                    lockA.notify();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Unexpected exception.");
                    testFailed = true;
                }
            }
            // give some time for BlockedThread to proceed
            Utils.goSleep(50);
        } // examine2()

        @Override
        public void run() {
            itself = Thread.currentThread();
            examine1();
        } // run()
    } // Examiner
}
