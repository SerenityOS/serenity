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

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses MethodHandles.lookup/publicLookup() acquiring performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class LookupAcquire {

    /*
        Implementation notes:
            - this test assesses acquiring lookup object only
            - baseline includes returning cached lookup object, i.e. measures infra overheads
            - additional baseline includes allocating object to understand Lookup instantiation costs
            - cached instance is static, because that provides (unbeatably) best performance
     */

    public static MethodHandles.Lookup cached;

    @Setup
    public void setup() {
        cached = MethodHandles.lookup();
    }

    @Benchmark
    public MethodHandles.Lookup baselineCached() throws Exception {
        return cached;
    }

    @Benchmark
    public MyLookup baselineNew() throws Exception {
        return new MyLookup(Object.class, 1);
    }

    @Benchmark
    public MethodHandles.Lookup testPublicLookup() throws Exception {
        return MethodHandles.publicLookup();
    }

    @Benchmark
    public MethodHandles.Lookup testLookup() throws Exception {
        return MethodHandles.lookup();
    }

    /**
     * Dummy Lookup-looking class.
     * Lookup is final, and all constructors are private.
     * This class mocks the hotpath.
     */
    private static class MyLookup {
        private final Class<?> klass;
        private final int mode;

        public MyLookup(Class<?> klass, int i) {
            this.klass = klass;
            this.mode = i;
        }
    }
}
