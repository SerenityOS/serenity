/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Arm Limited. All rights reserved.
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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;
import java.util.Arrays;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ArrayFill {
    @Param("65536") private int size;

    private byte[] ba;
    private short[] sa;
    private int[] ia;

    @Setup
    public void setup() {
        ba = new byte[size];
        sa = new short[size];
        ia = new int[size];
    }

    @Benchmark
    public void fillByteArray() {
        for (int i = 0; i < size; i++) {
            ba[i] = (byte) 123;
        }
    }

    @Benchmark
    public void fillShortArray() {
        for (int i = 0; i < size; i++) {
            sa[i] = (short) 12345;
        }
    }

    @Benchmark
    public void fillIntArray() {
        for (int i = 0; i < size; i++) {
            ia[i] = 1234567890;
        }
    }

    @Benchmark
    public void zeroByteArray() {
        for (int i = 0; i < size; i++) {
            ba[i] = 0;
        }
    }

    @Benchmark
    public void zeroShortArray() {
        for (int i = 0; i < size; i++) {
            sa[i] = 0;
        }
    }

    @Benchmark
    public void zeroIntArray() {
        for (int i = 0; i < size; i++) {
            ia[i] = 0;
        }
    }
}

