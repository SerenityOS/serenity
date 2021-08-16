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
 *    compiler.codegen.TestBooleanVect
 */

package compiler.codegen;

public class TestBooleanVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final int OFFSET = 3;
  private static final int SCALE = 2;
  private static final int ALIGN_OFF = 8;
  private static final int UNALIGN_OFF = 5;

  public static void main(String args[]) {
    System.out.println("Testing Boolean vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    boolean[] a1 = new boolean[ARRLEN];
    boolean[] a2 = new boolean[ARRLEN];
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_ci(a1);
      test_vi(a2, true);
      test_cp(a1, a2);
      test_2ci(a1, a2);
      test_2vi(a1, a2, true, true);
      test_ci_neg(a1);
      test_vi_neg(a2, true);
      test_cp_neg(a1, a2);
      test_2ci_neg(a1, a2);
      test_2vi_neg(a1, a2, true, true);
      test_ci_oppos(a1);
      test_vi_oppos(a2, true);
      test_cp_oppos(a1, a2);
      test_2ci_oppos(a1, a2);
      test_2vi_oppos(a1, a2, true, true);
      test_ci_off(a1);
      test_vi_off(a2, true);
      test_cp_off(a1, a2);
      test_2ci_off(a1, a2);
      test_2vi_off(a1, a2, true, true);
      test_ci_inv(a1, OFFSET);
      test_vi_inv(a2, true, OFFSET);
      test_cp_inv(a1, a2, OFFSET);
      test_2ci_inv(a1, a2, OFFSET);
      test_2vi_inv(a1, a2, true, true, OFFSET);
      test_ci_scl(a1);
      test_vi_scl(a2, true);
      test_cp_scl(a1, a2);
      test_2ci_scl(a1, a2);
      test_2vi_scl(a1, a2, true, true);
      test_cp_alndst(a1, a2);
      test_cp_alnsrc(a1, a2);
      test_2ci_aln(a1, a2);
      test_2vi_aln(a1, a2, true, true);
      test_cp_unalndst(a1, a2);
      test_cp_unalnsrc(a1, a2);
      test_2ci_unaln(a1, a2);
      test_2vi_unaln(a1, a2, true, true);
    }
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1[i] = false;
      a2[i] = false;
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      test_ci(a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci: a1", i, a1[i], false);
      }
      test_vi(a2, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi: a2", i, a2[i], true);
      }
      test_cp(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp: a1", i, a1[i], true);
      }
      test_2ci(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci: a1", i, a1[i], false);
        errn += verify("test_2ci: a2", i, a2[i], false);
      }
      test_2vi(a1, a2, true, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi: a1", i, a1[i], true);
        errn += verify("test_2vi: a2", i, a2[i], true);
      }
      // Reset for negative stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_ci_neg(a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_neg: a1", i, a1[i], false);
      }
      test_vi_neg(a2, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_neg: a2", i, a2[i], true);
      }
      test_cp_neg(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_neg: a1", i, a1[i], true);
      }
      test_2ci_neg(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci_neg: a1", i, a1[i], false);
        errn += verify("test_2ci_neg: a2", i, a2[i], false);
      }
      test_2vi_neg(a1, a2, true, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi_neg: a1", i, a1[i], true);
        errn += verify("test_2vi_neg: a2", i, a2[i], true);
      }
      // Reset for opposite stride
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_ci_oppos(a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_oppos: a1", i, a1[i], false);
      }
      test_vi_oppos(a2, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_oppos: a2", i, a2[i], true);
      }
      test_cp_oppos(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_oppos: a1", i, a1[i], true);
      }
      test_2ci_oppos(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci_oppos: a1", i, a1[i], false);
        errn += verify("test_2ci_oppos: a2", i, a2[i], false);
      }
      test_2vi_oppos(a1, a2, true, true);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi_oppos: a1", i, a1[i], true);
        errn += verify("test_2vi_oppos: a2", i, a2[i], true);
      }
      // Reset for indexing with offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_ci_off(a1);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_ci_off: a1", i, a1[i], false);
      }
      test_vi_off(a2, true);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_vi_off: a2", i, a2[i], true);
      }
      test_cp_off(a1, a2);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_cp_off: a1", i, a1[i], true);
      }
      test_2ci_off(a1, a2);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2ci_off: a1", i, a1[i], false);
        errn += verify("test_2ci_off: a2", i, a2[i], false);
      }
      test_2vi_off(a1, a2, true, true);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2vi_off: a1", i, a1[i], true);
        errn += verify("test_2vi_off: a2", i, a2[i], true);
      }
      for (int i=0; i<OFFSET; i++) {
        errn += verify("test_2vi_off: a1", i, a1[i], false);
        errn += verify("test_2vi_off: a2", i, a2[i], false);
      }
      // Reset for indexing with invariant offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_ci_inv(a1, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_ci_inv: a1", i, a1[i], false);
      }
      test_vi_inv(a2, true, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_vi_inv: a2", i, a2[i], true);
      }
      test_cp_inv(a1, a2, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_cp_inv: a1", i, a1[i], true);
      }
      test_2ci_inv(a1, a2, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2ci_inv: a1", i, a1[i], false);
        errn += verify("test_2ci_inv: a2", i, a2[i], false);
      }
      test_2vi_inv(a1, a2, true, true, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2vi_inv: a1", i, a1[i], true);
        errn += verify("test_2vi_inv: a2", i, a2[i], true);
      }
      for (int i=0; i<OFFSET; i++) {
        errn += verify("test_2vi_inv: a1", i, a1[i], false);
        errn += verify("test_2vi_inv: a2", i, a2[i], false);
      }
      // Reset for indexing with scale
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = true;
        a2[i] = false;
      }
      test_ci_scl(a1);
      for (int i=0; i<ARRLEN; i++) {
        boolean val = (i%SCALE != 0);
        errn += verify("test_ci_scl: a1", i, a1[i], val);
      }
      test_vi_scl(a2, true);
      for (int i=0; i<ARRLEN; i++) {
        boolean val = (i%SCALE == 0);
        errn += verify("test_vi_scl: a2", i, a2[i], val);
      }
      test_cp_scl(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_scl: a1", i, a1[i], true);
      }
      test_2ci_scl(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        if (i%SCALE != 0) {
          errn += verify("test_2ci_scl: a1", i, a1[i], true);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2ci_scl: a1", i*SCALE, a1[i*SCALE], false);
        }
        if (i%SCALE != 0) {
          errn += verify("test_2ci_scl: a2", i, a2[i], false);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2ci_scl: a2", i*SCALE, a2[i*SCALE], false);
        }
      }
      test_2vi_scl(a1, a2, false, true);
      for (int i=0; i<ARRLEN; i++) {
        if (i%SCALE != 0) {
          errn += verify("test_2vi_scl: a1", i, a1[i], true);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2vi_scl: a1", i*SCALE, a1[i*SCALE], false);
        }
        if (i%SCALE != 0) {
          errn += verify("test_2vi_scl: a2", i, a2[i], false);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2vi_scl: a2", i*SCALE, a2[i*SCALE], true);
        }
      }
      // Reset for 2 arrays with relative aligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_vi(a2, true);
      test_cp_alndst(a1, a2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], false);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alndst: a1", i, a1[i], true);
      }
      test_vi(a2, false);
      test_cp_alnsrc(a1, a2);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], false);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1[i], true);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_2ci_aln(a1, a2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln: a1", i, a1[i], false);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln: a1", i, a1[i], false);
      }
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln: a2", i, a2[i], false);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln: a2", i, a2[i], false);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_2vi_aln(a1, a2, true, true);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln: a1", i, a1[i], true);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln: a1", i, a1[i], false);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln: a2", i, a2[i], false);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln: a2", i, a2[i], true);
      }

      // Reset for 2 arrays with relative unaligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_vi(a2, true);
      test_cp_unalndst(a1, a2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], false);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1[i], true);
      }
      test_vi(a2, false);
      test_cp_unalnsrc(a1, a2);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], false);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1[i], true);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_2ci_unaln(a1, a2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln: a1", i, a1[i], false);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln: a1", i, a1[i], false);
      }
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln: a2", i, a2[i], false);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln: a2", i, a2[i], false);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
        a2[i] = false;
      }
      test_2vi_unaln(a1, a2, true, true);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln: a1", i, a1[i], true);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln: a1", i, a1[i], false);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln: a2", i, a2[i], false);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln: a2", i, a2[i], true);
      }

      // Reset for aligned overlap initialization
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i] = (i > 0);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_cp_alndst(a1, a1);
      for (int i=0; i<ARRLEN; i++) {
        boolean v = (i%ALIGN_OFF > 0);
        errn += verify("test_cp_alndst_overlap: a1", i, a1[i], v);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        a1[i+ALIGN_OFF] = false;
      }
      test_cp_alnsrc(a1, a1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], false);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        boolean v = (i%ALIGN_OFF > 0);
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1[i], v);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_2ci_aln(a1, a1);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln_overlap: a1", i, a1[i], false);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln_overlap: a1", i, a1[i], false);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_2vi_aln(a1, a1, true, true);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln_overlap: a1", i, a1[i], true);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln_overlap: a1", i, a1[i], true);
      }

      // Reset for unaligned overlap initialization
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i] = (i > 0);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_cp_unalndst(a1, a1);
      for (int i=0; i<ARRLEN; i++) {
        boolean v = (i%UNALIGN_OFF > 0);
        errn += verify("test_cp_unalndst_overlap: a1", i, a1[i], v);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1[i+UNALIGN_OFF] = false;
      }
      test_cp_unalnsrc(a1, a1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], false);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        boolean v = (i%UNALIGN_OFF > 0);
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1[i], v);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_2ci_unaln(a1, a1);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln_overlap: a1", i, a1[i], false);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln_overlap: a1", i, a1[i], false);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1[i] = false;
      }
      test_2vi_unaln(a1, a1, true, true);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln_overlap: a1", i, a1[i], true);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln_overlap: a1", i, a1[i], true);
      }

    }

    if (errn > 0)
      return errn;

    System.out.println("Time");
    long start, end;
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi(a2, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_neg(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_neg(a2, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_neg(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_neg(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_neg(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_neg: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_oppos(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_oppos(a2, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_oppos(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_oppos(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_oppos(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_oppos: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_off(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_off: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_off(a2, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_off: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_off(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_off: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_off(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_off: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_off(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_off: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_inv(a1, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_inv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_inv(a2, true, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_inv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_inv(a1, a2, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_inv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_inv(a1, a2, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_inv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_inv(a1, a2, true, true, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_inv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_scl(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_scl: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_scl(a2, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_vi_scl: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_scl(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_scl: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_scl(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_scl: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_scl(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_scl: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_alndst(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_alndst: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_alnsrc(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_alnsrc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_aln(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_aln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_aln(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_aln: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_unalndst(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_unalndst: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_cp_unalnsrc(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_cp_unalnsrc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2ci_unaln(a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2ci_unaln: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_2vi_unaln(a1, a2, true, true);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_unaln: " + (end - start));

    return errn;
  }

  static void test_ci(boolean[] a) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = false;
    }
  }
  static void test_vi(boolean[] a, boolean b) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b;
    }
  }
  static void test_cp(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[i];
    }
  }
  static void test_2ci(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = false;
      b[i] = false;
    }
  }
  static void test_2vi(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = 0; i < a.length; i+=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_ci_neg(boolean[] a) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = false;
    }
  }
  static void test_vi_neg(boolean[] a, boolean b) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = b;
    }
  }
  static void test_cp_neg(boolean[] a, boolean[] b) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = b[i];
    }
  }
  static void test_2ci_neg(boolean[] a, boolean[] b) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = false;
      b[i] = false;
    }
  }
  static void test_2vi_neg(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = a.length-1; i >= 0; i-=1) {
      a[i] = c;
      b[i] = d;
    }
  }
  static void test_ci_oppos(boolean[] a) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[limit-i] = false;
    }
  }
  static void test_vi_oppos(boolean[] a, boolean b) {
    int limit = a.length-1;
    for (int i = limit; i >= 0; i-=1) {
      a[limit-i] = b;
    }
  }
  static void test_cp_oppos(boolean[] a, boolean[] b) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[i] = b[limit-i];
    }
  }
  static void test_2ci_oppos(boolean[] a, boolean[] b) {
    int limit = a.length-1;
    for (int i = 0; i < a.length; i+=1) {
      a[limit-i] = false;
      b[i] = false;
    }
  }
  static void test_2vi_oppos(boolean[] a, boolean[] b, boolean c, boolean d) {
    int limit = a.length-1;
    for (int i = limit; i >= 0; i-=1) {
      a[i] = c;
      b[limit-i] = d;
    }
  }
  static void test_ci_off(boolean[] a) {
    for (int i = 0; i < a.length-OFFSET; i+=1) {
      a[i+OFFSET] = false;
    }
  }
  static void test_vi_off(boolean[] a, boolean b) {
    for (int i = 0; i < a.length-OFFSET; i+=1) {
      a[i+OFFSET] = b;
    }
  }
  static void test_cp_off(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-OFFSET; i+=1) {
      a[i+OFFSET] = b[i+OFFSET];
    }
  }
  static void test_2ci_off(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-OFFSET; i+=1) {
      a[i+OFFSET] = false;
      b[i+OFFSET] = false;
    }
  }
  static void test_2vi_off(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = 0; i < a.length-OFFSET; i+=1) {
      a[i+OFFSET] = c;
      b[i+OFFSET] = d;
    }
  }
  static void test_ci_inv(boolean[] a, int k) {
    for (int i = 0; i < a.length-k; i+=1) {
      a[i+k] = false;
    }
  }
  static void test_vi_inv(boolean[] a, boolean b, int k) {
    for (int i = 0; i < a.length-k; i+=1) {
      a[i+k] = b;
    }
  }
  static void test_cp_inv(boolean[] a, boolean[] b, int k) {
    for (int i = 0; i < a.length-k; i+=1) {
      a[i+k] = b[i+k];
    }
  }
  static void test_2ci_inv(boolean[] a, boolean[] b, int k) {
    for (int i = 0; i < a.length-k; i+=1) {
      a[i+k] = false;
      b[i+k] = false;
    }
  }
  static void test_2vi_inv(boolean[] a, boolean[] b, boolean c, boolean d, int k) {
    for (int i = 0; i < a.length-k; i+=1) {
      a[i+k] = c;
      b[i+k] = d;
    }
  }
  static void test_ci_scl(boolean[] a) {
    for (int i = 0; i*SCALE < a.length; i+=1) {
      a[i*SCALE] = false;
    }
  }
  static void test_vi_scl(boolean[] a, boolean b) {
    for (int i = 0; i*SCALE < a.length; i+=1) {
      a[i*SCALE] = b;
    }
  }
  static void test_cp_scl(boolean[] a, boolean[] b) {
    for (int i = 0; i*SCALE < a.length; i+=1) {
      a[i*SCALE] = b[i*SCALE];
    }
  }
  static void test_2ci_scl(boolean[] a, boolean[] b) {
    for (int i = 0; i*SCALE < a.length; i+=1) {
      a[i*SCALE] = false;
      b[i*SCALE] = false;
    }
  }
  static void test_2vi_scl(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = 0; i*SCALE < a.length; i+=1) {
      a[i*SCALE] = c;
      b[i*SCALE] = d;
    }
  }
  static void test_cp_alndst(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = b[i];
    }
  }
  static void test_cp_alnsrc(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = b[i+ALIGN_OFF];
    }
  }
  static void test_2ci_aln(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i+ALIGN_OFF] = false;
      b[i] = false;
    }
  }
  static void test_2vi_aln(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = 0; i < a.length-ALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+ALIGN_OFF] = d;
    }
  }
  static void test_cp_unalndst(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = b[i];
    }
  }
  static void test_cp_unalnsrc(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = b[i+UNALIGN_OFF];
    }
  }
  static void test_2ci_unaln(boolean[] a, boolean[] b) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i+UNALIGN_OFF] = false;
      b[i] = false;
    }
  }
  static void test_2vi_unaln(boolean[] a, boolean[] b, boolean c, boolean d) {
    for (int i = 0; i < a.length-UNALIGN_OFF; i+=1) {
      a[i] = c;
      b[i+UNALIGN_OFF] = d;
    }
  }

  static int verify(String text, int i, boolean elem, boolean val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
