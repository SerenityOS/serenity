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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * Tests how well the JVM can remove stores after allocation of objects.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class PostAllocationStores {

    /** Tests allocation with explicit stores of null/zero to all fields. */
    @Benchmark
    public Object testAllocationWithNullStores() throws Exception {
        return new TestWithNullStores();
    }

    /**
     * Tests allocation with explicit stores of non-null/non-zero to all fields. This test exists as a complement to the
     * one above.
     */
    @Benchmark
    public Object testAllocationWithNonNullStores() throws Exception {
        return new TestWithNonNullStores();
    }

    /** Tests allocation with explicit stores of null/zero to all fields, where all fields are volatile. */
    @Benchmark
    public Object testAllocationWithNullVolatileStores() throws Exception {
        return new TestWithNullVolatileStores();
    }

    /** Tests allocation without any explicit stores to any fields. */
    @Benchmark
    public Object testAllocationWithoutStores() throws Exception {
        return new TestWithoutStores();
    }

    static class TestWithNullStores {
        Object objectField1;
        Object objectField2;
        Object objectField3;
        int intField1;
        int intField2;
        long longField1;

        public TestWithNullStores() {
            objectField1 = null;
            objectField2 = null;
            objectField3 = null;
            intField1 = 0;
            intField2 = 0;
            longField1 = 0L;
        }
    }

    static class TestWithNonNullStores {
        Object objectField1;
        Object objectField2;
        Object objectField3;
        int intField1;
        int intField2;
        long longField1;

        public TestWithNonNullStores() {
            objectField1 = this;
            objectField2 = this;
            objectField3 = this;
            intField1 = 4;
            intField2 = 7;
            longField1 = 2L;
        }
    }

    static class TestWithNullVolatileStores {
        volatile Object objectField1;
        volatile Object objectField2;
        volatile Object objectField3;
        volatile int intField1;
        volatile int intField2;
        volatile long longField1;

        public TestWithNullVolatileStores() {
            objectField1 = null;
            objectField2 = null;
            objectField3 = null;
            intField1 = 0;
            intField2 = 0;
            longField1 = 0L;
        }
    }

    static class TestWithoutStores {
        Object objectField1;
        Object objectField2;
        Object objectField3;
        int intField1;
        int intField2;
        long longField1;
    }

}
