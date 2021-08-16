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
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   compiler.eliminateAutobox.TestByteBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::foob
 *                   compiler.eliminateAutobox.TestByteBoxing
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-EliminateAutoBox
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::dummy
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::foo
 *                   -XX:CompileCommand=exclude,compiler.eliminateAutobox.TestByteBoxing::foob
 *                   compiler.eliminateAutobox.TestByteBoxing
 */

package compiler.eliminateAutobox;

public class TestByteBoxing {

  static final Byte ibc = new Byte((byte)1);

  //===============================================
  // Non-inlined methods to test deoptimization info
  static void dummy()      { }
  static byte foo(byte i)  { return i; }
  static Byte foob(byte i) { return Byte.valueOf(i); }


  static byte simple(byte i) {
    Byte ib = new Byte(i);
    return ib;
  }

  static byte simpleb(byte i) {
    Byte ib = Byte.valueOf(i);
    return ib;
  }

  static byte simplec() {
    Byte ib = ibc;
    return ib;
  }

  static byte simplef(byte i) {
    Byte ib = foob(i);
    return ib;
  }

  static byte simplep(Byte ib) {
    return ib;
  }

  static byte simple2(byte i) {
    Byte ib1 = new Byte(i);
    Byte ib2 = new Byte((byte)(i+1));
    return (byte)(ib1 + ib2);
  }

  static byte simpleb2(byte i) {
    Byte ib1 = Byte.valueOf(i);
    Byte ib2 = Byte.valueOf((byte)(i+1));
    return (byte)(ib1 + ib2);
  }

  static byte simplem2(byte i) {
    Byte ib1 = new Byte(i);
    Byte ib2 = Byte.valueOf((byte)(i+1));
    return (byte)(ib1 + ib2);
  }

  static byte simplep2(byte i, Byte ib1) {
    Byte ib2 = Byte.valueOf((byte)(i+1));
    return (byte)(ib1 + ib2);
  }

  static byte simplec2(byte i) {
    Byte ib1 = ibc;
    Byte ib2 = Byte.valueOf((byte)(i+1));
    return (byte)(ib1 + ib2);
  }

  //===============================================
  static byte test(byte i) {
    Byte ib = new Byte(i);
    if ((i&1) == 0)
      ib = (byte)(i+1);
    return ib;
  }

  static byte testb(byte i) {
    Byte ib = i;
    if ((i&1) == 0)
      ib = (byte)(i+1);
    return ib;
  }

  static byte testm(byte i) {
    Byte ib = i;
    if ((i&1) == 0)
      ib = new Byte((byte)(i+1));
    return ib;
  }

  static byte testp(byte i, Byte ib) {
    if ((i&1) == 0)
      ib = new Byte((byte)(i+1));
    return ib;
  }

  static byte testc(byte i) {
    Byte ib = ibc;
    if ((i&1) == 0)
      ib = new Byte((byte)(i+1));
    return ib;
  }

  static byte test2(byte i) {
    Byte ib1 = new Byte(i);
    Byte ib2 = new Byte((byte)(i+1));
    if ((i&1) == 0) {
      ib1 = new Byte((byte)(i+1));
      ib2 = new Byte((byte)(i+2));
    }
    return (byte)(ib1+ib2);
  }

  static byte testb2(byte i) {
    Byte ib1 = i;
    Byte ib2 = (byte)(i+1);
    if ((i&1) == 0) {
      ib1 = (byte)(i+1);
      ib2 = (byte)(i+2);
    }
    return (byte)(ib1 + ib2);
  }

  static byte testm2(byte i) {
    Byte ib1 = new Byte(i);
    Byte ib2 = (byte)(i+1);
    if ((i&1) == 0) {
      ib1 = new Byte((byte)(i+1));
      ib2 = (byte)(i+2);
    }
    return (byte)(ib1 + ib2);
  }

  static byte testp2(byte i, Byte ib1) {
    Byte ib2 = (byte)(i+1);
    if ((i&1) == 0) {
      ib1 = new Byte((byte)(i+1));
      ib2 = (byte)(i+2);
    }
    return (byte)(ib1 + ib2);
  }

  static byte testc2(byte i) {
    Byte ib1 = ibc;
    Byte ib2 = (byte)(i+1);
    if ((i&1) == 0) {
      ib1 = (byte)(ibc+1);
      ib2 = (byte)(i+2);
    }
    return (byte)(ib1 + ib2);
  }

