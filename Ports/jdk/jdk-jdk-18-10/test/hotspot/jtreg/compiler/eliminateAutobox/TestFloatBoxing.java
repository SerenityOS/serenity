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
 *                   compiler.eliminateAutobox.TestFloatBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::foob
 *                   compiler.eliminateAutobox.TestFloatBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestFloatBoxing::foob
 *                   compiler.eliminateAutobox.TestFloatBoxing
 */

package compiler.eliminateAutobox;

public class TestFloatBoxing {

  static final Float ibc = new Float(1.f);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void  dummy()       { }
  static float foo(float i)  { return i; }
  static Float foob(float i) { return Float.valueOf(i); }


  static float simple(float i) {
    Float ib = new Float(i);
    return ib;
  }

  static float simpleb(float i) {
    Float ib = Float.valueOf(i);
    return ib;
  }

  static float simplec() {
    Float ib = ibc;
    return ib;
  }

  static float simplef(float i) {
    Float ib = foob(i);
    return ib;
  }

  static float simplep(Float ib) {
    return ib;
  }

  static float simple2(float i) {
    Float ib1 = new Float(i);
    Float ib2 = new Float(i+1.f);
    return ib1 + ib2;
  }

  static float simpleb2(float i) {
    Float ib1 = Float.valueOf(i);
    Float ib2 = Float.valueOf(i+1.f);
    return ib1 + ib2;
  }

  static float simplem2(float i) {
    Float ib1 = new Float(i);
    Float ib2 = Float.valueOf(i+1.f);
    return ib1 + ib2;
  }

  static float simplep2(float i, Float ib1) {
    Float ib2 = Float.valueOf(i+1.f);
    return ib1 + ib2;
  }

  static float simplec2(float i) {
    Float ib1 = ibc;
    Float ib2 = Float.valueOf(i+1.f);
    return ib1 + ib2;
  }

  //===============================================
  static float test(float f, int i) {
    Float ib = new Float(f);
    if ((i&1) == 0)
      ib = f+1.f;
    return ib;
  }

  static float testb(float f, int i) {
    Float ib = f;
    if ((i&1) == 0)
      ib = (f+1.f);
    return ib;
  }

  static float testm(float f, int i) {
    Float ib = f;
    if ((i&1) == 0)
      ib = new Float(f+1.f);
    return ib;
  }

  static float testp(float f, int i, Float ib) {
    if ((i&1) == 0)
      ib = new Float(f+1.f);
    return ib;
  }

  static float testc(float f, int i) {
    Float ib = ibc;
    if ((i&1) == 0)
      ib = new Float(f+1.f);
    return ib;
  }

  static float test2(float f, int i) {
    Float ib1 = new Float(f);
    Float ib2 = new Float(f+1.f);
    if ((i&1) == 0) {
      ib1 = new Float(f+1.f);
      ib2 = new Float(f+2.f);
    }
    return ib1+ib2;
  }

  static float testb2(float f, int i) {
    Float ib1 = f;
    Float ib2 = f+1.f;
    if ((i&1) == 0) {
      ib1 = (f+1.f);
      ib2 = (f+2.f);
    }
    return ib1+ib2;
  }

  static float testm2(float f, int i) {
    Float ib1 = new Float(f);
    Float ib2 = f+1.f;
    if ((i&1) == 0) {
      ib1 = new Float(f+1.f);
      ib2 = (f+2.f);
    }
    return ib1+ib2;
  }

  static float testp2(float f, int i, Float ib1) {
    Float ib2 = f+1.f;
    if ((i&1) == 0) {
      ib1 = new Float(f+1.f);
      ib2 = (f+2.f);
    }
    return ib1+ib2;
  }

  static float testc2(float f, int i) {
    Float ib1 = ibc;
    Float ib2 = f+1.f;
    if ((i&1) == 0) {
      ib1 = (ibc+1.f);
      ib2 = (f+2.f);
    }
    return ib1+ib2;
  }

  //===============================================
  static float sum(float[] a) {
    float result = 1.f;
    for (Float i : a)
        result += i;
    return result;
  }

  static float sumb(float[] a) {
    Float result = 1.f;
    for (Float i : a)
        result += i;
    return result;
  }

  static float sumc(float[] a) {
    Float result = ibc;
    for (Float i : a)
        result += i;
    return result;
  }

  static float sumf(float[] a) {
    Float result = foob(1.f);
    for (Float i : a)
        result += i;
    return result;
  }

  static float sump(float[] a, Float result) {
    for (Float i : a)
        result += i;
    return result;
  }

  static float sum2(float[] a) {
    float result1 = 1.f;
    float result2 = 1.f;
    for (Float i : a) {
        result1 += i;
        result2 += i + 1.f;
    }
    return result1 + result2;
  }

