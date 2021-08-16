/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.stress.thread;

import java.lang.management.*;
import java.io.*;
import nsk.share.*;
import nsk.monitoring.share.*;

public class strace001 {
    public final static String LIB_NAME = "StackTraceController";
    private final static String THREAD_NAME
        = "nsk.monitoring.stress.thread.RunningThread";
    private final static int ITERATIONS = 50;

    public static volatile boolean finish;
    public static volatile boolean testFailed = false;
    public static Object common = new Object();
    public static Integer activeThreads;
    public static String activeThreadsSync = "abc";
    private static Log log;
    private static int depth;
    private static int threadCount;
    private static String[] expectedTrace;
    private static ThreadMonitor monitor;
    private static ThreadController controller;

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        monitor = Monitor.getThreadMonitor(log, argHandler);
        threadCount = argHandler.getThreadCount();
        depth = argHandler.getThreadDepth();
        controller = new ThreadController(log, threadCount, depth,
                                          argHandler.getInvocationType());
        RunningThread threads[] = new RunningThread[threadCount];

        // Fill expectedTrace array according to invocation type that is set in
        // test options
        if ( !fillTrace() ) {
            log.complain("Unknown invocation type: "
                       + controller.getInvocationType());
            return Consts.TEST_FAILED;
        }

        for (int i = 0; i < ITERATIONS; i++) {
            log.display("\nIteration: " + i);
            activeThreads = Integer.valueOf(0);
            finish = false;

            // Start all threads. Half of them are user threads,
            // others - deamon
            for (int j = 0; j < threadCount; j++) {
                threads[j] = new RunningThread(j, controller, log, depth);
                threads[j].setDaemon(i % 2 == 0);
                threads[j].start();
            }

            // Wait for all threads to start
            while (activeThreads.intValue() < threadCount)
                Thread.currentThread().yield();
            log.display("All threads started: " + activeThreads);

            // Make a snapshot of stack trace for all threads and check it
            for (int j = 0; j < threadCount; j++) {
                boolean isAlive = threads[j].isAlive();
                ThreadInfo info = monitor.getThreadInfo(threads[j].getId(), Integer.MAX_VALUE);

                // A thread may be dead because of OutOfMemoryError or
                // StackOverflowError
                if (isAlive) {
                    if (info == null) {
                        log.complain("ThreadInfo for thread " + j + " is null, "
                                   + "but Thread.isAlive() returned true.");
                        testFailed = true;
                        continue;
                    }

                    StackTraceElement[] snapshot = info.getStackTrace();
                    if ( !checkTrace(snapshot) ) {
                        log.display("\nSnapshot of thread: " + j);
                        printStackTrace(snapshot);
                        testFailed = true;
                    }
                } else {
                    log.display("Thread " + j + " is dead, skipping it.");
                }
            }

            // Let all threads to complete their job
            finish = true;

            // Wait for all threads to be dead
            for (int j = 0; j < threadCount; j++)
                try {
                    threads[j].join();
                } catch (InterruptedException e) {
                    log.complain("Unexpected exception while joining thread "
                               + j);
                    e.printStackTrace(log.getOutStream());
                    testFailed = true;
                }
            log.display("All threads have died.");
        } // for i

        if (testFailed)
            log.complain("TEST FAILED.");

        return (testFailed) ? Consts.TEST_FAILED : Consts.TEST_PASSED;
    }

    // Fill expectedTrace array according to the invocation type that is set in
    // test options
    private static boolean fillTrace() {
        switch (controller.getInvocationType()) {
            case ThreadController.JAVA_TYPE:
                expectedTrace = new String[] {
                    "java.lang.Thread.sleep"
                    , "java.lang.Thread.yield"
                    , THREAD_NAME + ".waitForSign"
                    , THREAD_NAME + ".recursionJava"
                    , THREAD_NAME + ".run"
                };
                break;

            case ThreadController.NATIVE_TYPE:
                expectedTrace = new String[] {
                    "java.lang.Thread.sleep"
                    , "java.lang.Thread.yield"
                    , THREAD_NAME + ".waitForSign"
                    , THREAD_NAME + ".recursionNative"
                    , THREAD_NAME + ".run"
                };
                break;

            case ThreadController.MIXED_TYPE:
                expectedTrace = new String[] {
                    "java.lang.Thread.sleep"
                    , "java.lang.Thread.yield"
                    , THREAD_NAME + ".waitForSign"
                    , THREAD_NAME + ".recursionNative"
                    , THREAD_NAME + ".recursionJava"
                    , THREAD_NAME + ".run"
                };
                break;

            default:
                return false;
        }

        return true;
    }

    // The method prints stack trace in style JVM does
    private static void printStackTrace(StackTraceElement[] elements) {
        for (int i = 0; i < elements.length; i++) {
            String s = "\t " + i + ": " + elements[i].getClassName() + "."
                     + elements[i].getMethodName();

            if (elements[i].isNativeMethod())
                s = s + "(Native Method)";
            else
                s = s + "(" + elements[i].getFileName() + ":"
                  + elements[i].getLineNumber() + ")";
            log.display(s);
        }
    }

    // The method performs checks of the stack trace
    private static boolean checkTrace(StackTraceElement[] elements) {
        int length = elements.length;
        int expectedLength = depth +3;
        boolean result = true;

        // Check the length of the trace. It must not be greater than
        // expectedLength. Number of recursionJava() or recursionNative()
        // methods must not ne greater than depth, also one Object.wait() or
        // Thread.yield() method, one run( ) and one waitForSign().
        if (length > expectedLength) {
            log.complain("Length of the stack trace is " + length + ", but "
                       + "expected to be not greater than " + expectedLength);
            result = false;
        }

        // Check each element of the snapshot
        for (int i = 0; i < elements.length; i++) {
            if (i == elements.length - 1) {

                // The latest method of the snapshot must be RunningThread.run()
                if ( !checkLastElement(elements[i]) )
                    result = false;
            } else {

                // getClassName() and getMethodName() must return correct values
                // for each element
                if ( !checkElement(i, elements[i]) )
                    result = false;
            }
        }
        return result;
    }

    // The method checks that StackTraceElement.getClassName() and
    // StackTraceElement.getMethodName() return expected values
    private static boolean checkElement(int n, StackTraceElement element) {
        String name = element.getClassName() + "." + element.getMethodName();

        // The latest element is not checked, since it must be "run()"
        for (int i = 0; i < expectedTrace.length - 1; i++) {
            if (expectedTrace[i].equals(name))
                return true;
        }

        log.complain("Unexpected " + n + " element of the stack trace:\n\t"
                   + name);
        return false;
    }

    // The method checks that StackTraceElement.getClassName() returns
    // "RunningThread" and StackTraceElement.getMethodName() returns "run"
    // for the latest element of the snapshot
    private static boolean checkLastElement(StackTraceElement element) {
        String name = element.getClassName() + "." + element.getMethodName();
        String last = expectedTrace[expectedTrace.length - 1];

        if (!last.equals(name)) {
            log.complain("Unexpected last element of the stack trace:\n\t"
                   + name + "\nexpected:\n\t" + last);
            return false;
        }
        return true;
    }
}

