/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/* @test
 * @bug 8164102
 * @run main/othervm -ea -esa test.java.lang.invoke.CountedLoopIterationCountsTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class CountedLoopIterationCountsTest {

    public static void main(String[] args) throws Throwable {
        run(1, -10, 0);
        run(1, 0, 0);
        run(Integer.MAX_VALUE - 1, Integer.MIN_VALUE + 10, 0);
        run(Integer.MIN_VALUE, Integer.MIN_VALUE + 4, 4);
        run(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1, 1);
        run(Integer.MAX_VALUE - 1, 0, 0);
        run(Integer.MAX_VALUE - 1, 10, 0);
        run(Integer.MAX_VALUE - 1, -10, 0);
        run(Integer.MAX_VALUE, Integer.MIN_VALUE + 10, 0);
        run(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 1);
        run(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);

        if (failed) {
            throw new AssertionError("one or more tests failed");
        }
    }

    static boolean failed = false;

    private static void run(int start, int end, int expectedIterations) throws Throwable {
        System.out.println("run from " + start + " to " + end);
        MethodHandle loop = MethodHandles.countedLoop(
                MethodHandles.constant(int.class, start), // iterate from given start (inclusive) ...
                MethodHandles.constant(int.class, end),   // ... to given end (exclusive)
                MH_m1,                                    // initialise loop variable to -1
                MH_step);                                 // increment loop counter by one in each iteration
        // The loop variable's value, and hence the loop result, will be "number of iterations" minus one.
        int r = (int) loop.invoke();
        if (r + 1 != expectedIterations) {
            System.out.println("expected " + expectedIterations + " iterations, but got " + r);
            failed = true;
        }
    }

    static int step(int stepCount, int counter) {
        return stepCount + 1;
    }

    static final MethodHandle MH_m1;
    static final MethodHandle MH_step;
    static {
        try {
            MH_m1 = MethodHandles.constant(int.class, -1);
            MH_step = MethodHandles.lookup().findStatic(CountedLoopIterationCountsTest.class, "step",
                    MethodType.methodType(int.class, int.class, int.class));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

}
