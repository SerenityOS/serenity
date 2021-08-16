/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.text;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class DefFormatterBench {

    @Param({"1.23", "1.49", "1.80", "1.7", "0.0", "-1.49", "-1.50", "9999.9123", "1.494", "1.495", "1.03", "25.996", "-25.996"})
    public double value;

    private DefNumerFormat dnf = new DefNumerFormat();

    @Benchmark
    public void testDefNumberFormatter(final Blackhole blackhole) {
        blackhole.consume(this.dnf.format(this.value));
    }

    public static void main(String... args) throws Exception {
        Options opts = new OptionsBuilder().include(DefFormatterBench.class.getSimpleName()).shouldDoGC(true).build();
        new Runner(opts).run();
    }

    private static class DefNumerFormat {

        private final NumberFormat n;

        public DefNumerFormat() {
            this.n = NumberFormat.getInstance(Locale.ENGLISH);
            this.n.setMaximumFractionDigits(2);
            this.n.setMinimumFractionDigits(2);
            this.n.setGroupingUsed(false);
        }

        public String format(final double d) {
            return this.n.format(d);
        }
    }
}
