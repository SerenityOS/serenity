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
 * @summary converted from VM Testbase runtime/jbe/dead/dead11.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead11.dead11
 */

package vm.compiler.jbe.dead.dead11;

// dead11.java

/* -- Test the elimination of dead assignments to a local array
      Example:

      void foo()
      {
         int arr[] = new int[SIZE];
         arr[1] = 3;
      }

In the example below, most of the assignments to the local array a[] are dead and can be eliminated.
 */

public class dead11 {
  int SIZE = 30;

  public static void main(String args[]) {
    dead11 dce = new dead11();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead11 Passed.");
    } else {
      throw new Error("Test dead11 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    int a[] = new int[SIZE];

    a[0] = 0;
    a[1] = 1;
    a[2] = 2;
    a[3] = 3;
    a[4] = 4;
    a[5] = 5;
    a[6] = 6;
    a[7] = 7;
    a[8] = 8;
    a[9] = 9;
    a[10] = 10;
    a[11] = 11;
    a[12] = 12;
    a[13] = 13;
    a[14] = 14;
    a[15] = 15;
    a[16] = 16;
    a[17] = 17;
    a[18] = 18;
    a[19] = 19;
    a[20] = 20;
    a[21] = 21;
    a[22] = 22;
    a[23] = 23;
    a[24] = 24;
    a[25] = 25;
    a[26] = 26;
    a[27] = 27;
    a[28] = 28;
    a[29] = 29;

    return a[12];
  }

  // Code fragment after dead code elimination
  int fopt() {
    int a[] = new int[SIZE];

    a[12] = 12;
    return a[12] = 12;
  }
}
