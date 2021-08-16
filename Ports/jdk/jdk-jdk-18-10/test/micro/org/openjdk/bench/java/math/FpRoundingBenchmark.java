//
// Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//
//
package org.openjdk.bench.java.math;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FpRoundingBenchmark {

  @Param({"1024"})
  public int TESTSIZE;

  public double[] DargV1;

  public double[] Res;

  public final double[] DspecialVals = {
      0.0, -0.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY};

  @Setup(Level.Trial)
  public void BmSetup() {
    int i = 0;
    Random r = new Random(1024);
    DargV1 = new double[TESTSIZE];
    Res = new double[TESTSIZE];

    for (; i < DspecialVals.length; i++) {
      DargV1[i] = DspecialVals[i];
    }

    for (; i < TESTSIZE; i++) {
      DargV1[i] = r.nextDouble()*TESTSIZE;
    }
  }

  @Benchmark
  public void testceil(Blackhole bh) {
    for (int i = 0; i < TESTSIZE; i++)
      Res[i] = Math.ceil(DargV1[i]);
  }

  @Benchmark
  public void testfloor(Blackhole bh) {
    for (int i = 0; i < TESTSIZE; i++)
      Res[i] = Math.floor(DargV1[i]);
  }

  @Benchmark
  public void testrint(Blackhole bh) {
    for (int i = 0; i < TESTSIZE; i++)
      Res[i] = Math.rint(DargV1[i]);
  }
}
