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
 *    compiler.codegen.TestCharShortVect
 */

package compiler.codegen;

public class TestCharShortVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final int OFFSET = 3;
  private static final int SCALE = 2;
  private static final int ALIGN_OFF = 8;
  private static final int UNALIGN_OFF = 5;

  public static void main(String args[]) {
    System.out.println("Testing Char + Short vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    char[] a1 = new char[ARRLEN];
    char[] a2 = new char[ARRLEN];
    short[] b1 = new short[ARRLEN];
    short[] b2 = new short[ARRLEN];
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_ci(a1, b1);
      test_vi(a2, b2, (char)123, (short)103);
      test_cp(a1, a2, b1, b2);
      test_ci_neg(a1, b1);
      test_vi_neg(a1, b1, (char)123, (short)103);
      test_cp_neg(a1, a2, b1, b2);
      test_ci_oppos(a1, b1);
      test_vi_oppos(a1, b1, (char)123, (short)103);
      test_cp_oppos(a1, a2, b1, b2);
      test_ci_aln(a1, b1);
      test_vi_aln(a1, b1, (char)123, (short)103);
      test_cp_alndst(a1, a2, b1, b2);
      test_cp_alnsrc(a1, a2, b1, b2);
      test_ci_unaln(a1, b1);
      test_vi_unaln(a1, b1, (char)123, (short)103);
      test_cp_unalndst(a1, a2, b1, b2);
      test_cp_unalnsrc(a1, a2, b1, b2);
    }
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1[i] = (char)-1;
      a2[i] = (char)-1;
      b1[i] = (short)-1;
      b2[i] = (short)-1;
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      test_ci(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci: a1", i, a1[i], (char)-123);
        errn += verify("test_ci: b1", i, b1[i], (short)-103);
      }
      test_vi(a2, b2, (char)123, (short)103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi: a2", i, a2[i], (char)123);
        errn += verify("test_vi: b2", i, b2[i], (short)103);
      }
      test_cp(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp: a1", i, a1[i], (char)123);
        errn += verify("test_cp: b1", i, b1[i], (short)103);
      }

      // Reset for negative stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        a2[i] = (char)-1;
        b1[i] = (short)-1;
        b2[i] = (short)-1;
      }
      test_ci_neg(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_neg: a1", i, a1[i], (char)-123);
        errn += verify("test_ci_neg: b1", i, b1[i], (short)-103);
      }
      test_vi_neg(a2, b2, (char)123, (short)103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_neg: a2", i, a2[i], (char)123);
        errn += verify("test_vi_neg: b2", i, b2[i], (short)103);
      }
      test_cp_neg(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_neg: a1", i, a1[i], (char)123);
        errn += verify("test_cp_neg: b1", i, b1[i], (short)103);
      }

      // Reset for opposite stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        a2[i] = (char)-1;
        b1[i] = (short)-1;
        b2[i] = (short)-1;
      }
      test_ci_oppos(a1, b1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_oppos: a1", i, a1[i], (char)-123);
        errn += verify("test_ci_oppos: b1", i, b1[i], (short)-103);
      }
      test_vi_oppos(a2, b2, (char)123, (short)103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_oppos: a2", i, a2[i], (char)123);
        errn += verify("test_vi_oppos: b2", i, b2[i], (short)103);
      }
      test_cp_oppos(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_oppos: a1", i, a1[i], (char)123);
        errn += verify("test_cp_oppos: b1", i, b1[i], (short)103);
      }

      // Reset for 2 arrays with relative aligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        a2[i] = (char)123;
        b1[i] = (short)-1;
        b2[i] = (short)123;
      }
      test_cp_alndst(a1, a2, b1, b2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], (char)-1);
        errn += verify("test_cp_alndst: b1", i, b1[i], (short)-1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], (char)123);
        errn += verify("test_cp_alndst: b1", i, b1[i], (short)123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a2[i] = (char)-123;
        b2[i] = (short)-123;
      }
      test_cp_alnsrc(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], (char)-123);
        errn += verify("test_cp_alnsrc: b1", i, b1[i], (short)-123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], (char)123);
        errn += verify("test_cp_alnsrc: b1", i, b1[i], (short)123);
      }

      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_ci_aln(a1, b1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_ci_aln: a1", i, a1[i], (char)-1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_aln: a1", i, a1[i], (char)-123);
      }
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_ci_aln: b1", i, b1[i], (short)-103);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_aln: b1", i, b1[i], (short)-1);
      }

      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_vi_aln(a1, b1, (char)123, (short)103);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_vi_aln: a1", i, a1[i], (char)123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_aln: a1", i, a1[i], (char)-1);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_vi_aln: b1", i, b1[i], (short)-1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_aln: b1", i, b1[i], (short)103);
      }

      // Reset for 2 arrays with relative unaligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        a2[i] = (char)123;
        b1[i] = (short)-1;
        b2[i] = (short)123;
      }
      test_cp_unalndst(a1, a2, b1, b2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], (char)-1);
        errn += verify("test_cp_unalndst: b1", i, b1[i], (short)-1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], (char)123);
        errn += verify("test_cp_unalndst: b1", i, b1[i], (short)123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a2[i] = (char)-123;
        b2[i] = (short)-123;
      }
      test_cp_unalnsrc(a1, a2, b1, b2);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], (char)-123);
        errn += verify("test_cp_unalnsrc: b1", i, b1[i], (short)-123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], (char)123);
        errn += verify("test_cp_unalnsrc: b1", i, b1[i], (short)123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_ci_unaln(a1, b1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_ci_unaln: a1", i, a1[i], (char)-1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_unaln: a1", i, a1[i], (char)-123);
      }
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_ci_unaln: b1", i, b1[i], (short)-103);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_ci_unaln: b1", i, b1[i], (short)-1);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_vi_unaln(a1, b1, (char)123, (short)103);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_vi_unaln: a1", i, a1[i], (char)123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_unaln: a1", i, a1[i], (char)-1);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_vi_unaln: b1", i, b1[i], (short)-1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_vi_unaln: b1", i, b1[i], (short)103);
      }

      // Reset for aligned overlap initialization
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i] = (char)i;
        b1[i] = (short)i;
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_cp_alndst(a1, a1, b1, b1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alndst_overlap: a1", i, a1[i], (char)v);
        errn += verify("test_cp_alndst_overlap: b1", i, b1[i], (short)v);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i+ALIGN_OFF] = (char)-1;
        b1[i+ALIGN_OFF] = (short)-1;
      }
      test_cp_alnsrc(a1, a1, b1, b1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], (char)-1);
        errn += verify("test_cp_alnsrc_overlap: b1", i, b1[i], (short)-1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], (char)v);
        errn += verify("test_cp_alnsrc_overlap: b1", i, b1[i], (short)v);
      }

      // Reset for unaligned overlap initialization
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i] = (char)i;
        b1[i] = (short)i;
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = (char)-1;
        b1[i] = (short)-1;
      }
      test_cp_unalndst(a1, a1, b1, b1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalndst_overlap: a1", i, a1[i], (char)v);
        errn += verify("test_cp_unalndst_overlap: b1", i, b1[i], (short)v);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i+UNALIGN_OFF] = (char)-1;
        b1[i+UNALIGN_OFF] = (short)-1;
      }
      test_cp_unalnsrc(a1, a1, b1, b1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], (char)-1);
        errn += verify("test_cp_unalnsrc_overlap: b1", i, b1[i], (short)-1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], (char)v);
        errn += verify("test_cp_unalnsrc_overlap: b1", i, b1[i], (short)v);
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
      test_vi(a2, b2, (char)123, (short)103);
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
      test_vi_neg(a1, b1, (char)123, (short)103);
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
      test_vi_oppos(a1, b1, (char)123, (short)103);
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
      test_vi_aln(a1, b1, (char)123, (short)103);
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
      test_vi_unaln(a1, b1, (char)123, (short)103);
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

  static void test_ci(char[] a, short[] b) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = (char)-123;
      b[i] = (short)-103;
    }
  }
  static void test_vi(char[] a, short[] b, char c, short d) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_cp(char[] a, char[] b, short[] c, short[] d) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[i];
      c[i] = d[i];
    }
  }
  static void test_ci_neg(char[] a, short[] b) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = (char)-123;
      b[i] = (short)-103;
    }
  }
  static void test_vi_neg(char[] a, short[] b, char c, short d) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_cp_neg(char[] a, char[] b, short[] c, short[] d) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = b[i];
      c[i] = d[i];
    }
  }
  static void test_ci_oppos(char[] a, short[] b) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[limit-i] = (char)-123;
      b[i] = (short)-103;
    }
  }
  static void test_vi_oppos(char[] a, short[] b, char c, short d) {
    int limit = a.length-1;
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = c;
      b[limit-i] = d;
    }
  }
  static void test_cp_oppos(char[] a, char[] b, short[] c, short[] d) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[limit-i];
      c[limit-i] = d[i];
    }
  }
  static void test_ci_aln(char[] a, short[] b) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = (char)-123;
      b[i] = (short)-103;
    }
  }
  static void test_vi_aln(char[] a, short[] b, char c, short d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+ALIGN_OFF] = d;
    }
  }
  static void test_cp_alndst(char[] a, char[] b, short[] c, short[] d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = b[i];
      c[i+ALIGN_OFF] = d[i];
    }
  }
  static void test_cp_alnsrc(char[] a, char[] b, short[] c, short[] d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = b[i+ALIGN_OFF];
      c[i] = d[i+ALIGN_OFF];
    }
  }
  static void test_ci_unaln(char[] a, short[] b) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = (char)-123;
      b[i] = (short)-103;
    }
  }
  static void test_vi_unaln(char[] a, short[] b, char c, short d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+UNALIGN_OFF] = d;
    }
  }
  static void test_cp_unalndst(char[] a, char[] b, short[] c, short[] d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = b[i];
      c[i+UNALIGN_OFF] = d[i];
    }
  }
  static void test_cp_unalnsrc(char[] a, char[] b, short[] c, short[] d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = b[i+UNALIGN_OFF];
      c[i] = d[i+UNALIGN_OFF];
    }
  }

  static int verify(String text, int i, char elem, char val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
  static int verify(String text, int i, short elem, short val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
