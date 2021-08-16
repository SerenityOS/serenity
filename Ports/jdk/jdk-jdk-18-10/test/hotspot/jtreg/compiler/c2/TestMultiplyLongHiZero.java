/*
 * Copyright 2010 Google, Inc.  All Rights Reserved.
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
 *
 */

/*
 * @test
 * @bug 6921969
 * @summary Tests shorter long multiply sequences when the high 32 bits of long operands are known to be zero on x86_32
 * @run main/othervm -Xbatch -XX:-Inline
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testNormal
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testLeftOptimized
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testRightOptimized
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testOptimized
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testLeftOptimized_LoadUI2L
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testRightOptimized_LoadUI2L
 *    -XX:CompileCommand=compileonly,compiler.c2.TestMultiplyLongHiZero::testOptimized_LoadUI2L
 *    compiler.c2.TestMultiplyLongHiZero
 */

package compiler.c2;

// This test must run without any command line arguments.
public class TestMultiplyLongHiZero {

  private static void check(long leftFactor, long rightFactor, long optimizedProduct, long constantProduct) {
    long normalProduct = leftFactor * rightFactor; // unaffected by the new optimization
    if (optimizedProduct != constantProduct || normalProduct != constantProduct) {
      throw new RuntimeException("Not all three products are equal: " +
                                 Long.toHexString(normalProduct) + ", " +
                                 Long.toHexString(optimizedProduct) + ", " +
                                 Long.toHexString(constantProduct));
    }
  }

  private static int initInt(String[] args, int v) {
    if (args.length > 0) {
      try {
        return Integer.valueOf(args[0]);
      } catch (NumberFormatException e) { }
    }
    return v;
  }

  private static final long mask32 = 0x00000000FFFFFFFFL;

  private static void testNormal(int leftFactor, int rightFactor, long constantProduct) {
    check((long) leftFactor,
          (long) rightFactor,
          (long) leftFactor * (long) rightFactor, // unaffected by the new optimization
          constantProduct);
  }

  private static void testLeftOptimized(int leftFactor, int rightFactor, long constantProduct) {
    check((leftFactor & mask32),
          (long) rightFactor,
          (leftFactor & mask32) * (long) rightFactor, // left factor optimized
          constantProduct);
  }

  private static void testRightOptimized(int leftFactor, int rightFactor, long constantProduct) {
    check((long) leftFactor,
          (rightFactor & mask32),
          (long) leftFactor * (rightFactor & mask32), // right factor optimized
          constantProduct);
  }

  private static void testOptimized(int leftFactor, int rightFactor, long constantProduct) {
    check((leftFactor & mask32),
          (rightFactor & mask32),
          (leftFactor & mask32) * (rightFactor & mask32), // both factors optimized
          constantProduct);
  }

  private static void testLeftOptimized_LoadUI2L(int leftFactor, int rightFactor, long constantProduct, int[] factors) {
    check((leftFactor & mask32),
          (long) rightFactor,
          (factors[0] & mask32) * (long) rightFactor, // left factor optimized
          constantProduct);
  }

  private static void testRightOptimized_LoadUI2L(int leftFactor, int rightFactor, long constantProduct, int[] factors) {
    check((long) leftFactor,
          (rightFactor & mask32),
          (long) leftFactor * (factors[1] & mask32), // right factor optimized
          constantProduct);
  }

  private static void testOptimized_LoadUI2L(int leftFactor, int rightFactor, long constantProduct, int[] factors) {
    check((leftFactor & mask32),
          (rightFactor & mask32),
          (factors[0] & mask32) * (factors[1] & mask32), // both factors optimized
          constantProduct);
  }

  private static void test(int leftFactor, int rightFactor,
                           long normalConstantProduct,
                           long leftOptimizedConstantProduct,
                           long rightOptimizedConstantProduct,
                           long optimizedConstantProduct) {
    int[] factors = new int[2];
    factors[0] = leftFactor;
    factors[1] = rightFactor;
    testNormal(leftFactor, rightFactor, normalConstantProduct);
    testLeftOptimized(leftFactor, rightFactor, leftOptimizedConstantProduct);
    testRightOptimized(leftFactor, rightFactor, rightOptimizedConstantProduct);
    testOptimized(leftFactor, rightFactor, optimizedConstantProduct);
    testLeftOptimized_LoadUI2L(leftFactor, rightFactor, leftOptimizedConstantProduct, factors);
    testRightOptimized_LoadUI2L(leftFactor, rightFactor, rightOptimizedConstantProduct, factors);
    testOptimized_LoadUI2L(leftFactor, rightFactor, optimizedConstantProduct, factors);
  }

  public static void main(String[] args) {
    for (int i = 0; i < 100000; ++i) { // Trigger compilation
      int i0 = initInt(args, 1);
      int i1 = initInt(args, 3);
      int i2 = initInt(args, -1);
      int i3 = initInt(args, 0x7FFFFFFF);
      test(i0, i1, 3L, 3L, 3L, 3L);
      test(i0, i2, -1L, -1L, 0xFFFFFFFFL, 0xFFFFFFFFL);
      test(i0, i3, 0x7FFFFFFFL, 0x7FFFFFFFL, 0x7FFFFFFFL, 0x7FFFFFFFL);
      test(i1, i2, -3L, -3L, 0x2FFFFFFFDL, 0x2FFFFFFFDL);
      test(i1, i3, 0x17FFFFFFDL, 0x17FFFFFFDL, 0x17FFFFFFDL, 0x17FFFFFFDL);
      test(i2, i3, 0xFFFFFFFF80000001L, 0x7FFFFFFE80000001L,
           0xFFFFFFFF80000001L, 0x7FFFFFFE80000001L);
    }
  }
}
