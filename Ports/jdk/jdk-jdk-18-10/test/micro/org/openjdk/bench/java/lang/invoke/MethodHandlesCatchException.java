/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang.invoke;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses MethodHandles.catchException() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MethodHandlesCatchException {

    /**
     * Implementation notes:
     *   - emulating instance method handles because of current issue with instance methods
     *   - exception is cached to harness the MH code, not exception instantiation
     *   - measuring two modes:
     *     a) always going through normal code path;
     *     b) always going through exceptional one
     *   - baselines do the same thing in pure Java
     */

    private static final MyException MY_EXCEPTION = new MyException();

    private int i1;
    private int i2;

    private static MethodHandle methNormal;
    private static MethodHandle methExceptional;

    @Setup
    public void setup() throws Throwable {
        MethodHandle bodyNormal = MethodHandles.lookup()
            .findStatic(MethodHandlesCatchException.class, "doWorkNormal",
                MethodType.methodType(void.class, MethodHandlesCatchException.class));
        MethodHandle bodyExceptional = MethodHandles.lookup()
            .findStatic(MethodHandlesCatchException.class, "doWorkExceptional",
                MethodType.methodType(void.class, MethodHandlesCatchException.class));
        MethodHandle fallback = MethodHandles.lookup()
            .findStatic(MethodHandlesCatchException.class, "fallback",
                MethodType.methodType(void.class, MyException.class, MethodHandlesCatchException.class));

        methNormal = MethodHandles.catchException(bodyNormal, MyException.class, fallback);
        methExceptional = MethodHandles.catchException(bodyExceptional, MyException.class, fallback);
    }

    @Benchmark
    public void baselineNormal() {
        try {
            doWorkNormal(this);
        } catch (MyException e) {
            fallback(e, this);
        }
    }

    @Benchmark
    public void baselineExceptional() {
        try {
            doWorkExceptional(this);
        } catch (MyException e) {
            fallback(e, this);
        }
    }

    @Benchmark
    public void testNormal() throws Throwable {
        methNormal.invokeExact(this);
    }

    @Benchmark
    public void testExceptional() throws Throwable {
        methExceptional.invokeExact(this);
    }


    public static void doWorkNormal(MethodHandlesCatchException inst) throws MyException {
        inst.i1++;
    }

    public static void doWorkExceptional(MethodHandlesCatchException inst) throws MyException {
        inst.i1++;
        throw MY_EXCEPTION;
    }

    public static void fallback(MyException ex, MethodHandlesCatchException inst) {
        inst.i2++;
    }

    public static class MyException extends Exception {

    }

}