// This thread starts a recursion until it reaches specified depth. Then the
// thread waits until it gets a notification from main thread. Pure java
// and native methods are used in the thread. So, the thread is definitly in
// "running" state when main thread performs its checks.
class RunningThread extends Thread {
    private int num;
    private static ThreadController controller;
    private Log log;
    private int depth;
    private boolean mixed = false;
    native int recursionNative(int maxDepth, int currentDepth, boolean returnToJava);

    static {
        try {
            System.loadLibrary(strace001.LIB_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Cannot load library " + strace001.LIB_NAME);
            System.err.println("java.library.path: "
                             + System.getProperty("java.library.path"));
            throw e;
        }
    }

    RunningThread(int num, ThreadController controller, Log log, int depth) {
        this.num = num;
        this.controller = controller;
        this.log = log;
        this.depth = depth;
    }

    public void run() {
        int result = 0;
        int invocationType = controller.getInvocationType();

        // This instance of the thread is alive
        synchronized (strace001.common) {
            synchronized (strace001.activeThreadsSync) {
                strace001.activeThreads
                    = Integer.valueOf(strace001.activeThreads.intValue() + 1);
            }
        }

        // Choose a method (native or java) to continue recursion
        try {
            switch (invocationType) {
                case ThreadController.JAVA_TYPE:
                    recursionJava(depth, 0);
                    break;
                case ThreadController.NATIVE_TYPE:
                    result = recursionNative(depth, 0, false);

                    if (result == 1) {
                        log.display("Fatal error (OutOfMemoryError or "
                                + "StackOverflow) is thrown in native method of "
                                + " thread " + num);
                        return;
                    } else if (result == 2) {
                        log.complain("Unexpected exception is thrown in native "
                                + "method of thread " + num);
                        strace001.testFailed = true;
                        return;
                    }
                    break;
                case ThreadController.MIXED_TYPE:
                    mixed = true;
                    result = recursionNative(depth, 0, true);

                    if (result == 1) {
                        log.display("Fatal error (OutOfMemoryError or "
                                + "StackOverflow) is thrown in native method of "
                                + " thread " + num);
                        return;
                    } else if (result == 2) {
                        log.complain("Unexpected exception is thrown in native "
                                + "method of thread " + num);
                        strace001.testFailed = true;
                        return;
                    }
                    break;
                default:
                    log.complain("Unknown invocation type: "
                            + controller.getInvocationType());
                    strace001.testFailed = true;
            }
        } catch (OutOfMemoryError e) {
            // Recursion is too deep, so exit peacefully
            log.display("OutOfMemoryError is thrown in thread " + num);
        } catch (StackOverflowError e) {
            // Recursion is too deep, so exit peacefully
            log.display("StackOverflowError is thrown in thread " + num);
        }
    } // run()

    private void recursionJava(int maxDepth, int currentDepth) {
        // A short delay. Otherwise the method will reach the specified depth
        // almost instantly
        try {
            sleep(1);
        } catch (InterruptedException e) {
            log.complain("Unexpected exception");
            e.printStackTrace(log.getOutStream());
            strace001.testFailed = true;
        }

        currentDepth++;
        if (maxDepth > currentDepth) {
            Thread.yield();
            if (mixed) {
                int result = recursionNative(maxDepth, currentDepth, true);

                 if (result == 1) {
                     log.display("Fatal error (OutOfMemoryError or "
                               + "StackOverflow) is thrown in native method of "
                               + " thread " + num);
                     return;
                 } else if (result == 2) {
                     log.complain("Unexpected exception is thrown in native "
                                + "method of thread " + num);
                     strace001.testFailed = true;
                     return;
                 }
            } else
                recursionJava(maxDepth, currentDepth);
        }

        waitForSign();
    } // recursionJava()

    private void waitForSign() {
        // When the depth is reached, wait for a notification from main thread
        while (!strace001.finish) {
            try {
                sleep(1);
            } catch (InterruptedException e) {
                log.complain("Unexpected exception");
                e.printStackTrace(log.getOutStream());
                strace001.testFailed = true;
                break;
            }
        }
    } // waitForSign()
} // class RunningThread
