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
 *                   compiler.eliminateAutobox.TestShortBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::foob
 *                   compiler.eliminateAutobox.TestShortBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestShortBoxing::foob
 *                   compiler.eliminateAutobox.TestShortBoxing
 */

package compiler.eliminateAutobox;

public class TestShortBoxing {

  static final Short ibc = new Short((short)1);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void dummy()      { }
  static short foo(short i)  { return i; }
  static Short foob(short i) { return Short.valueOf(i); }


  static short simple(short i) {
    Short ib = new Short(i);
    return ib;
  }

  static short simpleb(short i) {
    Short ib = Short.valueOf(i);
    return ib;
  }

  static short simplec() {
    Short ib = ibc;
    return ib;
  }

  static short simplef(short i) {
    Short ib = foob(i);
    return ib;
  }

  static short simplep(Short ib) {
    return ib;
  }

  static short simple2(short i) {
    Short ib1 = new Short(i);
    Short ib2 = new Short((short)(i+1));
    return (short)(ib1 + ib2);
  }

  static short simpleb2(short i) {
    Short ib1 = Short.valueOf(i);
    Short ib2 = Short.valueOf((short)(i+1));
    return (short)(ib1 + ib2);
  }

  static short simplem2(short i) {
    Short ib1 = new Short(i);
    Short ib2 = Short.valueOf((short)(i+1));
    return (short)(ib1 + ib2);
  }

  static short simplep2(short i, Short ib1) {
    Short ib2 = Short.valueOf((short)(i+1));
    return (short)(ib1 + ib2);
  }

  static short simplec2(short i) {
    Short ib1 = ibc;
    Short ib2 = Short.valueOf((short)(i+1));
    return (short)(ib1 + ib2);
  }

  //===============================================
  static short test(short i) {
    Short ib = new Short(i);
    if ((i&1) == 0)
      ib = (short)(i+1);
    return ib;
  }

  static short testb(short i) {
    Short ib = i;
    if ((i&1) == 0)
      ib = (short)(i+1);
    return ib;
  }

  static short testm(short i) {
    Short ib = i;
    if ((i&1) == 0)
      ib = new Short((short)(i+1));
    return ib;
  }

  static short testp(short i, Short ib) {
    if ((i&1) == 0)
      ib = new Short((short)(i+1));
    return ib;
  }

  static short testc(short i) {
    Short ib = ibc;
    if ((i&1) == 0)
      ib = new Short((short)(i+1));
    return ib;
  }

  static short test2(short i) {
    Short ib1 = new Short(i);
    Short ib2 = new Short((short)(i+1));
    if ((i&1) == 0) {
      ib1 = new Short((short)(i+1));
      ib2 = new Short((short)(i+2));
    }
    return (short)(ib1+ib2);
  }

  static short testb2(short i) {
    Short ib1 = i;
    Short ib2 = (short)(i+1);
    if ((i&1) == 0) {
      ib1 = (short)(i+1);
      ib2 = (short)(i+2);
    }
    return (short)(ib1 + ib2);
  }

  static short testm2(short i) {
    Short ib1 = new Short(i);
    Short ib2 = (short)(i+1);
    if ((i&1) == 0) {
      ib1 = new Short((short)(i+1));
      ib2 = (short)(i+2);
    }
    return (short)(ib1 + ib2);
  }

  static short testp2(short i, Short ib1) {
    Short ib2 = (short)(i+1);
    if ((i&1) == 0) {
      ib1 = new Short((short)(i+1));
      ib2 = (short)(i+2);
    }
    return (short)(ib1 + ib2);
  }

  static short testc2(short i) {
    Short ib1 = ibc;
    Short ib2 = (short)(i+1);
    if ((i&1) == 0) {
      ib1 = (short)(ibc+1);
      ib2 = (short)(i+2);
    }
    return (short)(ib1 + ib2);
  }

  //===============================================
  static short sum(short[] a) {
    short result = 1;
    for (Short i : a)
        result += i;
    return result;
  }

  static short sumb(short[] a) {
    Short result = 1;
    for (Short i : a)
        result = (short)(result + i);
    return result;
  }

  static short sumc(short[] a) {
    Short result = ibc;
    for (Short i : a)
        result = (short)(result + i);
    return result;
  }

  static short sumf(short[] a) {
    Short result = foob((short)1);
    for (Short i : a)
        result = (short)(result + i);
    return result;
  }

  static short sump(short[] a, Short result) {
    for (Short i : a)
        result = (short)(result + i);
    return result;
  }

