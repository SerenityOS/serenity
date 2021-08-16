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
package org.openjdk.bench.vm.lambda.chain;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Chain of (capture + invocation) microbenchmark.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ChainAnonymCap1 extends ChainBase {

    public Level start1;
    public Level start2;
    public Level start4;
    public Level start8;

    @Setup
    public void init() {
        start1 = get1("capture me");
        start2 = get2("capture me");
        start4 = get4("capture me");
        start8 = get8("capture me");
    }

    @Benchmark
    @OperationsPerInvocation(1)
    public void call1(Blackhole bh) {
        process(bh, start1);
    }

    @Benchmark
    @OperationsPerInvocation(2)
    public void call2(Blackhole bh) {
        process(bh, start2);
    }

    @Benchmark
    @OperationsPerInvocation(4)
    public void call4(Blackhole bh) {
        process(bh, start4);
    }

    @Benchmark
    @OperationsPerInvocation(8)
    public void call8(Blackhole bh) {
        process(bh, start8);
    }

    public static TopLevel get0(String v) {
        return new TopLevel() {
            @Override
            public String getImage() {
                return "GOT: "+v;
            }
        };
    }

    public static Level get1(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get0(v);
            }
        };
    }

    public static Level get2(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get1(v);
            }
        };
    }

    public static Level get3(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get2(v);
            }
        };
    }

    public static Level get4(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get3(v);
            }
        };
    }

    public static Level get5(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get4(v);
            }
        };
    }

    public static Level get6(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get5(v);
            }
        };
    }

    public static Level get7(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get6(v);
            }
        };
    }

    public static Level get8(String v) {
        return new Level() {
            @Override
            public Level up() {
                return get7(v);
            }
        };
    }

}
