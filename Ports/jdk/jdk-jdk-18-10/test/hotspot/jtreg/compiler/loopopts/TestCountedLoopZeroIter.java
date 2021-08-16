/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8261308
 * @summary C2: assert(inner->is_valid_counted_loop(T_INT) && inner->is_strip_mined()) failed: OuterStripMinedLoop should have been removed
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestCountedLoopZeroIter TestCountedLoopZeroIter
 *
 */


public class TestCountedLoopZeroIter {
  static int N = 400;
  static boolean b;
  static long lArrFld[] = new long[N];
  static int iArrFld[] = new int[N];

  static void test() {
    int i19 = 9, i21 = 8;
    long l1;
    do
      if (!b) {
        iArrFld[1] /= l1 = 1;
        do {
          int i22 = 1;
          do {
            iArrFld[0] = (int)l1;
            iArrFld[i22 - 1] = i21;
          } while (++i22 < 1);
          lArrFld[1] = i21;
        } while (++l1 < 145);
      }
    while (++i19 < 173);
    System.out.println();
  }

  public static void main(String[] strArr) {
      for (int i = 0; i < 10; i++) {
        test();
     }
  }
}
