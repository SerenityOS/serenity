/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM testbase nsk/stress/stack/stack017.
 * VM testbase keywords: [stress, diehard, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     The test invokes infinitely recursive method from within stack
 *     overflow handler  -- repeatedly multiple times, and in multiple
 *     threads.
 *     The test is deemed passed, if VM have not crashed, and
 *     if exception other than due to stack overflow was not
 *     thrown.
 * COMMENTS
 *     This test crashes HS versions 2.0, 1.3, and 1.4 on both
 *     Solaris and Win32 platforms.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires (vm.opt.DeoptimizeALot != true & vm.compMode != "Xcomp" & vm.pageSize == 4096)
 * @library /vmTestbase
 * @build nsk.share.Terminator
 * @run main/othervm/timeout=900 -Xss220K nsk.stress.stack.stack017 -eager
 */

package nsk.stress.stack;


import nsk.share.Terminator;

import java.io.PrintStream;

public class stack017 extends Thread {
    private final static int THREADS = 10;
    private final static int CYCLES = 10;
    private final static int PROBES = 100;

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
        stack017.out = out;
        stack017 test = new stack017();
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

    private static int depthToTry;

    private int doRun() {
        //
        // Measure recursive depth before stack overflow:
        //
        try {
            recurse(0);
        } catch (StackOverflowError soe) {
        } catch (OutOfMemoryError oome) {
        }
        out.println("Maximal recursion depth: " + maxDepth);
        depthToTry = maxDepth;

        //
        // Run the tested threads:
        //
        stack017 threads[] = new stack017[THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new stack017();
            threads[i].setName("Thread: " + (i + 1) + "/" + THREADS);
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++)
            if (threads[i].isAlive())
                try {
                    threads[i].join();
                } catch (InterruptedException exception) {
                    exception.printStackTrace(out);
                    return 2;
                }

        //
        // Check if unexpected exceptions were thrown:
        //
        int exitCode = 0;
        for (int i = 0; i < threads.length; i++)
            if (threads[i].thrown != null) {
                threads[i].thrown.printStackTrace(out);
                exitCode = 2;
            }

        if (exitCode != 0)
            out.println("# TEST FAILED");
        return exitCode;
    }

    private int maxDepth = 0;

    private void recurse(int depth) {
        maxDepth = depth;
        recurse(depth + 1);
    }

    private void trickyRecurse(int depth) {
        try {
            maxDepth = depth;
            trickyRecurse(depth + 1);
        } catch (Error error) {
            if (!(error instanceof StackOverflowError) &&
                    !(error instanceof OutOfMemoryError))
                throw error;

            //
            // Stack problem caught: provoke it again,
            // if current stack is enough deep:
            //
            if (depth < depthToTry - PROBES)
                throw error;
            recurse(depth + 1);
        }
    }

    private Throwable thrown = null;

    public void run() {
        String threadName = Thread.currentThread().getName();
        for (int i = 1; i <= CYCLES; i++)
            try {
                display(threadName + ", iteration: " + i + "/" + CYCLES);
                trickyRecurse(0);
                throw new Exception(
                        "TEST_BUG: stack overflow was expected!");

            } catch (StackOverflowError oome) {
                // It's OK: stack overflow was expected.
            } catch (OutOfMemoryError oome) {
                // Also OK, if there is no memory for stack expansion.

            } catch (Throwable throwable) {
                if (throwable instanceof ThreadDeath)
                    throw (ThreadDeath) throwable;
                // It isn't OK!
                thrown = throwable;
                break;
            }
    }
}