  //===============================================
  static byte sum(byte[] a) {
    byte result = 1;
    for (Byte i : a)
        result += i;
    return result;
  }

  static byte sumb(byte[] a) {
    Byte result = 1;
    for (Byte i : a)
        result = (byte)(result + i);
    return result;
  }

  static byte sumc(byte[] a) {
    Byte result = ibc;
    for (Byte i : a)
        result = (byte)(result + i);
    return result;
  }

  static byte sumf(byte[] a) {
    Byte result = foob((byte)1);
    for (Byte i : a)
        result = (byte)(result + i);
    return result;
  }

  static byte sump(byte[] a, Byte result) {
    for (Byte i : a)
        result = (byte)(result + i);
    return result;
  }

  static byte sum2(byte[] a) {
    byte result1 = 1;
    byte result2 = 1;
    for (Byte i : a) {
        result1 += i;
        result2 += i + 1;
    }
    return (byte)(result1 + result2);
  }

  static byte sumb2(byte[] a) {
    Byte result1 = 1;
    Byte result2 = 1;
    for (Byte i : a) {
        result1 = (byte)(result1 + i);
        result2 = (byte)(result2 + i + 1);
    }
    return (byte)(result1 + result2);
  }

  static byte summ2(byte[] a) {
    Byte result1 = 1;
    Byte result2 = new Byte((byte)1);
    for (Byte i : a) {
        result1 = (byte)(result1 + i);
        result2 = (byte)(result2 + new Byte((byte)(i + 1)));
    }
    return (byte)(result1 + result2);
  }

  static byte sump2(byte[] a, Byte result2) {
    Byte result1 = 1;
    for (Byte i : a) {
        result1 = (byte)(result1 + i);
        result2 = (byte)(result2 + i + 1);
    }
    return (byte)(result1 + result2);
  }

  static byte sumc2(byte[] a) {
    Byte result1 = 1;
    Byte result2 = ibc;
    for (Byte i : a) {
        result1 = (byte)(result1 + i);
        result2 = (byte)(result2 + i + ibc);
    }
    return (byte)(result1 + result2);
  }

  //===============================================
  static byte remi_sum() {
    Byte j = new Byte((byte)1);
    for (int i = 0; i< 1000; i++) {
      j = new Byte((byte)(j + 1));
    }
    return j;
  }

