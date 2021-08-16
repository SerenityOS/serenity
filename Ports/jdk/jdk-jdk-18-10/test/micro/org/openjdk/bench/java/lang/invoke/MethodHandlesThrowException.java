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
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses MethodHandles.throwException() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MethodHandlesThrowException {

    /**
     * Implementation notes:
     *   - exceptions have a thorough call back to benchmark instance to prevent elimination (against dumb JITs)
     *   - testing in plain and cached modes
     *   - baselines do the same thing, but in pure Java
     */

    public int flag;
    private MethodHandle mh;
    private MyException cachedException;

    @Setup
    public void setup() {
        flag = 42;
        cachedException = new MyException();
        mh = MethodHandles.throwException(void.class, MyException.class);
    }

    @Benchmark
    public int baselineRaw() {
        try {
            throw new MyException();
        } catch (MyException my) {
            return my.getFlag();
        }
    }

    @Benchmark
    public int baselineRawCached() {
        try {
            throw cachedException;
        } catch (MyException my) {
            return my.getFlag();
        }
    }

    @Benchmark
    public int testInvoke() throws Throwable {
        try {
            mh.invoke(new MyException());
            throw new IllegalStateException("Should throw exception");
        } catch (MyException my) {
            return my.getFlag();
        }
    }

    @Benchmark
    public int testInvokeCached() throws Throwable {
        try {
            mh.invoke(cachedException);
            throw new IllegalStateException("Should throw exception");
        } catch (MyException my) {
            return my.getFlag();
        }
    }

    @Benchmark
    public MethodHandle interCreate() {
        return MethodHandles.throwException(void.class, MyException.class);
    }

    public class MyException extends Exception {

        public int getFlag() {
            return flag;
        }
    }

}
