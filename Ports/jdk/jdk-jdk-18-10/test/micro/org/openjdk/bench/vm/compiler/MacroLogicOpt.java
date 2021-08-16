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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class MacroLogicOpt {
  @Param({"64","128","256","512","1024","2048","4096"}) private int VECLEN;

  private  int [] ai = new int[VECLEN];
  private  int [] bi = new int[VECLEN];
  private  int [] ci = new int[VECLEN];
  private  int [] ri = new int[VECLEN];

  private  long [] al = new long[VECLEN];
  private  long [] bl = new long[VECLEN];
  private  long [] cl = new long[VECLEN];
  private  long [] dl = new long[VECLEN];
  private  long [] el = new long[VECLEN];
  private  long [] fl = new long[VECLEN];
  private  long [] rl = new long[VECLEN];

  private Random r = new Random();

  @Setup
  public void init() {
    ai = new int[VECLEN];
    bi = new int[VECLEN];
    ci = new int[VECLEN];
    ri = new int[VECLEN];

    al = new long[VECLEN];
    bl = new long[VECLEN];
    cl = new long[VECLEN];
    dl = new long[VECLEN];
    el = new long[VECLEN];
    fl = new long[VECLEN];
    rl = new long[VECLEN];
    for (int i=0; i<VECLEN; i++) {
      ai[i] = r.nextInt();
      bi[i] = r.nextInt();
      ci[i] = r.nextInt();

      al[i] = r.nextLong();
      bl[i] = r.nextLong();
      cl[i] = r.nextLong();
      dl[i] = r.nextLong();
      el[i] = r.nextLong();
      fl[i] = r.nextLong();
    }
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private int run_workload1(int count, int [] a , int [] b, int [] c, int [] r) {
      for(int i = 0 ; i < r.length ; i++)
          r[i] = (((a[i] & b[i]) ^ (a[i] & c[i]) ^ (b[i] & c[i]))  &  ((~a[i] & b[i]) | (~b[i] & c[i])  | ~c[i] & a[i]));
    return r[count];
  }

  @Benchmark
  public  void workload1_caller(Blackhole bh) {
    int r = 0;
    for(int i = 0 ; i < 10000; i++)
       r += run_workload1(i&(ri.length-1), ai, bi, ci, ri);
    bh.consume(r);
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private long run_workload2(int count, long [] a , long [] b, long [] c, long [] r) {
      for(int i = 0 ; i < r.length ; i++)
          r[i] = (((a[i] & b[i]) ^ (a[i] & c[i]) ^ (b[i] & c[i]))  &  ((~a[i] & b[i]) | (~b[i] & c[i])  | ~c[i] & a[i]));
    return r[count];
  }

  @Benchmark
  public void workload2_caller(Blackhole bh) {
    long r = 0;
    for(int i = 0 ; i < 100000; i++)
       r += run_workload2(i&(rl.length-1), al, bl, cl, rl);
    bh.consume(r);
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private long run_workload3(int count, long [] a , long [] b, long [] c,
                           long [] d, long [] e, long [] f, long [] r) {
    for(int i = 0 ; i < r.length ; i++)
      r[i] = (((~a[i] | ~b[i]) & (~c[i])) | (~d[i] & (~e[i] & f[i])));
    return r[count];
  }

  @Benchmark
  public void workload3_caller(Blackhole bh) {
    long r = 0;
    for(int i = 0 ; i < 10000; i++)
       r += run_workload3(i&(ri.length-1), al, bl, cl, dl, el, fl, rl);
    bh.consume(r);
  }
}