  static short sum2(short[] a) {
    short result1 = 1;
    short result2 = 1;
    for (Short i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return (short)(result1 + result2);
  }

  static short sumb2(short[] a) {
    Short result1 = 1;
    Short result2 = 1;
    for (Short i : a) {
        result1 = (short)(result1 + i);
        result2 = (short)(result2 + i + 1);
    }
    return (short)(result1 + result2);
  }

  static short summ2(short[] a) {
    Short result1 = 1;
    Short result2 = new Short((short)1);
    for (Short i : a) {
        result1 = (short)(result1 + i);
        result2 = (short)(result2 + new Short((short)(i + 1)));
    }
    return (short)(result1 + result2);
  }

  static short sump2(short[] a, Short result2) {
    Short result1 = 1;
    for (Short i : a) {
        result1 = (short)(result1 + i);
        result2 = (short)(result2 + i + 1);
    }
    return (short)(result1 + result2);
  }

  static short sumc2(short[] a) {
    Short result1 = 1;
    Short result2 = ibc;
    for (Short i : a) {
        result1 = (short)(result1 + i);
        result2 = (short)(result2 + i + ibc);
    }
    return (short)(result1 + result2);
  }

  //===============================================
  static short remi_sum() {
    Short j = new Short((short)1);
    for (int i = 0; i< 1000; i++) {
      j = new Short((short)(j + 1));
    }
    return j;
  }

  static short remi_sumb() {
    Short j = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      j = (short)(j + 1);
    }
    return j;
  }

  static short remi_sumf() {
    Short j = foob((short)1);
    for (int i = 0; i< 1000; i++) {
      j = (short)(j + 1);
    }
    return j;
  }

  static short remi_sump(Short j) {
    for (int i = 0; i< 1000; i++) {
      j = new Short((short)(j + 1));
    }
    return j;
  }

