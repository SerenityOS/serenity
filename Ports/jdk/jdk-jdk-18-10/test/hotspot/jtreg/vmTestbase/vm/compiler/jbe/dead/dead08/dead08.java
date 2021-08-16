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
 * @summary converted from VM Testbase runtime/jbe/dead/dead08.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead08.dead08
 */

package vm.compiler.jbe.dead.dead08;

/* -- Test the elimination of dead assignment to class fields within an IF statement

In the example below, the values assigned to i1-i7 in struct within th IF statement are never used,thus can be eliminated.

 */

class struct {
    int i1 = 2;
    int i2 = 3;
    int i3 = 4;
    int i4 = 5;
    int i5 = 6;
    int i6 = 7;
    int i7 = 8;
    int i8 = 9;
}

public class dead08 {
  boolean bol = true;

  public static void main(String args[]) {
    dead08 dce = new dead08();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead08 Passed.");
    } else {
      throw new Error("Test dead08 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    struct s = new struct();

    if (bol)
      s.i1 = 1;
    if (bol)
      s.i2 = 2;
    if (bol)
      s.i3 = 3;
    if (bol)
      s.i4 = 4;
    if (bol)
      s.i5 = 5;
    if (bol)
      s.i6 = 6;
    if (bol)
      s.i7 = 7;
    if (bol)
      s.i8 = 8;

    if (bol)
      s.i1 = 1;
    if (bol)
      s.i2 = 2;
    if (bol)
      s.i3 = 3;
    if (bol)
      s.i4 = 4;
    if (bol)
      s.i5 = 5;
    if (bol)
      s.i6 = 6;
    if (bol)
      s.i7 = 7;
    if (bol)
      s.i8 = 8;

    if (bol)
      s.i1 = 1;
    if (bol)
      s.i2 = 2;
    if (bol)
      s.i3 = 3;
    if (bol)
      s.i4 = 4;
    if (bol)
      s.i5 = 5;
    if (bol)
      s.i6 = 6;
    if (bol)
      s.i7 = 7;
    if (bol)
      s.i8 = 8;

    return s.i8;
  }

  // Code fragment after dead code elimination
  int fopt() {
    struct s = new struct();

    s.i8 = 8;
    return s.i8;
  }
}
