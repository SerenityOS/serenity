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
package org.openjdk.bench.vm.lambda.capture;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * evaluates anonymous classes creation (for comparison with lambda)
 *
 * Naming convention:
 *  - inner_N      - lambda captures N local variables
 *  - inner_this_N - lambda captures 'this' and N local variables
 *
 * @author Sergey Kuksenko (sergey.kuksenko@oracle.com)
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class Capture0 {

    /*
     *  volatile is used in order to avoid constant propagation
     *  That is why the bench is relevant only on TSO platforms (x86, SPARC).
     *  ARM & PPC - TBD when necessary.
     */
    public volatile int wh0 = 0;
    public volatile int wh1 = 1;
    public volatile int wh2 = 2;
    public volatile int wh3 = 3;
    public volatile int wh4 = 4;
    public volatile int wh5 = 5;
    public volatile int wh6 = 6;
    public volatile int wh7 = 7;

    public String fortyTwo = "42";

    @Benchmark()
    public FunctionalInterface0 inner0(){
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return "42";
            }
        };
    }

    @Benchmark()
    public FunctionalInterface0 inner_1(){
        final int l0 = wh0;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return "42" + l0;
            }
        };
    }


    @Benchmark()
    public FunctionalInterface0 inner_2(){
        final int l0 = wh0;
        final int l1 = wh1;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return "42" + l0 + " " + l1;
            }
        };
    }

    @Benchmark()
    public FunctionalInterface0 inner_4(){
        final int l0 = wh0;
        final int l1 = wh1;
        final int l2 = wh2;
        final int l3 = wh3;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return "42" + l0 + " " + l1 + " " + l2 + " " + l3;
            }
        };
    }


    @Benchmark()
    public FunctionalInterface0 inner_8(){
        final int l0 = wh0;
        final int l1 = wh1;
        final int l2 = wh2;
        final int l3 = wh3;
        final int l4 = wh4;
        final int l5 = wh5;
        final int l6 = wh6;
        final int l7 = wh7;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return "42" + l0 + " " + l1 + " " + l2 + " " + l3 +
                        " " + l4 + " " + l5 + " " + l6 + " " + l7;
            }
        };
    }

    @Benchmark()
    public FunctionalInterface0 inner_this_0(){
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return fortyTwo;
            }
        };
    }

    @Benchmark()
    public FunctionalInterface0 inner_this_1(){
        final int l0 = wh0;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return fortyTwo + l0;
            }
        };
    }


    @Benchmark()
    public FunctionalInterface0 inner_this_2(){
        final int l0 = wh0;
        final int l1 = wh1;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return fortyTwo + l0 + " " + l1;
            }
        };
    }

    @Benchmark()
    public FunctionalInterface0 inner_this_4(){
        final int l0 = wh0;
        final int l1 = wh1;
        final int l2 = wh2;
        final int l3 = wh3;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return fortyTwo + l0 + " " + l1 + " " + l2 + " " + l3;
            }
        };
    }


    @Benchmark()
    public FunctionalInterface0 inner_this_8(){
        final int l0 = wh0;
        final int l1 = wh1;
        final int l2 = wh2;
        final int l3 = wh3;
        final int l4 = wh4;
        final int l5 = wh5;
        final int l6 = wh6;
        final int l7 = wh7;
        return new FunctionalInterface0() {
            @Override
            public Object foo() {
                return fortyTwo + l0 + " " + l1 + " " + l2 + " " + l3 +
                        " " + l4 + " " + l5 + " " + l6 + " " + l7;
            }
        };
    }

//--------------------lambda part

    @Benchmark()
    public FunctionalInterface0 lambda_00(){
        return () -> "42";
    }

    @Benchmark()
    public FunctionalInterface0 lambda_01(){
        int l0 = wh0;
        return () -> "42" + l0;
    }


    @Benchmark()
    public FunctionalInterface0 lambda_02(){
        int l0 = wh0;
        int l1 = wh1;
        return () -> "42" + l0 + " " + l1;
    }

    @Benchmark()
    public FunctionalInterface0 lambda_04(){
        int l0 = wh0;
        int l1 = wh1;
        int l2 = wh2;
        int l3 = wh3;
        return () -> "42" + l0 + " " + l1 + " " + l2 + " " + l3;
    }


    @Benchmark()
    public FunctionalInterface0 lambda_08(){
        int l0 = wh0;
        int l1 = wh1;
        int l2 = wh2;
        int l3 = wh3;
        int l4 = wh4;
        int l5 = wh5;
        int l6 = wh6;
        int l7 = wh7;
        return () -> "42" + l0 + " " + l1 + " " + l2 + " " + l3 + " " + l4 + " " + l5 + " " + l6 + " " + l7;
    }

    @Benchmark()
    public FunctionalInterface0 lambda_this_0(){
        return () -> fortyTwo;
    }

    @Benchmark()
    public FunctionalInterface0 lambda_this_1(){
        int l0 = wh0;
        return () -> fortyTwo + l0;
    }


    @Benchmark()
    public FunctionalInterface0 lambda_this_2(){
        int l0 = wh0;
        int l1 = wh1;
        return () -> fortyTwo + l0 + " " + l1;
    }

    @Benchmark()
    public FunctionalInterface0 lambda_this_4(){
        int l0 = wh0;
        int l1 = wh1;
        int l2 = wh2;
        int l3 = wh3;
        return () -> fortyTwo + l0 + " " + l1 + " " + l2 + " " + l3;
    }


    @Benchmark()
    public FunctionalInterface0 lambda_this_8(){
        int l0 = wh0;
        int l1 = wh1;
        int l2 = wh2;
        int l3 = wh3;
        int l4 = wh4;
        int l5 = wh5;
        int l6 = wh6;
        int l7 = wh7;
        return () -> fortyTwo + l0 + " " + l1 + " " + l2 + " " + l3 +
                " " + l4 + " " + l5 + " " + l6 + " " + l7;
    }



}
