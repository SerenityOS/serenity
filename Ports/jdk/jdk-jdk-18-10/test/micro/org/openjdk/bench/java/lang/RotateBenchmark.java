//
// Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
public class RotateBenchmark {

  @Param({"1024"})
  public int TESTSIZE;

  @Param({"20"})
  public int SHIFT;

  public long [] larr;
  public int  [] iarr;

  public long [] lres;
  public int  [] ires;


  @Setup(Level.Trial)
  public void BmSetup() {
    Random r = new Random(1024);
    larr = new long[TESTSIZE];
    iarr = new int[TESTSIZE];
    lres = new long[TESTSIZE];
    ires = new int[TESTSIZE];

    for (int i = 0; i < TESTSIZE; i++) {
      larr[i] = r.nextLong();
    }

    for (int i = 0; i < TESTSIZE; i++) {
      iarr[i] = r.nextInt();
    }
  }

  @Benchmark
  public void testRotateLeftI() {
    for (int i = 0; i < TESTSIZE; i++)
       ires[i] = Integer.rotateLeft(iarr[i], SHIFT);
  }
  @Benchmark
  public void testRotateRightI() {
    for (int i = 0; i < TESTSIZE; i++)
       ires[i] = Integer.rotateRight(iarr[i], SHIFT);
  }
  @Benchmark
  public void testRotateLeftL() {
    for (int i = 0; i < TESTSIZE; i++)
       lres[i] = Long.rotateLeft(larr[i], SHIFT);
  }
  @Benchmark
  public void testRotateRightL() {
    for (int i = 0; i < TESTSIZE; i++)
       lres[i] = Long.rotateRight(larr[i], SHIFT);
  }

}
