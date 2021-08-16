/*
 * Copyright (c) BELLSOFT. All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class SkipIntToLongCast {

    private static final long ARRAYSIZE_L = 40L;

    public int[] intValues;
    public int intValue;

    @Setup
    public void setup() {
        int size = (int)ARRAYSIZE_L;
        intValues = new int[size];
        for (int i = 0; i < size; i++) {
            intValues[i] = i + 1;
        }
    }

    @Benchmark
    public int skipCastTestRight() {
        for (int i = 0; i < ARRAYSIZE_L; i++) {
            if (intValues[i] == ARRAYSIZE_L) {
                return i;
            }
        }
        return 0;
    }

    @Benchmark
    public int skipCastTestLeft() {
        for (int i = 0; i < ARRAYSIZE_L; i++) {
            if (ARRAYSIZE_L == intValues[i]) {
                return i;
            }
        }
        return 0;
    }

    @Benchmark
    public long skipMaskedSmallPositiveCast() {
        int value = intValue;
        return (long)(value & 0x1)    ^ (long)(value & 0x3)    ^ (long)(value & 0x7)    ^ (long)(value & 0xF)   ^
               (long)(value & 0x1F)   ^ (long)(value & 0x3F)   ^ (long)(value & 0x7F)   ^ (long)(value & 0xFF)  ^
               (long)(value & 0x1FF)  ^ (long)(value & 0x3FF)  ^ (long)(value & 0x7FF)  ^ (long)(value & 0xFFF) ^
               (long)(value & 0x1FFF) ^ (long)(value & 0x3FFF) ^ (long)(value & 0x7FFF) ^ (long)(value & 0xFFFF);
    }
}
