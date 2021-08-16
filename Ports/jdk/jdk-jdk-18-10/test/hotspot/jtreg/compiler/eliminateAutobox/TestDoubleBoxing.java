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

/*
 * @test
 * @bug 6934604
 * @summary enable parts of EliminateAutoBox by default
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   compiler.eliminateAutobox.TestDoubleBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::foob
 *                   compiler.eliminateAutobox.TestDoubleBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestDoubleBoxing::foob
 *                   compiler.eliminateAutobox.TestDoubleBoxing
 */

package compiler.eliminateAutobox;

public class TestDoubleBoxing {

  static final Double ibc = new Double(1.);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void   dummy()        { }
  static double foo(double i)  { return i; }
  static Double foob(double i) { return Double.valueOf(i); }


  static double simple(double i) {
    Double ib = new Double(i);
    return ib;
  }

  static double simpleb(double i) {
    Double ib = Double.valueOf(i);
    return ib;
  }

  static double simplec() {
    Double ib = ibc;
    return ib;
  }

  static double simplef(double i) {
    Double ib = foob(i);
    return ib;
  }

  static double simplep(Double ib) {
    return ib;
  }

  static double simple2(double i) {
    Double ib1 = new Double(i);
    Double ib2 = new Double(i+1.);
    return ib1 + ib2;
  }

  static double simpleb2(double i) {
    Double ib1 = Double.valueOf(i);
    Double ib2 = Double.valueOf(i+1.);
    return ib1 + ib2;
  }

  static double simplem2(double i) {
    Double ib1 = new Double(i);
    Double ib2 = Double.valueOf(i+1.);
    return ib1 + ib2;
  }

  static double simplep2(double i, Double ib1) {
    Double ib2 = Double.valueOf(i+1.);
    return ib1 + ib2;
  }

  static double simplec2(double i) {
    Double ib1 = ibc;
    Double ib2 = Double.valueOf(i+1.);
    return ib1 + ib2;
  }

  //===============================================
  static double test(double f, int i) {
    Double ib = new Double(f);
    if ((i&1) == 0)
      ib = f+1.;
    return ib;
  }

  static double testb(double f, int i) {
    Double ib = f;
    if ((i&1) == 0)
      ib = (f+1.);
    return ib;
  }

  static double testm(double f, int i) {
    Double ib = f;
    if ((i&1) == 0)
      ib = new Double(f+1.);
    return ib;
  }

  static double testp(double f, int i, Double ib) {
    if ((i&1) == 0)
      ib = new Double(f+1.);
    return ib;
  }

  static double testc(double f, int i) {
    Double ib = ibc;
    if ((i&1) == 0)
      ib = new Double(f+1.);
    return ib;
  }

  static double test2(double f, int i) {
    Double ib1 = new Double(f);
    Double ib2 = new Double(f+1.);
    if ((i&1) == 0) {
      ib1 = new Double(f+1.);
      ib2 = new Double(f+2.);
    }
    return ib1+ib2;
  }

  static double testb2(double f, int i) {
    Double ib1 = f;
    Double ib2 = f+1.;
    if ((i&1) == 0) {
      ib1 = (f+1.);
      ib2 = (f+2.);
    }
    return ib1+ib2;
  }

  static double testm2(double f, int i) {
    Double ib1 = new Double(f);
    Double ib2 = f+1.;
    if ((i&1) == 0) {
      ib1 = new Double(f+1.);
      ib2 = (f+2.);
    }
    return ib1+ib2;
  }

  static double testp2(double f, int i, Double ib1) {
    Double ib2 = f+1.;
    if ((i&1) == 0) {
      ib1 = new Double(f+1.);
      ib2 = (f+2.);
    }
    return ib1+ib2;
  }

  static double testc2(double f, int i) {
    Double ib1 = ibc;
    Double ib2 = f+1.;
    if ((i&1) == 0) {
      ib1 = (ibc+1.);
      ib2 = (f+2.);
    }
    return ib1+ib2;
  }

