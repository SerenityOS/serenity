/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6959129
 * @summary COMPARISON WITH INTEGER.MAX_INT DOES NOT WORK CORRECTLY IN THE CLIENT VM.
 *
 * @run main/othervm -ea compiler.c2.Test6959129
 */

package compiler.c2;

public class Test6959129 {

  public static void main(String[] args) {
    long start  = System.currentTimeMillis();
    int min = Integer.MAX_VALUE-30000;
    int max = Integer.MAX_VALUE;
    long maxmoves = 0;
    try {
      maxmoves = maxMoves(min, max);
    } catch (AssertionError e) {
      System.out.println("Passed");
      System.exit(95);
    }
    System.out.println("maxMove:" + maxmoves);
    System.out.println("FAILED");
    System.exit(97);
  }
  /**
   * Imperative implementation that returns the length hailstone moves
   * for a given number.
   */
  public static long hailstoneLengthImp(long n) {
    long moves = 0;
    while (n != 1) {
      assert n > 1;
      if (isEven(n)) {
        n = n / 2;
      } else {
        n = 3 * n + 1;
      }
      ++moves;
    }
    return moves;
  }

  private static boolean isEven(long n) {
    return n % 2 == 0;
  }

  /**
   * Returns the maximum length of the hailstone sequence for numbers
   * between min to max.
   *
   * For rec1 - Assume that min is bigger than max.
   */
  public static long maxMoves(int min, int max) {
    long maxmoves = 0;
    for (int n = min; n <= max; n++) {
      if ((n & 1023) == 0) System.out.println(n);
      long moves = hailstoneLengthImp(n);
      if (moves > maxmoves) {
        maxmoves = moves;
      }
    }
    return maxmoves;
  }
}

