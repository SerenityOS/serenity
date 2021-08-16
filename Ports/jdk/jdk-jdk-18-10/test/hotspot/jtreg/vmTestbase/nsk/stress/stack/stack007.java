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
 * @summary converted from VM testbase nsk/stress/stack/stack007.
 * VM testbase keywords: [stress, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     This test provokes multiple stack overflows in the same thread
 *     by invoking synchronized virtual recursive method for the given
 *     fixed depth of recursion (though, for a large depth).
 *     This test makes measures a number of recursive invocations
 *     before 1st StackOverflowError, and then tries to reproduce
 *     such StackOverflowError 10000 times -- each time by trying to
 *     invoke the same recursive method for the given fixed depth
 *     of invocations (which is 10 times that depth just measured).
 *     The test is deemed passed, if VM have not crashed.
 * COMMENTS
 *     This test crashes HS versions 1.3 and 1.4 on Win32, Solaris,
 *     and Linux platforms in all execution modes. However, it passes
 *     against HS 2.0 on Win32 platform.
 *     See also the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack007
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack007 implements stack007i {
    final static int ITERATIONS = 1000;
    final static int INCREMENT = 100;

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        stack007i test = new stack007();
        int depth;
        for (depth = 100; ; depth += INCREMENT)
            try {
                test.recurse(depth);
            } catch (StackOverflowError soe) {
                break;
            } catch (OutOfMemoryError oome) {
                break;
            }
        out.println("Max. depth: " + depth);
        for (int i = 0; i < ITERATIONS; i++)
            try {
                test.recurse(10 * depth);
                out.println("?");
            } catch (StackOverflowError soe) {
                // OK.
            } catch (OutOfMemoryError oome) {
                // Also OK.
            }
        return 0;
    }

    public synchronized void recurse(int depth) {
        if (depth > 0)
            recurse(depth - 1);
    }
}

interface stack007i {
    void recurse(int depth);
}
