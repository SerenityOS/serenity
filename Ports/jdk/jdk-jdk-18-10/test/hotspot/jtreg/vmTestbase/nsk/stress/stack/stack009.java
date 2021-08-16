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
 * @summary converted from VM testbase nsk/stress/stack/stack009.
 * VM testbase keywords: [stress, quick, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     The test provokes second stack overflow from within the
 *     stack overflow handler.
 *     This test measures a number of recursive invocations until
 *     StackOverflowError, and then tries to make an invocation
 *     for the fixed invocations depth from within the "catch"
 *     block just caught the 1st stack overflow. The depth of new
 *     invocations is 10 times that depth seen at the 1st stack
 *     overflow; so that another stack overflow occurs.
 *     The test is deemed passed, if VM have not crashed, and
 *     if there is no exception thrown other than due to stack
 *     overflow.
 * COMMENTS
 *     This test crashes HS versions 2.0, 1.3, and 1.4 on Win32
 *     and Solaris platforms.
 *     See the bug:
 *     4366625 (P4/S4) multiple stack overflow causes HS crash
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack009
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack009 {
    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        for (int depth = 100; ; depth += 100)
            try {
                recurse(depth);
            } catch (Error error1) {
                if (!(error1 instanceof StackOverflowError) &&
                        !(error1 instanceof OutOfMemoryError))
                    throw error1;

                out.println("Max. depth: " + depth);

                try {
                    recurse(10 * depth);
                    out.println("?");
                } catch (Error error2) {
                    if (!(error2 instanceof StackOverflowError) &&
                            !(error2 instanceof OutOfMemoryError))
                        throw error2;

                    // Stack overflow is OK here.
                }

                break;
            }
        return 0;
    }

    static void recurse(int depth) {
        if (depth > 0)
            recurse(depth - 1);
    }
}
