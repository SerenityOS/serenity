/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160717
 * @run main/othervm -ea -esa -Djava.lang.invoke.MethodHandle.COMPILE_THRESHOLD=-1 test.java.lang.invoke.LoopCombinatorLongSignatureTest
 * @run main/othervm -ea -esa test.java.lang.invoke.LoopCombinatorLongSignatureTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * If a loop with an excessive amount of clauses is created, so that the number of parameters to the resulting loop
 * handle exceeds the allowed maximum, an IAE must be signalled. The test is run first in LambdaForm interpretation mode
 * and then in default mode, wherein bytecode generation falls back to LFI mode due to excessively long methods.
 * <p>
 * By default, the test run only checks whether loop handle construction succeeds and fails. If executing the generated
 * loops is desired, this should be indicated by setting the {@code java.lang.invoke.LoopCombinatorLongSignatureTest.RUN}
 * environment variable to {@code true}. This is disabled by default as it considerably increases the time needed to run
 * the test.
 */
public class LoopCombinatorLongSignatureTest {

    static final MethodHandle INIT = MethodHandles.constant(int.class, 0);
    static final MethodHandle STEP = MethodHandles.identity(int.class);
    static final MethodHandle PRED_F = MethodHandles.constant(boolean.class, false);
    static final MethodHandle PRED_T = MethodHandles.constant(boolean.class, true);
    static final MethodHandle FINI = MethodHandles.identity(int.class);

    static final int ARG_LIMIT = 254; // for internal reasons, this is the maximum allowed number of arguments

    public static void main(String[] args) {
        boolean run = Boolean.parseBoolean(
                System.getProperty("java.lang.invoke.LoopCombinatorLongSignatureTest.RUN", "false"));
        for (int loopArgs = 0; loopArgs < 2; ++loopArgs) {
            testLongSignature(loopArgs, false, run);
            testLongSignature(loopArgs, true, run);
        }
    }

    static void testLongSignature(int loopArgs, boolean excessive, boolean run) {
        int nClauses = ARG_LIMIT - loopArgs + (excessive ? 1 : 0);

        System.out.print((excessive ? "(EXCESSIVE)" : "(LONG     )") + " arguments: " + loopArgs + ", clauses: " + nClauses + " -> ");

        // extend init to denote what arguments the loop should accept
        Class<?>[] argTypes = new Class<?>[loopArgs];
        Arrays.fill(argTypes, int.class);
        MethodHandle init = MethodHandles.dropArguments(INIT, 0, argTypes);

        // build clauses
        MethodHandle[][] clauses = new MethodHandle[nClauses][];
        MethodHandle[] clause = {init, STEP, PRED_T, FINI};
        MethodHandle[] fclause = {init, STEP, PRED_F, FINI};
        Arrays.fill(clauses, clause);
        clauses[nClauses - 1] = fclause; // make the last clause terminate the loop

        try {
            MethodHandle loop = MethodHandles.loop(clauses);
            if (excessive) {
                throw new AssertionError("loop construction should have failed");
            } else if (run) {
                int r;
                if (loopArgs == 0) {
                    r = (int) loop.invoke();
                } else {
                    Object[] args = new Object[loopArgs];
                    Arrays.fill(args, 0);
                    r = (int) loop.invokeWithArguments(args);
                }
                System.out.println("SUCCEEDED (OK) -> " + r);
            } else {
                System.out.println("SUCCEEDED (OK)");
            }
        } catch (IllegalArgumentException iae) {
            if (excessive) {
                System.out.println("FAILED    (OK)");
            } else {
                iae.printStackTrace(System.out);
                throw new AssertionError("loop construction should not have failed (see above)");
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            throw new AssertionError("unexpected failure (see above)");
        }
    }

}
