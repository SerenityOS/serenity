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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Tests for removal of redundant stores. It's crucial for the tests to be valid that inlining and allocation is
 * performed as described in this file.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StoreAfterStore {
    public int s1 = 1, s2 = 2, s3 = 3, s4 = 4, s5 = 5, s6 = 6, s7 = 7, s8 = 8;

    /**
     * Test removal of redundant zero stores following an object allocation.
     */
    @Benchmark
    public AllocAndZeroStoreHelper testAllocAndZeroStore() throws Exception {
        return new AllocAndZeroStoreHelper(s1, s2, s3, s4, s5, s6, s7, s8);
    }

    /**
     * Test removal of stores followed by stores to the same memory location.
     */
    @Benchmark
    public StoreAndStoreHelper testStoreAndStore() throws Exception {
        return new StoreAndStoreHelper(s1, s2, s3, s4, s5, s6, s7, s8);
    }


    /**
     * Helper for alloc followed by zero store testing.
     */
    static class AllocAndZeroStoreHelper {
        public volatile int vf1, vf2, vf3, vf4, vf5, vf6, vf7, vf8;

        public static int s1, s2, s3, s4, s5, s6, s7, s8;

        private AllocAndZeroStoreHelper() {
            this.vf1 = 0;
            this.vf2 = 0;
            this.vf3 = 0;
            this.vf4 = 0;
            this.vf5 = 0;
            this.vf6 = 0;
            this.vf7 = 0;
            this.vf8 = 0;
        }

        private AllocAndZeroStoreHelper(int l1, int l2, int l3, int l4, int l5, int l6, int l7, int l8) {
            this(); // Redundant initialization to zero here

            // dummy stores
            s1 = l1;
            s2 = l2;
            s3 = l3;
            s4 = l4;
            s5 = l5;
            s6 = l6;
            s7 = l7;
            s8 = l8;
        }

    }

    /**
     * Helper for store made redundant by subsequent store testing.
     */
    static class StoreAndStoreHelper {
        public volatile int vf1, vf2, vf3, vf4, vf5, vf6, vf7, vf8;

        private StoreAndStoreHelper() {
            this.vf1 = -1;
            this.vf2 = -1;
            this.vf3 = -1;
            this.vf4 = -1;
            this.vf5 = -1;
            this.vf6 = -1;
            this.vf7 = -1;
            this.vf8 = -1;
        }

        private StoreAndStoreHelper(int l1, int l2, int l3, int l4, int l5, int l6, int l7, int l8) {
            this(); // Initialize all to -1 here, redundant wrt to the below stores

            this.vf1 = l1;
            this.vf2 = l2;
            this.vf3 = l3;
            this.vf4 = l4;
            this.vf5 = l5;
            this.vf6 = l6;
            this.vf7 = l7;
            this.vf8 = l8;
        }

    }
}
