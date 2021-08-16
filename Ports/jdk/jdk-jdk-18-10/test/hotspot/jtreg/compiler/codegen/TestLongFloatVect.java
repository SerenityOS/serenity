/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7119644
 * @summary Increase superword's vector size up to 256 bits
 *
 * @run main/othervm/timeout=300 -Xbatch -XX:+IgnoreUnrecognizedVMOptions
 *    -XX:-TieredCompilation -XX:-OptimizeFill
 *    compiler.codegen.TestLongFloatVect
 */

package compiler.codegen;

public class TestLongFloatVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final int OFFSET = 3;
  private static final int SCALE = 2;
  private static final int ALIGN_OFF = 8;
  private static final int UNALIGN_OFF = 5;

  public static void main(String args[]) {
    System.out.println("Testing Long + Float vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    long[] a1 = new long[ARRLEN];
    long[] a2 = new long[ARRLEN];
    float[] b1 = new float[ARRLEN];
    float[] b2 = new float[ARRLEN];
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_ci(a1, b1);
      test_vi(a2, b2, (long)123, 103.f);
      test_cp(a1, a2, b1, b2);
      test_ci_neg(a1, b1);
      test_vi_neg(a1, b1, (long)123, 103.f);
      test_cp_neg(a1, a2, b1, b2);
      test_ci_oppos(a1, b1);
      test_vi_oppos(a1, b1, (long)123, 103.f);
      test_cp_oppos(a1, a2, b1, b2);
      test_ci_aln(a1, b1);
      test_vi_aln(a1, b1, (long)123, 103.f);
      test_cp_alndst(a1, a2, b1, b2);
      test_cp_alnsrc(a1, a2, b1, b2);
      test_ci_unaln(a1, b1);
      test_vi_unaln(a1, b1, (long)123, 103.f);
      test_cp_unalndst(a1, a2, b1, b2);
      test_cp_unalnsrc(a1, a2, b1, b2);
    }
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1[i] = -1;
      a2[i] = -1;
      b1[i] = -1.f;
      b2[i] = -1.f;
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      test_ci(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci: a1", i, a1[i], (long)-123);
        errn += verify("test_ci: b1", i, b1[i], -103.f);
      }
      test_vi(a2, b2, (long)123, 103.f);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi: a2", i, a2[i], (long)123);
        errn += verify("test_vi: b2", i, b2[i], 103.f);
      }
      test_cp(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp: a1", i, a1[i], (long)123);
        errn += verify("test_cp: b1", i, b1[i], 103.f);
      }

      // Reset for negative stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        a2[i] = -1;
        b1[i] = -1.f;
        b2[i] = -1.f;
      }
      test_ci_neg(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_neg: a1", i, a1[i], (long)-123);
        errn += verify("test_ci_neg: b1", i, b1[i], -103.f);
      }
      test_vi_neg(a2, b2, (long)123, 103.f);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_neg: a2", i, a2[i], (long)123);
        errn += verify("test_vi_neg: b2", i, b2[i], 103.f);
      }
      test_cp_neg(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_neg: a1", i, a1[i], (long)123);
        errn += verify("test_cp_neg: b1", i, b1[i], 103.f);
      }

      // Reset for opposite stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        a2[i] = -1;
        b1[i] = -1.f;
        b2[i] = -1.f;
      }
      test_ci_oppos(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_oppos: a1", i, a1[i], (long)-123);
        errn += verify("test_ci_oppos: b1", i, b1[i], -103.f);
      }
      test_vi_oppos(a2, b2, (long)123, 103.f);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_oppos: a2", i, a2[i], (long)123);
        errn += verify("test_vi_oppos: b2", i, b2[i], 103.f);
      }
      test_cp_oppos(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_oppos: a1", i, a1[i], (long)123);
        errn += verify("test_cp_oppos: b1", i, b1[i], 103.f);
      }

      // Reset for 2 arrays with relative aligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        a2[i] = 123;
        b1[i] = -1.f;
        b2[i] = 123.f;
      }
      test_cp_alndst(a1, a2, b1, b2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], (long)-1);
        errn += verify("test_cp_alndst: b1", i, b1[i], -1.f);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], (long)123);
        errn += verify("test_cp_alndst: b1", i, b1[i], 123.f);
      }
      for (int i=0; i<ARRLEN; i++) {
        a2[i] = -123;
        b2[i] = -123.f;
      }
      test_cp_alnsrc(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], (long)-123);
        errn += verify("test_cp_alnsrc: b1", i, b1[i], -123.f);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], (long)123);
        errn += verify("test_cp_alnsrc: b1", i, b1[i], 123.f);
      }

      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1.f;
      }
      test_ci_aln(a1, b1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_ci_aln: a1", i, a1[i], (long)-1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_aln: a1", i, a1[i], (long)-123);
      }
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_ci_aln: b1", i, b1[i], -103.f);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_aln: b1", i, b1[i], -1.f);
      }

      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1.f;
      }
      test_vi_aln(a1, b1, (long)123, 103.f);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_vi_aln: a1", i, a1[i], (long)123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_aln: a1", i, a1[i], (long)-1);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_vi_aln: b1", i, b1[i], -1.f);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_aln: b1", i, b1[i], 103.f);
      }

      // Reset for 2 arrays with relative unaligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        a2[i] = 123;
        b1[i] = -1.f;
        b2[i] = 123.f;
      }
      test_cp_unalndst(a1, a2, b1, b2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], (long)-1);
        errn += verify("test_cp_unalndst: b1", i, b1[i], -1.f);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], (long)123);
        errn += verify("test_cp_unalndst: b1", i, b1[i], 123.f);
      }
      for (int i=0; i<ARRLEN; i++) {
        a2[i] = -123;
        b2[i] = -123.f;
      }
      test_cp_unalnsrc(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], (long)-123);
        errn += verify("test_cp_unalnsrc: b1", i, b1[i], -123.f);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], (long)123);
        errn += verify("test_cp_unalnsrc: b1", i, b1[i], 123.f);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1;
      }
      test_ci_unaln(a1, b1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_ci_unaln: a1", i, a1[i], (long)-1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_unaln: a1", i, a1[i], (long)-123);
      }
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_ci_unaln: b1", i, b1[i], -103.f);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_unaln: b1", i, b1[i], -1.f);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1;
      }
      test_vi_unaln(a1, b1, (long)123, 103.f);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_vi_unaln: a1", i, a1[i], (long)123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_unaln: a1", i, a1[i], (long)-1);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_vi_unaln: b1", i, b1[i], -1.f);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_unaln: b1", i, b1[i], 103.f);
      }

      // Reset for aligned overlap initialization
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i] = (long)i;
        b1[i] = (float)i;
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1.f;
      }
      test_cp_alndst(a1, a1, b1, b1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alndst_overlap: a1", i, a1[i], (long)v);
        errn += verify("test_cp_alndst_overlap: b1", i, b1[i], (float)v);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i+ALIGN_OFF] = -1;
        b1[i+ALIGN_OFF] = -1.f;
      }
      test_cp_alnsrc(a1, a1, b1, b1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], (long)-1);
        errn += verify("test_cp_alnsrc_overlap: b1", i, b1[i], -1.f);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], (long)v);
        errn += verify("test_cp_alnsrc_overlap: b1", i, b1[i], (float)v);
      }

      // Reset for unaligned overlap initialization
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i] = (long)i;
        b1[i] = (float)i;
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = -1;
        b1[i] = -1.f;
      }
      test_cp_unalndst(a1, a1, b1, b1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalndst_overlap: a1", i, a1[i], (long)v);
        errn += verify("test_cp_unalndst_overlap: b1", i, b1[i], (float)v);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i+UNALIGN_OFF] = -1;
        b1[i+UNALIGN_OFF] = -1.f;
      }
      test_cp_unalnsrc(a1, a1, b1, b1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], (long)-1);
        errn += verify("test_cp_unalnsrc_overlap: b1", i, b1[i], -1.f);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], (long)v);
        errn += verify("test_cp_unalnsrc_overlap: b1", i, b1[i], (float)v);
      }

    }

    if (errn > 0)
      return errn;

    System.out.println("Time");
    long start, end;
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci(a1, b1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi(a2, b2, (long)123, 103.f);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_neg(a1, b1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_neg(a1, b1, (long)123, 103.f);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_neg(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_oppos(a1, b1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_oppos(a1, b1, (long)123, 103.f);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_oppos(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_aln(a1, b1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_aln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_aln(a1, b1, (long)123, 103.f);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_aln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_alndst(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_alndst: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_alnsrc(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_alnsrc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_unaln(a1, b1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_unaln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_unaln(a1, b1, (long)123, 103.f);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_unaln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_unalndst(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_unalndst: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_unalnsrc(a1, a2, b1, b2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_unalnsrc: " + (end - start));
    return errn;
  }

  static void test_ci(long[] a, float[] b) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = -123;
      b[i] = -103.f;
    }
  }
  static void test_vi(long[] a, float[] b, long c, float d) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_cp(long[] a, long[] b, float[] c, float[] d) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[i];
      c[i] = d[i];
    }
  }
  static void test_ci_neg(long[] a, float[] b) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = -123;
      b[i] = -103.f;
    }
  }
  static void test_vi_neg(long[] a, float[] b, long c, float d) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_cp_neg(long[] a, long[] b, float[] c, float[] d) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = b[i];
      c[i] = d[i];
    }
  }
  static void test_ci_oppos(long[] a, float[] b) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[limit-i] = -123;
      b[i] = -103.f;
    }
  }
  static void test_vi_oppos(long[] a, float[] b, long c, float d) {
    int limit = a.length-1;
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = c;
      b[limit-i] = d;
    }
  }
  static void test_cp_oppos(long[] a, long[] b, float[] c, float[] d) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[limit-i];
      c[limit-i] = d[i];
    }
  }
  static void test_ci_aln(long[] a, float[] b) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = -123;
      b[i] = -103.f;
    }
  }
  static void test_vi_aln(long[] a, float[] b, long c, float d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+ALIGN_OFF] = d;
    }
  }
  static void test_cp_alndst(long[] a, long[] b, float[] c, float[] d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = b[i];
      c[i+ALIGN_OFF] = d[i];
    }
  }
  static void test_cp_alnsrc(long[] a, long[] b, float[] c, float[] d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = b[i+ALIGN_OFF];
      c[i] = d[i+ALIGN_OFF];
    }
  }
  static void test_ci_unaln(long[] a, float[] b) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = -123;
      b[i] = -103.f;
    }
  }
  static void test_vi_unaln(long[] a, float[] b, long c, float d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+UNALIGN_OFF] = d;
    }
  }
  static void test_cp_unalndst(long[] a, long[] b, float[] c, float[] d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = b[i];
      c[i+UNALIGN_OFF] = d[i];
    }
  }
  static void test_cp_unalnsrc(long[] a, long[] b, float[] c, float[] d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = b[i+UNALIGN_OFF];
      c[i] = d[i+UNALIGN_OFF];
    }
  }

  static int verify(String text, int i, long elem, long val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
  static int verify(String text, int i, float elem, float val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
