/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.reflect;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring java.lang.reflect.Method.invoke speed.
 * <p/>
 * TODO: Add tests for virtual and interface methods.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodInvoke {

    private Method staticMeth_0;
    private Method staticMeth_6ref;
    private Method staticMeth_6prim;

    private Object[] args_0;
    private Object[] args_6ref;
    private Object[] args_6prim;

    @Setup
    public void setup() {
        args_0 = new Object[]{};
        args_6ref = new Object[]{ new Object(), new Object(),
                new Object(), new Object(), new Object(), new Object()};
        args_6prim = new Object[]{
                1, 5L,
                5.6d, 23.11f,
                Boolean.TRUE, 'd'
        };

        staticMeth_0 = getMethodWithName("staticMethodWithoutParams");
        staticMeth_6ref = getMethodWithName("staticMethodWithSixObjectParams");
        staticMeth_6prim = getMethodWithName("staticMethodWithSixPrimitiveParams");
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void staticMethodWithoutParams() {
        // intentionally left blank
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void staticMethodWithSixObjectParams(Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
        // intentionally left blank
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void staticMethodWithSixPrimitiveParams(int i, long j, double d, float f, boolean z, char c) {
        // intentionally left blank
    }

    /* inner method to get the method to invoke. */

    private static Method getMethodWithName(String methodName) {
        Method[] methodArray = MethodInvoke.class.getMethods();
        for (Method m : methodArray) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    @Benchmark
    public void invokeWithoutParams() throws InvocationTargetException, IllegalAccessException {
        staticMeth_0.invoke(null, args_0);
    }

    @Benchmark
    public void invokeWithSixObjectParams() throws Exception {
        staticMeth_6ref.invoke(null, args_6ref);
    }

    @Benchmark
    public void invokeWithSixPrimitiveParams() throws Exception {
        staticMeth_6prim.invoke(null, args_6prim);
    }

}