  static float sumb2(float[] a) {
    Float result1 = 1.f;
    Float result2 = 1.f;
    for (Float i : a) {
        result1 += i;
        result2 += i + 1.f;
    }
    return result1 + result2;
  }

  static float summ2(float[] a) {
    Float result1 = 1.f;
    Float result2 = new Float(1.f);
    for (Float i : a) {
        result1 += i;
        result2 += new Float(i + 1.f);
    }
    return result1 + result2;
  }

  static float sump2(float[] a, Float result2) {
    Float result1 = 1.f;
    for (Float i : a) {
        result1 += i;
        result2 += i + 1.f;
    }
    return result1 + result2;
  }

  static float sumc2(float[] a) {
    Float result1 = 1.f;
    Float result2 = ibc;
    for (Float i : a) {
        result1 += i;
        result2 += i + ibc;
    }
    return result1 + result2;
  }

  //===============================================
  static float remi_sum() {
    Float j = new Float(1.f);
    for (int i = 0; i< 1000; i++) {
      j = new Float(j + 1.f);
    }
    return j;
  }

  static float remi_sumb() {
    Float j = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      j = j + 1.f;
    }
    return j;
  }

  static float remi_sumf() {
    Float j = foob(1.f);
    for (int i = 0; i< 1000; i++) {
      j = j + 1.f;
    }
    return j;
  }

  static float remi_sump(Float j) {
    for (int i = 0; i< 1000; i++) {
      j = new Float(j + 1.f);
    }
    return j;
  }

  static float remi_sumc() {
    Float j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = j + ibc;
    }
    return j;
  }

  static float remi_sum2() {
    Float j1 = new Float(1.f);
    Float j2 = new Float(1.f);
    for (int i = 0; i< 1000; i++) {
      j1 = new Float(j1 + 1.f);
      j2 = new Float(j2 + 2.f);
    }
    return j1 + j2;
  }

  static float remi_sumb2() {
    Float j1 = Float.valueOf(1.f);
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + 1.f;
      j2 = j2 + 2.f;
    }
    return j1 + j2;
  }

  static float remi_summ2() {
    Float j1 = new Float(1.f);
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      j1 = new Float(j1 + 1.f);
      j2 = j2 + 2.f;
    }
    return j1 + j2;
  }

  static float remi_sump2(Float j1) {
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      j1 = new Float(j1 + 1.f);
      j2 = j2 + 2.f;
    }
    return j1 + j2;
  }

  static float remi_sumc2() {
    Float j1 = ibc;
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + ibc;
      j2 = j2 + 2.f;
    }
    return j1 + j2;
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static float simple_deop(float i) {
    Float ib = new Float(foo(i));
    dummy();
    return ib;
  }

  static float simpleb_deop(float i) {
    Float ib = Float.valueOf(foo(i));
    dummy();
    return ib;
  }

  static float simplef_deop(float i) {
    Float ib = foob(i);
    dummy();
    return ib;
  }

  static float simplep_deop(Float ib) {
    dummy();
    return ib;
  }

  static float simplec_deop(float i) {
    Float ib = ibc;
    dummy();
    return ib;
  }

  static float test_deop(float f, int i) {
    Float ib = new Float(foo(f));
    if ((i&1) == 0)
      ib = foo(f+1.f);
    dummy();
    return ib;
  }

  static float testb_deop(float f, int i) {
    Float ib = foo(f);
    if ((i&1) == 0)
      ib = foo(f+1.f);
    dummy();
    return ib;
  }

  static float testf_deop(float f, int i) {
    Float ib = foob(f);
    if ((i&1) == 0)
      ib = foo(f+1.f);
    dummy();
    return ib;
  }

  static float testp_deop(float f, int i, Float ib) {
    if ((i&1) == 0)
      ib = foo(f+1.f);
    dummy();
    return ib;
  }

  static float testc_deop(float f, int i) {
    Float ib = ibc;
    if ((i&1) == 0)
      ib = foo(f+1.f);
    dummy();
    return ib;
  }

  static float sum_deop(float[] a) {
    float result = 1.f;
    for (Float i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static float sumb_deop(float[] a) {
    Float result = 1.f;
    for (Float i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static float sumf_deop(float[] a) {
    Float result = 1.f;
    for (Float i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static float sump_deop(float[] a, Float result) {
    for (Float i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static float sumc_deop(float[] a) {
    Float result = ibc;
    for (Float i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static float remi_sum_deop() {
    Float j = new Float(foo(1.f));
    for (int i = 0; i< 1000; i++) {
      j = new Float(foo(j + 1.f));
    }
    dummy();
    return j;
  }

  static float remi_sumb_deop() {
    Float j = Float.valueOf(foo(1.f));
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.f);
    }
    dummy();
    return j;
  }

  static float remi_sumf_deop() {
    Float j = foob(1.f);
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.f);
    }
    dummy();
    return j;
  }

  static float remi_sump_deop(Float j) {
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.f);
    }
    dummy();
    return j;
  }

  static float remi_sumc_deop() {
    Float j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1.f);
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static float remi_sum_cond() {
    Float j = new Float(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Float(j + 1.f);
      }
    }
    return j;
  }

  static float remi_sumb_cond() {
    Float j = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.f;
      }
    }
    return j;
  }

  static float remi_sumf_cond() {
    Float j = foob(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.f;
      }
    }
    return j;
  }

  static float remi_sump_cond(Float j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1.f;
      }
    }
    return j;
  }

  static float remi_sumc_cond() {
    Float j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + ibc;
      }
    }
    return j;
  }

  static float remi_sum2_cond() {
    Float j1 = new Float(1.f);
    Float j2 = new Float(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Float(j1 + 1.f);
      } else {
        j2 = new Float(j2 + 2.f);
      }
    }
    return j1 + j2;
  }

  static float remi_sumb2_cond() {
    Float j1 = Float.valueOf(1.f);
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = j1 + 1.f;
      } else {
        j2 = j2 + 2.f;
      }
    }
    return j1 + j2;
  }

  static float remi_summ2_cond() {
    Float j1 = new Float(1.f);
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Float(j1 + 1.f);
      } else {
        j2 = j2 + 2.f;
      }
    }
    return j1 + j2;
  }

  static float remi_sump2_cond(Float j1) {
    Float j2 = Float.valueOf(1.f);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Float(j1 + 1.f);
      } else {
        j2 = j2 + 2.f;
      }
    }
    return j1 + j2;
  }

  static float remi_sumc2_cond() {
    Float j1 = ibc;
    Float j2 = Float.valueOf(1.f);
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

    final float[] val = new float[] {
       71990896.f,  71990896.f,    12000.f,  71990896.f,  71990896.f,
      144000000.f, 144000000.f, 72014896.f, 144000000.f, 144000000.f,
       71990896.f,  71990896.f,    12000.f,  71990896.f,  71990896.f,
       72000000.f,  72000000.f, 36004096.f,  72000000.f,  72000000.f,
      144012288.f, 144012288.f, 72033096.f, 144012288.f, 144012288.f,
       72000000.f,  72000000.f, 36004096.f,  72000000.f,  72000000.f,
         499501.f,    499501.f,   499501.f,    499501.f,    499501.f,
        1000002.f,   1000002.f,  1000002.f,   1000002.f,   1000002.f,
         499501.f,    499501.f,   499501.f,    499501.f,    499501.f,
           1001.f,      1001.f,     1001.f,      1001.f,      1001.f,
           3002.f,      3002.f,     3002.f,      3002.f,      3002.f,
           1001.f,      1001.f,     1001.f,      1001.f,      1001.f,
            501.f,       501.f,      501.f,       501.f,       501.f,
           1502.f,      1502.f,     1502.f,      1502.f,      1502.f
    };

    float[] res = new float[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0.f;
    }


    for (int i = 0; i < 12000; i++) {
      res[0] += simple(i);
      res[1] += simpleb(i);
      res[2] += simplec();
      res[3] += simplef(i);
      res[4] += simplep((float)i);

      res[5] += simple2((float)i);
      res[6] += simpleb2((float)i);
      res[7] += simplec2((float)i);
      res[8] += simplem2((float)i);
      res[9] += simplep2((float)i, (float)i);

      res[10] += simple_deop((float)i);
      res[11] += simpleb_deop((float)i);
      res[12] += simplec_deop((float)i);
      res[13] += simplef_deop((float)i);
      res[14] += simplep_deop((float)i);

      res[15] += test((float)i, i);
      res[16] += testb((float)i, i);
      res[17] += testc((float)i, i);
      res[18] += testm((float)i, i);
      res[19] += testp((float)i, i, (float)i);

      res[20] += test2((float)i, i);
      res[21] += testb2((float)i, i);
      res[22] += testc2((float)i, i);
      res[23] += testm2((float)i, i);
      res[24] += testp2((float)i, i, (float)i);

      res[25] += test_deop((float)i, i);
      res[26] += testb_deop((float)i, i);
      res[27] += testc_deop((float)i, i);
      res[28] += testf_deop((float)i, i);
      res[29] += testp_deop((float)i, i, (float)i);
    }

    float[] ia = new float[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, 1.f);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, 1.f);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, 1.f);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump(1.f);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2(1.f);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop(1.f);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond(1.f);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond(1.f);
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
