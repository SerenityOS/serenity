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
package org.openjdk.bench.jdk.incubator.foreign.points;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.bench.jdk.incubator.foreign.points.support.BBPoint;
import org.openjdk.bench.jdk.incubator.foreign.points.support.JNIPoint;
import org.openjdk.bench.jdk.incubator.foreign.points.support.PanamaPoint;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign", "--enable-native-access=ALL-UNNAMED" })
public class PointsAccess {

    BBPoint BBPoint;
    PanamaPoint panamaPoint;
    JNIPoint JNIPoint;

    @Setup
    public void setup() {
        BBPoint = new BBPoint(0, 0);
        panamaPoint = new PanamaPoint(0, 0);
        JNIPoint = new JNIPoint(0, 0);
    }

    @TearDown
    public void tearDown() {
        JNIPoint.free();
        panamaPoint.close();
    }

    @Benchmark
    public void BB_set() throws Throwable {
        BBPoint.setX(10);
    }

    @Benchmark
    public int BB_get() throws Throwable {
        return BBPoint.getX();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int BB_get_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            sum += BBPoint.getX();
        }
        return sum;
    }

    @Benchmark
    public void jni_set() throws Throwable {
        JNIPoint.setX(10);
    }

    @Benchmark
    public int jni_get() throws Throwable {
        return JNIPoint.getX();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int jni_get_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            sum += JNIPoint.getX();
        }
        return sum;
    }

    @Benchmark
    public void panama_set() throws Throwable {
        panamaPoint.setX(10);
    }

    @Benchmark
    public int panama_get() throws Throwable {
        return panamaPoint.getX();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int panama_get_loop() throws Throwable {
        int sum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            sum += panamaPoint.getX();
        }
        return sum;
    }

}
