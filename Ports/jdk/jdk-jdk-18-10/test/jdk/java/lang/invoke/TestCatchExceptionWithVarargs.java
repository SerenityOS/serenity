/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8019184
 * @library /test/lib /java/lang/invoke/common
 * @summary MethodHandles.catchException() fails when methods have 8 args + varargs
 * @run main TestCatchExceptionWithVarargs
 */

import test.java.lang.invoke.lib.CodeCacheOverflowProcessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.LinkedList;
import java.util.List;

public class TestCatchExceptionWithVarargs {

    private static final Class<?> CLASS = TestCatchExceptionWithVarargs.class;
    private static final int MAX_MH_ARITY = 254;

    public static MethodHandle target;
    public static MethodHandle handler;

    private static Object firstArg;

    static class MyException extends Exception {
    }

    public static Object target(Object... a) throws Exception {
        if (a[0] != firstArg) {
            throw new AssertionError("first argument different than expected: "
                    + a[0] + " != " + firstArg);
        }
        throw new MyException();
    }

    public static Object handler(Object... a) {
        if (a[0] != firstArg) {
            throw new AssertionError("first argument different than expected: "
                    + a[0] + " != " + firstArg);
        }
        return a[0];
    }

    static {
        try {
            MethodType mtype = MethodType.methodType(Object.class, Object[].class);
            target = MethodHandles.lookup().findStatic(CLASS, "target", mtype);
            handler = MethodHandles.lookup().findStatic(CLASS, "handler", mtype);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        CodeCacheOverflowProcessor
                .runMHTest(TestCatchExceptionWithVarargs::test);
    }

    public static void test() throws Throwable {
        List<Class<?>> ptypes = new LinkedList<>();
        ptypes.add(Object[].class);

        // We use MAX_MH_ARITY - 1 here to account for the Object[] argument.
        for (int i = 1; i < MAX_MH_ARITY - 1; i++) {
            ptypes.add(0, Object.class);

            MethodHandle targetWithArgs = target.asType(
                    MethodType.methodType(Object.class, ptypes));
            MethodHandle handlerWithArgs = handler.asType(
                    MethodType.methodType(Object.class, ptypes));
            handlerWithArgs = MethodHandles.dropArguments(
                    handlerWithArgs, 0, MyException.class);

            MethodHandle gwc1 = MethodHandles.catchException(
                    targetWithArgs, MyException.class, handlerWithArgs);

            // The next line throws an IllegalArgumentException if there is a bug.
            MethodHandle gwc2 = MethodHandles.catchException(
                    gwc1, MyException.class, handlerWithArgs);

            // This is only to verify that the method handles can actually be invoked and do the right thing.
            firstArg = new Object();
            Object o = gwc2.asSpreader(Object[].class, ptypes.size() - 1)
                           .invoke(firstArg, new Object[i]);
            if (o != firstArg) {
                throw new AssertionError("return value different than expected: "
                        + o + " != " + firstArg);
            }
        }
    }
}