  static short remi_sumc() {
    Short j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = (short)(j + ibc);
    }
    return j;
  }

  static short remi_sum2() {
    Short j1 = new Short((short)1);
    Short j2 = new Short((short)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Short((short)(j1 + 1));
      j2 = new Short((short)(j2 + 2));
    }
    return (short)(j1 + j2);
  }

  static short remi_sumb2() {
    Short j1 = Short.valueOf((short)1);
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      j1 = (short)(j1 + 1);
      j2 = (short)(j2 + 2);
    }
    return (short)(j1 + j2);
  }

  static short remi_summ2() {
    Short j1 = new Short((short)1);
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Short((short)(j1 + 1));
      j2 = (short)(j2 + 2);
    }
    return (short)(j1 + j2);
  }

  static short remi_sump2(Short j1) {
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Short((short)(j1 + 1));
      j2 = (short)(j2 + 2);
    }
    return (short)(j1 + j2);
  }

  static short remi_sumc2() {
    Short j1 = ibc;
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      j1 = (short)(j1 + ibc);
      j2 = (short)(j2 + 2);
    }
    return (short)(j1 + j2);
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static short simple_deop(short i) {
    Short ib = new Short(foo(i));
    dummy();
    return ib;
  }

  static short simpleb_deop(short i) {
    Short ib = Short.valueOf(foo(i));
    dummy();
    return ib;
  }

  static short simplef_deop(short i) {
    Short ib = foob(i);
    dummy();
    return ib;
  }

  static short simplep_deop(Short ib) {
    dummy();
    return ib;
  }

  static short simplec_deop(short i) {
    Short ib = ibc;
    dummy();
    return ib;
  }

  static short test_deop(short i) {
    Short ib = new Short(foo(i));
    if ((i&1) == 0)
      ib = foo((short)(i+1));
    dummy();
    return ib;
  }

  static short testb_deop(short i) {
    Short ib = foo(i);
    if ((i&1) == 0)
      ib = foo((short)(i+1));
    dummy();
    return ib;
  }

  static short testf_deop(short i) {
    Short ib = foob(i);
    if ((i&1) == 0)
      ib = foo((short)(i+1));
    dummy();
    return ib;
  }

  static short testp_deop(short i, Short ib) {
    if ((i&1) == 0)
      ib = foo((short)(i+1));
    dummy();
    return ib;
  }

  static short testc_deop(short i) {
    Short ib = ibc;
    if ((i&1) == 0)
      ib = foo((short)(i+1));
    dummy();
    return ib;
  }

  static short sum_deop(short[] a) {
    short result = 1;
    for (Short i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static short sumb_deop(short[] a) {
    Short result = 1;
    for (Short i : a)
        result = (short)(result + foo(i));
    dummy();
    return result;
  }

  static short sumf_deop(short[] a) {
    Short result = 1;
    for (Short i : a)
        result = (short)(result + foob(i));
    dummy();
    return result;
  }

  static short sump_deop(short[] a, Short result) {
    for (Short i : a)
        result = (short)(result + foob(i));
    dummy();
    return result;
  }

  static short sumc_deop(short[] a) {
    Short result = ibc;
    for (Short i : a)
        result = (short)(result + foo(i));
    dummy();
    return result;
  }

  static short remi_sum_deop() {
    Short j = new Short(foo((short)1));
    for (int i = 0; i< 1000; i++) {
      j = new Short(foo((short)(j + 1)));
    }
    dummy();
    return j;
  }

  static short remi_sumb_deop() {
    Short j = Short.valueOf(foo((short)1));
    for (int i = 0; i< 1000; i++) {
      j = foo((short)(j + 1));
    }
    dummy();
    return j;
  }

  static short remi_sumf_deop() {
    Short j = foob((short)1);
    for (int i = 0; i< 1000; i++) {
      j = foo((short)(j + 1));
    }
    dummy();
    return j;
  }

  static short remi_sump_deop(Short j) {
    for (int i = 0; i< 1000; i++) {
      j = foo((short)(j + 1));
    }
    dummy();
    return j;
  }

  static short remi_sumc_deop() {
    Short j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo((short)(j + 1));
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static short remi_sum_cond() {
    Short j = new Short((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Short((short)(j + 1));
      }
    }
    return j;
  }

  static short remi_sumb_cond() {
    Short j = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (short)(j + 1);
      }
    }
    return j;
  }

  static short remi_sumf_cond() {
    Short j = foob((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (short)(j + 1);
      }
    }
    return j;
  }

  static short remi_sump_cond(Short j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (short)(j + 1);
      }
    }
    return j;
  }

  static short remi_sumc_cond() {
    Short j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (short)(j + ibc);
      }
    }
    return j;
  }

  static short remi_sum2_cond() {
    Short j1 = new Short((short)1);
    Short j2 = new Short((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Short((short)(j1 + 1));
      } else {
        j2 = new Short((short)(j2 + 2));
      }
    }
    return (short)(j1 + j2);
  }

  static short remi_sumb2_cond() {
    Short j1 = Short.valueOf((short)1);
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = (short)(j1 + 1);
      } else {
        j2 = (short)(j2 + 2);
      }
    }
    return (short)(j1 + j2);
  }

  static short remi_summ2_cond() {
    Short j1 = new Short((short)1);
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Short((short)(j1 + 1));
      } else {
        j2 = (short)(j2 + 2);
      }
    }
    return (short)(j1 + j2);
  }

  static short remi_sump2_cond(Short j1) {
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Short((short)(j1 + 1));
      } else {
        j2 = (short)(j2 + 2);
      }
    }
    return (short)(j1 + j2);
  }

  static short remi_sumc2_cond() {
    Short j1 = ibc;
    Short j2 = Short.valueOf((short)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = (short)(j1 + ibc);
      } else {
        j2 = (short)(j2 + 2);
      }
    }
    return (short)(j1 + j2);
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

    final int[] val = new int[] {
       71994000,  71994000,    12000,  71994000,  71994000,
      144000000, 144000000, 72018000, 144000000, 144000000,
       71994000,  71994000,    12000,  71994000,  71994000,
       72000000,  72000000, 36006000,  72000000,  72000000,
      144012000, 144012000, 72030000, 144012000, 144012000,
       72000000,  72000000, 36006000,  72000000,  72000000,
         -24787,    -24787,   -24787,    -24787,    -24787,
          16962,     16962,    16962,     16962,     16962,
         -24787,    -24787,   -24787,    -24787,    -24787,
           1001,      1001,     1001,      1001,      1001,
           3002,      3002,     3002,      3002,      3002,
           1001,      1001,     1001,      1001,      1001,
            501,       501,      501,       501,       501,
           1502,      1502,     1502,      1502,      1502
    };

    int[] res = new int[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0;
    }


    for (int i = 0; i < 12000; i++) {
      res[0] += simple((short)i);
      res[1] += simpleb((short)i);
      res[2] += simplec();
      res[3] += simplef((short)i);
      res[4] += simplep((short)i);

      res[5] += simple2((short)i);
      res[6] += simpleb2((short)i);
      res[7] += simplec2((short)i);
      res[8] += simplem2((short)i);
      res[9] += simplep2((short)i, (short)i);

      res[10] += simple_deop((short)i);
      res[11] += simpleb_deop((short)i);
      res[12] += simplec_deop((short)i);
      res[13] += simplef_deop((short)i);
      res[14] += simplep_deop((short)i);

      res[15] += test((short)i);
      res[16] += testb((short)i);
      res[17] += testc((short)i);
      res[18] += testm((short)i);
      res[19] += testp((short)i, (short)i);

      res[20] += test2((short)i);
      res[21] += testb2((short)i);
      res[22] += testc2((short)i);
      res[23] += testm2((short)i);
      res[24] += testp2((short)i, (short)i);

      res[25] += test_deop((short)i);
      res[26] += testb_deop((short)i);
      res[27] += testc_deop((short)i);
      res[28] += testf_deop((short)i);
      res[29] += testp_deop((short)i, (short)i);
    }

    short[] ia = new short[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = (short)i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, (short)1);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, (short)1);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, (short)1);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump((short)1);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2((short)1);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop((short)1);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond((short)1);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond((short)1);
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
