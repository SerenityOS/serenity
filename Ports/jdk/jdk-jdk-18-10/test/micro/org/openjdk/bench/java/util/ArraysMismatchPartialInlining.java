/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ArraysMismatchPartialInlining {

    @Param({"3", "4", "5", "6", "7", "15", "31", "63", "95", "800"})
    private static int size;

    byte [] barray1;
    char [] carray1;
    short [] sarray1;
    int [] iarray1;
    long [] larray1;
    float [] farray1;
    double [] darray1;

    byte [] barray2;
    char [] carray2;
    short [] sarray2;
    int [] iarray2;
    long [] larray2;
    float [] farray2;
    double [] darray2;

    @Setup
    public void setup() {
      barray1 = new byte[size];
      carray1 = new char[size];
      sarray1 = new short[size];
      iarray1 = new int[size];
      larray1 = new long[size];
      farray1 = new float[size];
      darray1 = new double[size];

      barray2 = new byte[size];
      carray2 = new char[size];
      sarray2 = new short[size];
      iarray2 = new int[size];
      larray2 = new long[size];
      farray2 = new float[size];
      darray2 = new double[size];

      Arrays.fill(barray1 , (byte)0xF);
      Arrays.fill(carray1 , (char)0xFF);
      Arrays.fill(sarray1 , (short)0xFF);
      Arrays.fill(iarray1 , -1);
      Arrays.fill(larray1 , -1L);
      Arrays.fill(farray1 , -1.0f);
      Arrays.fill(darray1, -1.0);

      Arrays.fill(barray2 , (byte)0xF);
      Arrays.fill(carray2 , (char)0xFF);
      Arrays.fill(sarray2 , (short)0xFF);
      Arrays.fill(iarray2 , -1);
      Arrays.fill(larray2 , -1L);
      Arrays.fill(farray2 , -1.0F);
      Arrays.fill(darray2, -1.0);

      barray2[size-1] = (byte)1;
      carray2[size-1] = (char)1;
      sarray2[size-1] = (short)1;
      iarray2[size-1] = 1;
      larray2[size-1] = 1L;
      farray2[size-1] = 1.0f;
      darray2[size-1] = 1.0;
    }

    @Benchmark
    public int testByteMatch() {
      return Arrays.mismatch(barray1, barray2);
    }

    @Benchmark
    public int testCharMatch() {
      return Arrays.mismatch(carray1, carray2);
    }

    @Benchmark
    public int testShortMatch() {
      return Arrays.mismatch(sarray1, sarray2);
    }

    @Benchmark
    public int testIntMatch() {
      return Arrays.mismatch(iarray1, iarray2);
    }

    @Benchmark
    public int testLongMatch() {
      return Arrays.mismatch(larray1, larray2);
    }

    @Benchmark
    public int testFloatMatch() {
      return Arrays.mismatch(farray1, farray2);
    }

    @Benchmark
    public int testDoubleMatch() {
      return Arrays.mismatch(darray1, darray2);
    }
}
