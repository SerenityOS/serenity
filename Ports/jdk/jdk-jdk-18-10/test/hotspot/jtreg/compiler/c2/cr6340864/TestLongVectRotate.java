/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8248830
 * @summary Implement Rotate vectorization optimizations in hotspot-server
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m compiler.c2.cr6340864.TestLongVectRotate
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=8 compiler.c2.cr6340864.TestLongVectRotate
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=16 compiler.c2.cr6340864.TestLongVectRotate
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:MaxVectorSize=32 compiler.c2.cr6340864.TestLongVectRotate
 * @run main/othervm -Xbatch -XX:CompileCommand=exclude,*::test() -Xmx128m -XX:+IgnoreUnrecognizedVMOptions -XX:UseAVX=3 compiler.c2.cr6340864.TestLongVectRotate
 */

package compiler.c2.cr6340864;

public class TestLongVectRotate {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  private static final long ADD_INIT = Long.MAX_VALUE-500;
  private static final int VALUE = 31;
  private static final int SHIFT = 64;
  private static final int SHIFT_LT_IMM8 = -128;
  private static final int SHIFT_GT_IMM8 = 128;

  public static void main(String args[]) {
    System.out.println("Testing Long Rotate vectors");
    test();
    int errn = verify();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    times();
    System.out.println("PASSED");
  }

  static long[] a0 = new long[ARRLEN];
  static long[] a1 = new long[ARRLEN];

  static void test() {
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      long val = (long)(ADD_INIT+i);
      a1[i] = val;
    }
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test1_ror(a0, a1);
      test1_rol(a0, a1);
      test2_ror(a0, a1);
      test2_rol(a0, a1);
      test3_ror(a0, a1, SHIFT);
      test3_rol(a0, a1, SHIFT);

      test_rolc(a0, a1);
      test_rolv(a0, a1, VALUE);
      test_rorc(a0, a1);
      test_rorv(a0, a1, VALUE);

      test_rolc_n(a0, a1);
      test_rolv(a0, a1, -VALUE);
      test_rorc_n(a0, a1);
      test_rorv(a0, a1, -VALUE);

      test_rolc_o(a0, a1);
      test_rolv(a0, a1, SHIFT);
      test_rorc_o(a0, a1);
      test_rorv(a0, a1, SHIFT);