  //===============================================
  static double sum(double[] a) {
    double result = 1.;
    for (Double i : a)
        result += i;
    return result;
  }

  static double sumb(double[] a) {
    Double result = 1.;
    for (Double i : a)
        result += i;
    return result;
  }

  static double sumc(double[] a) {
    Double result = ibc;
    for (Double i : a)
        result += i;
    return result;
  }

  static double sumf(double[] a) {
    Double result = foob(1.);
    for (Double i : a)
        result += i;
    return result;
  }

  static double sump(double[] a, Double result) {
    for (Double i : a)
        result += i;
    return result;
  }

  static double sum2(double[] a) {
    double result1 = 1.;
    double result2 = 1.;
    for (Double i : a) {
        result1 += i;
        result2 += i + 1.;
    }
    return result1 + result2;
  }

  static double sumb2(double[] a) {
    Double result1 = 1.;
    Double result2 = 1.;
    for (Double i : a) {
        result1 += i;
        result2 += i + 1.;
    }
    return result1 + result2;
  }

  static double summ2(double[] a) {
    Double result1 = 1.;
    Double result2 = new Double(1.);
    for (Double i : a) {
        result1 += i;
        result2 += new Double(i + 1.);
    }
    return result1 + result2;
  }

  static double sump2(double[] a, Double result2) {
    Double result1 = 1.;
    for (Double i : a) {
        result1 += i;
        result2 += i + 1.;
    }
    return result1 + result2;
  }

  static double sumc2(double[] a) {
    Double result1 = 1.;
    Double result2 = ibc;
    for (Double i : a) {
        result1 += i;
        result2 += i + ibc;
    }
    return result1 + result2;
  }

  //===============================================
  static double remi_sum() {
    Double j = new Double(1.);
    for (int i = 0; i< 1000; i++) {
      j = new Double(j + 1.);
    }
    return j;
  }

