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
 * @summary converted from VM Testbase runtime/jbe/dead/dead06.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead06.dead06
 */

package vm.compiler.jbe.dead.dead06;

/* -- Test the elimination of dead assignment to local variable within an IF statement
      Example:

      boolean bol;
      void foo()
      {
         int i;

         if (bol) i = 1;
         if (bol) i = 2;
      }

The first assignment to i is dead, and can be eliminated.

 */

public class dead06 {
  boolean bol = true;

  public static void main(String args[]) {
    dead06 dce = new dead06();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead06 Passed.");
    } else {
      throw new Error("Test dead06 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    int i = 0;

    if (bol)
      i = 1;
    if (bol)
      i = 2;
    if (bol)
      i = 3;
    if (bol)
      i = 4;
    if (bol)
      i = 5;
    if (bol)
      i = 6;
    if (bol)
      i = 7;
    if (bol)
      i = 8;

    if (bol)
      i = 1;
    if (bol)
      i = 2;
    if (bol)
      i = 3;
    if (bol)
      i = 4;
    if (bol)
      i = 5;
    if (bol)
      i = 6;
    if (bol)
      i = 7;
    if (bol)
      i = 8;

    if (bol)
      i = 1;
    if (bol)
      i = 2;
    if (bol)
      i = 3;
    if (bol)
      i = 4;
    if (bol)
      i = 5;
    if (bol)
      i = 6;
    if (bol)
      i = 7;
    if (bol)
      i = 8;

    return i;
  }

  // Code fragment after dead code elimination
  int fopt() {
    int i = 0;

    i = 8;
    return i;
  }
}
