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
 * @summary converted from VM Testbase runtime/jbe/dead/dead04.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead04.dead04
 */

package vm.compiler.jbe.dead.dead04;
/* -- Test the elimination of dead assignment to class fields
In the example below, the values assigned to i1-i7 in struct are never used,thus can be eliminated.

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

public class dead04 {

  public static void main(String args[]) {
    dead04 dce = new dead04();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead04 Passed.");
    } else {
      throw new Error("Test dead04 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }

  int f() {
    struct s = new struct();

    s.i1 = 1;
    s.i2 = 2;
    s.i3 = 3;
    s.i4 = 4;
    s.i5 = 5;
    s.i6 = 6;
    s.i7 = 7;
    s.i8 = 8;

    s.i1 = 1;
    s.i2 = 2;
    s.i3 = 3;
    s.i4 = 4;
    s.i5 = 5;
    s.i6 = 6;
    s.i7 = 7;
    s.i8 = 8;

    s.i1 = 1;
    s.i2 = 2;
    s.i3 = 3;
    s.i4 = 4;
    s.i5 = 5;
    s.i6 = 6;
    s.i7 = 7;
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
