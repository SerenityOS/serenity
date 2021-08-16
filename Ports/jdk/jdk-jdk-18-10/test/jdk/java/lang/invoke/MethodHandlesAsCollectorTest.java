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
 * @compile MethodHandlesTest.java MethodHandlesAsCollectorTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesAsCollectorTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import static org.junit.Assert.*;

public class MethodHandlesAsCollectorTest extends MethodHandlesTest {

    @Test // SLOW
    public void testAsCollector() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testAsCollector0);
        CodeCacheOverflowProcessor.runMHTest(this::testAsCollector1);
    }

    public void testAsCollector0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("asCollector");
        for (Class<?> argType : new Class<?>[]{Object.class, Integer.class, int.class}) {
            if (verbosity >= 3)
                System.out.println("asCollector "+argType);
            for (int nargs = 0; nargs < 50; nargs++) {
                if (CAN_TEST_LIGHTLY && nargs > 11)  break;
                for (int pos = 0; pos <= nargs; pos++) {
                    if (CAN_TEST_LIGHTLY && pos > 2 && pos < nargs-2)  continue;
                    if (nargs > 10 && pos > 4 && pos < nargs-4 && pos % 10 != 3)
                        continue;
                    testAsCollector(argType, pos, nargs);
                }
            }
        }
    }

    public void testAsCollector(Class<?> argType, int pos, int nargs) throws Throwable {
        countTest();
        // fake up a MH with the same type as the desired adapter:
        MethodHandle fake = varargsArray(nargs);
        fake = changeArgTypes(fake, argType);
        MethodType newType = fake.type();
        Object[] args = randomArgs(newType.parameterArray());
        // here is what should happen:
        Object[] collectedArgs = Arrays.copyOfRange(args, 0, pos+1);
        collectedArgs[pos] = Arrays.copyOfRange(args, pos, args.length);
        // here is the MH which will witness the collected argument tail:
        MethodHandle target = varargsArray(pos+1);
        target = changeArgTypes(target, 0, pos, argType);
        target = changeArgTypes(target, pos, pos+1, Object[].class);
        if (verbosity >= 3)
            System.out.println("collect from "+Arrays.asList(args)+" ["+pos+".."+nargs+"]");
        MethodHandle result = target.asCollector(Object[].class, nargs-pos).asType(newType);
        Object[] returnValue = (Object[]) result.invokeWithArguments(args);
        assertArrayEquals(collectedArgs, returnValue);
    }

    public void testAsCollector1() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("asCollector/pos");
        for (Class<?> argType : new Class<?>[]{Object.class, Integer.class, int.class}) {
            if (verbosity >= 3)
                System.out.println("asCollector/pos "+argType);
            for (int nargs = 0; nargs < 50; nargs++) {
                if (CAN_TEST_LIGHTLY && nargs > 11)  break;
                for (int pos = 0; pos <= nargs; pos++) {
                    if (CAN_TEST_LIGHTLY && pos > 2 && pos < nargs-2)  continue;
                    if (nargs > 10 && pos > 4 && pos < nargs-4 && pos % 10 != 3)
                        continue;
                    for (int coll = 1; coll < nargs - pos; ++coll) {
                        if (coll > 4 && coll != 7 && coll != 11 && coll != 20 && coll < nargs - pos - 4) continue;
                        testAsCollector(argType, pos, coll, nargs);
                    }
                }
            }
        }
    }

    public void testAsCollector(Class<?> argType, int pos, int collect, int nargs) throws Throwable {
        countTest();
        // fake up a MH with the same type as the desired adapter:
        MethodHandle fake = varargsArray(nargs);
        fake = changeArgTypes(fake, argType);
        MethodType newType = fake.type();
        Object[] args = randomArgs(newType.parameterArray());
        // here is what should happen:
        // new arg list has "collect" less arguments, but one extra for collected arguments array
        int collectedLength = nargs-(collect-1);
        Object[] collectedArgs = new Object[collectedLength];
        System.arraycopy(args, 0, collectedArgs, 0, pos);
        collectedArgs[pos] = Arrays.copyOfRange(args, pos, pos+collect);
        System.arraycopy(args, pos+collect, collectedArgs, pos+1, args.length-(pos+collect));
        // here is the MH which will witness the collected argument part (not tail!):
        MethodHandle target = varargsArray(collectedLength);
        target = changeArgTypes(target, 0, pos, argType);
        target = changeArgTypes(target, pos, pos+1, Object[].class);
        target = changeArgTypes(target, pos+1, collectedLength, argType);
        if (verbosity >= 3)
            System.out.println("collect "+collect+" from "+Arrays.asList(args)+" ["+pos+".."+(pos+collect)+"[");
        MethodHandle result = target.asCollector(pos, Object[].class, collect).asType(newType);
        Object[] returnValue = (Object[]) result.invokeWithArguments(args);
        assertArrayEquals(collectedArgs, returnValue);
    }
}
