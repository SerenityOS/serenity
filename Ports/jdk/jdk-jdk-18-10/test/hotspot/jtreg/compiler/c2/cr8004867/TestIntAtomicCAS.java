/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004867
 * @summary VM crashing with assert "share/vm/opto/node.hpp:357 - assert(i < _max) failed: oob"
 *
 * @run main/othervm/timeout=300 -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation
 *    -XX:-OptimizeFill
 *    compiler.c2.cr8004867.TestIntAtomicCAS
 * @run main/othervm/timeout=300 -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation
 *    -XX:+OptimizeFill
 *    compiler.c2.cr8004867.TestIntAtomicCAS
 */

package compiler.c2.cr8004867;

import java.util.concurrent.atomic.AtomicIntegerArray;

public class TestIntAtomicCAS {
  private static final int ARRLEN = 97;
  private static final int ITERS  = 11000;
  private static final int OFFSET = 3;
  private static final int SCALE = 2;
  private static final int ALIGN_OFF = 8;
  private static final int UNALIGN_OFF = 5;

  public static void main(String args[]) {
    System.out.println("Testing Integer array atomic CAS operations");
    int errn = test(false);
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test(boolean test_only) {
    AtomicIntegerArray a1 = new AtomicIntegerArray(ARRLEN);
    AtomicIntegerArray a2 = new AtomicIntegerArray(ARRLEN);
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1.set(i, -1);
      a2.set(i, -1);
    }
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_ci(a1);
      test_vi(a2, 123, -1);
      test_cp(a1, a2);
      test_2ci(a1, a2);
      test_2vi(a1, a2, 123, 103);
      test_ci_neg(a1, 123);
      test_vi_neg(a2, 123, 103);
      test_cp_neg(a1, a2);
      test_2ci_neg(a1, a2);
      test_2vi_neg(a1, a2, 123, 103);
      test_ci_oppos(a1, 123);
      test_vi_oppos(a2, 123, 103);
      test_cp_oppos(a1, a2);
      test_2ci_oppos(a1, a2);
      test_2vi_oppos(a1, a2, 123, 103);
      test_ci_off(a1, 123);
      test_vi_off(a2, 123, 103);
      test_cp_off(a1, a2);
      test_2ci_off(a1, a2);
      test_2vi_off(a1, a2, 123, 103);
      test_ci_inv(a1, OFFSET, 123);
      test_vi_inv(a2, 123, OFFSET, 103);
      test_cp_inv(a1, a2, OFFSET);
      test_2ci_inv(a1, a2, OFFSET);
      test_2vi_inv(a1, a2, 123, 103, OFFSET);
      test_ci_scl(a1, 123);
      test_vi_scl(a2, 123, 103);
      test_cp_scl(a1, a2);
      test_2ci_scl(a1, a2);
      test_2vi_scl(a1, a2, 123, 103);
      test_cp_alndst(a1, a2);
      test_cp_alnsrc(a1, a2);
      test_2ci_aln(a1, a2);
      test_2vi_aln(a1, a2, 123, 103);
      test_cp_unalndst(a1, a2);
      test_cp_unalnsrc(a1, a2);
      test_2ci_unaln(a1, a2);
      test_2vi_unaln(a1, a2, 123, 103);
    }
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1.set(i, -1);
      a2.set(i, -1);
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      test_ci(a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci: a1", i, a1.get(i), -123);
      }
      test_vi(a2, 123, -1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi: a2", i, a2.get(i), 123);
      }
      test_cp(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp: a1", i, a1.get(i), 123);
      }
      test_2ci(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci: a1", i, a1.get(i), -123);
        errn += verify("test_2ci: a2", i, a2.get(i), -103);
      }
      test_2vi(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi: a1", i, a1.get(i), 123);
        errn += verify("test_2vi: a2", i, a2.get(i), 103);
      }
      // Reset for negative stride
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_ci_neg(a1, -1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_neg: a1", i, a1.get(i), -123);
      }
      test_vi_neg(a2, 123, -1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_neg: a2", i, a2.get(i), 123);
      }
      test_cp_neg(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_neg: a1", i, a1.get(i), 123);
      }
      test_2ci_neg(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci_neg: a1", i, a1.get(i), -123);
        errn += verify("test_2ci_neg: a2", i, a2.get(i), -103);
      }
      test_2vi_neg(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi_neg: a1", i, a1.get(i), 123);
        errn += verify("test_2vi_neg: a2", i, a2.get(i), 103);
      }
      // Reset for opposite stride
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_ci_oppos(a1, -1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ci_oppos: a1", i, a1.get(i), -123);
      }
      test_vi_oppos(a2, 123, -1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_vi_oppos: a2", i, a2.get(i), 123);
      }
      test_cp_oppos(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_cp_oppos: a1", i, a1.get(i), 123);
      }
      test_2ci_oppos(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2ci_oppos: a1", i, a1.get(i), -123);
        errn += verify("test_2ci_oppos: a2", i, a2.get(i), -103);
      }
      test_2vi_oppos(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_2vi_oppos: a1", i, a1.get(i), 123);
        errn += verify("test_2vi_oppos: a2", i, a2.get(i), 103);
      }
      // Reset for indexing with offset
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_ci_off(a1, -1);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_ci_off: a1", i, a1.get(i), -123);
      }
      test_vi_off(a2, 123, -1);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_vi_off: a2", i, a2.get(i), 123);
      }
      test_cp_off(a1, a2);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_cp_off: a1", i, a1.get(i), 123);
      }
      test_2ci_off(a1, a2);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2ci_off: a1", i, a1.get(i), -123);
        errn += verify("test_2ci_off: a2", i, a2.get(i), -103);
      }
      test_2vi_off(a1, a2, 123, 103);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2vi_off: a1", i, a1.get(i), 123);
        errn += verify("test_2vi_off: a2", i, a2.get(i), 103);
      }
      for (int i=0; i<OFFSET; i++) {
        errn += verify("test_2vi_off: a1", i, a1.get(i), -1);
        errn += verify("test_2vi_off: a2", i, a2.get(i), -1);
      }
      // Reset for indexing with invariant offset
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_ci_inv(a1, OFFSET, -1);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_ci_inv: a1", i, a1.get(i), -123);
      }
      test_vi_inv(a2, 123, OFFSET, -1);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_vi_inv: a2", i, a2.get(i), 123);
      }
      test_cp_inv(a1, a2, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_cp_inv: a1", i, a1.get(i), 123);
      }
      test_2ci_inv(a1, a2, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2ci_inv: a1", i, a1.get(i), -123);
        errn += verify("test_2ci_inv: a2", i, a2.get(i), -103);
      }
      test_2vi_inv(a1, a2, 123, 103, OFFSET);
      for (int i=OFFSET; i<ARRLEN; i++) {
        errn += verify("test_2vi_inv: a1", i, a1.get(i), 123);
        errn += verify("test_2vi_inv: a2", i, a2.get(i), 103);
      }
      for (int i=0; i<OFFSET; i++) {
        errn += verify("test_2vi_inv: a1", i, a1.get(i), -1);
        errn += verify("test_2vi_inv: a2", i, a2.get(i), -1);
      }
      // Reset for indexing with scale
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_ci_scl(a1, -1);
      for (int i=0; i<ARRLEN; i++) {
        int val = (i%SCALE != 0) ? -1 : -123;
        errn += verify("test_ci_scl: a1", i, a1.get(i), val);
      }
      test_vi_scl(a2, 123, -1);
      for (int i=0; i<ARRLEN; i++) {
        int val = (i%SCALE != 0) ? -1 : 123;
        errn += verify("test_vi_scl: a2", i, a2.get(i), val);
      }
      test_cp_scl(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        int val = (i%SCALE != 0) ? -1 : 123;
        errn += verify("test_cp_scl: a1", i, a1.get(i), val);
      }
      test_2ci_scl(a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        if (i%SCALE != 0) {
          errn += verify("test_2ci_scl: a1", i, a1.get(i), -1);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2ci_scl: a1", i*SCALE, a1.get(i*SCALE), -123);
        }
        if (i%SCALE != 0) {
          errn += verify("test_2ci_scl: a2", i, a2.get(i), -1);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2ci_scl: a2", i*SCALE, a2.get(i*SCALE), -103);
        }
      }
      test_2vi_scl(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN; i++) {
        if (i%SCALE != 0) {
          errn += verify("test_2vi_scl: a1", i, a1.get(i), -1);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2vi_scl: a1", i*SCALE, a1.get(i*SCALE), 123);
        }
        if (i%SCALE != 0) {
          errn += verify("test_2vi_scl: a2", i, a2.get(i), -1);
        } else if (i*SCALE < ARRLEN) {
          errn += verify("test_2vi_scl: a2", i*SCALE, a2.get(i*SCALE), 103);
        }
      }
      // Reset for 2 arrays with relative aligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_vi(a2, 123, -1);
      test_cp_alndst(a1, a2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alndst: a1", i, a1.get(i), -1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alndst: a1", i, a1.get(i), 123);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        a1.set(i, 123);
      }
      test_vi(a2, -123, 123);
      test_cp_alnsrc(a1, a2);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1.get(i), -123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_alnsrc: a1", i, a1.get(i), 123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_2ci_aln(a1, a2);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln: a1", i, a1.get(i), -1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln: a1", i, a1.get(i), -123);
      }
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln: a2", i, a2.get(i), -103);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln: a2", i, a2.get(i), -1);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_2vi_aln(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln: a1", i, a1.get(i), 123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln: a1", i, a1.get(i), -1);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln: a2", i, a2.get(i), -1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln: a2", i, a2.get(i), 103);
      }

      // Reset for 2 arrays with relative unaligned offset
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_vi(a2, 123, -1);
      test_cp_unalndst(a1, a2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1.get(i), -1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalndst: a1", i, a1.get(i), 123);
      }
      test_vi(a2, -123, 123);
      test_cp_unalnsrc(a1, a2);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1.get(i), -123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_cp_unalnsrc: a1", i, a1.get(i), 123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_2ci_unaln(a1, a2);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln: a1", i, a1.get(i), -1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln: a1", i, a1.get(i), -123);
      }
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln: a2", i, a2.get(i), -103);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln: a2", i, a2.get(i), -1);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
        a2.set(i, -1);
      }
      test_2vi_unaln(a1, a2, 123, 103);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln: a1", i, a1.get(i), 123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln: a1", i, a1.get(i), -1);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln: a2", i, a2.get(i), -1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln: a2", i, a2.get(i), 103);
      }

      // Reset for aligned overlap initialization
      for (int i=0; i<ALIGN_OFF; i++) {
        a1.set(i, i);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_cp_alndst(a1, a1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alndst_overlap: a1", i, a1.get(i), v);
      }
      for (int i=0; i<ALIGN_OFF; i++) {
        a1.set((i+ALIGN_OFF), -1);
      }
      test_cp_alnsrc(a1, a1);
      for (int i=0; i<ALIGN_OFF; i++) {
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1.get(i), -1);
      }
      for (int i=ALIGN_OFF; i<ARRLEN; i++) {
        int v = i%ALIGN_OFF;
        errn += verify("test_cp_alnsrc_overlap: a1", i, a1.get(i), v);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_2ci_aln(a1, a1);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2ci_aln_overlap: a1", i, a1.get(i), -103);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_aln_overlap: a1", i, a1.get(i), -123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_2vi_aln(a1, a1, 123, 103);
      for (int i=0; i<ARRLEN-ALIGN_OFF; i++) {
        errn += verify("test_2vi_aln_overlap: a1", i, a1.get(i), 123);
      }
      for (int i=ARRLEN-ALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_aln_overlap: a1", i, a1.get(i), 103);
      }

      // Reset for unaligned overlap initialization
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1.set(i, i);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_cp_unalndst(a1, a1);
      for (int i=0; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalndst_overlap: a1", i, a1.get(i), v);
      }
      for (int i=0; i<UNALIGN_OFF; i++) {
        a1.set((i+UNALIGN_OFF), -1);
      }
      test_cp_unalnsrc(a1, a1);
      for (int i=0; i<UNALIGN_OFF; i++) {
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1.get(i), -1);
      }
      for (int i=UNALIGN_OFF; i<ARRLEN; i++) {
        int v = i%UNALIGN_OFF;
        errn += verify("test_cp_unalnsrc_overlap: a1", i, a1.get(i), v);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_2ci_unaln(a1, a1);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2ci_unaln_overlap: a1", i, a1.get(i), -103);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2ci_unaln_overlap: a1", i, a1.get(i), -123);
      }
      for (int i=0; i<ARRLEN; i++) {
        a1.set(i, -1);
      }
      test_2vi_unaln(a1, a1, 123, 103);
      for (int i=0; i<ARRLEN-UNALIGN_OFF; i++) {
        errn += verify("test_2vi_unaln_overlap: a1", i, a1.get(i), 123);
      }
      for (int i=ARRLEN-UNALIGN_OFF; i<ARRLEN; i++) {
        errn += verify("test_2vi_unaln_overlap: a1", i, a1.get(i), 103);
      }

    }

    if (errn > 0 || test_only)
      return errn;

    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1.set(i, -1);
      a2.set(i, -1);
    }
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
      test_vi(a2, 123, -1);
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
      test_2vi(a1, a2, 123, 103);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_neg(a1, 123);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_neg: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_neg(a2, 123, 103);
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
      test_2vi_neg(a1, a2, 123, 103);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_neg: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_oppos(a1, 123);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_oppos: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_oppos(a2, 123, 103);
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
      test_2vi_oppos(a1, a2, 123, 103);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_oppos: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_off(a1, 123);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_off: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_off(a2, 123, 103);
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
      test_2vi_off(a1, a2, 123, 103);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_off: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_inv(a1, OFFSET, 123);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_inv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_inv(a2, 123, OFFSET, 103);
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
      test_2vi_inv(a1, a2, 123, 103, OFFSET);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_inv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ci_scl(a1, 123);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ci_scl: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_vi_scl(a2, 123, 103);
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
      test_2vi_scl(a1, a2, 123, 103);
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
      test_2vi_aln(a1, a2, 123, 103);
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
      test_2vi_unaln(a1, a2, 123, 103);
    }
    end = System.currentTimeMillis();
    System.out.println("test_2vi_unaln: " + (end - start));

    return errn;
  }

  static void test_ci(AtomicIntegerArray a) {
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, -1, -123);
    }
  }
  static void test_vi(AtomicIntegerArray a, int b, int old) {
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, old, b);
    }
  }
  static void test_cp(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, -123, b.get(i));
    }
  }
  static void test_2ci(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, 123, -123);
      b.compareAndSet(i, 123, -103);
    }
  }
  static void test_2vi(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, -123, c);
      b.compareAndSet(i, -103, d);
    }
  }
  static void test_ci_neg(AtomicIntegerArray a, int old) {
    for (int i = ARRLEN-1; i >= 0; i-=1) {
      a.compareAndSet(i, old, -123);
    }
  }
  static void test_vi_neg(AtomicIntegerArray a, int b, int old) {
    for (int i = ARRLEN-1; i >= 0; i-=1) {
      a.compareAndSet(i, old, b);
    }
  }
  static void test_cp_neg(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = ARRLEN-1; i >= 0; i-=1) {
      a.compareAndSet(i, -123, b.get(i));
    }
  }
  static void test_2ci_neg(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = ARRLEN-1; i >= 0; i-=1) {
      a.compareAndSet(i, 123, -123);
      b.compareAndSet(i, 123, -103);
    }
  }
  static void test_2vi_neg(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = ARRLEN-1; i >= 0; i-=1) {
      a.compareAndSet(i, -123, c);
      b.compareAndSet(i, -103, d);
    }
  }
  static void test_ci_oppos(AtomicIntegerArray a, int old) {
    int limit = ARRLEN-1;
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet((limit-i), old, -123);
    }
  }
  static void test_vi_oppos(AtomicIntegerArray a, int b, int old) {
    int limit = ARRLEN-1;
    for (int i = limit; i >= 0; i-=1) {
      a.compareAndSet((limit-i), old, b);
    }
  }
  static void test_cp_oppos(AtomicIntegerArray a, AtomicIntegerArray b) {
    int limit = ARRLEN-1;
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet(i, -123, b.get(limit-i));
    }
  }
  static void test_2ci_oppos(AtomicIntegerArray a, AtomicIntegerArray b) {
    int limit = ARRLEN-1;
    for (int i = 0; i < ARRLEN; i+=1) {
      a.compareAndSet((limit-i), 123, -123);
      b.compareAndSet(i, 123, -103);
    }
  }
  static void test_2vi_oppos(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    int limit = ARRLEN-1;
    for (int i = limit; i >= 0; i-=1) {
      a.compareAndSet(i, -123, c);
      b.compareAndSet((limit-i), -103, d);
    }
  }
  static void test_ci_off(AtomicIntegerArray a, int old) {
    for (int i = 0; i < ARRLEN-OFFSET; i+=1) {
      a.compareAndSet((i+OFFSET), old, -123);
    }
  }
  static void test_vi_off(AtomicIntegerArray a, int b, int old) {
    for (int i = 0; i < ARRLEN-OFFSET; i+=1) {
      a.compareAndSet((i+OFFSET), old, b);
    }
  }
  static void test_cp_off(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-OFFSET; i+=1) {
      a.compareAndSet((i+OFFSET), -123, b.get(i+OFFSET));
    }
  }
  static void test_2ci_off(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-OFFSET; i+=1) {
      a.compareAndSet((i+OFFSET), 123, -123);
      b.compareAndSet((i+OFFSET), 123, -103);
    }
  }
  static void test_2vi_off(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = 0; i < ARRLEN-OFFSET; i+=1) {
      a.compareAndSet((i+OFFSET), -123, c);
      b.compareAndSet((i+OFFSET), -103, d);
    }
  }
  static void test_ci_inv(AtomicIntegerArray a, int k, int old) {
    for (int i = 0; i < ARRLEN-k; i+=1) {
      a.compareAndSet((i+k), old, -123);
    }
  }
  static void test_vi_inv(AtomicIntegerArray a, int b, int k, int old) {
    for (int i = 0; i < ARRLEN-k; i+=1) {
      a.compareAndSet((i+k), old, b);
    }
  }
  static void test_cp_inv(AtomicIntegerArray a, AtomicIntegerArray b, int k) {
    for (int i = 0; i < ARRLEN-k; i+=1) {
      a.compareAndSet((i+k), -123, b.get(i+k));
    }
  }
  static void test_2ci_inv(AtomicIntegerArray a, AtomicIntegerArray b, int k) {
    for (int i = 0; i < ARRLEN-k; i+=1) {
      a.compareAndSet((i+k), 123, -123);
      b.compareAndSet((i+k), 123, -103);
    }
  }
  static void test_2vi_inv(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d, int k) {
    for (int i = 0; i < ARRLEN-k; i+=1) {
      a.compareAndSet((i+k), -123, c);
      b.compareAndSet((i+k), -103, d);
    }
  }
  static void test_ci_scl(AtomicIntegerArray a, int old) {
    for (int i = 0; i*SCALE < ARRLEN; i+=1) {
      a.compareAndSet((i*SCALE), old, -123);
    }
  }
  static void test_vi_scl(AtomicIntegerArray a, int b, int old) {
    for (int i = 0; i*SCALE < ARRLEN; i+=1) {
      a.compareAndSet((i*SCALE), old, b);
    }
  }
  static void test_cp_scl(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i*SCALE < ARRLEN; i+=1) {
      a.compareAndSet((i*SCALE), -123, b.get(i*SCALE));
    }
  }
  static void test_2ci_scl(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i*SCALE < ARRLEN; i+=1) {
      a.compareAndSet((i*SCALE), 123, -123);
      b.compareAndSet((i*SCALE), 123, -103);
    }
  }
  static void test_2vi_scl(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = 0; i*SCALE < ARRLEN; i+=1) {
      a.compareAndSet((i*SCALE), -123, c);
      b.compareAndSet((i*SCALE), -103, d);
    }
  }
  static void test_cp_alndst(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-ALIGN_OFF; i+=1) {
      a.compareAndSet((i+ALIGN_OFF), -1, b.get(i));
    }
  }
  static void test_cp_alnsrc(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-ALIGN_OFF; i+=1) {
      a.getAndSet(i, b.get(i+ALIGN_OFF));
    }
  }
  static void test_2ci_aln(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-ALIGN_OFF; i+=1) {
      a.compareAndSet((i+ALIGN_OFF), -1, -123);
      b.getAndSet(i, -103);
    }
  }
  static void test_2vi_aln(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = 0; i < ARRLEN-ALIGN_OFF; i+=1) {
      a.getAndSet(i, c);
      b.getAndSet((i+ALIGN_OFF), d);
    }
  }
  static void test_cp_unalndst(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-UNALIGN_OFF; i+=1) {
      a.compareAndSet((i+UNALIGN_OFF), -1, b.get(i));
    }
  }
  static void test_cp_unalnsrc(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-UNALIGN_OFF; i+=1) {
      a.getAndSet(i, b.get(i+UNALIGN_OFF));
    }
  }
  static void test_2ci_unaln(AtomicIntegerArray a, AtomicIntegerArray b) {
    for (int i = 0; i < ARRLEN-UNALIGN_OFF; i+=1) {
      a.compareAndSet((i+UNALIGN_OFF), -1, -123);
      b.getAndSet(i, -103);
    }
  }
  static void test_2vi_unaln(AtomicIntegerArray a, AtomicIntegerArray b, int c, int d) {
    for (int i = 0; i < ARRLEN-UNALIGN_OFF; i+=1) {
      a.getAndSet(i, c);
      b.getAndSet((i+UNALIGN_OFF), d);
    }
  }

  static int verify(String text, int i, int elem, int val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
