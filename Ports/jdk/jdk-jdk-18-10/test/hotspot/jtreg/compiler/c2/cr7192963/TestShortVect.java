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
 * @bug 7192963
 * @summary assert(_in[req-1] == this) failed: Must pass arg count to 'new'
 *
 * @run main/othervm/timeout=400 -Xbatch -Xmx128m compiler.c2.cr7192963.TestShortVect
 */

package compiler.c2.cr7192963;

public class TestShortVect {
  private static final int ARRLEN = 997;
  private static final int ITERS  = 11000;
  public static void main(String args[]) {
    System.out.println("Testing Short vectors");
    int errn = test();
    if (errn > 0) {
      System.err.println("FAILED: " + errn + " errors");
      System.exit(97);
    }
    System.out.println("PASSED");
  }

  static int test() {
    short[] a0 = new short[ARRLEN];
    short[] a1 = new short[ARRLEN];
    // Initialize
    for (int i=0; i<ARRLEN; i++) {
      a1[i] = (short)i;
    }
    System.out.println("Warmup");
    for (int i=0; i<ITERS; i++) {
      test_init(a0);
      test_addi(a0, a1);
      test_lsai(a0, a1);
      test_unrl_init(a0);
      test_unrl_addi(a0, a1);
      test_unrl_lsai(a0, a1);
    }
    // Test and verify results
    System.out.println("Verification");
    int errn = 0;
    {
      test_init(a0);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_init: ", i, a0[i], (short)(i&3));
      }
      test_addi(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_addi: ", i, a0[i], (short)(i+(i&3)));
      }
      test_lsai(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_lsai: ", i, a0[i], (short)(i<<(i&3)));
      }
      test_unrl_init(a0);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_unrl_init: ", i, a0[i], (short)(i&3));
      }
      test_unrl_addi(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_unrl_addi: ", i, a0[i], (short)(i+(i&3)));
      }
      test_unrl_lsai(a0, a1);
      for (int i=0; i<ARRLEN; i++) {
        errn += verify("test_unrl_lsai: ", i, a0[i], (short)(i<<(i&3)));
      }
    }

    if (errn > 0)
      return errn;

    System.out.println("Time");
    long start, end;

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_init(a0);
    }
    end = System.currentTimeMillis();
    System.out.println("test_init: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_addi(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_addi: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_lsai(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_lsai: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unrl_init(a0);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unrl_init: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unrl_addi(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unrl_addi: " + (end - start));

    start = System.currentTimeMillis();
    for (int i=0; i<ITERS; i++) {
      test_unrl_lsai(a0, a1);
    }
    end = System.currentTimeMillis();
    System.out.println("test_unrl_lsai: " + (end - start));

    return errn;
  }

  static void test_init(short[] a0) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (short)(i&3);
    }
  }
  static void test_addi(short[] a0, short[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (short)(a1[i]+(i&3));
    }
  }
  static void test_lsai(short[] a0, short[] a1) {
    for (int i = 0; i < a0.length; i+=1) {
      a0[i] = (short)(a1[i]<<(i&3));
    }
  }
  static void test_unrl_init(short[] a0) {
    int i = 0;
    for (; i < a0.length-4; i+=4) {
      a0[i+0] = 0;
      a0[i+1] = 1;
      a0[i+2] = 2;
      a0[i+3] = 3;
    }
    for (; i < a0.length; i++) {
      a0[i] = (short)(i&3);
    }
  }
  static void test_unrl_addi(short[] a0, short[] a1) {
    int i = 0;
    for (; i < a0.length-4; i+=4) {
      a0[i+0] = (short)(a1[i+0]+0);
      a0[i+1] = (short)(a1[i+1]+1);
      a0[i+2] = (short)(a1[i+2]+2);
      a0[i+3] = (short)(a1[i+3]+3);
    }
    for (; i < a0.length; i++) {
      a0[i] = (short)(a1[i]+(i&3));
    }
  }
  static void test_unrl_lsai(short[] a0, short[] a1) {
    int i = 0;
    for (; i < a0.length-4; i+=4) {
      a0[i+0] = (short)(a1[i+0]<<0);
      a0[i+1] = (short)(a1[i+1]<<1);
      a0[i+2] = (short)(a1[i+2]<<2);
      a0[i+3] = (short)(a1[i+3]<<3);
    }
    for (; i < a0.length; i++) {
      a0[i] = (short)(a1[i]<<(i&3));
    }
  }

  static int verify(String text, int i, short elem, short val) {
    if (elem != val) {
      System.err.println(text + "[" + i + "] = " + elem + " != " + val);
      return 1;
    }
    return 0;
  }
}
