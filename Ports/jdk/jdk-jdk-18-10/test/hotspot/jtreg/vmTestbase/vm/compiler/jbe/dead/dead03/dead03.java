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
 * @summary converted from VM Testbase runtime/jbe/dead/dead03.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead03.dead03
 */

package vm.compiler.jbe.dead.dead03;

/* -- Test the elimination of dead assignment to global variables
In the example below, the value assigned to i is never used, all dead stores to global can be eliminated, and the last return statement in f() is unreachable; Both dead/unused stores and unreachable statement can be eliminated.

 */

public class dead03 {
  int global;
  int i;

  public static void main(String args[]) {
    dead03 dce = new dead03();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead03 Passed.");
    } else {
      throw new Error("Test dead03 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {

    i = 1;           /* dead store */
    global = 8;      /* dead store */
    global = 7;      /* dead store */
    global = 6;      /* dead store */
    global = 5;      /* dead store */
    global = 4;      /* dead store */
    global = 3;      /* dead store */
    global = 2;      /* dead store */
    global = 1;      /* dead store */
    global = 0;      /* dead store */
    global = -1;     /* dead store */
    global = -2;     /* dead store */

    i = 1;           /* dead store */
    global = 8;      /* dead store */
    global = 7;      /* dead store */
    global = 6;      /* dead store */
    global = 5;      /* dead store */
    global = 4;      /* dead store */
    global = 3;      /* dead store */
    global = 2;      /* dead store */
    global = 1;      /* dead store */
    global = 0;      /* dead store */
    global = -1;     /* dead store */
    global = -2;     /* dead store */

    i = 1;           /* dead store */
    global = 8;      /* dead store */
    global = 7;      /* dead store */
    global = 6;      /* dead store */
    global = 5;      /* dead store */
    global = 4;      /* dead store */
    global = 3;      /* dead store */
    global = 2;      /* dead store */
    global = 1;      /* dead store */
    global = 0;      /* dead store */
    global = -1;     /* dead store */
    global = -2;

    if (Math.abs(global) >= 0)
      return global;
    return global;   /* unreachable */
  }

  // Code fragment after dead code elimination
  int fopt() {

    global = -2;
    return global;
  }
}
