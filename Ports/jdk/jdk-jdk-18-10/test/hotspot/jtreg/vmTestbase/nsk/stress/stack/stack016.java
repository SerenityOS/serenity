/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM testbase nsk/stress/stack/stack016.
 * VM testbase keywords: [stress, diehard, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     The test provokes second stack overflow from within the
 *     stack overflow handler -- repeatedly multiple times, and
 *     in multiple threads.
 *     This test measures a number of recursive invocations until
 *     stack overflow, and then tries to provoke similar stack overflows
 *     in 10 times in each of 10 threads. Each provocation consists of
 *     invoking that recursive method for the given fixed depth
 *     of invocations which is 10 times that depth measured before,
 *     and then trying to invoke that recursive method once again
 *     from within the catch clause just caught StackOverflowError.
 *     The test is deemed passed, if VM have not crashed, and
 *     if exception other than due to stack overflow was not
 *     thrown.
 * COMMENTS
 *     This test crashes HS versions 2.0, 1.3, and 1.4 on both
 *     Solaris and Win32 platforms.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires (vm.opt.DeoptimizeALot != true & vm.compMode != "Xcomp")
 * @library /vmTestbase
 * @build nsk.share.Terminator
 * @run main/othervm/timeout=900 -Xint -Xss448K nsk.stress.stack.stack016 -eager
 * @run main/othervm/timeout=900 -Xcomp -Xss448K nsk.stress.stack.stack016 -eager
 * @run main/othervm/timeout=900 -Xcomp -XX:-TieredCompilation -Xss448K nsk.stress.stack.stack016 -eager
 */

package nsk.stress.stack;


import nsk.share.Terminator;

import java.io.PrintStream;

public class stack016 extends Thread {
    private final static int THREADS = 10;
    private final static int CYCLES = 10;
    private final static int STEP = 10;
    private final static int RESERVE = 10;
    private final static int PROBES = STEP * RESERVE;

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        verbose = false;
        boolean eager = false;
        for (int i = 0; i < args.length; i++)
            if (args[i].toLowerCase().equals("-verbose"))
                verbose = true;
            else if (args[i].toLowerCase().equals("-eager"))
                eager = true;
        if (!eager)
            Terminator.appoint(Terminator.parseAppointment(args));
        stack016.out = out;
        stack016 test = new stack016();
        return test.doRun();
    }

    private static boolean verbose;
    private static PrintStream out;

    private void display(Object message) {
        if (!verbose)
            return;
        synchronized (out) {
            out.println(message.toString());
        }
    }

    private int doRun() {
        //
        // Measure recursive depth before stack overflow:
        //
        int maxDepth = 0;
        for (depthToTry = 0; ; depthToTry += STEP) {
            try {
                trickyRecurse(depthToTry);
                maxDepth = depthToTry;
            } catch (StackOverflowError | OutOfMemoryError ex) {
                break;
            }
        }
        out.println("Maximal recursion depth: " + maxDepth);

        //
        // Run the tested threads:
        //
        stack016 threads[] = new stack016[THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new stack016();
            threads[i].setName("Thread: " + (i + 1) + "/" + THREADS);
            threads[i].depthToTry = RESERVE * maxDepth;
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].isAlive()) {
                try {
                    threads[i].join();
                } catch (InterruptedException exception) {
                    exception.printStackTrace(out);
                    return 2;
                }
            }
        }

        //
        // Check if unexpected exceptions were thrown:
        //
        int exitCode = 0;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i].thrown != null) {
                threads[i].thrown.printStackTrace(out);
                exitCode = 2;
            }
        }
        if (exitCode != 0)
            out.println("# TEST FAILED");
        return exitCode;
    }

    private int stackTop = 0;
    private int depthToTry = 0;
    private Throwable thrown = null;

    private void trickyRecurse(int depth) {
        stackTop = depthToTry - depth;
        if (depth > 0) {
            try {
                trickyRecurse(depth - 1);
            } catch (Error error) {
                if (!(error instanceof StackOverflowError) &&
                        !(error instanceof OutOfMemoryError))
                    throw error;

                //
                // Provoke more stack overflow,
                // if current stack is deep enough:
                //
                if (depthToTry - depth < stackTop - PROBES)
                    throw error;
                recurse(depthToTry);

                throw new Error("TEST_RFE: try deeper recursion!");
            }
        }
    }

    private static void recurse(int depth) {
        if (depth > 0)
            recurse(depth - 1);
    }

    public void run() {
        String threadName = Thread.currentThread().getName();
        for (int i = 1; i <= CYCLES; i++) {
            try {
                display(threadName + ", iteration: " + i + "/" + CYCLES +
                        ", depthToTry: " + depthToTry);
                trickyRecurse(depthToTry);
                throw new Error(
                        "TEST_BUG: trickyRecursion() must throw an error anyway!");

            } catch (StackOverflowError error) {
                // It's OK: stack overflow was expected.
            } catch (OutOfMemoryError oome) {
                // Also OK, if there is no memory for stack expansion.

            } catch (Throwable throwable) {
                if (throwable instanceof ThreadDeath)
                    throw (ThreadDeath) throwable;
                thrown = throwable;
                break;
            }
        }
    }
}
