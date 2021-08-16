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
 * @summary converted from VM Testbase runtime/jbe/dead/dead01.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead01.dead01
 */

package vm.compiler.jbe.dead.dead01;

/* -- Test the elimination of dead assignment to local variables
In the example below, the value assigned to i is never used, all dead stores to local can be eliminated, and the last return statement in f() is unreachable; Both dead/unused stores and unreachable statement can be eliminated.

 */

public class dead01 {

  public static void main(String args[]) {
    dead01 dce = new dead01();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead01 Passed.");
    } else {
      throw new Error("Test dead01 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    int local;
    int i;

    i = 1;          /* dead store */
    local = 8;      /* dead store */
    local = 7;      /* dead store */
    local = 6;      /* dead store */
    local = 5;      /* dead store */
    local = 4;      /* dead store */
    local = 3;      /* dead store */
    local = 2;      /* dead store */
    local = 1;      /* dead store */
    local = 0;      /* dead store */
    local = -1;     /* dead store */
    local = -2;     /* dead store */

    i = 1;           /* dead store */
    local = 8;      /* dead store */
    local = 7;      /* dead store */
    local = 6;      /* dead store */
    local = 5;      /* dead store */
    local = 4;      /* dead store */
    local = 3;      /* dead store */
    local = 2;      /* dead store */
    local = 1;      /* dead store */
    local = 0;      /* dead store */
    local = -1;     /* dead store */
    local = -2;     /* dead store */

    i = 1;           /* dead store */
    local = 8;      /* dead store */
    local = 7;      /* dead store */
    local = 6;      /* dead store */
    local = 5;      /* dead store */
    local = 4;      /* dead store */
    local = 3;      /* dead store */
    local = 2;      /* dead store */
    local = 1;      /* dead store */
    local = 0;      /* dead store */
    local = -1;     /* dead store */
    local = -2;

    if (Math.abs(local) >= 0)
      return local;
    return local;   /* unreachable */
  }

  // Code fragment after dead code elimination
  static int fopt() {
    int local;

    local = -2;
    return local;
  }
}
