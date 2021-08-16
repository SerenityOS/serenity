/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.Thread.State;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Map;
import jdk.test.lib.RandomFactory;

/**
 * @test
 * @bug 8010939
 * @summary check for deadlock between findLogger() and drainLoggerRefQueueBounded()
 * @author jim.gish@oracle.com
 * @modules java.logging
 *          java.management
 * @library /test/lib
 * @build DrainFindDeadlockTest
 * @run main/othervm DrainFindDeadlockTest
 * @key randomness
 */

/**
 * This test is checking for a deadlock between
 * LogManager$LoggerContext.findLogger() and
 * LogManager.drainLoggerRefQueueBounded() (which could happen by calling
 * Logger.getLogger() and LogManager.readConfiguration() in different threads)
 */
public class DrainFindDeadlockTest {
    private LogManager mgr = LogManager.getLogManager();
    private static final int MAX_ITERATIONS = 100;
    private static final Random random = RandomFactory.getRandom();
    private static int preventLoopElision;

    // Get a ThreadMXBean so we can check for deadlock.  N.B. this may
    // not be supported on all platforms, which means we will have to
    // resort to the traditional test timeout method. However, if
    // we have the support we'll get the deadlock details if one
    // is detected.
    private static final ThreadMXBean threadMXBean =
            ManagementFactory.getThreadMXBean();
    private final boolean threadMXBeanDeadlockSupported =
            threadMXBean.isSynchronizerUsageSupported();

    public static void main(String... args) throws IOException, Exception {
        new DrainFindDeadlockTest().testForDeadlock();
    }

    public static void randomDelay() {
        int runs = random.nextInt(1000000);
        int c = 0;

        for (int i=0; i<runs; ++i) {
            c=c+i;
        }
        preventLoopElision = c;
    }

    public void testForDeadlock() throws IOException, Exception {
        System.out.println("Deadlock detection "
                + (threadMXBeanDeadlockSupported ? "is" : "is not") +
                            " available.");
        Thread setup = new Thread(new SetupLogger(), "SetupLogger");
        Thread readConfig = new Thread(new ReadConfig(), "ReadConfig");
        Thread check = new Thread(new DeadlockChecker(setup, readConfig),
                                   "DeadlockChecker");

        // make the threads daemon threads so they will go away when the
        // test exits
        setup.setDaemon(true);
        readConfig.setDaemon(true);
        check.setDaemon(true);

        check.start(); setup.start(); readConfig.start();
        try {
            check.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        try {
            readConfig.join();
            setup.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        System.out.println("Test passed");
    }

    class SetupLogger implements Runnable {
        Logger logger = null;

        @Override
        public void run() {
            System.out.println("Running " + Thread.currentThread().getName());

            try {
                for (int i=0; i < MAX_ITERATIONS; i++) {
                    logger = Logger.getLogger("DrainFindDeadlockTest"+i);
                    DrainFindDeadlockTest.randomDelay();
                }
            } finally {
                System.out.println("Completed " + Thread.currentThread().getName());
            }
        }
    }

    class ReadConfig implements Runnable {
        @Override
        public void run() {
            System.out.println("Running " + Thread.currentThread().getName());
            try {
                for (int i=0; i < MAX_ITERATIONS; i++) {
                    try {
                        mgr.readConfiguration();
                    } catch (IOException | SecurityException ex) {
                        throw new RuntimeException("FAILED: test setup problem", ex);
                    }
                    DrainFindDeadlockTest.randomDelay();
                }
            } finally {
                System.out.println("Completed " + Thread.currentThread().getName());
            }
        }
    }

    class DeadlockChecker implements Runnable {
        Thread t1, t2;

        DeadlockChecker(Thread t1, Thread t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        void checkState(Thread x, Thread y) {
            //            System.out.println("checkstate");
            boolean isXblocked = x.getState().equals(State.BLOCKED);
            boolean isYblocked = y.getState().equals(State.BLOCKED);
            long[] deadlockedThreads = null;

            if (isXblocked && isYblocked) {
                System.out.println("threads blocked");
                // they are both blocked, but this doesn't necessarily mean
                // they are deadlocked
                if (threadMXBeanDeadlockSupported) {
                    System.out.println("checking for deadlock");
                    deadlockedThreads = threadMXBean.findDeadlockedThreads();
                } else {
                    System.out.println("Can't check for deadlock");
                }
                if (deadlockedThreads != null) {
                    System.out.println("We detected a deadlock! ");
                    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(
                            deadlockedThreads, true, true);
                    for (ThreadInfo threadInfo: threadInfos) {
                        System.out.println(threadInfo);
                    }
                    throw new RuntimeException("TEST FAILED: Deadlock detected");
                }
                System.out.println("We may have a deadlock");
                Map<Thread, StackTraceElement[]> threadMap =
                        Thread.getAllStackTraces();
                dumpStack(threadMap.get(x), x);
                dumpStack(threadMap.get(y), y);
            }
        }

        private void dumpStack(StackTraceElement[] aStackElt, Thread aThread) {
            if (aStackElt != null) {
                 System.out.println("Thread:" + aThread.getName() + ": " +
                                    aThread.getState());
                 for (StackTraceElement element: aStackElt) {
                    System.out.println("   " + element);
                 }
            }
        }

        @Override
        public void run() {
            System.out.println("Running " + Thread.currentThread().getName());
            try {
                for (int i=0; i < MAX_ITERATIONS*2; i++) {
                    checkState(t1, t2);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                    }
                }
            } finally {
                System.out.println("Completed " + Thread.currentThread().getName());
            }
        }
    }
}
