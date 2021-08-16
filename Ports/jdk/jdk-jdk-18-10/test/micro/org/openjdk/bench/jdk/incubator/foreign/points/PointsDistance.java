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

import org.openjdk.bench.jdk.incubator.foreign.points.support.BBPoint;
import org.openjdk.bench.jdk.incubator.foreign.points.support.JNIPoint;
import org.openjdk.bench.jdk.incubator.foreign.points.support.PanamaPoint;
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

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign", "--enable-native-access=ALL-UNNAMED" })
public class PointsDistance {

    BBPoint jniP1;
    BBPoint jniP2;

    JNIPoint nativeP1;
    JNIPoint nativeP2;

    PanamaPoint panamaPointP1;
    PanamaPoint panamaPointP2;

    @Setup
    public void setup() {
        jniP1 = new BBPoint(0, 0);
        jniP2 = new BBPoint(1, 1);

        nativeP1 = new JNIPoint(0, 0);
        nativeP2 = new JNIPoint(1, 1);

        panamaPointP1 = new PanamaPoint(0, 0);
        panamaPointP2 = new PanamaPoint(1, 1);
    }

    @TearDown
    public void tearDown() {
        nativeP1.free();
        nativeP2.free();

        panamaPointP1.close();
        panamaPointP2.close();
    }

    @Benchmark
    public double jni_ByteBuffer() throws Throwable {
        return jniP1.distanceTo(jniP2);
    }

    @Benchmark
    public double jni_long() throws Throwable {
        return nativeP1.distanceTo(nativeP2);
    }

    @Benchmark
    public double panama_MemorySegment() throws Throwable {
        return panamaPointP1.distanceTo(panamaPointP2);
    }

    @Benchmark
    public double panama_MemoryAddress() throws Throwable {
        return panamaPointP1.distanceToPtrs(panamaPointP2);
    }

}
