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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring Unaligned System.arraycopy.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ArrayCopyUnalignedBoth {

    @Param({"1", "3", "5", "10", "20", "70", "150", "300", "600", "1200"})
    int length;

    int fromPos, toPos;
    byte[] fromByteArr, toByteArr;
    char[] fromCharArr, toCharArr;
    int[] fromIntArr, toIntArr;
    long[] fromLongArr, toLongArr;

    @Setup
    public void setup() {
        // Both positions Unaligned
        fromPos = 9;
        toPos = 10;

        fromByteArr = new byte[1210];
        toByteArr = new byte[1210];
        fromCharArr = new char[1210];
        toCharArr = new char[1210];
        fromIntArr = new int[1210];
        toIntArr = new int[1210];
        fromLongArr = new long[1210];
        toLongArr = new long[1210];
    }

    @Benchmark
    public void testByte() {
        System.arraycopy(fromByteArr, fromPos, toByteArr, toPos, length);
    }

    @Benchmark
    public void testChar() {
        System.arraycopy(fromCharArr, fromPos, toCharArr, toPos, length);
    }

    @Benchmark
    public void testInt() {
        System.arraycopy(fromIntArr, fromPos, toIntArr, toPos, length);
    }

    @Benchmark
    public void testLong() {
        System.arraycopy(fromLongArr, fromPos, toLongArr, toPos, length);
    }
}
