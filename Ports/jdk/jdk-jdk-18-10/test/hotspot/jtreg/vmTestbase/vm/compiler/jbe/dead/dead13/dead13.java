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
 * @summary converted from VM Testbase runtime/jbe/dead/dead13.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead13.dead13
 */

package vm.compiler.jbe.dead.dead13;

// dead13.java

/* -- Test the elimination of dead assignments to a chain of array references.
      Example:

      void foo()
      {
         int arr[] = new int[SIZE];
         arr[0] = 0;
         arr[1] = arr[0];
         arr[2] = arr[1];
      }

The assignmnet to arr[2] is dead.  Therefore, arr[1] is not referenced, and the assignment to arr[1] is dead, as is the assignment to arr[0].
 */

public class dead13 {
  int SIZE = 30;

  public static void main(String args[]) {
    dead13 dce = new dead13();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead13 Passed.");
    } else {
      throw new Error("Test dead13 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {

    int a[] = new int[SIZE];

    a[0] = 0;
    a[1] = a[0] + 1;
    a[2] = a[1] + 2;
    a[3] = a[2] + 3;
    a[4] = a[3] + 4;
    a[5] = a[4] + 5;
    a[6] = a[5] + 6;
    a[7] = a[6] + 7;
    a[8] = a[7] + 8;
    a[9] = a[8] + 9;
    a[10] = a[9] + 10;
    a[11] = a[10] + 11;
    a[12] = a[11] + 12;
    a[13] = a[12] + 13;
    a[14] = a[13] + 14;
    a[15] = a[14] + 15;
    a[16] = a[15] + 16;
    a[17] = a[16] + 17;
    a[18] = a[17] + 18;
    a[19] = a[18] + 19;
    a[20] = a[19] + 20;
    a[21] = a[20] + 21;
    a[22] = a[21] + 22;
    a[23] = a[22] + 23;
    a[24] = a[23] + 24;
    a[25] = a[24] + 25;
    a[26] = a[25] + 26;
    a[27] = a[26] + 27;
    a[28] = a[27] + 28;
    a[29] = a[28] + 29;

    return a[1];
  }
  // Code fragment after dead code elimination
  int fopt() {
    int a[] = new int[SIZE];

    a[0] = 0;
    a[1] = a[0] + 1;

    return a[1];
  }
}
