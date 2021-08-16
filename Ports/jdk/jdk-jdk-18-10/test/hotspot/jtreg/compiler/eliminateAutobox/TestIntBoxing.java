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
 *                   compiler.eliminateAutobox.TestIntBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::foob
 *                   compiler.eliminateAutobox.TestIntBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestIntBoxing::foob
 *                   compiler.eliminateAutobox.TestIntBoxing
 */

package compiler.eliminateAutobox;

public class TestIntBoxing {

  static final Integer ibc = new Integer(1);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void    dummy()     { }
  static int     foo(int i)  { return i; }
  static Integer foob(int i) { return Integer.valueOf(i); }


  static int simple(int i) {
    Integer ib = new Integer(i);
    return ib;
  }

  static int simpleb(int i) {
    Integer ib = Integer.valueOf(i);
    return ib;
  }

  static int simplec() {
    Integer ib = ibc;
    return ib;
  }

  static int simplef(int i) {
    Integer ib = foob(i);
    return ib;
  }

  static int simplep(Integer ib) {
    return ib;
  }

  static int simple2(int i) {
    Integer ib1 = new Integer(i);
    Integer ib2 = new Integer(i+1);
    return ib1 + ib2;
  }

  static int simpleb2(int i) {
    Integer ib1 = Integer.valueOf(i);
    Integer ib2 = Integer.valueOf(i+1);
    return ib1 + ib2;
  }

  static int simplem2(int i) {
    Integer ib1 = new Integer(i);
    Integer ib2 = Integer.valueOf(i+1);
    return ib1 + ib2;
  }

  static int simplep2(int i, Integer ib1) {
    Integer ib2 = Integer.valueOf(i+1);
    return ib1 + ib2;
  }

  static int simplec2(int i) {
    Integer ib1 = ibc;
    Integer ib2 = Integer.valueOf(i+1);
    return ib1 + ib2;
  }

  //===============================================
  static int test(int i) {
    Integer ib = new Integer(i);
    if ((i&1) == 0)
      ib = i+1;
    return ib;
  }

  static int testb(int i) {
    Integer ib = i;
    if ((i&1) == 0)
      ib = (i+1);
    return ib;
  }

  static int testm(int i) {
    Integer ib = i;
    if ((i&1) == 0)
      ib = new Integer(i+1);
    return ib;
  }

  static int testp(int i, Integer ib) {
    if ((i&1) == 0)
      ib = new Integer(i+1);
    return ib;
  }

  static int testc(int i) {
    Integer ib = ibc;
    if ((i&1) == 0)
      ib = new Integer(i+1);
    return ib;
  }

  static int test2(int i) {
    Integer ib1 = new Integer(i);
    Integer ib2 = new Integer(i+1);
    if ((i&1) == 0) {
      ib1 = new Integer(i+1);
      ib2 = new Integer(i+2);
    }
    return ib1+ib2;
  }

