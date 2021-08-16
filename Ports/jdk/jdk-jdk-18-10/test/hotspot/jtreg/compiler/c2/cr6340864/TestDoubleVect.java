/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6340864
 * @summary Implement vectorization optimizations in hotspot-server
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m compiler.c2.cr6340864.TestDoubleVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=8 compiler.c2.cr6340864.TestDoubleVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=16 compiler.c2.cr6340864.TestDoubleVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=32 compiler.c2.cr6340864.TestDoubleVect
 */

package compiler.c2.cr6340864;

public class TestDoubleVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final double ADD_INIT = -7500.;
  private static final double VALUE = 15.;

  public static void main(String args[]) {
    System.out.println("Testing Double vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    double[] a0 = new double[ARRLEN];
    double[] a1 = new double[ARRLEN];
    double[] a2 = new double[ARRLEN];
    double[] a3 = new double[ARRLEN];
    // Initialize
    double gold_sum = 0;
    for (int i=0; i<ARRLEN; i++) {
      double val = ADD_INIT+(double)i;
      gold_sum += val;
      a1[i] = val;
      a2[i] = VALUE;
      a3[i] = -VALUE;
    }

    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_sum(a1);
      test_addc(a0, a1);
      test_addv(a0, a1, VALUE);
      test_adda(a0, a1, a2);
      test_subc(a0, a1);
      test_subv(a0, a1, VALUE);
      test_suba(a0, a1, a2);
      test_mulc(a0, a1);
      test_mulv(a0, a1, VALUE);
      test_mula(a0, a1, a2);
      test_divc(a0, a1);
      test_divv(a0, a1, VALUE);
      test_diva(a0, a1, a2);
      test_mulc_n(a0, a1);
      test_mulv(a0, a1, -VALUE);
      test_mula(a0, a1, a3);
      test_divc_n(a0, a1);
      test_divv(a0, a1, -VALUE);
      test_diva(a0, a1, a3);
      test_negc(a0, a1);
      test_rint(a0, a1);
      test_ceil(a0, a1);
      test_floor(a0, a1);
      test_sqrt(a0, a1);
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      double sum = test_sum(a1);
      if (sum != gold_sum) {
        System.err.println("test_sum:  " + sum + " != " + gold_sum);
        errn++;
      }
      // Overwrite with NaN values
      a1[0] = Double.NaN;
      a1[1] = Double.POSITIVE_INFINITY;
      a1[2] = Double.NEGATIVE_INFINITY;
      a1[3] = Double.MAX_VALUE;
      a1[4] = Double.MIN_VALUE;
      a1[5] = Double.MIN_NORMAL;

      a2[6] = a1[0];
      a2[7] = a1[1];
      a2[8] = a1[2];
      a2[9] = a1[3];
      a2[10] = a1[4];
      a2[11] = a1[5];

      a3[6] = -a2[6];
      a3[7] = -a2[7];
      a3[8] = -a2[8];
      a3[9] = -a2[9];
      a3[10] = -a2[10];
      a3[11] = -a2[11];

      test_addc(a0, a1);
      errn += verify("test_addc: ", 0, a0[0], (Double.NaN+VALUE));
      errn += verify("test_addc: ", 1, a0[1], (Double.POSITIVE_INFINITY+VALUE));
      errn += verify("test_addc: ", 2, a0[2], (Double.NEGATIVE_INFINITY+VALUE));
      errn += verify("test_addc: ", 3, a0[3], (Double.MAX_VALUE+VALUE));
      errn += verify("test_addc: ", 4, a0[4], (Double.MIN_VALUE+VALUE));
      errn += verify("test_addc: ", 5, a0[5], (Double.MIN_NORMAL+VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_addc: ", i, a0[i], ((ADD_INIT+i)+VALUE));
      }
      test_addv(a0, a1, VALUE);
      errn += verify("test_addv: ", 0, a0[0], (Double.NaN+VALUE));
      errn += verify("test_addv: ", 1, a0[1], (Double.POSITIVE_INFINITY+VALUE));
      errn += verify("test_addv: ", 2, a0[2], (Double.NEGATIVE_INFINITY+VALUE));
      errn += verify("test_addv: ", 3, a0[3], (Double.MAX_VALUE+VALUE));
      errn += verify("test_addv: ", 4, a0[4], (Double.MIN_VALUE+VALUE));
      errn += verify("test_addv: ", 5, a0[5], (Double.MIN_NORMAL+VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_addv: ", i, a0[i], ((ADD_INIT+i)+VALUE));
      }
      test_adda(a0, a1, a2);
      errn += verify("test_adda: ", 0, a0[0], (Double.NaN+VALUE));
      errn += verify("test_adda: ", 1, a0[1], (Double.POSITIVE_INFINITY+VALUE));
      errn += verify("test_adda: ", 2, a0[2], (Double.NEGATIVE_INFINITY+VALUE));
      errn += verify("test_adda: ", 3, a0[3], (Double.MAX_VALUE+VALUE));
      errn += verify("test_adda: ", 4, a0[4], (Double.MIN_VALUE+VALUE));
      errn += verify("test_adda: ", 5, a0[5], (Double.MIN_NORMAL+VALUE));
      errn += verify("test_adda: ", 6, a0[6], ((ADD_INIT+6)+Double.NaN));
      errn += verify("test_adda: ", 7, a0[7], ((ADD_INIT+7)+Double.POSITIVE_INFINITY));
      errn += verify("test_adda: ", 8, a0[8], ((ADD_INIT+8)+Double.NEGATIVE_INFINITY));
      errn += verify("test_adda: ", 9, a0[9], ((ADD_INIT+9)+Double.MAX_VALUE));
      errn += verify("test_adda: ", 10, a0[10], ((ADD_INIT+10)+Double.MIN_VALUE));
      errn += verify("test_adda: ", 11, a0[11], ((ADD_INIT+11)+Double.MIN_NORMAL));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_adda: ", i, a0[i], ((ADD_INIT+i)+VALUE));
      }

      test_subc(a0, a1);
      errn += verify("test_subc: ", 0, a0[0], (Double.NaN-VALUE));
      errn += verify("test_subc: ", 1, a0[1], (Double.POSITIVE_INFINITY-VALUE));
      errn += verify("test_subc: ", 2, a0[2], (Double.NEGATIVE_INFINITY-VALUE));
      errn += verify("test_subc: ", 3, a0[3], (Double.MAX_VALUE-VALUE));
      errn += verify("test_subc: ", 4, a0[4], (Double.MIN_VALUE-VALUE));
      errn += verify("test_subc: ", 5, a0[5], (Double.MIN_NORMAL-VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_subc: ", i, a0[i], ((ADD_INIT+i)-VALUE));
      }
      test_subv(a0, a1, VALUE);
      errn += verify("test_subv: ", 0, a0[0], (Double.NaN-VALUE));
      errn += verify("test_subv: ", 1, a0[1], (Double.POSITIVE_INFINITY-VALUE));
      errn += verify("test_subv: ", 2, a0[2], (Double.NEGATIVE_INFINITY-VALUE));
      errn += verify("test_subv: ", 3, a0[3], (Double.MAX_VALUE-VALUE));
      errn += verify("test_subv: ", 4, a0[4], (Double.MIN_VALUE-VALUE));
      errn += verify("test_subv: ", 5, a0[5], (Double.MIN_NORMAL-VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_subv: ", i, a0[i], ((ADD_INIT+i)-VALUE));
      }
      test_suba(a0, a1, a2);
      errn += verify("test_suba: ", 0, a0[0], (Double.NaN-VALUE));
      errn += verify("test_suba: ", 1, a0[1], (Double.POSITIVE_INFINITY-VALUE));
      errn += verify("test_suba: ", 2, a0[2], (Double.NEGATIVE_INFINITY-VALUE));
      errn += verify("test_suba: ", 3, a0[3], (Double.MAX_VALUE-VALUE));
      errn += verify("test_suba: ", 4, a0[4], (Double.MIN_VALUE-VALUE));
      errn += verify("test_suba: ", 5, a0[5], (Double.MIN_NORMAL-VALUE));
      errn += verify("test_suba: ", 6, a0[6], ((ADD_INIT+6)-Double.NaN));
      errn += verify("test_suba: ", 7, a0[7], ((ADD_INIT+7)-Double.POSITIVE_INFINITY));
      errn += verify("test_suba: ", 8, a0[8], ((ADD_INIT+8)-Double.NEGATIVE_INFINITY));
      errn += verify("test_suba: ", 9, a0[9], ((ADD_INIT+9)-Double.MAX_VALUE));
      errn += verify("test_suba: ", 10, a0[10], ((ADD_INIT+10)-Double.MIN_VALUE));
      errn += verify("test_suba: ", 11, a0[11], ((ADD_INIT+11)-Double.MIN_NORMAL));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_suba: ", i, a0[i], ((ADD_INIT+i)-VALUE));
      }

      test_mulc(a0, a1);
      errn += verify("test_mulc: ", 0, a0[0], (Double.NaN*VALUE));
      errn += verify("test_mulc: ", 1, a0[1], (Double.POSITIVE_INFINITY*VALUE));
      errn += verify("test_mulc: ", 2, a0[2], (Double.NEGATIVE_INFINITY*VALUE));
      errn += verify("test_mulc: ", 3, a0[3], (Double.MAX_VALUE*VALUE));
      errn += verify("test_mulc: ", 4, a0[4], (Double.MIN_VALUE*VALUE));
      errn += verify("test_mulc: ", 5, a0[5], (Double.MIN_NORMAL*VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_mulc: ", i, a0[i], ((ADD_INIT+i)*VALUE));
      }
      test_mulv(a0, a1, VALUE);
      errn += verify("test_mulv: ", 0, a0[0], (Double.NaN*VALUE));
      errn += verify("test_mulv: ", 1, a0[1], (Double.POSITIVE_INFINITY*VALUE));
      errn += verify("test_mulv: ", 2, a0[2], (Double.NEGATIVE_INFINITY*VALUE));
      errn += verify("test_mulv: ", 3, a0[3], (Double.MAX_VALUE*VALUE));
      errn += verify("test_mulv: ", 4, a0[4], (Double.MIN_VALUE*VALUE));
      errn += verify("test_mulv: ", 5, a0[5], (Double.MIN_NORMAL*VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_mulv: ", i, a0[i], ((ADD_INIT+i)*VALUE));
      }
      test_mula(a0, a1, a2);
      errn += verify("test_mula: ", 0, a0[0], (Double.NaN*VALUE));
      errn += verify("test_mula: ", 1, a0[1], (Double.POSITIVE_INFINITY*VALUE));
      errn += verify("test_mula: ", 2, a0[2], (Double.NEGATIVE_INFINITY*VALUE));
      errn += verify("test_mula: ", 3, a0[3], (Double.MAX_VALUE*VALUE));
      errn += verify("test_mula: ", 4, a0[4], (Double.MIN_VALUE*VALUE));
      errn += verify("test_mula: ", 5, a0[5], (Double.MIN_NORMAL*VALUE));
      errn += verify("test_mula: ", 6, a0[6], ((ADD_INIT+6)*Double.NaN));
      errn += verify("test_mula: ", 7, a0[7], ((ADD_INIT+7)*Double.POSITIVE_INFINITY));
      errn += verify("test_mula: ", 8, a0[8], ((ADD_INIT+8)*Double.NEGATIVE_INFINITY));
      errn += verify("test_mula: ", 9, a0[9], ((ADD_INIT+9)*Double.MAX_VALUE));
      errn += verify("test_mula: ", 10, a0[10], ((ADD_INIT+10)*Double.MIN_VALUE));
      errn += verify("test_mula: ", 11, a0[11], ((ADD_INIT+11)*Double.MIN_NORMAL));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_mula: ", i, a0[i], ((ADD_INIT+i)*VALUE));
      }

      test_divc(a0, a1);
      errn += verify("test_divc: ", 0, a0[0], (Double.NaN/VALUE));
      errn += verify("test_divc: ", 1, a0[1], (Double.POSITIVE_INFINITY/VALUE));
      errn += verify("test_divc: ", 2, a0[2], (Double.NEGATIVE_INFINITY/VALUE));
      errn += verify("test_divc: ", 3, a0[3], (Double.MAX_VALUE/VALUE));
      errn += verify("test_divc: ", 4, a0[4], (Double.MIN_VALUE/VALUE));
      errn += verify("test_divc: ", 5, a0[5], (Double.MIN_NORMAL/VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_divc: ", i, a0[i], ((ADD_INIT+i)/VALUE));
      }
      test_divv(a0, a1, VALUE);
      errn += verify("test_divv: ", 0, a0[0], (Double.NaN/VALUE));
      errn += verify("test_divv: ", 1, a0[1], (Double.POSITIVE_INFINITY/VALUE));
      errn += verify("test_divv: ", 2, a0[2], (Double.NEGATIVE_INFINITY/VALUE));
      errn += verify("test_divv: ", 3, a0[3], (Double.MAX_VALUE/VALUE));
      errn += verify("test_divv: ", 4, a0[4], (Double.MIN_VALUE/VALUE));
      errn += verify("test_divv: ", 5, a0[5], (Double.MIN_NORMAL/VALUE));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_divv: ", i, a0[i], ((ADD_INIT+i)/VALUE));
      }
      test_diva(a0, a1, a2);
      errn += verify("test_diva: ", 0, a0[0], (Double.NaN/VALUE));
      errn += verify("test_diva: ", 1, a0[1], (Double.POSITIVE_INFINITY/VALUE));
      errn += verify("test_diva: ", 2, a0[2], (Double.NEGATIVE_INFINITY/VALUE));
      errn += verify("test_diva: ", 3, a0[3], (Double.MAX_VALUE/VALUE));
      errn += verify("test_diva: ", 4, a0[4], (Double.MIN_VALUE/VALUE));
      errn += verify("test_diva: ", 5, a0[5], (Double.MIN_NORMAL/VALUE));
      errn += verify("test_diva: ", 6, a0[6], ((ADD_INIT+6)/Double.NaN));
      errn += verify("test_diva: ", 7, a0[7], ((ADD_INIT+7)/Double.POSITIVE_INFINITY));
      errn += verify("test_diva: ", 8, a0[8], ((ADD_INIT+8)/Double.NEGATIVE_INFINITY));
      errn += verify("test_diva: ", 9, a0[9], ((ADD_INIT+9)/Double.MAX_VALUE));
      errn += verify("test_diva: ", 10, a0[10], ((ADD_INIT+10)/Double.MIN_VALUE));
      errn += verify("test_diva: ", 11, a0[11], ((ADD_INIT+11)/Double.MIN_NORMAL));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_diva: ", i, a0[i], ((ADD_INIT+i)/VALUE));
      }

      test_mulc_n(a0, a1);
      errn += verify("test_mulc_n: ", 0, a0[0], (Double.NaN*(-VALUE)));
      errn += verify("test_mulc_n: ", 1, a0[1], (Double.POSITIVE_INFINITY*(-VALUE)));
      errn += verify("test_mulc_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY*(-VALUE)));
      errn += verify("test_mulc_n: ", 3, a0[3], (Double.MAX_VALUE*(-VALUE)));
      errn += verify("test_mulc_n: ", 4, a0[4], (Double.MIN_VALUE*(-VALUE)));
      errn += verify("test_mulc_n: ", 5, a0[5], (Double.MIN_NORMAL*(-VALUE)));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_mulc_n: ", i, a0[i], ((ADD_INIT+i)*(-VALUE)));
      }
      test_mulv(a0, a1, -VALUE);
      errn += verify("test_mulv_n: ", 0, a0[0], (Double.NaN*(-VALUE)));
      errn += verify("test_mulv_n: ", 1, a0[1], (Double.POSITIVE_INFINITY*(-VALUE)));
      errn += verify("test_mulv_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY*(-VALUE)));
      errn += verify("test_mulv_n: ", 3, a0[3], (Double.MAX_VALUE*(-VALUE)));
      errn += verify("test_mulv_n: ", 4, a0[4], (Double.MIN_VALUE*(-VALUE)));
      errn += verify("test_mulv_n: ", 5, a0[5], (Double.MIN_NORMAL*(-VALUE)));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_mulv_n: ", i, a0[i], ((ADD_INIT+i)*(-VALUE)));
      }
      test_mula(a0, a1, a3);
      errn += verify("test_mula_n: ", 0, a0[0], (Double.NaN*(-VALUE)));
      errn += verify("test_mula_n: ", 1, a0[1], (Double.POSITIVE_INFINITY*(-VALUE)));
      errn += verify("test_mula_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY*(-VALUE)));
      errn += verify("test_mula_n: ", 3, a0[3], (Double.MAX_VALUE*(-VALUE)));
      errn += verify("test_mula_n: ", 4, a0[4], (Double.MIN_VALUE*(-VALUE)));
      errn += verify("test_mula_n: ", 5, a0[5], (Double.MIN_NORMAL*(-VALUE)));
      errn += verify("test_mula_n: ", 6, a0[6], ((ADD_INIT+6)*(-Double.NaN)));
      errn += verify("test_mula_n: ", 7, a0[7], ((ADD_INIT+7)*(-Double.POSITIVE_INFINITY)));
      errn += verify("test_mula_n: ", 8, a0[8], ((ADD_INIT+8)*(-Double.NEGATIVE_INFINITY)));
      errn += verify("test_mula_n: ", 9, a0[9], ((ADD_INIT+9)*(-Double.MAX_VALUE)));
      errn += verify("test_mula_n: ", 10, a0[10], ((ADD_INIT+10)*(-Double.MIN_VALUE)));
      errn += verify("test_mula_n: ", 11, a0[11], ((ADD_INIT+11)*(-Double.MIN_NORMAL)));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_mula_n: ", i, a0[i], ((ADD_INIT+i)*(-VALUE)));
      }

      test_divc_n(a0, a1);
      errn += verify("test_divc_n: ", 0, a0[0], (Double.NaN/(-VALUE)));
      errn += verify("test_divc_n: ", 1, a0[1], (Double.POSITIVE_INFINITY/(-VALUE)));
      errn += verify("test_divc_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY/(-VALUE)));
      errn += verify("test_divc_n: ", 3, a0[3], (Double.MAX_VALUE/(-VALUE)));
      errn += verify("test_divc_n: ", 4, a0[4], (Double.MIN_VALUE/(-VALUE)));
      errn += verify("test_divc_n: ", 5, a0[5], (Double.MIN_NORMAL/(-VALUE)));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_divc_n: ", i, a0[i], ((ADD_INIT+i)/(-VALUE)));
      }
      test_divv(a0, a1, -VALUE);
      errn += verify("test_divv_n: ", 0, a0[0], (Double.NaN/(-VALUE)));
      errn += verify("test_divv_n: ", 1, a0[1], (Double.POSITIVE_INFINITY/(-VALUE)));
      errn += verify("test_divv_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY/(-VALUE)));
      errn += verify("test_divv_n: ", 3, a0[3], (Double.MAX_VALUE/(-VALUE)));
      errn += verify("test_divv_n: ", 4, a0[4], (Double.MIN_VALUE/(-VALUE)));
      errn += verify("test_divv_n: ", 5, a0[5], (Double.MIN_NORMAL/(-VALUE)));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_divv_n: ", i, a0[i], ((ADD_INIT+i)/(-VALUE)));
      }
      test_diva(a0, a1, a3);
      errn += verify("test_diva_n: ", 0, a0[0], (Double.NaN/(-VALUE)));
      errn += verify("test_diva_n: ", 1, a0[1], (Double.POSITIVE_INFINITY/(-VALUE)));
      errn += verify("test_diva_n: ", 2, a0[2], (Double.NEGATIVE_INFINITY/(-VALUE)));
      errn += verify("test_diva_n: ", 3, a0[3], (Double.MAX_VALUE/(-VALUE)));
      errn += verify("test_diva_n: ", 4, a0[4], (Double.MIN_VALUE/(-VALUE)));
      errn += verify("test_diva_n: ", 5, a0[5], (Double.MIN_NORMAL/(-VALUE)));
      errn += verify("test_diva_n: ", 6, a0[6], ((ADD_INIT+6)/(-Double.NaN)));
      errn += verify("test_diva_n: ", 7, a0[7], ((ADD_INIT+7)/(-Double.POSITIVE_INFINITY)));
      errn += verify("test_diva_n: ", 8, a0[8], ((ADD_INIT+8)/(-Double.NEGATIVE_INFINITY)));
      errn += verify("test_diva_n: ", 9, a0[9], ((ADD_INIT+9)/(-Double.MAX_VALUE)));
      errn += verify("test_diva_n: ", 10, a0[10], ((ADD_INIT+10)/(-Double.MIN_VALUE)));
      errn += verify("test_diva_n: ", 11, a0[11], ((ADD_INIT+11)/(-Double.MIN_NORMAL)));
      for (int i=12; i<ARRLEN; i++) {
        errn += verify("test_diva_n: ", i, a0[i], ((ADD_INIT+i)/(-VALUE)));
      }
      test_negc(a0, a1);
      errn += verify("test_negc: ", 0, a0[0], (Double.NaN));
      errn += verify("test_negc: ", 1, a0[1], (Double.NEGATIVE_INFINITY));
      errn += verify("test_negc: ", 2, a0[2], (Double.POSITIVE_INFINITY));
      errn += verify("test_negc: ", 3, a0[3], (double)(-Double.MAX_VALUE));
      errn += verify("test_negc: ", 4, a0[4], (double)(-Double.MIN_VALUE));
      errn += verify("test_negc: ", 5, a0[5], (double)(-Double.MIN_NORMAL));
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_negc: ", i, a0[i], (double)(-((double)(ADD_INIT+i))));
      }

      // To test -ve and +ve Zero scenarios.
      double [] other_corner_cases     = { -0.0, 0.0, 9.007199254740992E15 };
      double [] other_corner_cases_res = new double[3];
      test_floor(a0, a1);
      errn += verify("test_floor: ", 0, a0[0], Double.NaN);
      errn += verify("test_floor: ", 1, a0[1], Double.POSITIVE_INFINITY);
      errn += verify("test_floor: ", 2, a0[2], Double.NEGATIVE_INFINITY);
      errn += verify("test_floor: ", 3, a0[3], Double.MAX_VALUE);
      errn += verify("test_floor: ", 4, a0[4], 0.0);
      errn += verify("test_floor: ", 5, a0[5], 0.0);
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_floor: ", i, a0[i], ((double)(ADD_INIT+i)));
      }
      test_floor_cc(other_corner_cases_res, other_corner_cases);
      errn += verify("test_floor_cc: ", 0, other_corner_cases_res[0], -0.0);
      errn += verify("test_floor_cc: ", 1, other_corner_cases_res[1], 0.0);
      errn += verify("test_floor_cc: ", 2, other_corner_cases_res[2], 9.007199254740992E15);

      test_ceil(a0, a1);
      errn += verify("test_ceil: ", 0, a0[0], Double.NaN);
      errn += verify("test_ceil: ", 1, a0[1], Double.POSITIVE_INFINITY);
      errn += verify("test_ceil: ", 2, a0[2], Double.NEGATIVE_INFINITY);
      errn += verify("test_ceil: ", 3, a0[3], Double.MAX_VALUE);
      errn += verify("test_ceil: ", 4, a0[4], 1.0);
      errn += verify("test_ceil: ", 5, a0[5], 1.0);
      for (int i=6; i<ARRLEN; i++) {
        errn += verify("test_ceil: ", i, a0[i], ((double)(ADD_INIT+i+1.0)));
      }
      test_ceil_cc(other_corner_cases_res, other_corner_cases);
      errn += verify("test_ceil_cc: ", 0, other_corner_cases_res[0], -0.0);
      errn += verify("test_ceil_cc: ", 1, other_corner_cases_res[1], 0.0);
      errn += verify("test_ceil_cc: ", 2, other_corner_cases_res[2], 9.007199254740992E15);

      test_rint(a0, a1);
      errn += verify("test_rint: ", 0, a0[0], Double.NaN);
      errn += verify("test_rint: ", 1, a0[1], Double.POSITIVE_INFINITY);
      errn += verify("test_rint: ", 2, a0[2], Double.NEGATIVE_INFINITY);
      errn += verify("test_rint: ", 3, a0[3], Double.MAX_VALUE);
      errn += verify("test_rint: ", 4, a0[4], 0.0);
      errn += verify("test_rint: ", 5, a0[5], 0.0);
      for (int i=6; i<ARRLEN; i++) {
        if ( i <= 500 )
           errn += verify("test_rint: ", i, a0[i], ((double)(ADD_INIT+i)));
        else
           errn += verify("test_rint: ", i, a0[i], ((double)(ADD_INIT+i+1.0)));
      }
      test_rint_cc(other_corner_cases_res, other_corner_cases);
      errn += verify("test_rint_cc: ", 0, other_corner_cases_res[0], -0.0);
      errn += verify("test_rint_cc: ", 1, other_corner_cases_res[1], 0.0);
      errn += verify("test_rint_cc: ", 2, other_corner_cases_res[2], 9.007199254740992E15);

      // Overwrite with +0.0/-0.0 values
      a1[6] = (double)0.0;
      a1[7] = (double)-0.0;
      test_sqrt(a0, a1);
      errn += verify("test_sqrt: ", 0, a0[0], (Double.NaN));
      errn += verify("test_sqrt: ", 1, a0[1], (Double.POSITIVE_INFINITY));
      errn += verify("test_sqrt: ", 2, a0[2], (Double.NaN));
      errn += verify("test_sqrt: ", 3, a0[3], Math.sqrt(Double.MAX_VALUE));
      errn += verify("test_sqrt: ", 4, a0[4], Math.sqrt(Double.MIN_VALUE));
      errn += verify("test_sqrt: ", 5, a0[5], Math.sqrt(Double.MIN_NORMAL));
      errn += verify("test_sqrt: ", 6, a0[6], (double)0.0);
      errn += verify("test_sqrt: ", 7, a0[7], (double)-0.0);
      for (int i=8; i<ARRLEN; i++) {
        errn += verify("test_sqrt: ", i, a0[i], Math.sqrt((double)(ADD_INIT+i)));
      }
    }

    if (errn > 0)
      return errn;

    System.out.println("Time");
    long start, end;

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sum(a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sum: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_addc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_addc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_addv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_addv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_adda(a0, a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_adda: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_subc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_subc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_subv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_subv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_suba(a0, a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_suba: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mulc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mulc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mulv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mulv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mula(a0, a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mula: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_divc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_divc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_divv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_divv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_diva(a0, a1, a2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_diva: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mulc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mulc_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mulv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mulv_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_mula(a0, a1, a3);
    }
    end = System.currentTimeMillis();
    System.out.println("test_mula_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_divc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_divc_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_divv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_divv_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_diva(a0, a1, a3);
    }
    end = System.currentTimeMillis();
    System.out.println("test_diva_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_negc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_negc_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sqrt(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sqrt_n: " + (end - start));

    return errn;
  }

  static double test_sum(double[] a1) {
    double sum = 0;
    for (int i = 0; i < a1.length; i+=1) {
      sum += a1[i];
    }
    return sum;
  }

  static void test_addc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]+VALUE);
    }
  }
  static void test_addv(double[] a0, double[] a1, double b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]+b);
    }
  }
  static void test_adda(double[] a0, double[] a1, double[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]+a2[i]);
    }
  }

  static void test_subc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]-VALUE);
    }
  }
  static void test_subv(double[] a0, double[] a1, double b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]-b);
    }
  }
  static void test_suba(double[] a0, double[] a1, double[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]-a2[i]);
    }
  }

  static void test_mulc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]*VALUE);
    }
  }
  static void test_mulc_n(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]*(-VALUE));
    }
  }
  static void test_mulv(double[] a0, double[] a1, double b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]*b);
    }
  }
  static void test_mula(double[] a0, double[] a1, double[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]*a2[i]);
    }
  }

  static void test_divc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]/VALUE);
    }
  }
  static void test_divc_n(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]/(-VALUE));
    }
  }
  static void test_divv(double[] a0, double[] a1, double b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]/b);
    }
  }
  static void test_diva(double[] a0, double[] a1, double[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (a1[i]/a2[i]);
    }
  }
  static void test_negc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (double)(-((double)a1[i]));
    }
  }

  static void test_rint(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.rint(a1[i] + ((double)(i))/1000);
    }
  }
  static void test_ceil(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.ceil(a1[i] + ((double)(i))/1000);
    }
  }
  static void test_floor(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.floor(a1[i] + ((double)(i))/1000);
    }
  }
  static void test_rint_cc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.rint(a1[i]);
    }
  }
  static void test_ceil_cc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.ceil(a1[i]);
    }
  }
  static void test_floor_cc(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = Math.floor(a1[i]);
    }
  }

  static void test_sqrt(double[] a0, double[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (double)(Math.sqrt((double)a1[i]));
    }
  }

  static int verify(String text, int i, double elem, double val) {
    if (elem != val && !(Double.isNaN(elem) && Double.isNaN(val))) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
