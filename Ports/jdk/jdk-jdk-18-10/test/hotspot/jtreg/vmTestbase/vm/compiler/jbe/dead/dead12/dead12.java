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
 * @summary converted from VM Testbase runtime/jbe/dead/dead12.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead12.dead12
 */

package vm.compiler.jbe.dead.dead12;

// dead12.java

/* -- Test the elimination of dead assignments to a local array within IF statement
      Example:

      boolean bol = true;
      void foo()
      {
         int arr[] = new int[SIZE];
         if (bol) arr[1] = 3;
      }

In the example below, most of the assignments to the local array a[] are dead and can be eliminated.
 */

public class dead12 {
  int SIZE = 30;
  boolean bol = true;

  public static void main(String args[]) {
    dead12 dce = new dead12();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead12 Passed.");
    } else {
      throw new Error("Test dead12 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {

    int a[] = new int[SIZE];

    if (bol)
      a[0] = 0;
    if (bol)
      a[1] = 1;
    if (bol)
      a[2] = 2;
    if (bol)
      a[3] = 3;
    if (bol)
      a[4] = 4;
    if (bol)
      a[5] = 5;
    if (bol)
      a[6] = 6;
    if (bol)
      a[7] = 7;
    if (bol)
      a[8] = 8;
    if (bol)
      a[9] = 9;
    if (bol)
      a[10] = 10;
    if (bol)
      a[11] = 11;
    if (bol)
      a[12] = 12;
    if (bol)
      a[13] = 13;
    if (bol)
      a[14] = 14;
    if (bol)
      a[15] = 15;
    if (bol)
      a[16] = 16;
    if (bol)
      a[17] = 17;
    if (bol)
      a[18] = 18;
    if (bol)
      a[19] = 19;
    if (bol)
      a[20] = 20;
    if (bol)
      a[21] = 21;
    if (bol)
      a[22] = 22;
    if (bol)
      a[23] = 23;
    if (bol)
      a[24] = 24;
    if (bol)
      a[25] = 25;
    if (bol)
      a[26] = 26;
    if (bol)
      a[27] = 27;
     if (bol)
       a[28] = 28;
    if (bol)
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
