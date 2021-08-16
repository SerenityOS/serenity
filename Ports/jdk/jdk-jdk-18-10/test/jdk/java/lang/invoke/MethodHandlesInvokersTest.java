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
 * @compile MethodHandlesTest.java MethodHandlesInvokersTest.java remote/RemoteExample.java
 * @run junit/othervm/timeout=2500 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:-VerifyDependencies
 *                                 -esa
 *                                 test.java.lang.invoke.MethodHandlesInvokersTest
 */

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class MethodHandlesInvokersTest extends MethodHandlesTest {

    @Test  // SLOW
    public void testInvokers() throws Throwable {
        CodeCacheOverflowProcessor.runMHTest(this::testInvokers0);
    }

    public void testInvokers0() throws Throwable {
        if (CAN_SKIP_WORKING)  return;
        startTest("exactInvoker, genericInvoker, varargsInvoker, dynamicInvoker");
        // exactInvoker, genericInvoker, varargsInvoker[0..N], dynamicInvoker
        Set<MethodType> done = new HashSet<>();
        for (int i = 0; i <= 6; i++) {
            if (CAN_TEST_LIGHTLY && i > 3)  break;
            MethodType gtype = MethodType.genericMethodType(i);
            for (Class<?> argType : new Class<?>[]{Object.class, Integer.class, int.class}) {
                for (int j = -1; j < i; j++) {
                    MethodType type = gtype;
                    if (j < 0)
                        type = type.changeReturnType(argType);
                    else if (argType == void.class)
                        continue;
                    else
                        type = type.changeParameterType(j, argType);
                    if (done.add(type))
                        testInvokersWithCatch(type);
                    MethodType vtype = type.changeReturnType(void.class);
                    if (done.add(vtype))
                        testInvokersWithCatch(vtype);
                }
            }
        }
    }

    public void testInvokersWithCatch(MethodType type) throws Throwable {
        try {
            testInvokers(type);
        } catch (Throwable ex) {
            System.out.println("*** testInvokers on "+type+" => ");
            ex.printStackTrace(System.out);
        }
    }

    public void testInvokers(MethodType type) throws Throwable {
        if (verbosity >= 3)
            System.out.println("test invokers for "+type);
        int nargs = type.parameterCount();
        boolean testRetCode = type.returnType() != void.class;
        MethodHandle target = PRIVATE.findStatic(MethodHandlesTest.class, "invokee",
                                                 MethodType.genericMethodType(0, true));
        assertTrue(target.isVarargsCollector());
        target = target.asType(type);
        Object[] args = randomArgs(type.parameterArray());
        List<Object> targetPlusArgs = new ArrayList<>(Arrays.asList(args));
        targetPlusArgs.add(0, target);
        int code = (Integer) invokee(args);
        Object log = logEntry("invokee", args);
        assertEquals(log.hashCode(), code);
        assertCalled("invokee", args);
        MethodHandle inv;
        Object result;
        // exact invoker
        countTest();
        calledLog.clear();
        inv = MethodHandles.exactInvoker(type);
        result = inv.invokeWithArguments(targetPlusArgs);
        if (testRetCode)  assertEquals(code, result);
        assertCalled("invokee", args);
        // generic invoker
        countTest();
        inv = MethodHandles.invoker(type);
        if (nargs <= 3 && type == type.generic()) {
            calledLog.clear();
            switch (nargs) {
            case 0:
                result = inv.invokeExact(target);
                break;
            case 1:
                result = inv.invokeExact(target, args[0]);
                break;
            case 2:
                result = inv.invokeExact(target, args[0], args[1]);
                break;
            case 3:
                result = inv.invokeExact(target, args[0], args[1], args[2]);
                break;
            }
            if (testRetCode)  assertEquals(code, result);
            assertCalled("invokee", args);
        }
        calledLog.clear();
        result = inv.invokeWithArguments(targetPlusArgs);
        if (testRetCode)  assertEquals(code, result);
        assertCalled("invokee", args);
        // varargs invoker #0
        calledLog.clear();
        inv = MethodHandles.spreadInvoker(type, 0);
        if (type.returnType() == Object.class) {
            result = inv.invokeExact(target, args);
        } else if (type.returnType() == void.class) {
            result = null; inv.invokeExact(target, args);
        } else {
            result = inv.invokeWithArguments(target, (Object) args);
        }
        if (testRetCode)  assertEquals(code, result);
        assertCalled("invokee", args);
        if (nargs >= 1 && type == type.generic()) {
            // varargs invoker #1
            calledLog.clear();
            inv = MethodHandles.spreadInvoker(type, 1);
            result = inv.invokeExact(target, args[0], Arrays.copyOfRange(args, 1, nargs));
            if (testRetCode)  assertEquals(code, result);
            assertCalled("invokee", args);
        }
        if (nargs >= 2 && type == type.generic()) {
            // varargs invoker #2
            calledLog.clear();
            inv = MethodHandles.spreadInvoker(type, 2);
            result = inv.invokeExact(target, args[0], args[1], Arrays.copyOfRange(args, 2, nargs));
            if (testRetCode)  assertEquals(code, result);
            assertCalled("invokee", args);
        }
        if (nargs >= 3 && type == type.generic()) {
            // varargs invoker #3
            calledLog.clear();
            inv = MethodHandles.spreadInvoker(type, 3);
            result = inv.invokeExact(target, args[0], args[1], args[2], Arrays.copyOfRange(args, 3, nargs));
            if (testRetCode)  assertEquals(code, result);
            assertCalled("invokee", args);
        }
        for (int k = 0; k <= nargs; k++) {
            // varargs invoker #0..N
            if (CAN_TEST_LIGHTLY && (k > 1 || k < nargs - 1))  continue;
            countTest();
            calledLog.clear();
            inv = MethodHandles.spreadInvoker(type, k);
            MethodType expType = (type.dropParameterTypes(k, nargs)
                                 .appendParameterTypes(Object[].class)
                                 .insertParameterTypes(0, MethodHandle.class));
            assertEquals(expType, inv.type());
            List<Object> targetPlusVarArgs = new ArrayList<>(targetPlusArgs);
            List<Object> tailList = targetPlusVarArgs.subList(1+k, 1+nargs);
            Object[] tail = tailList.toArray();
            tailList.clear(); tailList.add(tail);
            result = inv.invokeWithArguments(targetPlusVarArgs);
            if (testRetCode)  assertEquals(code, result);
            assertCalled("invokee", args);
        }

        // dynamic invoker
        countTest();
        CallSite site = new MutableCallSite(type);
        inv = site.dynamicInvoker();

        // see if we get the result of the original target:
        try {
            result = inv.invokeWithArguments(args);
            assertTrue("should not reach here", false);
        } catch (IllegalStateException ex) {
            String msg = ex.getMessage();
            assertTrue(msg, msg.contains("site"));
        }

        // set new target after invoker is created, to make sure we track target
        site.setTarget(target);
        calledLog.clear();
        result = inv.invokeWithArguments(args);
        if (testRetCode)  assertEquals(code, result);
        assertCalled("invokee", args);
    }
}
