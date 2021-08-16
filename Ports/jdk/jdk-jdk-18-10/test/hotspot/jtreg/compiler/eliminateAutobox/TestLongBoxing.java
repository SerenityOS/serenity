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
 *                   compiler.eliminateAutobox.TestLongBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::foob
 *                   compiler.eliminateAutobox.TestLongBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestLongBoxing::foob
 *                   compiler.eliminateAutobox.TestLongBoxing
 */

package compiler.eliminateAutobox;

public class TestLongBoxing {

  static final Long ibc = new Long(1);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void dummy()     { }
  static long  foo(long i)  { return i; }
  static Long  foob(long i) { return Long.valueOf(i); }


  static long simple(long i) {
    Long ib = new Long(i);
    return ib;
  }

  static long simpleb(long i) {
    Long ib = Long.valueOf(i);
    return ib;
  }

  static long simplec() {
    Long ib = ibc;
    return ib;
  }

  static long simplef(long i) {
    Long ib = foob(i);
    return ib;
  }

  static long simplep(Long ib) {
    return ib;
  }

  static long simple2(long i) {
    Long ib1 = new Long(i);
    Long ib2 = new Long(i+1);
    return ib1 + ib2;
  }

  static long simpleb2(long i) {
    Long ib1 = Long.valueOf(i);
    Long ib2 = Long.valueOf(i+1);
    return ib1 + ib2;
  }

  static long simplem2(long i) {
    Long ib1 = new Long(i);
    Long ib2 = Long.valueOf(i+1);
    return ib1 + ib2;
  }

  static long simplep2(long i, Long ib1) {
    Long ib2 = Long.valueOf(i+1);
    return ib1 + ib2;
  }

  static long simplec2(long i) {
    Long ib1 = ibc;
    Long ib2 = Long.valueOf(i+1);
    return ib1 + ib2;
  }

  //===============================================
  static long test(long i) {
    Long ib = new Long(i);
    if ((i&1) == 0)
      ib = i+1;
    return ib;
  }

  static long testb(long i) {
    Long ib = i;
    if ((i&1) == 0)
      ib = (i+1);
    return ib;
  }

  static long testm(long i) {
    Long ib = i;
    if ((i&1) == 0)
      ib = new Long(i+1);
    return ib;
  }

  static long testp(long i, Long ib) {
    if ((i&1) == 0)
      ib = new Long(i+1);
    return ib;
  }

  static long testc(long i) {
    Long ib = ibc;
    if ((i&1) == 0)
      ib = new Long(i+1);
    return ib;
  }

  static long test2(long i) {
    Long ib1 = new Long(i);
    Long ib2 = new Long(i+1);
    if ((i&1) == 0) {
      ib1 = new Long(i+1);
      ib2 = new Long(i+2);
    }
    return ib1+ib2;
  }

