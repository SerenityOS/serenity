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
 * @summary converted from VM testbase nsk/stress/stack/stack011.
 * VM testbase keywords: [stress, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     This test provokes multiple stack overflows in the multiple
 *     threads -- by invoking final static recursive method for the
 *     given fixed depth of recursion (though, for a large depth).
 *     This test measures a number of recursive invocations until
 *     stack overflow, and then tries to provoke similar stack overflows
 *     10 times in each of 10 threads. Each provocation consists of
 *     invoking that recursive method for the given fixed depth
 *     of invocations which is 10 times that depth measured before.
 *     The test is deemed passed, if VM have not crashed, and
 *     if exception other than due to stack overflow was not
 *     thrown.
 * COMMENTS
 *     This test crashes HS versions 2.0, 1.3, 1.4 on Win32 and Solaris
 *     platforms.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack011
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack011 extends Thread {
    final static int THREADS = 10;
    final static int CYCLES = 10;

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        //
        // Measure maximal recursion depth until stack overflow:
        //
        int maxDepth = 0;
        for (int depth = 10; ; depth += 10)
            try {
                recurse(depth);
                maxDepth = depth;
            } catch (StackOverflowError soe) {
                break;
            } catch (OutOfMemoryError oome) {
                break;
            }
        out.println("Max. depth: " + maxDepth);

        //
        // Execute multiple threads repeatedly provoking stack overflows:
        //
        stack011 threads[] = new stack011[THREADS];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new stack011();
            threads[i].depthToTry = 10 * maxDepth;
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
        // Check if unexpected exceptions were not thrown:
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

    int depthToTry = 0;
    Throwable thrown = null;

    public void run() {
        for (int i = 0; i < CYCLES; i++)
            try {
                recurse(depthToTry);
                throw new Exception(
                        "TEST_RFE: no stack overflow thrown" +
                                ", need to try deeper recursion?");

            } catch (StackOverflowError error) {
                // It's OK: stack overflow was expected.
            } catch (OutOfMemoryError oome) {
                // Also OK: recursion may result in memory lack.

            } catch (Throwable throwable) {
                if (throwable instanceof ThreadDeath)
                    throw (ThreadDeath) throwable;
                // It isn't OK!
                thrown = throwable;
                break;
            }
    }

    final static void recurse(int depth) {
        if (depth > 0)
            recurse(depth - 1);
    }
}
