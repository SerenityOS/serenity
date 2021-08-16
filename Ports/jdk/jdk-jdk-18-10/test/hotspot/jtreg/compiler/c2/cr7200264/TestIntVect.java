/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.c2.cr7200264;
/*
 * Copy of test/compiler/6340864/TestIntVect.java without performance tests.
 */
public class TestIntVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final int ADD_INIT = Integer.MAX_VALUE-500;
  private static final int BIT_MASK = 0xEC80F731;
  private static final int VALUE = 15;
  private static final int SHIFT = 32;

  public static void main(String args[]) {
    System.out.println("Testing Integer vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    int[] a0 = new int[ARRLEN];
    int[] a1 = new int[ARRLEN];
    int[] a2 = new int[ARRLEN];
    int[] a3 = new int[ARRLEN];
    int[] a4 = new int[ARRLEN];
    long[] p2 = new long[ARRLEN/2];
    // Initialize
    int gold_sum = 0;
    for (int i=0; i<ARRLEN; i++) {
      int val = (int)(ADD_INIT+i);
      gold_sum += val;
      a1[i] = val;
      a2[i] = (int)VALUE;
      a3[i] = (int)-VALUE;
      a4[i] = (int)BIT_MASK;
    }
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_sum(a1);
      test_addc(a0, a1);
      test_addv(a0, a1, (int)VALUE);
      test_adda(a0, a1, a2);
      test_subc(a0, a1);
      test_subv(a0, a1, (int)VALUE);
      test_suba(a0, a1, a2);
      test_mulc(a0, a1);
      test_mulv(a0, a1, (int)VALUE);
      test_mula(a0, a1, a2);
      test_divc(a0, a1);
      test_divv(a0, a1, (int)VALUE);
      test_diva(a0, a1, a2);
      test_mulc_n(a0, a1);
      test_mulv(a0, a1, (int)-VALUE);
      test_mula(a0, a1, a3);
      test_divc_n(a0, a1);
      test_divv(a0, a1, (int)-VALUE);
      test_diva(a0, a1, a3);
      test_andc(a0, a1);
      test_andv(a0, a1, (int)BIT_MASK);
      test_anda(a0, a1, a4);
      test_orc(a0, a1);
      test_orv(a0, a1, (int)BIT_MASK);
      test_ora(a0, a1, a4);
      test_xorc(a0, a1);
      test_xorv(a0, a1, (int)BIT_MASK);
      test_xora(a0, a1, a4);
      test_sllc(a0, a1);
      test_sllv(a0, a1, VALUE);
      test_srlc(a0, a1);
      test_srlv(a0, a1, VALUE);
      test_srac(a0, a1);
      test_srav(a0, a1, VALUE);
      test_sllc_n(a0, a1);
      test_sllv(a0, a1, -VALUE);
      test_srlc_n(a0, a1);
      test_srlv(a0, a1, -VALUE);
      test_srac_n(a0, a1);
      test_srav(a0, a1, -VALUE);
      test_sllc_o(a0, a1);
      test_sllv(a0, a1, SHIFT);
      test_srlc_o(a0, a1);
      test_srlv(a0, a1, SHIFT);
      test_srac_o(a0, a1);
      test_srav(a0, a1, SHIFT);
      test_sllc_on(a0, a1);
      test_sllv(a0, a1, -SHIFT);
      test_srlc_on(a0, a1);
      test_srlv(a0, a1, -SHIFT);
      test_srac_on(a0, a1);
      test_srav(a0, a1, -SHIFT);
      test_pack2(p2, a1);
      test_unpack2(a0, p2);
      test_pack2_swap(p2, a1);
      test_unpack2_swap(a0, p2);
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      int sum = test_sum(a1);
      if (sum != gold_sum) {
        System.err.println("test_sum:  " + sum + " != " + gold_sum);
        errn++;
      }

      test_addc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_addc: ", i, a0[i], (int)((int)(ADD_INIT+i)+VALUE));
      }
      test_addv(a0, a1, (int)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_addv: ", i, a0[i], (int)((int)(ADD_INIT+i)+VALUE));
      }
      test_adda(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_adda: ", i, a0[i], (int)((int)(ADD_INIT+i)+VALUE));
      }

      test_subc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_subc: ", i, a0[i], (int)((int)(ADD_INIT+i)-VALUE));
      }
      test_subv(a0, a1, (int)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_subv: ", i, a0[i], (int)((int)(ADD_INIT+i)-VALUE));
      }
      test_suba(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_suba: ", i, a0[i], (int)((int)(ADD_INIT+i)-VALUE));
      }

      test_mulc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulc: ", i, a0[i], (int)((int)(ADD_INIT+i)*VALUE));
      }
      test_mulv(a0, a1, (int)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulv: ", i, a0[i], (int)((int)(ADD_INIT+i)*VALUE));
      }
      test_mula(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mula: ", i, a0[i], (int)((int)(ADD_INIT+i)*VALUE));
      }

      test_divc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divc: ", i, a0[i], (int)((int)(ADD_INIT+i)/VALUE));
      }
      test_divv(a0, a1, (int)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divv: ", i, a0[i], (int)((int)(ADD_INIT+i)/VALUE));
      }
      test_diva(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_diva: ", i, a0[i], (int)((int)(ADD_INIT+i)/VALUE));
      }

      test_mulc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulc_n: ", i, a0[i], (int)((int)(ADD_INIT+i)*(-VALUE)));
      }
      test_mulv(a0, a1, (int)-VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulv_n: ", i, a0[i], (int)((int)(ADD_INIT+i)*(-VALUE)));
      }
      test_mula(a0, a1, a3);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mula_n: ", i, a0[i], (int)((int)(ADD_INIT+i)*(-VALUE)));
      }

      test_divc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divc_n: ", i, a0[i], (int)((int)(ADD_INIT+i)/(-VALUE)));
      }
      test_divv(a0, a1, (int)-VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divv_n: ", i, a0[i], (int)((int)(ADD_INIT+i)/(-VALUE)));
      }
      test_diva(a0, a1, a3);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_diva_n: ", i, a0[i], (int)((int)(ADD_INIT+i)/(-VALUE)));
      }

      test_andc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_andc: ", i, a0[i], (int)((int)(ADD_INIT+i)&BIT_MASK));
      }
      test_andv(a0, a1, (int)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_andv: ", i, a0[i], (int)((int)(ADD_INIT+i)&BIT_MASK));
      }
      test_anda(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_anda: ", i, a0[i], (int)((int)(ADD_INIT+i)&BIT_MASK));
      }

      test_orc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_orc: ", i, a0[i], (int)((int)(ADD_INIT+i)|BIT_MASK));
      }
      test_orv(a0, a1, (int)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_orv: ", i, a0[i], (int)((int)(ADD_INIT+i)|BIT_MASK));
      }
      test_ora(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ora: ", i, a0[i], (int)((int)(ADD_INIT+i)|BIT_MASK));
      }

      test_xorc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xorc: ", i, a0[i], (int)((int)(ADD_INIT+i)^BIT_MASK));
      }
      test_xorv(a0, a1, (int)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xorv: ", i, a0[i], (int)((int)(ADD_INIT+i)^BIT_MASK));
      }
      test_xora(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xora: ", i, a0[i], (int)((int)(ADD_INIT+i)^BIT_MASK));
      }

      test_sllc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc: ", i, a0[i], (int)((int)(ADD_INIT+i)<<VALUE));
      }
      test_sllv(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv: ", i, a0[i], (int)((int)(ADD_INIT+i)<<VALUE));
      }

      test_srlc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>VALUE));
      }
      test_srlv(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>VALUE));
      }

      test_srac(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac: ", i, a0[i], (int)((int)(ADD_INIT+i)>>VALUE));
      }
      test_srav(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav: ", i, a0[i], (int)((int)(ADD_INIT+i)>>VALUE));
      }

      test_sllc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_n: ", i, a0[i], (int)((int)(ADD_INIT+i)<<(-VALUE)));
      }
      test_sllv(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_n: ", i, a0[i], (int)((int)(ADD_INIT+i)<<(-VALUE)));
      }

      test_srlc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_n: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>(-VALUE)));
      }
      test_srlv(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_n: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>(-VALUE)));
      }

      test_srac_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_n: ", i, a0[i], (int)((int)(ADD_INIT+i)>>(-VALUE)));
      }
      test_srav(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_n: ", i, a0[i], (int)((int)(ADD_INIT+i)>>(-VALUE)));
      }

      test_sllc_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_o: ", i, a0[i], (int)((int)(ADD_INIT+i)<<SHIFT));
      }
      test_sllv(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_o: ", i, a0[i], (int)((int)(ADD_INIT+i)<<SHIFT));
      }

      test_srlc_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_o: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>SHIFT));
      }
      test_srlv(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_o: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>SHIFT));
      }

      test_srac_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_o: ", i, a0[i], (int)((int)(ADD_INIT+i)>>SHIFT));
      }
      test_srav(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_o: ", i, a0[i], (int)((int)(ADD_INIT+i)>>SHIFT));
      }

      test_sllc_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_on: ", i, a0[i], (int)((int)(ADD_INIT+i)<<(-SHIFT)));
      }
      test_sllv(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_on: ", i, a0[i], (int)((int)(ADD_INIT+i)<<(-SHIFT)));
      }

      test_srlc_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_on: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>(-SHIFT)));
      }
      test_srlv(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_on: ", i, a0[i], (int)((int)(ADD_INIT+i)>>>(-SHIFT)));
      }

      test_srac_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_on: ", i, a0[i], (int)((int)(ADD_INIT+i)>>(-SHIFT)));
      }
      test_srav(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_on: ", i, a0[i], (int)((int)(ADD_INIT+i)>>(-SHIFT)));
      }

      test_pack2(p2, a1);
      for (int i=0; i<ARRLEN/2; i++) {
        errn += verify("test_pack2: ", i, p2[i], ((long)(ADD_INIT+2*i) & 0xFFFFFFFFl) | ((long)(ADD_INIT+2*i+1) << 32));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack2(a0, p2);
      for (int i=0; i<(ARRLEN&(-2)); i++) {
        errn += verify("test_unpack2: ", i, a0[i], (ADD_INIT+i));
      }

      test_pack2_swap(p2, a1);
      for (int i=0; i<ARRLEN/2; i++) {
        errn += verify("test_pack2_swap: ", i, p2[i], ((long)(ADD_INIT+2*i+1) & 0xFFFFFFFFl) | ((long)(ADD_INIT+2*i) << 32));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack2_swap(a0, p2);
      for (int i=0; i<(ARRLEN&(-2)); i++) {
        errn += verify("test_unpack2_swap: ", i, a0[i], (ADD_INIT+i));
      }

    }

    return errn;
  }

  static int test_sum(int[] a1) {
    int sum = 0;
    for (int i = 0; i < a1.length; i+=1) {
      sum += a1[i];
    }
    return sum;
  }

  static void test_addc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]+VALUE);
    }
  }
  static void test_addv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]+b);
    }
  }
  static void test_adda(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]+a2[i]);
    }
  }

  static void test_subc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]-VALUE);
    }
  }
  static void test_subv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]-b);
    }
  }
  static void test_suba(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]-a2[i]);
    }
  }

  static void test_mulc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]*VALUE);
    }
  }
  static void test_mulc_n(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]*(-VALUE));
    }
  }
  static void test_mulv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]*b);
    }
  }
  static void test_mula(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]*a2[i]);
    }
  }

  static void test_divc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]/VALUE);
    }
  }
  static void test_divc_n(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]/(-VALUE));
    }
  }
  static void test_divv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]/b);
    }
  }
  static void test_diva(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]/a2[i]);
    }
  }

  static void test_andc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]&BIT_MASK);
    }
  }
  static void test_andv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]&b);
    }
  }
  static void test_anda(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]&a2[i]);
    }
  }

  static void test_orc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]|BIT_MASK);
    }
  }
  static void test_orv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]|b);
    }
  }
  static void test_ora(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]|a2[i]);
    }
  }

  static void test_xorc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]^BIT_MASK);
    }
  }
  static void test_xorv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]^b);
    }
  }
  static void test_xora(int[] a0, int[] a1, int[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]^a2[i]);
    }
  }

  static void test_sllc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]<<VALUE);
    }
  }
  static void test_sllc_n(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]<<(-VALUE));
    }
  }
  static void test_sllc_o(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]<<SHIFT);
    }
  }
  static void test_sllc_on(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]<<(-SHIFT));
    }
  }
  static void test_sllv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]<<b);
    }
  }

  static void test_srlc(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>>VALUE);
    }
  }
  static void test_srlc_n(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>>(-VALUE));
    }
  }
  static void test_srlc_o(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>>SHIFT);
    }
  }
  static void test_srlc_on(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>>(-SHIFT));
    }
  }
  static void test_srlv(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>>b);
    }
  }

  static void test_srac(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>VALUE);
    }
  }
  static void test_srac_n(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>(-VALUE));
    }
  }
  static void test_srac_o(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>SHIFT);
    }
  }
  static void test_srac_on(int[] a0, int[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>(-SHIFT));
    }
  }
  static void test_srav(int[] a0, int[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (int)(a1[i]>>b);
    }
  }

  static void test_pack2(long[] p2, int[] a1) {
    if (p2.length*2 > a1.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      long l0 = (long)a1[i*2+0];
      long l1 = (long)a1[i*2+1];
      p2[i] = (l1 << 32) | (l0 & 0xFFFFFFFFl);
    }
  }
  static void test_unpack2(int[] a0, long[] p2) {
    if (p2.length*2 > a0.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      long l = p2[i];
      a0[i*2+0] = (int)(l & 0xFFFFFFFFl);
      a0[i*2+1] = (int)(l >> 32);
    }
  }
  static void test_pack2_swap(long[] p2, int[] a1) {
    if (p2.length*2 > a1.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      long l0 = (long)a1[i*2+0];
      long l1 = (long)a1[i*2+1];
      p2[i] = (l0 << 32) | (l1 & 0xFFFFFFFFl);
    }
  }
  static void test_unpack2_swap(int[] a0, long[] p2) {
    if (p2.length*2 > a0.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      long l = p2[i];
      a0[i*2+0] = (int)(l >> 32);
      a0[i*2+1] = (int)(l & 0xFFFFFFFFl);
    }
  }

  static int verify(String text, int i, int elem, int val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }

  static int verify(String text, int i, long elem, long val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + Long.toHexString(elem) + " != " + Long.toHexString(val));
      return 1;
    }
    return 0;
  }
}