  static int testb2(int i) {
    Integer ib1 = i;
    Integer ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = (i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static int testm2(int i) {
    Integer ib1 = new Integer(i);
    Integer ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = new Integer(i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static int testp2(int i, Integer ib1) {
    Integer ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = new Integer(i+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  static int testc2(int i) {
    Integer ib1 = ibc;
    Integer ib2 = i+1;
    if ((i&1) == 0) {
      ib1 = (ibc+1);
      ib2 = (i+2);
    }
    return ib1+ib2;
  }

  //===============================================
  static int sum(int[] a) {
    int result = 1;
    for (Integer i : a)
        result += i;
    return result;
  }

  static int sumb(int[] a) {
    Integer result = 1;
    for (Integer i : a)
        result += i;
    return result;
  }

  static int sumc(int[] a) {
    Integer result = ibc;
    for (Integer i : a)
        result += i;
    return result;
  }

  static int sumf(int[] a) {
    Integer result = foob(1);
    for (Integer i : a)
        result += i;
    return result;
  }

  static int sump(int[] a, Integer result) {
    for (Integer i : a)
        result += i;
    return result;
  }

  static int sum2(int[] a) {
    int result1 = 1;
    int result2 = 1;
    for (Integer i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static int sumb2(int[] a) {
    Integer result1 = 1;
    Integer result2 = 1;
    for (Integer i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static int summ2(int[] a) {
    Integer result1 = 1;
    Integer result2 = new Integer(1);
    for (Integer i : a) {
        result1 += i;
        result2 += new Integer(i + 1);
    }
    return result1 + result2;
  }

  static int sump2(int[] a, Integer result2) {
    Integer result1 = 1;
    for (Integer i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return result1 + result2;
  }

  static int sumc2(int[] a) {
    Integer result1 = 1;
    Integer result2 = ibc;
    for (Integer i : a) {
        result1 += i;
        result2 += i + ibc;
    }
    return result1 + result2;
  }

  //===============================================
  static int remi_sum() {
    Integer j = new Integer(1);
    for (int i = 0; i< 1000; i++) {
      j = new Integer(j + 1);
    }
    return j;
  }

  static int remi_sumb() {
    Integer j = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j = j + 1;
    }
    return j;
  }

  static int remi_sumf() {
    Integer j = foob(1);
    for (int i = 0; i< 1000; i++) {
      j = j + 1;
    }
    return j;
  }

  static int remi_sump(Integer j) {
    for (int i = 0; i< 1000; i++) {
      j = new Integer(j + 1);
    }
    return j;
  }

  static int remi_sumc() {
    Integer j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = j + ibc;
    }
    return j;
  }

  static int remi_sum2() {
    Integer j1 = new Integer(1);
    Integer j2 = new Integer(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Integer(j1 + 1);
      j2 = new Integer(j2 + 2);
    }
    return j1 + j2;
  }

  static int remi_sumb2() {
    Integer j1 = Integer.valueOf(1);
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + 1;
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static int remi_summ2() {
    Integer j1 = new Integer(1);
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Integer(j1 + 1);
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static int remi_sump2(Integer j1) {
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Integer(j1 + 1);
      j2 = j2 + 2;
    }
    return j1 + j2;
  }

  static int remi_sumc2() {
    Integer j1 = ibc;
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      j1 = j1 + ibc;
      j2 = j2 + 2;
    }
    return j1 + j2;
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static int simple_deop(int i) {
    Integer ib = new Integer(foo(i));
    dummy();
    return ib;
  }

  static int simpleb_deop(int i) {
    Integer ib = Integer.valueOf(foo(i));
    dummy();
    return ib;
  }

  static int simplef_deop(int i) {
    Integer ib = foob(i);
    dummy();
    return ib;
  }

  static int simplep_deop(Integer ib) {
    dummy();
    return ib;
  }

  static int simplec_deop(int i) {
    Integer ib = ibc;
    dummy();
    return ib;
  }

  static int test_deop(int i) {
    Integer ib = new Integer(foo(i));
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static int testb_deop(int i) {
    Integer ib = foo(i);
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static int testf_deop(int i) {
    Integer ib = foob(i);
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static int testp_deop(int i, Integer ib) {
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static int testc_deop(int i) {
    Integer ib = ibc;
    if ((i&1) == 0)
      ib = foo(i+1);
    dummy();
    return ib;
  }

  static int sum_deop(int[] a) {
    int result = 1;
    for (Integer i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static int sumb_deop(int[] a) {
    Integer result = 1;
    for (Integer i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static int sumf_deop(int[] a) {
    Integer result = 1;
    for (Integer i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static int sump_deop(int[] a, Integer result) {
    for (Integer i : a)
        result += foob(i);
    dummy();
    return result;
  }

  static int sumc_deop(int[] a) {
    Integer result = ibc;
    for (Integer i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static int remi_sum_deop() {
    Integer j = new Integer(foo(1));
    for (int i = 0; i< 1000; i++) {
      j = new Integer(foo(j + 1));
    }
    dummy();
    return j;
  }

  static int remi_sumb_deop() {
    Integer j = Integer.valueOf(foo(1));
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static int remi_sumf_deop() {
    Integer j = foob(1);
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static int remi_sump_deop(Integer j) {
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  static int remi_sumc_deop() {
    Integer j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo(j + 1);
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static int remi_sum_cond() {
    Integer j = new Integer(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Integer(j + 1);
      }
    }
    return j;
  }

  static int remi_sumb_cond() {
    Integer j = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static int remi_sumf_cond() {
    Integer j = foob(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static int remi_sump_cond(Integer j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + 1;
      }
    }
    return j;
  }

  static int remi_sumc_cond() {
    Integer j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = j + ibc;
      }
    }
    return j;
  }

  static int remi_sum2_cond() {
    Integer j1 = new Integer(1);
    Integer j2 = new Integer(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Integer(j1 + 1);
      } else {
        j2 = new Integer(j2 + 2);
      }
    }
    return j1 + j2;
  }

  static int remi_sumb2_cond() {
    Integer j1 = Integer.valueOf(1);
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = j1 + 1;
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static int remi_summ2_cond() {
    Integer j1 = new Integer(1);
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Integer(j1 + 1);
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static int remi_sump2_cond(Integer j1) {
    Integer j2 = Integer.valueOf(1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Integer(j1 + 1);
      } else {
        j2 = j2 + 2;
      }
    }
    return j1 + j2;
  }

  static int remi_sumc2_cond() {
    Integer j1 = ibc;
    Integer j2 = Integer.valueOf(1);
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

    final int[] val = new int[] {
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

    int[] res = new int[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0;
    }


    for (int i = 0; i < 12000; i++) {
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

    int[] ia = new int[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, 1);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, 1);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, 1);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump(1);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2(1);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop(1);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond(1);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond(1);
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
