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
 * @summary converted from VM testbase nsk/stress/stack/stack001.
 * VM testbase keywords: [stress, quick, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     Provoke StackOverflowError by infinite recursion in Java method,
 *     intercept the exception try to make one more invocation.
 * COMMENTS
 *     Kestrel for Solaris_JDK_1.3-b10 crashes while trying to execute
 *     this test with Client HS VM.
 *     See lots of bugs concerning similar failures:
 *     Evaluated:
 *     4217960 [native stack overflow bug] reflection test causes crash
 *     Accepted:
 *     4285716 native stack overflow causes crash on Solaris
 *     4281578 Second stack overflow crashes HotSpot VM
 *     Closed (duplicate):
 *     4027933     Native stack overflows not detected or handled correctly
 *     4134353     (hpi) sysThreadCheckStack is a no-op on win32
 *     4185411     Various crashes when using recursive reflection.
 *     4167055     infinite recursion in FindClass
 *     4222359     Infinite recursion crashes jvm
 *     Closed (will not fix):
 *     4231968 StackOverflowError in a native method causes Segmentation Fault
 *     4254634     println() while catching StackOverflowError causes hotspot VM crash
 *     4302288 the second stack overflow causes Classic VM to exit on win32
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack001
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack001 {
    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        stack001 test = new stack001();
        test.recurse(0);
        out.println("Maximal depth: " + test.maxdepth);
        return 0;
    }

    private int maxdepth;

    private void recurse(int depth) {
        maxdepth = depth;
        try {
            recurse(depth + 1);
        } catch (Error error) {
            if (!(error instanceof StackOverflowError) &&
                    !(error instanceof OutOfMemoryError))
                throw error;

            if (maxdepth == depth)
                recurse(depth + 1);
        }
    }
}
