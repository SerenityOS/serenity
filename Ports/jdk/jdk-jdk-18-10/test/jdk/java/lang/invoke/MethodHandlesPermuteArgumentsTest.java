/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary unit tests for java.lang.invoke.MethodHandles
 * @library /test/lib /java/lang/invoke/common
 * @compile MethodHandlesTest.java MethodHandlesPermuteArgumentsTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesPermuteArgumentsTest
 */


package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MethodHandlesPermuteArgumentsTest extends test.java.lang.invoke.MethodHandlesTest {

    @Test // SLOW
    public void testPermuteArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testPermuteArguments0);
    }

    public void testPermuteArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("permuteArguments");
        testPermuteArguments(4, Integer.class, 2, long.class, 6);
        if (CAN_TEST_LIGHTLY)  return;
        testPermuteArguments(4, Integer.class, 2, String.class, 0);
        testPermuteArguments(6, Integer.class, 0, null, 30);

        testBadReorderArrayLength();
        testBadReorderIndex();
        testReturnTypeMismatch();
        testReorderTypeMismatch();

        testPermuteWithEmpty();
    }

    public void testPermuteArguments(int max, Class<?> type1, int t2c, Class<?> type2, int dilution) throws Throwable {
        if (verbosity >= 2)
            System.out.println("permuteArguments "+max+"*"+type1.getName()
                    +(t2c==0?"":"/"+t2c+"*"+type2.getName())
                    +(dilution > 0 ? " with dilution "+dilution : ""));
        int t2pos = t2c == 0 ? 0 : 1;
        for (int inargs = t2pos+1; inargs <= max; inargs++) {
            Class<?>[] types = new Class<?>[inargs];
            Arrays.fill(types, type1);
            if (t2c != 0) {
                // Fill in a middle range with type2:
                Arrays.fill(types, t2pos, Math.min(t2pos+t2c, inargs), type2);
            }
            Object[] args = randomArgs(types);
            int numcases = 1;
            for (int outargs = 0; outargs <= max; outargs++) {
                if (outargs - inargs >= MAX_ARG_INCREASE)  continue;
                int casStep = dilution + 1;
                // Avoid some common factors:
                while ((casStep > 2 && casStep % 2 == 0 && inargs % 2 == 0) ||
                       (casStep > 3 && casStep % 3 == 0 && inargs % 3 == 0))
                    casStep++;
                testPermuteArguments(args, types, outargs, numcases, casStep);
                numcases *= inargs;
                if (CAN_TEST_LIGHTLY && outargs < max-2)  continue;
                if (dilution > 10 && outargs >= 4) {
                    if (CAN_TEST_LIGHTLY)  continue;
                    int[] reorder = new int[outargs];
                    // Do some special patterns, which we probably missed.
                    // Replication of a single argument or argument pair.
                    for (int i = 0; i < inargs; i++) {
                        Arrays.fill(reorder, i);
                        testPermuteArguments(args, types, reorder);
                        for (int d = 1; d <= 2; d++) {
                            if (i + d >= inargs)  continue;
                            for (int j = 1; j < outargs; j += 2)
                                reorder[j] += 1;
                            testPermuteArguments(args, types, reorder);
                            testPermuteArguments(args, types, reverse(reorder));
                        }
                    }
                    // Repetition of a sequence of 3 or more arguments.
                    for (int i = 1; i < inargs; i++) {
                        for (int len = 3; len <= inargs; len++) {
                            for (int j = 0; j < outargs; j++)
                                reorder[j] = (i + (j % len)) % inargs;
                            testPermuteArguments(args, types, reorder);
                            testPermuteArguments(args, types, reverse(reorder));
                        }
                    }
                }
            }
        }
    }

    public void testPermuteArguments(Object[] args, Class<?>[] types,
                                     int outargs, int numcases, int casStep) throws Throwable {
        int inargs = args.length;
        int[] reorder = new int[outargs];
        for (int cas = 0; cas < numcases; cas += casStep) {
            for (int i = 0, c = cas; i < outargs; i++) {
                reorder[i] = c % inargs;
                c /= inargs;
            }
            if (CAN_TEST_LIGHTLY && outargs >= 3 &&
               (reorder[0] == reorder[1] || reorder[1] == reorder[2]))
                   continue;
            testPermuteArguments(args, types, reorder);
        }
    }

    static int[] reverse(int[] reorder) {
        reorder = reorder.clone();
        for (int i = 0, imax = reorder.length / 2; i < imax; i++) {
            int j = reorder.length - 1 - i;
            int tem = reorder[i];
            reorder[i] = reorder[j];
            reorder[j] = tem;
        }
        return reorder;
    }

    void testPermuteArguments(Object[] args, Class<?>[] types, int[] reorder) throws Throwable {
        countTest();
        if (args == null && types == null) {
            int max = 0;
            for (int j : reorder) {
                if (max < j)  max = j;
            }
            args = randomArgs(max+1, Integer.class);
        }
        if (args == null) {
            args = randomArgs(types);
        }
        if (types == null) {
            types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++)
                types[i] = args[i].getClass();
        }
        int inargs = args.length, outargs = reorder.length;
        assertTrue(inargs == types.length);
        if (verbosity >= 3)
            System.out.println("permuteArguments "+Arrays.toString(reorder));
        Object[] permArgs = new Object[outargs];
        Class<?>[] permTypes = new Class<?>[outargs];
        for (int i = 0; i < outargs; i++) {
            permArgs[i] = args[reorder[i]];
            permTypes[i] = types[reorder[i]];
        }
        if (verbosity >= 4) {
            System.out.println("in args:   "+Arrays.asList(args));
            System.out.println("out args:  "+Arrays.asList(permArgs));
            System.out.println("in types:  "+Arrays.asList(types));
            System.out.println("out types: "+Arrays.asList(permTypes));
        }
        MethodType inType  = MethodType.methodType(Object.class, types);
        MethodType outType = MethodType.methodType(Object.class, permTypes);
        MethodHandle target = varargsList(outargs).asType(outType);
        MethodHandle newTarget = MethodHandles.permuteArguments(target, inType, reorder);
        if (verbosity >= 5)  System.out.println("newTarget = "+newTarget);
        Object result = newTarget.invokeWithArguments(args);
        Object expected = Arrays.asList(permArgs);
        if (!expected.equals(result)) {
            System.out.println("*** failed permuteArguments "+Arrays.toString(reorder)+
                               " types="+Arrays.asList(types));
            System.out.println("in args:   "+Arrays.asList(args));
            System.out.println("out args:  "+expected);
            System.out.println("bad args:  "+result);
        }
        assertEquals(expected, result);
    }

    public void testBadReorderArrayLength() throws Throwable {
        MethodHandle mh = MethodHandles.empty(MethodType.methodType(void.class, int.class, int.class, String.class));
        MethodType newType = MethodType.methodType(void.class, int.class, String.class);
        assertThrows(() -> MethodHandles.permuteArguments(mh, newType, 0, 1),
                IllegalArgumentException.class, ".*old type parameter count and reorder array length do not match.*");
    }

    public void testBadReorderIndex() throws Throwable {
        MethodHandle mh = MethodHandles.empty(MethodType.methodType(void.class, int.class, int.class, String.class));
        MethodType newType = MethodType.methodType(void.class, int.class, String.class);
        assertThrows(() -> MethodHandles.permuteArguments(mh, newType, 0, 0, 2),
                IllegalArgumentException.class, ".*index is out of bounds for new type.*");
        assertThrows(() -> MethodHandles.permuteArguments(mh, newType, 0, 0, -1),
                IllegalArgumentException.class, ".*index is out of bounds for new type.*");
    }

    public void testReturnTypeMismatch() throws Throwable {
        MethodHandle mh = MethodHandles.empty(MethodType.methodType(void.class, int.class, int.class, String.class));
        MethodType newType = MethodType.methodType(int.class, int.class, String.class);
        assertThrows(() -> MethodHandles.permuteArguments(mh, newType, 0, 0, 1),
                IllegalArgumentException.class, ".*return types do not match.*");
    }

    public void testReorderTypeMismatch() throws Throwable {
        MethodHandle mh = MethodHandles.empty(MethodType.methodType(void.class, int.class, int.class, String.class));
        MethodType newType = MethodType.methodType(void.class, double.class, String.class);
        assertThrows(() -> MethodHandles.permuteArguments(mh, newType, 0, 0, 1),
                IllegalArgumentException.class, ".*parameter types do not match after reorder.*");
    }

    // for JDK-8255531
    private void testPermuteWithEmpty() {
        MethodHandle mh = MethodHandles.empty(MethodType.methodType(void.class, int.class, int.class));
        MethodHandles.permuteArguments(mh, MethodType.methodType(void.class, int.class), 0, 0);
    }

    private interface RunnableX {
        void run() throws Throwable;
    }

    private static void assertThrows(RunnableX r, Class<?> exceptionClass, String messagePattern) throws Throwable {
        try {
            r.run();
            fail("Exception expected");
        } catch (Throwable e) {
            if (exceptionClass.isInstance(e)) {
                assertMatches(e.getMessage(), messagePattern);
            } else {
                throw e;
            }
        }
    }

    private static void assertMatches(String str, String pattern) {
        if (!str.matches(pattern)) {
            throw new AssertionError("'" + str + "' did not match the pattern '" + pattern + "'.");
        }
    }
}
