/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase runtime/jbe/dead/dead10.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead10.dead10
 */

package vm.compiler.jbe.dead.dead10;

// dead10.java

/* -- Test the elimination of dead integer and boolean expressions
      Example:

      int x, y, z;
      boolean a, b, c;
      void foo()
      {
         int r;
         r = x + y * z;
         a = x == y;
      }

In the example below, the values assigned to i1-i13 are dead in the first couple assignments and can be eliminated. In addition, boolean values assigned to i14-i18 are dead and never come to play, thus can be eliminated. This test tests the elimination of integer expresions with the operators addition, subtraction, negation, multiplication, division, modulus, logical negation, bitwise complement, bitwise and, bitwise or, bitwise xor, logical and logical or, question/colon, comma, right shift, left shift, less than, greater than, equal, and not equal.


 */

public class dead10 {
  int i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13;
  boolean i14, i15, i16, i17, i18;
  int j = 21;
  boolean bol = true;

  public static void main(String args[]) {
    dead10 dce = new dead10();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead10 Passed.");
    } else {
      throw new Error("Test dead10 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    int res;

    i1 = j + 1;
    i2 = j - 1;
    i3 = j * 3;
    i4 = j / 31;
    i5 = j % 71;
    i6 = j << 3;
    i7 = j >> 4;
    i8 = j >>> 5;
    i9 = bol ? 7 : 9;
    i10 = ~j;
    i11 = j & 3;
    i12 = j | 4;
    i13 = j ^ 4;
    i14 = bol && (j < 3);
    i15 = bol || (j > 4);
    i16 = !bol;
    i17 = j == 9;
    i18 = j != 10;

    res = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13;
    i14 = i14 && i15 && i16 && i17 && i18;

    i1 = j + 1;
    i2 = j - 1;
    i3 = j * 3;
    i4 = j / 31;
    i5 = j % 71;
    i6 = j << 3;
    i7 = j >> 4;
    i8 = j >>> 5;
    i9 = bol ? 7 : 9;
    i10 = ~j;
    i11 = j & 3;
    i12 = j | 4;
    i13 = j ^ 4;
    i14 = bol && (j < 3);
    i15 = bol || (j > 4);
    i16 = !bol;
    i17 = j == 9;
    i18 = j != 10;

    res = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13;
    i14 = i14 && i15 && i16 && i17 && i18;

    i1 = j + 1;
    i2 = j - 1;
    i3 = j * 3;
    i4 = j / 31;
    i5 = j % 71;
    i6 = j << 3;
    i7 = j >> 4;
    i8 = j >>> 5;
    i9 = bol ? 7 : 9;
    i10 = ~j;
    i11 = j & 3;
    i12 = j | 4;
    i13 = j ^ 4;
    i14 = bol && (j < 3);
    i15 = bol || (j > 4);
    i16 = !bol;
    i17 = j == 9;
    i18 = j != 10;

    res = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13;
    i14 = i14 && i15 && i16 && i17 && i18;


    return res;
  }

  // Code fragment after dead code elimination
  int fopt() {
    int res;

    i1 = j + 1;
    i2 = j - 1;
    i3 = j * 3;
    i4 = j / 31;
    i5 = j % 71;
    i6 = j << 3;
    i7 = j >> 4;
    i8 = j >>> 5;
    i9 = bol ? 7 : 9;
    i10 = ~j;
    i11 = j & 3;
    i12 = j | 4;
    i13 = j ^ 4;

    res = i1 + i2 + i3 + i4 + i5 + i6 + i7 + i8 + i9 + i10 + i11 + i12 + i13;
    return res;
  }
}