  static byte remi_sumb() {
    Byte j = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      j = (byte)(j + 1);
    }
    return j;
  }

  static byte remi_sumf() {
    Byte j = foob((byte)1);
    for (int i = 0; i< 1000; i++) {
      j = (byte)(j + 1);
    }
    return j;
  }

  static byte remi_sump(Byte j) {
    for (int i = 0; i< 1000; i++) {
      j = new Byte((byte)(j + 1));
    }
    return j;
  }

  static byte remi_sumc() {
    Byte j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = (byte)(j + ibc);
    }
    return j;
  }

  static byte remi_sum2() {
    Byte j1 = new Byte((byte)1);
    Byte j2 = new Byte((byte)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Byte((byte)(j1 + 1));
      j2 = new Byte((byte)(j2 + 2));
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sumb2() {
    Byte j1 = Byte.valueOf((byte)1);
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      j1 = (byte)(j1 + 1);
      j2 = (byte)(j2 + 2);
    }
    return (byte)(j1 + j2);
  }

  static byte remi_summ2() {
    Byte j1 = new Byte((byte)1);
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Byte((byte)(j1 + 1));
      j2 = (byte)(j2 + 2);
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sump2(Byte j1) {
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      j1 = new Byte((byte)(j1 + 1));
      j2 = (byte)(j2 + 2);
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sumc2() {
    Byte j1 = ibc;
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      j1 = (byte)(j1 + ibc);
      j2 = (byte)(j2 + 2);
    }
    return (byte)(j1 + j2);
  }


  //===============================================
  // Safepointa and debug info for deoptimization
  static byte simple_deop(byte i) {
    Byte ib = new Byte(foo(i));
    dummy();
    return ib;
  }

  static byte simpleb_deop(byte i) {
    Byte ib = Byte.valueOf(foo(i));
    dummy();
    return ib;
  }

  static byte simplef_deop(byte i) {
    Byte ib = foob(i);
    dummy();
    return ib;
  }

  static byte simplep_deop(Byte ib) {
    dummy();
    return ib;
  }

  static byte simplec_deop(byte i) {
    Byte ib = ibc;
    dummy();
    return ib;
  }

  static byte test_deop(byte i) {
    Byte ib = new Byte(foo(i));
    if ((i&1) == 0)
      ib = foo((byte)(i+1));
    dummy();
    return ib;
  }

  static byte testb_deop(byte i) {
    Byte ib = foo(i);
    if ((i&1) == 0)
      ib = foo((byte)(i+1));
    dummy();
    return ib;
  }

  static byte testf_deop(byte i) {
    Byte ib = foob(i);
    if ((i&1) == 0)
      ib = foo((byte)(i+1));
    dummy();
    return ib;
  }

  static byte testp_deop(byte i, Byte ib) {
    if ((i&1) == 0)
      ib = foo((byte)(i+1));
    dummy();
    return ib;
  }

  static byte testc_deop(byte i) {
    Byte ib = ibc;
    if ((i&1) == 0)
      ib = foo((byte)(i+1));
    dummy();
    return ib;
  }

  static byte sum_deop(byte[] a) {
    byte result = 1;
    for (Byte i : a)
        result += foo(i);
    dummy();
    return result;
  }

  static byte sumb_deop(byte[] a) {
    Byte result = 1;
    for (Byte i : a)
        result = (byte)(result + foo(i));
    dummy();
    return result;
  }

  static byte sumf_deop(byte[] a) {
    Byte result = 1;
    for (Byte i : a)
        result = (byte)(result + foob(i));
    dummy();
    return result;
  }

  static byte sump_deop(byte[] a, Byte result) {
    for (Byte i : a)
        result = (byte)(result + foob(i));
    dummy();
    return result;
  }

  static byte sumc_deop(byte[] a) {
    Byte result = ibc;
    for (Byte i : a)
        result = (byte)(result + foo(i));
    dummy();
    return result;
  }

  static byte remi_sum_deop() {
    Byte j = new Byte(foo((byte)1));
    for (int i = 0; i< 1000; i++) {
      j = new Byte(foo((byte)(j + 1)));
    }
    dummy();
    return j;
  }

  static byte remi_sumb_deop() {
    Byte j = Byte.valueOf(foo((byte)1));
    for (int i = 0; i< 1000; i++) {
      j = foo((byte)(j + 1));
    }
    dummy();
    return j;
  }

  static byte remi_sumf_deop() {
    Byte j = foob((byte)1);
    for (int i = 0; i< 1000; i++) {
      j = foo((byte)(j + 1));
    }
    dummy();
    return j;
  }

  static byte remi_sump_deop(Byte j) {
    for (int i = 0; i< 1000; i++) {
      j = foo((byte)(j + 1));
    }
    dummy();
    return j;
  }

  static byte remi_sumc_deop() {
    Byte j = ibc;
    for (int i = 0; i< 1000; i++) {
      j = foo((byte)(j + 1));
    }
    dummy();
    return j;
  }

  //===============================================
  // Conditional increment
  static byte remi_sum_cond() {
    Byte j = new Byte((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = new Byte((byte)(j + 1));
      }
    }
    return j;
  }

  static byte remi_sumb_cond() {
    Byte j = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (byte)(j + 1);
      }
    }
    return j;
  }

  static byte remi_sumf_cond() {
    Byte j = foob((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (byte)(j + 1);
      }
    }
    return j;
  }

  static byte remi_sump_cond(Byte j) {
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (byte)(j + 1);
      }
    }
    return j;
  }

  static byte remi_sumc_cond() {
    Byte j = ibc;
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j = (byte)(j + ibc);
      }
    }
    return j;
  }

  static byte remi_sum2_cond() {
    Byte j1 = new Byte((byte)1);
    Byte j2 = new Byte((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Byte((byte)(j1 + 1));
      } else {
        j2 = new Byte((byte)(j2 + 2));
      }
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sumb2_cond() {
    Byte j1 = Byte.valueOf((byte)1);
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = (byte)(j1 + 1);
      } else {
        j2 = (byte)(j2 + 2);
      }
    }
    return (byte)(j1 + j2);
  }

  static byte remi_summ2_cond() {
    Byte j1 = new Byte((byte)1);
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Byte((byte)(j1 + 1));
      } else {
        j2 = (byte)(j2 + 2);
      }
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sump2_cond(Byte j1) {
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = new Byte((byte)(j1 + 1));
      } else {
        j2 = (byte)(j2 + 2);
      }
    }
    return (byte)(j1 + j2);
  }

  static byte remi_sumc2_cond() {
    Byte j1 = ibc;
    Byte j2 = Byte.valueOf((byte)1);
    for (int i = 0; i< 1000; i++) {
      if ((i&1) == 0) {
        j1 = (byte)(j1 + ibc);
      } else {
        j2 = (byte)(j2 + 2);
      }
    }
    return (byte)(j1 + j2);
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
      -5488, -5488, 12000, -5488, -5488,
       1024,  1024, -5552,  1024,  1024,
      -5488, -5488, 12000, -5488, -5488,
        512,   512,  6256,   512,   512,
      13024, 13024, -5584, 13024, 13024,
        512,   512,  6256,   512,   512,
         45,    45,    45,    45,    45,
         66,    66,    66,    66,    66,
         45,    45,    45,    45,    45,
        -23,   -23,   -23,   -23,   -23,
        -70,   -70,   -70,   -70,   -70,
        -23,   -23,   -23,   -23,   -23,
        -11,   -11,   -11,   -11,   -11,
        -34,   -34,   -34,   -34,   -34
    };

    int[] res = new int[ntests];
    for (int i = 0; i < ntests; i++) {
      res[i] = 0;
    }


    for (int i = 0; i < 12000; i++) {
      res[0] += simple((byte)i);
      res[1] += simpleb((byte)i);
      res[2] += simplec();
      res[3] += simplef((byte)i);
      res[4] += simplep((byte)i);

      res[5] += simple2((byte)i);
      res[6] += simpleb2((byte)i);
      res[7] += simplec2((byte)i);
      res[8] += simplem2((byte)i);
      res[9] += simplep2((byte)i, (byte)i);

      res[10] += simple_deop((byte)i);
      res[11] += simpleb_deop((byte)i);
      res[12] += simplec_deop((byte)i);
      res[13] += simplef_deop((byte)i);
      res[14] += simplep_deop((byte)i);

      res[15] += test((byte)i);
      res[16] += testb((byte)i);
      res[17] += testc((byte)i);
      res[18] += testm((byte)i);
      res[19] += testp((byte)i, (byte)i);

      res[20] += test2((byte)i);
      res[21] += testb2((byte)i);
      res[22] += testc2((byte)i);
      res[23] += testm2((byte)i);
      res[24] += testp2((byte)i, (byte)i);

      res[25] += test_deop((byte)i);
      res[26] += testb_deop((byte)i);
      res[27] += testc_deop((byte)i);
      res[28] += testf_deop((byte)i);
      res[29] += testp_deop((byte)i, (byte)i);
    }

    byte[] ia = new byte[1000];
    for (int i = 0; i < 1000; i++) {
      ia[i] = (byte)i;
    }

    for (int i = 0; i < 100; i++) {
      res[30] = sum(ia);
      res[31] = sumb(ia);
      res[32] = sumc(ia);
      res[33] = sumf(ia);
      res[34] = sump(ia, (byte)1);

      res[35] = sum2(ia);
      res[36] = sumb2(ia);
      res[37] = sumc2(ia);
      res[38] = summ2(ia);
      res[39] = sump2(ia, (byte)1);

      res[40] = sum_deop(ia);
      res[41] = sumb_deop(ia);
      res[42] = sumc_deop(ia);
      res[43] = sumf_deop(ia);
      res[44] = sump_deop(ia, (byte)1);

      res[45] = remi_sum();
      res[46] = remi_sumb();
      res[47] = remi_sumc();
      res[48] = remi_sumf();
      res[49] = remi_sump((byte)1);

      res[50] = remi_sum2();
      res[51] = remi_sumb2();
      res[52] = remi_sumc2();
      res[53] = remi_summ2();
      res[54] = remi_sump2((byte)1);

      res[55] = remi_sum_deop();
      res[56] = remi_sumb_deop();
      res[57] = remi_sumc_deop();
      res[58] = remi_sumf_deop();
      res[59] = remi_sump_deop((byte)1);

      res[60] = remi_sum_cond();
      res[61] = remi_sumb_cond();
      res[62] = remi_sumc_cond();
      res[63] = remi_sumf_cond();
      res[64] = remi_sump_cond((byte)1);

      res[65] = remi_sum2_cond();
      res[66] = remi_sumb2_cond();
      res[67] = remi_sumc2_cond();
      res[68] = remi_summ2_cond();
      res[69] = remi_sump2_cond((byte)1);
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
