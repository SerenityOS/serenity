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
package org.openjdk.bench.vm.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Tests throwing exceptions.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Throw {

    public static boolean alwaysTrue = true;
    private static Object nullObject = null;
    public Object useObject = new Object();

    @Benchmark
    public void throwSyncException(Blackhole bh) {
        try {
            throwingMethod();
        } catch (Exception ex) {
            bh.consume(useObject);
        }
    }

    @Benchmark
    public void throwASyncException(Blackhole bh) {
        try {
            throwNullpointer();
        } catch (Exception ex) {
            bh.consume(useObject);
        }
    }

    @Benchmark
    public void throwSyncExceptionUseException(Blackhole bh) {
        try {
            throwingMethod();
        } catch (Exception ex) {
            bh.consume(ex);
        }
    }

    @Benchmark
    public void throwSyncExceptionUseMessage(Blackhole bh) {
        try {
            throwingMethod();
        } catch (Exception ex) {
            bh.consume(ex.getMessage());
        }
    }

    @Benchmark
    public void throwSyncExceptionUseStacktrace(Blackhole bh) {
        try {
            throwingMethod();
        } catch (Exception ex) {
            bh.consume(ex.getStackTrace());
        }
    }

    @Benchmark
    public void throwWith16Frames(Blackhole bh) {
        try {
            throwingMethod(16);
        } catch (Exception ex) {
            bh.consume(useObject);
        }
    }

    @Benchmark
    public void throwWith32Frames(Blackhole bh) {
        try {
            throwingMethod(32);
        } catch (Exception ex) {
            bh.consume(useObject);
        }
    }

    @Benchmark
    public void throwWith64Frames(Blackhole bh) {
        try {
            throwingMethod(64);
        } catch (Exception ex) {
            bh.consume(useObject);
        }
    }

    public void throwingMethod() throws Exception {
        if (alwaysTrue) {
            throw new Exception();
        }
    }

    public void throwingMethod(int i) throws Exception {
        if (i == 0) {
            throw new Exception();
        }
        throwingMethod(i - 1);
    }

    public void throwNullpointer() {
        nullObject.hashCode();
    }
}