  static double remi_sumb() {
    Double j = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      j = j + 1.;
    }
    return j;
  }

  static double remi_sumf() {
    Double j = foob(1.);
    for (int i = 0; i< 1000; i++) {
      j = j + 1.;
    }
    return j;
  }

  static double remi_sump(Double j) {
    for (int i = 0; i< 1000; i++) {
      j = new Double(j + 1.);
    }
    return j;
  }

  static double remi_sumc() {
    Double j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = j + ibc;
    }
    return j;
  }

  static double remi_sum2() {
    Double j1 = new Double(1.);
    Double j2 = new Double(1.);
    for (int i = 0; i< 1000; i++) {
      j1 = new Double(j1 + 1.);
      j2 = new Double(j2 + 2.);
    }
    return j1 + j2;
  }

  static double remi_sumb2() {
    Double j1 = Double.valueOf(1.);
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + 1.;
      j2 = j2 + 2.;
    }
    return j1 + j2;
  }

  static double remi_summ2() {
    Double j1 = new Double(1.);
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      j1 = new Double(j1 + 1.);
      j2 = j2 + 2.;
    }
    return j1 + j2;
  }

  static double remi_sump2(Double j1) {
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      j1 = new Double(j1 + 1.);
      j2 = j2 + 2.;
    }
    return j1 + j2;
  }

  static double remi_sumc2() {
    Double j1 = ibc;
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + ibc;
      j2 = j2 + 2.;
    }
    return j1 + j2;
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static double simple_deop(double i) {
    Double ib = new Double(foo(i));
    dummy();
    return ib;
  }

  static double simpleb_deop(double i) {
    Double ib = Double.valueOf(foo(i));
    dummy();
    return ib;
  }

  static double simplef_deop(double i) {
    Double ib = foob(i);
    dummy();
    return ib;
  }

  static double simplep_deop(Double ib) {
    dummy();
    return ib;
  }

  static double simplec_deop(double i) {
    Double ib = ibc;
    dummy();
    return ib;
  }

  static double test_deop(double f, int i) {
    Double ib = new Double(foo(f));
    if ((i&1) == 0)
      ib = foo(f+1.);
    dummy();
    return ib;
  }

  static double testb_deop(double f, int i) {
    Double ib = foo(f);
    if ((i&1) == 0)
      ib = foo(f+1.);
    dummy();
    return ib;
  }

  static double testf_deop(double f, int i) {
    Double ib = foob(f);
    if ((i&1) == 0)
      ib = foo(f+1.);
    dummy();
    return ib;
  }

  static double testp_deop(double f, int i, Double ib) {
    if ((i&1) == 0)
      ib = foo(f+1.);
    dummy();
    return ib;
  }

  static double testc_deop(double f, int i) {
    Double ib = ibc;
    if ((i&1) == 0)
      ib = foo(f+1.);
    dummy();
    return ib;
  }

  static double sum_deop(double[] a) {
    double result = 1.;
    for (Double i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static double sumb_deop(double[] a) {
    Double result = 1.;
    for (Double i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static double sumf_deop(double[] a) {
    Double result = 1.;
    for (Double i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static double sump_deop(double[] a, Double result) {
    for (Double i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static double sumc_deop(double[] a) {
    Double result = ibc;
    for (Double i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static double remi_sum_deop() {
    Double j = new Double(foo(1.));
    for (int i = 0; i< 1000; i++) {
      j = new Double(foo(j + 1.));
    }
    dummy();
    return j;
  }

  static double remi_sumb_deop() {
    Double j = Double.valueOf(foo(1.));
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.);
    }
    dummy();
    return j;
  }

  static double remi_sumf_deop() {
    Double j = foob(1.);
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.);
    }
    dummy();
    return j;
  }

  static double remi_sump_deop(Double j) {
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.);
    }
    dummy();
    return j;
  }

  static double remi_sumc_deop() {
    Double j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.);
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static double remi_sum_cond() {
    Double j = new Double(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Double(j + 1.);
      }
    }
    return j;
  }

  static double remi_sumb_cond() {
    Double j = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.;
      }
    }
    return j;
  }

  static double remi_sumf_cond() {
    Double j = foob(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.;
      }
    }
    return j;
  }

  static double remi_sump_cond(Double j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.;
      }
    }
    return j;
  }

  static double remi_sumc_cond() {
    Double j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + ibc;
      }
    }
    return j;
  }

  static double remi_sum2_cond() {
    Double j1 = new Double(1.);
    Double j2 = new Double(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Double(j1 + 1.);
      } else {
        j2 = new Double(j2 + 2.);
      }
    }
    return j1 + j2;
  }

  static double remi_sumb2_cond() {
    Double j1 = Double.valueOf(1.);
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = j1 + 1.;
      } else {
        j2 = j2 + 2.;
      }
    }
    return j1 + j2;
  }

  static double remi_summ2_cond() {
    Double j1 = new Double(1.);
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Double(j1 + 1.);
      } else {
        j2 = j2 + 2.;
      }
    }
    return j1 + j2;
  }

  static double remi_sump2_cond(Double j1) {
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Double(j1 + 1.);
      } else {
        j2 = j2 + 2.;
      }
    }
    return j1 + j2;
  }

  static double remi_sumc2_cond() {
    Double j1 = ibc;
    Double j2 = Double.valueOf(1.);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = j1 + ibc;
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }


  public static void main(String[] args) {
    final int ntests = 70;

    String[] test_name = new String[] {
        "simple",      "simpleb",      "simplec",      "simplef",      "simplep",
        "simple2",     "simpleb2",     "simplec2",     "simplem2",     "simplep2",
        "simple_deop", "simpleb_deop", "simplec_deop", "simplef_deop", "simplep_deop",
        "test",        "testb",        "testc",        "testm",        "testp",
        "test2",       "testb2",       "testc2",       "testm2",       "testp2",
        "test_deop",   "testb_deop",   "testc_deop",   "testf_deop",   "testp_deop",
        "sum",         "sumb",         "sumc",         "sumf",         "sump",
        "sum2",        "sumb2",        "sumc2",        "summ2",        "sump2",
        "sum_deop",    "sumb_deop",    "sumc_deop",    "sumf_deop",    "sump_deop",
        "remi_sum",       "remi_sumb",       "remi_sumc",       "remi_sumf",       "remi_sump",
        "remi_sum2",      "remi_sumb2",      "remi_sumc2",      "remi_summ2",      "remi_sump2",
        "remi_sum_deop",  "remi_sumb_deop",  "remi_sumc_deop",  "remi_sumf_deop",  "remi_sump_deop",
        "remi_sum_cond",  "remi_sumb_cond",  "remi_sumc_cond",  "remi_sumf_cond",  "remi_sump_cond",
        "remi_sum2_cond", "remi_sumb2_cond", "remi_sumc2_cond", "remi_summ2_cond", "remi_sump2_cond"
    };

    final double[] val = new double[] {
       71994000.,  71994000.,    12000.,  71994000.,  71994000.,
      144000000., 144000000., 72018000., 144000000., 144000000.,
       71994000.,  71994000.,    12000.,  71994000.,  71994000.,
       72000000.,  72000000., 36006000.,  72000000.,  72000000.,
      144012000., 144012000., 72030000., 144012000., 144012000.,
       72000000.,  72000000., 36006000.,  72000000.,  72000000.,
         499501.,    499501.,   499501.,    499501.,    499501.,
        1000002.,   1000002.,  1000002.,   1000002.,   1000002.,
         499501.,    499501.,   499501.,    499501.,    499501.,
           1001.,      1001.,     1001.,      1001.,      1001.,
           3002.,      3002.,     3002.,      3002.,      3002.,
           1001.,      1001.,     1001.,      1001.,      1001.,
            501.,       501.,      501.,       501.,       501.,
           1502.,      1502.,     1502.,      1502.,      1502.
    };

    double[] res = new double[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0.;
    }


    for (int i = 0; i < 12000; i++) {
      res[0] += simple(i);
      res[1] += simpleb(i);
      res[2] += simplec();
      res[3] += simplef(i);
      res[4] += simplep((double)i);

      res[5] += simple2((double)i);
      res[6] += simpleb2((double)i);
      res[7] += simplec2((double)i);
      res[8] += simplem2((double)i);
      res[9] += simplep2((double)i, (double)i);

      res[10] += simple_deop((double)i);
      res[11] += simpleb_deop((double)i);
      res[12] += simplec_deop((double)i);
      res[13] += simplef_deop((double)i);
      res[14] += simplep_deop((double)i);

      res[15] += test((double)i, i);
      res[16] += testb((double)i, i);
      res[17] += testc((double)i, i);
      res[18] += testm((double)i, i);
      res[19] += testp((double)i, i, (double)i);

      res[20] += test2((double)i, i);
      res[21] += testb2((double)i, i);
      res[22] += testc2((double)i, i);
      res[23] += testm2((double)i, i);
      res[24] += testp2((double)i, i, (double)i);

      res[25] += test_deop((double)i, i);
      res[26] += testb_deop((double)i, i);
      res[27] += testc_deop((double)i, i);
      res[28] += testf_deop((double)i, i);
      res[29] += testp_deop((double)i, i, (double)i);
    }

    double[] ia = new double[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, 1.);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, 1.);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, 1.);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump(1.);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2(1.);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop(1.);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond(1.);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond(1.);
    }

    int failed = 0;
    for (int i = 0; i < ntests; i++) {
      if (res[i] != val[i]) {
        System.err.println(test_name[i] + ": " + res[i] + " != " + val[i]);
        failed++;
      }
    }
    if (failed > 0) {
      System.err.println("Failed " + failed + " tests.");
      throw new InternalError();
    } else {
      System.out.println("Passed.");
    }
  }
}
