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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring the gain of removing array bound checks in various cases
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ArrayBoundCheckRemoval {

    private int[] a;
    private int j, u, v;

    @Setup
    public void setup() {
        a = new int[200];
    }

    @Benchmark
    public int[] testForLoopAccess() throws Exception {
        int[] a = this.a;
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        return a;
    }

    @Benchmark
    public int[] testBubblesortStripped() throws Exception {
        int[] a = this.a;
        int limit = a.length;
        int st = -1;

        while (st < limit) {
            st++;
            limit--;
            for (j = st; j < limit; j++) {
                u = a[j];
                v = a[j + 1];
            }
        }
        return a;
    }

    @Benchmark
    public int[] testBubblesort() throws Exception {
        int[] a = this.a;
        int j1;
        int limit = a.length;
        int st = -1;
        while (st < limit) {
            boolean flipped = false;
            st++;
            limit--;
            for (j1 = st; j1 < limit; j1++) {
                if (a[j1] > a[j1 + 1]) {
                    int T = a[j1];
                    a[j1] = a[j1 + 1];
                    a[j1 + 1] = T;
                    flipped = true;
                }
            }
            if (!flipped) {
                return a;
            }
            flipped = false;
            for (j1 = limit; --j1 >= st; ) {
                if (a[j1] > a[j1 + 1]) {
                    int T = a[j1];
                    a[j1] = a[j1 + 1];
                    a[j1 + 1] = T;
                    flipped = true;
                }
            }
            if (!flipped) {
                return a;
            }
        }
        return a;
    }

}