  static long testb2(long i) {
    Long ib1 = i;
    Long ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = (i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static long testm2(long i) {
    Long ib1 = new Long(i);
    Long ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = new Long(i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static long testp2(long i, Long ib1) {
    Long ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = new Long(i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static long testc2(long i) {
    Long ib1 = ibc;
    Long ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = (ibc+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  //===============================================
  static long sum(long[] a) {
    long result = 1;
    for (Long i : a)
        result += i;
    return result;
  }

  static long sumb(long[] a) {
    Long result = 1l;
    for (Long i : a)
        result += i;
    return result;
  }

  static long sumc(long[] a) {
    Long result = ibc;
    for (Long i : a)
        result += i;
    return result;
  }

  static long sumf(long[] a) {
    Long result = foob(1);
    for (Long i : a)
        result += i;
    return result;
  }

  static long sump(long[] a, Long result) {
    for (Long i : a)
        result += i;
    return result;
  }

  static long sum2(long[] a) {
    long result1 = 1;
    long result2 = 1;
    for (Long i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static long sumb2(long[] a) {
    Long result1 = 1l;
    Long result2 = 1l;
    for (Long i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static long summ2(long[] a) {
    Long result1 = 1l;
    Long result2 = new Long(1);
    for (Long i : a) {
        result1 += i;
        result2 += new Long(i + 1);
    }
    return result1 + result2;
  }

  static long sump2(long[] a, Long result2) {
    Long result1 = 1l;
    for (Long i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static long sumc2(long[] a) {
    Long result1 = 1l;
    Long result2 = ibc;
    for (Long i : a) {
        result1 += i;
        result2 += i + ibc;
    }
    return result1 + result2;
  }

  //===============================================
  static long remi_sum() {
    Long j = new Long(1);
    for (int i = 0; i< 1000; i++) {
      j = new Long(j + 1);
    }
    return j;
  }

  static long remi_sumb() {
    Long j = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j = j + 1;
    }
    return j;
  }

  static long remi_sumf() {
    Long j = foob(1);
    for (int i = 0; i< 1000; i++) {
      j = j + 1;
    }
    return j;
  }

  static long remi_sump(Long j) {
    for (int i = 0; i< 1000; i++) {
      j = new Long(j + 1);
    }
    return j;
  }

  static long remi_sumc() {
    Long j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = j + ibc;
    }
    return j;
  }

  static long remi_sum2() {
    Long j1 = new Long(1);
    Long j2 = new Long(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Long(j1 + 1);
      j2 = new Long(j2 + 2);
    }
    return j1 + j2;
  }

  static long remi_sumb2() {
    Long j1 = Long.valueOf(1);
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + 1;
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static long remi_summ2() {
    Long j1 = new Long(1);
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Long(j1 + 1);
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static long remi_sump2(Long j1) {
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Long(j1 + 1);
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static long remi_sumc2() {
    Long j1 = ibc;
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + ibc;
      j2 = j2 + 2;
    }
    return j1 + j2;
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static long simple_deop(long i) {
    Long ib = new Long(foo(i));
    dummy();
    return ib;
  }

  static long simpleb_deop(long i) {
    Long ib = Long.valueOf(foo(i));
    dummy();
    return ib;
  }

  static long simplef_deop(long i) {
    Long ib = foob(i);
    dummy();
    return ib;
  }

  static long simplep_deop(Long ib) {
    dummy();
    return ib;
  }

  static long simplec_deop(long i) {
    Long ib = ibc;
    dummy();
    return ib;
  }

  static long test_deop(long i) {
    Long ib = new Long(foo(i));
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static long testb_deop(long i) {
    Long ib = foo(i);
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static long testf_deop(long i) {
    Long ib = foob(i);
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static long testp_deop(long i, Long ib) {
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static long testc_deop(long i) {
    Long ib = ibc;
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static long sum_deop(long[] a) {
    long result = 1;
    for (Long i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static long sumb_deop(long[] a) {
    Long result = 1l;
    for (Long i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static long sumf_deop(long[] a) {
    Long result = 1l;
    for (Long i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static long sump_deop(long[] a, Long result) {
    for (Long i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static long sumc_deop(long[] a) {
    Long result = ibc;
    for (Long i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static long remi_sum_deop() {
    Long j = new Long(foo(1));
    for (int i = 0; i< 1000; i++) {
      j = new Long(foo(j + 1));
    }
    dummy();
    return j;
  }

  static long remi_sumb_deop() {
    Long j = Long.valueOf(foo(1));
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static long remi_sumf_deop() {
    Long j = foob(1);
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static long remi_sump_deop(Long j) {
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static long remi_sumc_deop() {
    Long j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static long remi_sum_cond() {
    Long j = new Long(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Long(j + 1);
      }
    }
    return j;
  }

  static long remi_sumb_cond() {
    Long j = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static long remi_sumf_cond() {
    Long j = foob(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static long remi_sump_cond(Long j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static long remi_sumc_cond() {
    Long j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + ibc;
      }
    }
    return j;
  }

  static long remi_sum2_cond() {
    Long j1 = new Long(1);
    Long j2 = new Long(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Long(j1 + 1);
      } else {
        j2 = new Long(j2 + 2);
      }
    }
    return j1 + j2;
  }

  static long remi_sumb2_cond() {
    Long j1 = Long.valueOf(1);
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = j1 + 1;
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static long remi_summ2_cond() {
    Long j1 = new Long(1);
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Long(j1 + 1);
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static long remi_sump2_cond(Long j1) {
    Long j2 = Long.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Long(j1 + 1);
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static long remi_sumc2_cond() {
    Long j1 = ibc;
    Long j2 = Long.valueOf(1);
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

    final long[] val = new long[] {
       71994000,  71994000,    12000,  71994000,  71994000,
      144000000, 144000000, 72018000, 144000000, 144000000,
       71994000,  71994000,    12000,  71994000,  71994000,
       72000000,  72000000, 36006000,  72000000,  72000000,
      144012000, 144012000, 72030000, 144012000, 144012000,
       72000000,  72000000, 36006000,  72000000,  72000000,
         499501,    499501,   499501,    499501,    499501,
        1000002,   1000002,  1000002,   1000002,   1000002,
         499501,    499501,   499501,    499501,    499501,
           1001,      1001,     1001,      1001,      1001,
           3002,      3002,     3002,      3002,      3002,
           1001,      1001,     1001,      1001,      1001,
            501,       501,      501,       501,       501,
           1502,      1502,     1502,      1502,      1502
    };

    long[] res = new long[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0;
    }


    for (long i = 0; i < 12000; i++) {
      res[0] += simple(i);
      res[1] += simpleb(i);
      res[2] += simplec();
      res[3] += simplef(i);
      res[4] += simplep(i);

      res[5] += simple2(i);
      res[6] += simpleb2(i);
      res[7] += simplec2(i);
      res[8] += simplem2(i);
      res[9] += simplep2(i, i);

      res[10] += simple_deop(i);
      res[11] += simpleb_deop(i);
      res[12] += simplec_deop(i);
      res[13] += simplef_deop(i);
      res[14] += simplep_deop(i);

      res[15] += test(i);
      res[16] += testb(i);
      res[17] += testc(i);
      res[18] += testm(i);
      res[19] += testp(i, i);

      res[20] += test2(i);
      res[21] += testb2(i);
      res[22] += testc2(i);
      res[23] += testm2(i);
      res[24] += testp2(i, i);

      res[25] += test_deop(i);
      res[26] += testb_deop(i);
      res[27] += testc_deop(i);
      res[28] += testf_deop(i);
      res[29] += testp_deop(i, i);
    }

    long[] ia = new long[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, (long)1);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, (long)1);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, (long)1);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump((long)1);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2((long)1);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop((long)1);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond((long)1);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond((long)1);
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
