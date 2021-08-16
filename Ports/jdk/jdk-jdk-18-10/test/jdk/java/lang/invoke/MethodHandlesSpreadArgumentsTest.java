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
 * @compile MethodHandlesTest.java MethodHandlesSpreadArgumentsTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesSpreadArgumentsTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MethodHandlesSpreadArgumentsTest extends MethodHandlesTest {

    @Test // SLOW
    public void testSpreadArguments() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testSpreadArguments0);
        CodeCacheOverflowProcessor.runMHTest(this::testSpreadArguments1);
    }

    public void testSpreadArguments0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("spreadArguments");
        for (Class<?> argType : new Class<?>[]{Object.class, Integer.class, int.class}) {
            if (verbosity >= 3)
                System.out.println("spreadArguments "+argType);
            Class<?> arrayType = java.lang.reflect.Array.newInstance(argType, 0).getClass();
            for (int nargs = 0; nargs < 50; nargs++) {
                if (CAN_TEST_LIGHTLY && nargs > 11)  break;
                for (int pos = 0; pos <= nargs; pos++) {
                    if (CAN_TEST_LIGHTLY && pos > 2 && pos < nargs-2)  continue;
                    if (nargs > 10 && pos > 4 && pos < nargs-4 && pos % 10 != 3)
                        continue;
                    testSpreadArguments(argType, arrayType, pos, nargs);
                }
            }
        }
    }

    public void testSpreadArguments(Class<?> argType, Class<?> arrayType, int pos, int nargs) throws Throwable {
        countTest();
        MethodHandle target2 = varargsArray(arrayType, nargs);
        MethodHandle target = target2.asType(target2.type().generic());
        if (verbosity >= 3)
            System.out.println("spread into "+target2+" ["+pos+".."+nargs+"]");
        Object[] args = randomArgs(target2.type().parameterArray());
        // make sure the target does what we think it does:
        checkTarget(argType, pos, nargs, target, args);
        List<Class<?>> newParams = new ArrayList<>(target2.type().parameterList());
        {   // modify newParams in place
            List<Class<?>> spreadParams = newParams.subList(pos, nargs);
            spreadParams.clear(); spreadParams.add(arrayType);
        }
        MethodType newType = MethodType.methodType(arrayType, newParams);
        MethodHandle result = target2.asSpreader(arrayType, nargs-pos);
        assert(result.type() == newType) : Arrays.asList(result, newType);
        result = result.asType(newType.generic());
        Object returnValue;
        if (pos == 0) {
            Object args2 = ValueConversions.changeArrayType(arrayType, Arrays.copyOfRange(args, pos, args.length));
            returnValue = result.invokeExact(args2);
        } else {
            Object[] args1 = Arrays.copyOfRange(args, 0, pos+1);
            args1[pos] = ValueConversions.changeArrayType(arrayType, Arrays.copyOfRange(args, pos, args.length));
            returnValue = result.invokeWithArguments(args1);
        }
        checkReturnValue(argType, args, result, returnValue);
    }

    public void testSpreadArguments1() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("spreadArguments/pos");
        for (Class<?> argType : new Class<?>[]{Object.class, Integer.class, int.class}) {
            if (verbosity >= 3)
                System.out.println("spreadArguments "+argType);
            Class<?> arrayType = java.lang.reflect.Array.newInstance(argType, 0).getClass();
            for (int nargs = 0; nargs < 50; nargs++) {
                if (CAN_TEST_LIGHTLY && nargs > 11)  break;
                for (int pos = 0; pos <= nargs; pos++) {
                    if (CAN_TEST_LIGHTLY && pos > 2 && pos < nargs-2)  continue;
                    if (nargs > 10 && pos > 4 && pos < nargs-4 && pos % 10 != 3)
                        continue;
                    for (int spr = 1; spr < nargs - pos; ++spr) {
                        if (spr > 4 && spr != 7 && spr != 11 && spr != 20 && spr < nargs - pos - 4) continue;
                        testSpreadArguments(argType, arrayType, pos, spr, nargs);
                    }
                }
            }
        }
    }

    public void testSpreadArguments(Class<?> argType, Class<?> arrayType,
                                    int pos, int spread, int nargs) throws Throwable {
        countTest();
        MethodHandle target2 = varargsArray(arrayType, nargs);
        MethodHandle target = target2.asType(target2.type().generic());
        if (verbosity >= 3)
            System.out.println("spread into " + target2 + " [" + pos + ".." + (pos + spread) + "[");
        Object[] args = randomArgs(target2.type().parameterArray());
        // make sure the target does what we think it does:
        checkTarget(argType, pos, nargs, target, args);
        List<Class<?>> newParams = new ArrayList<>(target2.type().parameterList());
        {   // modify newParams in place
            List<Class<?>> spreadParams = newParams.subList(pos, pos + spread);
            spreadParams.clear();
            spreadParams.add(arrayType);
        }
        MethodType newType = MethodType.methodType(arrayType, newParams);
        MethodHandle result = target2.asSpreader(pos, arrayType, spread);
        assert (result.type() == newType) : Arrays.asList(result, newType);
        result = result.asType(newType.generic());
        // args1 has nargs-spread entries, plus one for the to-be-spread array
        int args1Length = nargs - (spread - 1);
        Object[] args1 = new Object[args1Length];
        System.arraycopy(args, 0, args1, 0, pos);
        args1[pos] = ValueConversions.changeArrayType(arrayType, Arrays.copyOfRange(args, pos, pos + spread));
        System.arraycopy(args, pos + spread, args1, pos + 1, nargs - spread - pos);
        Object returnValue = result.invokeWithArguments(args1);
        checkReturnValue(argType, args, result, returnValue);
    }

    private static void checkTarget(Class<?> argType, int pos, int nargs,
                                    MethodHandle target, Object[] args) throws Throwable {
        if (pos == 0 && nargs < 5 && !argType.isPrimitive()) {
            Object[] check = (Object[]) target.invokeWithArguments(args);
            assertArrayEquals(args, check);
            switch (nargs) {
                case 0:
                    check = (Object[]) (Object) target.invokeExact();
                    assertArrayEquals(args, check);
                    break;
                case 1:
                    check = (Object[]) (Object) target.invokeExact(args[0]);
                    assertArrayEquals(args, check);
                    break;
                case 2:
                    check = (Object[]) (Object) target.invokeExact(args[0], args[1]);
                    assertArrayEquals(args, check);
                    break;
            }
        }
    }

    private static void checkReturnValue(Class<?> argType, Object[] args, MethodHandle result, Object returnValue) {
        String argstr = Arrays.toString(args);
        if (!argType.isPrimitive()) {
            Object[] rv = (Object[]) returnValue;
            String rvs = Arrays.toString(rv);
            if (!Arrays.equals(args, rv)) {
                System.out.println("method:   "+result);
                System.out.println("expected: "+argstr);
                System.out.println("returned: "+rvs);
                assertArrayEquals(args, rv);
            }
        } else if (argType == int.class) {
            String rvs = Arrays.toString((int[]) returnValue);
            if (!argstr.equals(rvs)) {
                System.out.println("method:   "+result);
                System.out.println("expected: "+argstr);
                System.out.println("returned: "+rvs);
                assertEquals(argstr, rvs);
            }
        } else if (argType == long.class) {
            String rvs = Arrays.toString((long[]) returnValue);
            if (!argstr.equals(rvs)) {
                System.out.println("method:   "+result);
                System.out.println("expected: "+argstr);
                System.out.println("returned: "+rvs);
                assertEquals(argstr, rvs);
            }
        } else {
            // cannot test...
        }
    }

}
