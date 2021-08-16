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
 * @bug 6340864
 * @summary Implement vectorization optimizations in hotspot-server
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m compiler.c2.cr6340864.TestByteVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=8 compiler.c2.cr6340864.TestByteVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=16 compiler.c2.cr6340864.TestByteVect
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=32 compiler.c2.cr6340864.TestByteVect
 */

package compiler.c2.cr6340864;

public class TestByteVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final int ADD_INIT = 63;
  private static final int BIT_MASK = 0xB7;
  private static final int VALUE = 3;
  private static final int SHIFT = 8;

  public static void main(String args[]) {
    System.out.println("Testing Byte vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    byte[] a0 = new byte[ARRLEN];
    byte[] a1 = new byte[ARRLEN];
    byte[] a2 = new byte[ARRLEN];
    byte[] a3 = new byte[ARRLEN];
    byte[] a4 = new byte[ARRLEN];
    short[] p2 = new short[ARRLEN/2];
      int[] p4 = new   int[ARRLEN/4];
     long[] p8 = new  long[ARRLEN/8];
    // Initialize
    int gold_sum = 0;
    for (int i=0; i<ARRLEN; i++) {
      byte val = (byte)(ADD_INIT+i);
      gold_sum += val;
      a1[i] = val;
      a2[i] = (byte)VALUE;
      a3[i] = (byte)-VALUE;
      a4[i] = (byte)BIT_MASK;
    }
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_sum(a1);
      test_addc(a0, a1);
      test_addv(a0, a1, (byte)VALUE);
      test_adda(a0, a1, a2);
      test_subc(a0, a1);
      test_subv(a0, a1, (byte)VALUE);
      test_suba(a0, a1, a2);

      test_mulc(a0, a1);
      test_mulv(a0, a1, (byte)VALUE);
      test_mula(a0, a1, a2);
      test_divc(a0, a1);
      test_divv(a0, a1, (byte)VALUE);
      test_diva(a0, a1, a2);
      test_mulc_n(a0, a1);
      test_mulv(a0, a1, (byte)-VALUE);
      test_mula(a0, a1, a3);
      test_divc_n(a0, a1);
      test_divv(a0, a1, (byte)-VALUE);
      test_diva(a0, a1, a3);

      test_andc(a0, a1);
      test_andv(a0, a1, (byte)BIT_MASK);
      test_anda(a0, a1, a4);
      test_orc(a0, a1);
      test_orv(a0, a1, (byte)BIT_MASK);
      test_ora(a0, a1, a4);
      test_xorc(a0, a1);
      test_xorv(a0, a1, (byte)BIT_MASK);
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

      test_sllc_add(a0, a1);
      test_sllv_add(a0, a1, ADD_INIT);
      test_srlc_add(a0, a1);
      test_srlv_add(a0, a1, ADD_INIT);
      test_srac_add(a0, a1);
      test_srav_add(a0, a1, ADD_INIT);

      test_sllc_and(a0, a1);
      test_sllv_and(a0, a1, BIT_MASK);
      test_srlc_and(a0, a1);
      test_srlv_and(a0, a1, BIT_MASK);
      test_srac_and(a0, a1);
      test_srav_and(a0, a1, BIT_MASK);

      test_pack2(p2, a1);
      test_unpack2(a0, p2);
      test_pack2_swap(p2, a1);
      test_unpack2_swap(a0, p2);
      test_pack4(p4, a1);
      test_unpack4(a0, p4);
      test_pack4_swap(p4, a1);
      test_unpack4_swap(a0, p4);
      test_pack8(p8, a1);
      test_unpack8(a0, p8);
      test_pack8_swap(p8, a1);
      test_unpack8_swap(a0, p8);
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
        errn += verify("test_addc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)+VALUE));
      }
      test_addv(a0, a1, (byte)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_addv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)+VALUE));
      }
      test_adda(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_adda: ", i, a0[i], (byte)((byte)(ADD_INIT+i)+VALUE));
      }

      test_subc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_subc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)-VALUE));
      }
      test_subv(a0, a1, (byte)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_subv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)-VALUE));
      }
      test_suba(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_suba: ", i, a0[i], (byte)((byte)(ADD_INIT+i)-VALUE));
      }

      test_mulc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*VALUE));
      }
      test_mulv(a0, a1, (byte)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*VALUE));
      }
      test_mula(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mula: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*VALUE));
      }

      test_divc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/VALUE));
      }
      test_divv(a0, a1, (byte)VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/VALUE));
      }
      test_diva(a0, a1, a2);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_diva: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/VALUE));
      }

      test_mulc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulc_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*(-VALUE)));
      }
      test_mulv(a0, a1, (byte)-VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mulv_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*(-VALUE)));
      }
      test_mula(a0, a1, a3);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_mula_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)*(-VALUE)));
      }

      test_divc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divc_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/(-VALUE)));
      }
      test_divv(a0, a1, (byte)-VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_divv_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/(-VALUE)));
      }
      test_diva(a0, a1, a3);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_diva_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)/(-VALUE)));
      }

      test_andc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_andc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)&BIT_MASK));
      }
      test_andv(a0, a1, (byte)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_andv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)&BIT_MASK));
      }
      test_anda(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_anda: ", i, a0[i], (byte)((byte)(ADD_INIT+i)&BIT_MASK));
      }

      test_orc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_orc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)|BIT_MASK));
      }
      test_orv(a0, a1, (byte)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_orv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)|BIT_MASK));
      }
      test_ora(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_ora: ", i, a0[i], (byte)((byte)(ADD_INIT+i)|BIT_MASK));
      }

      test_xorc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xorc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)^BIT_MASK));
      }
      test_xorv(a0, a1, (byte)BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xorv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)^BIT_MASK));
      }
      test_xora(a0, a1, a4);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_xora: ", i, a0[i], (byte)((byte)(ADD_INIT+i)^BIT_MASK));
      }

      test_sllc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<VALUE));
      }
      test_sllv(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<VALUE));
      }

      test_srlc(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>VALUE));
      }
      test_srlv(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>VALUE));
      }

      test_srac(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>VALUE));
      }
      test_srav(a0, a1, VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>VALUE));
      }

      test_sllc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<(-VALUE)));
      }
      test_sllv(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<(-VALUE)));
      }

      test_srlc_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>(-VALUE)));
      }
      test_srlv(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>(-VALUE)));
      }

      test_srac_n(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>(-VALUE)));
      }
      test_srav(a0, a1, -VALUE);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_n: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>(-VALUE)));
      }

      test_sllc_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<SHIFT));
      }
      test_sllv(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<SHIFT));
      }

      test_srlc_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>SHIFT));
      }
      test_srlv(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>SHIFT));
      }

      test_srac_o(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>SHIFT));
      }
      test_srav(a0, a1, SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_o: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>SHIFT));
      }

      test_sllc_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<(-SHIFT)));
      }
      test_sllv(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)<<(-SHIFT)));
      }

      test_srlc_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>(-SHIFT)));
      }
      test_srlv(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>>(-SHIFT)));
      }

      test_srac_on(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>(-SHIFT)));
      }
      test_srav(a0, a1, -SHIFT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_on: ", i, a0[i], (byte)((byte)(ADD_INIT+i)>>(-SHIFT)));
      }

      test_sllc_add(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)<<VALUE));
      }
      test_sllv_add(a0, a1, ADD_INIT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)<<VALUE));
      }

      test_srlc_add(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)>>>VALUE));
      }
      test_srlv_add(a0, a1, ADD_INIT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)>>>VALUE));
      }

      test_srac_add(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)>>VALUE));
      }
      test_srav_add(a0, a1, ADD_INIT);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_add: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) + ADD_INIT)>>VALUE));
      }

      test_sllc_and(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllc_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)<<VALUE));
      }
      test_sllv_and(a0, a1, BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_sllv_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)<<VALUE));
      }

      test_srlc_and(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlc_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)>>>VALUE));
      }
      test_srlv_and(a0, a1, BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srlv_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)>>>VALUE));
      }

      test_srac_and(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srac_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)>>VALUE));
      }
      test_srav_and(a0, a1, BIT_MASK);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_srav_and: ", i, a0[i], (byte)(((byte)(ADD_INIT+i) & BIT_MASK)>>VALUE));
      }

      test_pack2(p2, a1);
      for (int i=0; i<ARRLEN/2; i++) {
        errn += verify("test_pack2: ", i, p2[i], (short)(((short)(ADD_INIT+2*i) & 0xFF) | ((short)(ADD_INIT+2*i+1) << 8)));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack2(a0, p2);
      for (int i=0; i<(ARRLEN&(-2)); i++) {
        errn += verify("test_unpack2: ", i, a0[i], (byte)(ADD_INIT+i));
      }

      test_pack2_swap(p2, a1);
      for (int i=0; i<ARRLEN/2; i++) {
        errn += verify("test_pack2_swap: ", i, p2[i], (short)(((short)(ADD_INIT+2*i+1) & 0xFF) | ((short)(ADD_INIT+2*i) << 8)));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack2_swap(a0, p2);
      for (int i=0; i<(ARRLEN&(-2)); i++) {
        errn += verify("test_unpack2_swap: ", i, a0[i], (byte)(ADD_INIT+i));
      }

      test_pack4(p4, a1);
      for (int i=0; i<ARRLEN/4; i++) {
        errn += verify("test_pack4: ", i, p4[i],  ((int)(ADD_INIT+4*i+0) & 0xFF) |
                                                 (((int)(ADD_INIT+4*i+1) & 0xFF) <<  8)  |
                                                 (((int)(ADD_INIT+4*i+2) & 0xFF) << 16)  |
                                                 (((int)(ADD_INIT+4*i+3) & 0xFF) << 24));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack4(a0, p4);
      for (int i=0; i<(ARRLEN&(-4)); i++) {
        errn += verify("test_unpack4: ", i, a0[i], (byte)(ADD_INIT+i));
      }

      test_pack4_swap(p4, a1);
      for (int i=0; i<ARRLEN/4; i++) {
        errn += verify("test_pack4_swap: ", i, p4[i],  ((int)(ADD_INIT+4*i+3) & 0xFF) |
                                                      (((int)(ADD_INIT+4*i+2) & 0xFF) <<  8)  |
                                                      (((int)(ADD_INIT+4*i+1) & 0xFF) << 16)  |
                                                      (((int)(ADD_INIT+4*i+0) & 0xFF) << 24));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack4_swap(a0, p4);
      for (int i=0; i<(ARRLEN&(-4)); i++) {
        errn += verify("test_unpack4_swap: ", i, a0[i], (byte)(ADD_INIT+i));
      }

      test_pack8(p8, a1);
      for (int i=0; i<ARRLEN/8; i++) {
        errn += verify("test_pack8: ", i, p8[i],  ((long)(ADD_INIT+8*i+0) & 0xFFl) |
                                                 (((long)(ADD_INIT+8*i+1) & 0xFFl) <<  8)  |
                                                 (((long)(ADD_INIT+8*i+2) & 0xFFl) << 16)  |
                                                 (((long)(ADD_INIT+8*i+3) & 0xFFl) << 24)  |
                                                 (((long)(ADD_INIT+8*i+4) & 0xFFl) << 32)  |
                                                 (((long)(ADD_INIT+8*i+5) & 0xFFl) << 40)  |
                                                 (((long)(ADD_INIT+8*i+6) & 0xFFl) << 48)  |
                                                 (((long)(ADD_INIT+8*i+7) & 0xFFl) << 56));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack8(a0, p8);
      for (int i=0; i<(ARRLEN&(-8)); i++) {
        errn += verify("test_unpack8: ", i, a0[i], (byte)(ADD_INIT+i));
      }

      test_pack8_swap(p8, a1);
      for (int i=0; i<ARRLEN/8; i++) {
        errn += verify("test_pack8_swap: ", i, p8[i],  ((long)(ADD_INIT+8*i+7) & 0xFFl) |
                                                      (((long)(ADD_INIT+8*i+6) & 0xFFl) <<  8)  |
                                                      (((long)(ADD_INIT+8*i+5) & 0xFFl) << 16)  |
                                                      (((long)(ADD_INIT+8*i+4) & 0xFFl) << 24)  |
                                                      (((long)(ADD_INIT+8*i+3) & 0xFFl) << 32)  |
                                                      (((long)(ADD_INIT+8*i+2) & 0xFFl) << 40)  |
                                                      (((long)(ADD_INIT+8*i+1) & 0xFFl) << 48)  |
                                                      (((long)(ADD_INIT+8*i+0) & 0xFFl) << 56));
      }
      for (int i=0; i<ARRLEN; i++) {
        a0[i] = -1;
      }
      test_unpack8_swap(a0, p8);
      for (int i=0; i<(ARRLEN&(-8)); i++) {
        errn += verify("test_unpack8_swap: ", i, a0[i], (byte)(ADD_INIT+i));
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
      test_addv(a0, a1, (byte)VALUE);
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
      test_subv(a0, a1, (byte)VALUE);
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
      test_mulv(a0, a1, (byte)VALUE);
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
      test_divv(a0, a1, (byte)VALUE);
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
      test_mulv(a0, a1, (byte)-VALUE);
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
      test_divv(a0, a1, (byte)-VALUE);
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
      test_andc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_andc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_andv(a0, a1, (byte)BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_andv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_anda(a0, a1, a4);
    }
    end = System.currentTimeMillis();
    System.out.println("test_anda: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_orc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_orc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_orv(a0, a1, (byte)BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_orv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_ora(a0, a1, a4);
    }
    end = System.currentTimeMillis();
    System.out.println("test_ora: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_xorc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_xorc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_xorv(a0, a1, (byte)BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_xorv: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_xora(a0, a1, a4);
    }
    end = System.currentTimeMillis();
    System.out.println("test_xora: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac_n: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc_o(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc_o: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc_o(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc_o: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac_o(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac_o: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc_on(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc_on: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv(a0, a1, -SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc_on(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc_on: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv(a0, a1, -SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac_on(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac_on: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav(a0, a1, -SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc_add(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc_add: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv_add(a0, a1, ADD_INIT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv_add: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc_add(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc_add: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv_add(a0, a1, ADD_INIT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv_add: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac_add(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac_add: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav_add(a0, a1, ADD_INIT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav_add: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllc_and(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllc_and: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_sllv_and(a0, a1, BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_sllv_and: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlc_and(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlc_and: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srlv_and(a0, a1, BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srlv_and: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srac_and(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srac_and: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_srav_and(a0, a1, BIT_MASK);
    }
    end = System.currentTimeMillis();
    System.out.println("test_srav_and: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack2(p2, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack2: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack2(a0, p2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack2: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack2_swap(p2, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack2_swap: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack2_swap(a0, p2);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack2_swap: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack4(p4, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack4: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack4(a0, p4);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack4: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack4_swap(p4, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack4_swap: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack4_swap(a0, p4);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack4_swap: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack8(p8, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack8: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack8(a0, p8);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack8: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_pack8_swap(p8, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_pack8_swap: " + (end - start));
    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unpack8_swap(a0, p8);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unpack8_swap: " + (end - start));

    return errn;
  }

  static int test_sum(byte[] a1) {
    int sum = 0;
    for (int i = 0; i < a1.length; i+=1) {
      sum += a1[i];
    }
    return sum;
  }

  static void test_addc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]+VALUE);
    }
  }
  static void test_addv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]+b);
    }
  }
  static void test_adda(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]+a2[i]);
    }
  }

  static void test_subc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]-VALUE);
    }
  }
  static void test_subv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]-b);
    }
  }
  static void test_suba(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]-a2[i]);
    }
  }

  static void test_mulc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]*VALUE);
    }
  }
  static void test_mulc_n(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]*(-VALUE));
    }
  }
  static void test_mulv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]*b);
    }
  }
  static void test_mula(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]*a2[i]);
    }
  }

  static void test_divc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]/VALUE);
    }
  }
  static void test_divc_n(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]/(-VALUE));
    }
  }
  static void test_divv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]/b);
    }
  }
  static void test_diva(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]/a2[i]);
    }
  }

  static void test_andc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]&BIT_MASK);
    }
  }
  static void test_andv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]&b);
    }
  }
  static void test_anda(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]&a2[i]);
    }
  }

  static void test_orc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]|BIT_MASK);
    }
  }
  static void test_orv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]|b);
    }
  }
  static void test_ora(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]|a2[i]);
    }
  }

  static void test_xorc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]^BIT_MASK);
    }
  }
  static void test_xorv(byte[] a0, byte[] a1, byte b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]^b);
    }
  }
  static void test_xora(byte[] a0, byte[] a1, byte[] a2) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]^a2[i]);
    }
  }

  static void test_sllc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]<<VALUE);
    }
  }
  static void test_sllc_n(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]<<(-VALUE));
    }
  }
  static void test_sllc_o(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]<<SHIFT);
    }
  }
  static void test_sllc_on(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]<<(-SHIFT));
    }
  }
  static void test_sllv(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]<<b);
    }
  }
  static void test_sllc_add(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + ADD_INIT)<<VALUE);
    }
  }
  static void test_sllv_add(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + b)<<VALUE);
    }
  }
  static void test_sllc_and(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & BIT_MASK)<<VALUE);
    }
  }
  static void test_sllv_and(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & b)<<VALUE);
    }
  }

  static void test_srlc(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>>VALUE);
    }
  }
  static void test_srlc_n(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>>(-VALUE));
    }
  }
  static void test_srlc_o(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>>SHIFT);
    }
  }
  static void test_srlc_on(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>>(-SHIFT));
    }
  }
  static void test_srlv(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>>b);
    }
  }
  static void test_srlc_add(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + ADD_INIT)>>>VALUE);
    }
  }
  static void test_srlv_add(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + b)>>>VALUE);
    }
  }
  static void test_srlc_and(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & BIT_MASK)>>>VALUE);
    }
  }
  static void test_srlv_and(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & b)>>>VALUE);
    }
  }

  static void test_srac(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>VALUE);
    }
  }
  static void test_srac_n(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>(-VALUE));
    }
  }
  static void test_srac_o(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>SHIFT);
    }
  }
  static void test_srac_on(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>(-SHIFT));
    }
  }
  static void test_srav(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)(a1[i]>>b);
    }
  }
  static void test_srac_add(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + ADD_INIT)>>VALUE);
    }
  }
  static void test_srav_add(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] + b)>>VALUE);
    }
  }
  static void test_srac_and(byte[] a0, byte[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & BIT_MASK)>>VALUE);
    }
  }
  static void test_srav_and(byte[] a0, byte[] a1, int b) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (byte)((a1[i] & b)>>VALUE);
    }
  }

  static void test_pack2(short[] p2, byte[] a1) {
    if (p2.length*2 > a1.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      short l0 = (short)a1[i*2+0];
      short l1 = (short)a1[i*2+1];
      p2[i] = (short)((l1 << 8) | (l0 & 0xFF));
    }
  }
  static void test_unpack2(byte[] a0, short[] p2) {
    if (p2.length*2 > a0.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      short l = p2[i];
      a0[i*2+0] = (byte)(l & 0xFF);
      a0[i*2+1] = (byte)(l >> 8);
    }
  }
  static void test_pack2_swap(short[] p2, byte[] a1) {
    if (p2.length*2 > a1.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      short l0 = (short)a1[i*2+0];
      short l1 = (short)a1[i*2+1];
      p2[i] = (short)((l0 << 8) | (l1 & 0xFF));
    }
  }
  static void test_unpack2_swap(byte[] a0, short[] p2) {
    if (p2.length*2 > a0.length) return;
    for (int i = 0; i < p2.length; i+=1) {
      short l = p2[i];
      a0[i*2+0] = (byte)(l >> 8);
      a0[i*2+1] = (byte)(l & 0xFF);
    }
  }

  static void test_pack4(int[] p4, byte[] a1) {
    if (p4.length*4 > a1.length) return;
    for (int i = 0; i < p4.length; i+=1) {
      int l0 = (int)a1[i*4+0];
      int l1 = (int)a1[i*4+1];
      int l2 = (int)a1[i*4+2];
      int l3 = (int)a1[i*4+3];
      p4[i] = (l0 & 0xFF) |
             ((l1 & 0xFF) <<  8) |
             ((l2 & 0xFF) << 16) |
             ((l3 & 0xFF) << 24);
    }
  }
  static void test_unpack4(byte[] a0, int[] p4) {
    if (p4.length*4 > a0.length) return;
    for (int i = 0; i < p4.length; i+=1) {
      int l = p4[i];
      a0[i*4+0] = (byte)(l & 0xFF);
      a0[i*4+1] = (byte)(l >>  8);
      a0[i*4+2] = (byte)(l >> 16);
      a0[i*4+3] = (byte)(l >> 24);
    }
  }
  static void test_pack4_swap(int[] p4, byte[] a1) {
    if (p4.length*4 > a1.length) return;
    for (int i = 0; i < p4.length; i+=1) {
      int l0 = (int)a1[i*4+0];
      int l1 = (int)a1[i*4+1];
      int l2 = (int)a1[i*4+2];
      int l3 = (int)a1[i*4+3];
      p4[i] = (l3 & 0xFF) |
             ((l2 & 0xFF) <<  8) |
             ((l1 & 0xFF) << 16) |
             ((l0 & 0xFF) << 24);
    }
  }
  static void test_unpack4_swap(byte[] a0, int[] p4) {
    if (p4.length*4 > a0.length) return;
    for (int i = 0; i < p4.length; i+=1) {
      int l = p4[i];
      a0[i*4+0] = (byte)(l >> 24);
      a0[i*4+1] = (byte)(l >> 16);
      a0[i*4+2] = (byte)(l >>  8);
      a0[i*4+3] = (byte)(l & 0xFF);
    }
  }

  static void test_pack8(long[] p8, byte[] a1) {
    if (p8.length*8 > a1.length) return;
    for (int i = 0; i < p8.length; i+=1) {
      long l0 = (long)a1[i*8+0];
      long l1 = (long)a1[i*8+1];
      long l2 = (long)a1[i*8+2];
      long l3 = (long)a1[i*8+3];
      long l4 = (long)a1[i*8+4];
      long l5 = (long)a1[i*8+5];
      long l6 = (long)a1[i*8+6];
      long l7 = (long)a1[i*8+7];
      p8[i] = (l0 & 0xFFl) |
             ((l1 & 0xFFl) <<  8) |
             ((l2 & 0xFFl) << 16) |
             ((l3 & 0xFFl) << 24) |
             ((l4 & 0xFFl) << 32) |
             ((l5 & 0xFFl) << 40) |
             ((l6 & 0xFFl) << 48) |
             ((l7 & 0xFFl) << 56);
    }
  }
  static void test_unpack8(byte[] a0, long[] p8) {
    if (p8.length*8 > a0.length) return;
    for (int i = 0; i < p8.length; i+=1) {
      long l = p8[i];
      a0[i*8+0] = (byte)(l & 0xFFl);
      a0[i*8+1] = (byte)(l >>  8);
      a0[i*8+2] = (byte)(l >> 16);
      a0[i*8+3] = (byte)(l >> 24);
      a0[i*8+4] = (byte)(l >> 32);
      a0[i*8+5] = (byte)(l >> 40);
      a0[i*8+6] = (byte)(l >> 48);
      a0[i*8+7] = (byte)(l >> 56);
    }
  }
  static void test_pack8_swap(long[] p8, byte[] a1) {
    if (p8.length*8 > a1.length) return;
    for (int i = 0; i < p8.length; i+=1) {
      long l0 = (long)a1[i*8+0];
      long l1 = (long)a1[i*8+1];
      long l2 = (long)a1[i*8+2];
      long l3 = (long)a1[i*8+3];
      long l4 = (long)a1[i*8+4];
      long l5 = (long)a1[i*8+5];
      long l6 = (long)a1[i*8+6];
      long l7 = (long)a1[i*8+7];
      p8[i] = (l7 & 0xFFl) |
             ((l6 & 0xFFl) <<  8) |
             ((l5 & 0xFFl) << 16) |
             ((l4 & 0xFFl) << 24) |
             ((l3 & 0xFFl) << 32) |
             ((l2 & 0xFFl) << 40) |
             ((l1 & 0xFFl) << 48) |
             ((l0 & 0xFFl) << 56);
    }
  }
  static void test_unpack8_swap(byte[] a0, long[] p8) {
    if (p8.length*8 > a0.length) return;
    for (int i = 0; i < p8.length; i+=1) {
      long l = p8[i];
      a0[i*8+0] = (byte)(l >> 56);
      a0[i*8+1] = (byte)(l >> 48);
      a0[i*8+2] = (byte)(l >> 40);
      a0[i*8+3] = (byte)(l >> 32);
      a0[i*8+4] = (byte)(l >> 24);
      a0[i*8+5] = (byte)(l >> 16);
      a0[i*8+6] = (byte)(l >>  8);
      a0[i*8+7] = (byte)(l & 0xFFl);
    }
  }

  static int verify(String text, int i, byte elem, byte val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }

  static int verify(String text, int i, short elem, short val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }

  static int verify(String text, int i, int elem, int val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + Integer.toHexString(elem) + " != " + Integer.toHexString(val));
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