      test_rolc_on(a0, a1);
      test_rolv(a0, a1, -SHIFT);
      test_rorc_on(a0, a1);
      test_rorv(a0, a1, -SHIFT);
    }
  }

  // Test and verify results
  static int verify() {
    System.out.println("Verification");
    int errn = 0;

    test1_ror(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test1_ror: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>SHIFT_GT_IMM8) | (long)(ADD_INIT+i)<<-SHIFT_GT_IMM8));
    }
    test1_rol(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test1_rol: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<SHIFT_GT_IMM8) | (long)(ADD_INIT+i)>>>-SHIFT_GT_IMM8));
    }
    test2_ror(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test2_ror: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>SHIFT_LT_IMM8) | (long)(ADD_INIT+i)<<-SHIFT_LT_IMM8));
    }
    test2_rol(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test2_rol: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<SHIFT_LT_IMM8) | (long)(ADD_INIT+i)>>>-SHIFT_LT_IMM8));
    }
    test3_rol(a0, a1, SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test3_rol: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<SHIFT) | (long)(ADD_INIT+i)>>>-SHIFT));
    }
    test3_ror(a0, a1, SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test3_ror: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>SHIFT) | (long)(ADD_INIT+i)<<-SHIFT));
    }

    test_rolc(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolc: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<VALUE) | (long)(ADD_INIT+i)>>>(-VALUE)));
    }
    test_rolv(a0, a1, VALUE);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolv: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<VALUE) | (long)(ADD_INIT+i)>>>(-VALUE)));
    }

    test_rorc(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorc: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>VALUE) | (long)(ADD_INIT+i)<<(-VALUE)));
    }
    test_rorv(a0, a1, VALUE);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorv: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>VALUE) | (long)(ADD_INIT+i)<<(-VALUE)));
    }

    test_rolc_n(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolc_n: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<(-VALUE)) | (long)(ADD_INIT+i)>>>VALUE));
    }
    test_rolv(a0, a1, -VALUE);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolv_n: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<(-VALUE)) | (long)(ADD_INIT+i)>>>VALUE));
    }

    test_rorc_n(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorc_n: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>(-VALUE)) | (long)(ADD_INIT+i)<<VALUE));
    }
    test_rorv(a0, a1, -VALUE);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorv_n: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>(-VALUE)) | (long)(ADD_INIT+i)<<VALUE));
    }

    test_rolc_o(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolc_o: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<SHIFT) | (long)(ADD_INIT+i)>>>(-SHIFT)));
    }
    test_rolv(a0, a1, SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolv_o: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<SHIFT) | (long)(ADD_INIT+i)>>>(-SHIFT)));
    }

    test_rorc_o(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorc_o: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>SHIFT) | (long)(ADD_INIT+i)<<(-SHIFT)));
    }
    test_rorv(a0, a1, SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorv_o: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>SHIFT) | (long)(ADD_INIT+i)<<(-SHIFT)));
    }

    test_rolc_on(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolc_on: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<(-SHIFT)) | (long)(ADD_INIT+i)>>>SHIFT));
    }
    test_rolv(a0, a1, -SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rolv_on: ", i, a0[i], (long)(((long)(ADD_INIT+i)<<(-SHIFT)) | (long)(ADD_INIT+i)>>>SHIFT));
    }

    test_rorc_on(a0, a1);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorc_on: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>(-SHIFT)) | (long)(ADD_INIT+i)<<SHIFT));
    }
    test_rorv(a0, a1, -SHIFT);
    for (int i=0; i<ARRLEN; i++) {
      errn += verify("test_rorc_on: ", i, a0[i], (long)(((long)(ADD_INIT+i)>>>(-SHIFT)) | (long)(ADD_INIT+i)<<SHIFT));
    }

    return errn;
  }

  static void times() {
    System.out.println("Time");
    long start, end;

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test1_rol(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test1_rol: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test1_ror(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test1_ror: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test2_rol(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test2_rol: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test2_ror(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test2_ror: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test3_rol(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test3_rol: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test3_ror(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test3_ror: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolc: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorc(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorc: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorv(a0, a1, VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorv: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolc_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolv_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorc_n(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorc_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorv(a0, a1, -VALUE);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorv_n: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolc_o(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolc_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolv(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolv_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorc_o(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorc_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorv(a0, a1, SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorv_o: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolc_on(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolc_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rolv(a0, a1, -SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rolv_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorc_on(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorc_on: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_rorv(a0, a1, -SHIFT);
    }
    end = System.currentTimeMillis();
    System.out.println("test_rorv_on: " + (end - start));
  }

  static void test_rolc(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], VALUE));
    }
  }

  static void test_rolc_n(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], (-VALUE)));
    }
  }

  static void test_rolc_o(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], SHIFT));
    }
  }

  static void test_rolc_on(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], (-SHIFT)));
    }
  }

  static void test_rolv(long[] a0, long[] a1, int shift) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], shift));
    }
  }

  static void test_rorc(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], VALUE));
    }
  }

  static void test_rorc_n(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], (-VALUE)));
    }
  }

  static void test_rorc_o(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], SHIFT));
    }
  }

  static void test_rorc_on(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], (-SHIFT)));
    }
  }

  static void test_rorv(long[] a0, long[] a1, int shift) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], shift));
    }
  }

  static void test1_rol(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], SHIFT_GT_IMM8));
    }
  }

  static void test1_ror(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], SHIFT_GT_IMM8));
    }
  }

  static void test2_rol(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], SHIFT_LT_IMM8));
    }
  }

  static void test2_ror(long[] a0, long[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], SHIFT_LT_IMM8));
    }
  }

  static void test3_rol(long[] a0, long[] a1, int shift) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateLeft(a1[i], shift));
    }
  }

  static void test3_ror(long[] a0, long[] a1, int shift) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (long)(Long.rotateRight(a1[i], shift));
    }
  }

  static int verify(String text, int i, long elem, long val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }

}
