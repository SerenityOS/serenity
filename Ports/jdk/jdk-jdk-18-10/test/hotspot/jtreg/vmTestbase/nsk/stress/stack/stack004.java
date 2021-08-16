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
 * @summary converted from VM testbase nsk/stress/stack/stack004.
 * VM testbase keywords: [stress, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     This test provokes multiple stack overflows in the same thread
 *     by invoking final static recursive method for the given fixed
 *     depth of recursion (though, for a large depth).
 *     This test makes measures a number of recursive invocations
 *     before 1st StackOverflowError, and then tries to reproduce
 *     such StackOverflowError 100 times -- each time by trying to
 *     invoke the same recursive method for the given fixed depth
 *     of invocations (which is twice that depth just measured).
 *     The test is deemed passed, if VM have not crashed.
 * COMMENTS
 *     This test crashes all HS versions (2.0, 1.3, 1.4) on all
 *     platforms (Win32, Solaris, Linux) in all execution modes
 *     (-Xint, -Xmixed, -Xcomp) in 100% of executions in which
 *     I had tryied it.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack004
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack004 {
    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        stack004 test = new stack004();
        int exitCode = test.doRun(args, out);
        return exitCode;
    }

    public int doRun(String args[], PrintStream out) {
        int depth;
        for (depth = 100; ; depth += 100)
            try {
                recurse(depth);
            } catch (StackOverflowError soe) {
                break;
            } catch (OutOfMemoryError oome) {
                break;
            }
        out.println("Max. depth: " + depth);
        for (int i = 0; i < 100; i++)
            try {
                recurse(2 * depth);
                out.println("?");
            } catch (StackOverflowError soe) {
                // OK.
            } catch (OutOfMemoryError oome) {
                // Also OK.
            }
        return 0;
    }

    final static void recurse(int depth) {
        if (depth > 0)
            recurse(depth - 1);
    }
}
