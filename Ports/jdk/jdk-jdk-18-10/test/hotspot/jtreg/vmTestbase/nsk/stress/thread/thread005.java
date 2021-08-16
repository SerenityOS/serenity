/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/thread/thread005.
 * VM testbase keywords: [stress, diehard, slow, nonconcurrent, quick]
 * VM testbase readme:
 * DESCRIPTION
 *     Try many threads starting simultaneously.
 *
 * @run main/othervm nsk.stress.thread.thread005 500 2m 5s
 */

package nsk.stress.thread;

import java.io.PrintStream;

/**
 * Try many threads starting simultaneously.
 */
public class thread005 extends Thread {
    /**
     * Enable/disable printing of debugging info.
     */
    private static boolean DEBUG_MODE = false;

    /**
     * The number of threads that the tested JVM must support.
     */
    private static int THREADS_EXPECTED = 1000;

    /**
     * Timeout (in milliseconds) after which all threads must halt.
     */
    private static long TIMEOUT = 300000; // 5 minutes

    /**
     * Wait few seconds to allow child threads actually start.
     */
    private static long YIELD_TIME = 5000; // 5 seconds

    /**
     * Once <code>arg</code> is ``XXXs'', or ``XXXm'', or ``XXXms'',
     * return the given number of seconds, minutes, or milliseconds
     * correspondingly.
     */
    private static long parseTime(String arg) {
        for (int i = arg.lastIndexOf("ms"); i > -1; )
            return Long.parseLong(arg.substring(0, i));
        for (int i = arg.lastIndexOf("s"); i > -1; )
            return Long.parseLong(arg.substring(0, i)) * 1000;
        for (int i = arg.lastIndexOf("m"); i > -1; )
            return Long.parseLong(arg.substring(0, i)) * 60000;
        throw new IllegalArgumentException(
                "cannot recognize time scale: " + arg);
    }

    /**
     * Re-invoke to <code>run(args,out)</code> to follow JCK style.
     */
    public static void main(String args[]) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    /**
     * JavaTest-like entry: <code>args[]</code> may reset
     * <code>THREADS_EXPECTED</code> and <code>TIMEOUT</code>
     * values.
     */
    public static int run(String args[], PrintStream out) {
        if (args.length > 0)
            THREADS_EXPECTED = Integer.parseInt(args[0]);
        if (args.length > 1)
            TIMEOUT = parseTime(args[1]);
        if (args.length > 2)
            YIELD_TIME = parseTime(args[2]);
        if (args.length > 3)
            DEBUG_MODE = args[3].toLowerCase().startsWith("-v");
        if (args.length > 4) {
            out.println("#");
            out.println("# Too namy command-line arguments!");
            out.println("#");
            return 2;
        }

        if (DEBUG_MODE) {
            out.println(
                    "Start " + THREADS_EXPECTED + " threads, "
                            + "halt after " + TIMEOUT + " milliseconds:");
        }

        Thread thread[] = new Thread[THREADS_EXPECTED];
        for (int i = 0; i < THREADS_EXPECTED; i++)
            try {
                thread[i] = new thread005();
                thread[i].start();
                if (DEBUG_MODE)
                    out.println("Threads started: " + (i + 1));
            } catch (OutOfMemoryError oome) {
                oome.printStackTrace(out);
                out.println("#");
                out.println("# The test have FAILED:");
                out.println("# Only " + i + " threads could start,");
                out.println("# while at least " + THREADS_EXPECTED +
                        " were expected.");
                out.println("#");
                return 2;
            }

        // Actually start:
        GO = true;
        // ...and let them go:
        try {
            doSleep(YIELD_TIME);
        } catch (InterruptedException ie) {
            ie.printStackTrace(out);
            out.println("#");
            out.println("# OOPS! Could not let threads actually start!");
            out.println("#");
            return 2;
        }
        STOP = true;

        if (DEBUG_MODE)
            out.println("The test have PASSED.");
        return 0;
    }

    private static boolean GO = false;
    private static boolean STOP = false;

    /**
     * The thread activity: do nothing special, but do not
     * free CPU time so that the thread's memory could not
     * be moved to swap file.
     */
    public void run() {
        while (!GO && !timeout())
            Thread.yield();
        while (!STOP && !timeout())
            ;
    }

    private static long startTime = System.currentTimeMillis();

    /**
     * Check if timeout for this test is exceeded.
     */
    private boolean timeout() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime > TIMEOUT;
    }

    /**
     * Yield to other threads for the given amount of
     * <code>time</code> (milliseconds).
     */
    private static void doSleep(long time) throws InterruptedException {
        //
        // Since Java 2, the method Thread.sleep() doesn't guarantee
        // to yield to other threads. So, call Object.wait() to yield:
        //
        Object lock = new Object(); // local scope, nobody can notify it
        synchronized (lock) {
            lock.wait(time);
        }
    }
}
