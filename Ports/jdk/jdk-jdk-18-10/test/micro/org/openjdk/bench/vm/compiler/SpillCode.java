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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Test spill code generation.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class SpillCode {

    @Param("10")
    private int iterations;

    private int dummy;

    private int doSomeCalcsInLargeBlockWithInts(int start, int iter) {

        int a1, a2, a3, a4;
        int b1, b2, b3, b4;
        int c1, c2, c3, c4;
        int d1, d2, d3, d4;
        int e1, e2, e3, e4;
        int f1, f2, f3, f4;
        int g1, g2, g3, g4;
        int h1, h2, h3, h4;

        // a1 = b1 = c1 = d1 = e1 = f1 = g1 = h1 = (start % 2);
        a2 = b2 = c2 = d2 = e2 = f2 = g2 = h2 = (start % 2);
        a3 = b3 = c3 = d3 = e3 = f3 = g3 = h3 = (start % 4);
        a4 = b4 = c4 = d4 = e4 = f4 = g4 = h4 = (start % 8);

        for (int i = 0; i < iter; i++) {

            // for each section, only x1 needs to survive.

            // a1 = a2 = a3 = a4 = (start + a1) % 2;
            a1 = start;
            a1 = a1 + a2 + a3 + a4;
            a2 = a1 + a2 + a3 + a4;
            a3 = a1 + a2 + a3 + a4;
            a4 = a1 + a2 + a3 + a4;
            a1 = a1 + a2 + a3 + a4;
            a2 = a1 + a2 + a3 + a4;
            a3 = a1 + a2 + a3 + a4;
            a4 = a1 + a2 + a3 + a4;
            a1 = a1 + a2 + a3 + a4;
            a2 = a1 + a2 + a3 + a4;
            a3 = a1 + a2 + a3 + a4;
            a4 = a1 + a2 + a3 + a4;

            // b1 = b2 = b3 = b4 = (a4 + b1) % 2;
            b1 = a4;
            b1 = b1 + b2 + b3 + b4;
            b2 = b1 + b2 + b3 + b4;
            b3 = b1 + b2 + b3 + b4;
            b4 = b1 + b2 + b3 + b4;
            b1 = b1 + b2 + b3 + b4;
            b2 = b1 + b2 + b3 + b4;
            b3 = b1 + b2 + b3 + b4;
            b4 = b1 + b2 + b3 + b4;
            b1 = b1 + b2 + b3 + b4;
            b2 = b1 + b2 + b3 + b4;
            b3 = b1 + b2 + b3 + b4;
            b4 = b1 + b2 + b3 + b4;

            // c1 = c2 = c3 = c4 = (b4 + c1) % 2;
            c1 = b4;
            c1 = c1 + c2 + c3 + c4;
            c2 = c1 + c2 + c3 + c4;
            c3 = c1 + c2 + c3 + c4;
            c4 = c1 + c2 + c3 + c4;
            c1 = c1 + c2 + c3 + c4;
            c2 = c1 + c2 + c3 + c4;
            c3 = c1 + c2 + c3 + c4;
            c4 = c1 + c2 + c3 + c4;
            c1 = c1 + c2 + c3 + c4;
            c2 = c1 + c2 + c3 + c4;
            c3 = c1 + c2 + c3 + c4;
            c4 = c1 + c2 + c3 + c4;

            // d1 = d2 = d3 = d4 = (c4 + d1) % 2;
            d1 = c4;
            d1 = d1 + d2 + d3 + d4;
            d2 = d1 + d2 + d3 + d4;
            d3 = d1 + d2 + d3 + d4;
            d4 = d1 + d2 + d3 + d4;
            d1 = d1 + d2 + d3 + d4;
            d2 = d1 + d2 + d3 + d4;
            d3 = d1 + d2 + d3 + d4;
            d4 = d1 + d2 + d3 + d4;
            d1 = d1 + d2 + d3 + d4;
            d2 = d1 + d2 + d3 + d4;
            d3 = d1 + d2 + d3 + d4;
            d4 = d1 + d2 + d3 + d4;

            // e1 = e2 = e3 = e4 = (d4 + e1) % 2;
            e1 = d4;
            e1 = e1 + e2 + e3 + e4;
            e2 = e1 + e2 + e3 + e4;
            e3 = e1 + e2 + e3 + e4;
            e4 = e1 + e2 + e3 + e4;
            e1 = e1 + e2 + e3 + e4;
            e2 = e1 + e2 + e3 + e4;
            e3 = e1 + e2 + e3 + e4;
            e4 = e1 + e2 + e3 + e4;
            e1 = e1 + e2 + e3 + e4;
            e2 = e1 + e2 + e3 + e4;
            e3 = e1 + e2 + e3 + e4;
            e4 = e1 + e2 + e3 + e4;

            // f1 = f2 = f3 = f4 = (e4 + f1) % 2;
            f1 = e4;
            f1 = f1 + f2 + f3 + f4;
            f2 = f1 + f2 + f3 + f4;
            f3 = f1 + f2 + f3 + f4;
            f4 = f1 + f2 + f3 + f4;
            f1 = f1 + f2 + f3 + f4;
            f2 = f1 + f2 + f3 + f4;
            f3 = f1 + f2 + f3 + f4;
            f4 = f1 + f2 + f3 + f4;
            f1 = f1 + f2 + f3 + f4;
            f2 = f1 + f2 + f3 + f4;
            f3 = f1 + f2 + f3 + f4;
            f4 = f1 + f2 + f3 + f4;

            // g1 = g2 = g3 = g4 = (f4 + g1) % 2;
            g1 = f4;
            g1 = g1 + g2 + g3 + g4;
            g2 = g1 + g2 + g3 + g4;
            g3 = g1 + g2 + g3 + g4;
            g4 = g1 + g2 + g3 + g4;
            g1 = g1 + g2 + g3 + g4;
            g2 = g1 + g2 + g3 + g4;
            g3 = g1 + g2 + g3 + g4;
            g4 = g1 + g2 + g3 + g4;
            g1 = g1 + g2 + g3 + g4;
            g2 = g1 + g2 + g3 + g4;
            g3 = g1 + g2 + g3 + g4;
            g4 = g1 + g2 + g3 + g4;

            // h1 = h2 = h3 = h4 = (g4 + h1) % 2;
            h1 = g4;
            h1 = h1 + h2 + h3 + h4;
            h2 = h1 + h2 + h3 + h4;
            h3 = h1 + h2 + h3 + h4;
            h4 = h1 + h2 + h3 + h4;
            h1 = h1 + h2 + h3 + h4;
            h2 = h1 + h2 + h3 + h4;
            h3 = h1 + h2 + h3 + h4;
            h4 = h1 + h2 + h3 + h4;
            h1 = h1 + h2 + h3 + h4;
            h2 = h1 + h2 + h3 + h4;
            h3 = h1 + h2 + h3 + h4;
            h4 = h1 + h2 + h3 + h4;

            start = h4;
        }

        return start;
    }

    /**
     * The test runs a loop with many local variables. The issue it reproduces is that if handled wrong, too many variables
     * are put on and referenced on stack. The number of iterations is taken from global variable, to prevent static loop
     * unrolling. Many of the variables used in the larger loop are local inside the block and do dnot need to survive from
     * one iteration to the next.
     */
    @Benchmark
    public int testSpillForManyInts() throws Exception {
        return doSomeCalcsInLargeBlockWithInts(dummy, iterations);
    }
}
