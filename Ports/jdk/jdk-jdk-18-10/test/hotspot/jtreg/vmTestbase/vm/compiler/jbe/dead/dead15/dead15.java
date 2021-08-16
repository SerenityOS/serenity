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
 * @summary converted from VM Testbase runtime/jbe/dead/dead15.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead15.dead15
 */

package vm.compiler.jbe.dead.dead15;

// dead15.java

/* -- Test the elimination of a global variable assignment to itself.
      Example:

      int bar;
      void foo()
      {
         bar = bar;
      }

      The assignment to bar can be eliminated. Though assignments of this
      nature are unlikley to occur in an original code, it can actually
      happen as a result of other optimizations. For example, consider the
      following assignments after constant propagation.

      bat = 0;
      bar = bar + bat;

      After constant propagation, the second statement becomes bar = bar + 0,
      or bar = bar.  This test ensures that the final transformation, (i.e.
      the elimination of this assignment) will occur.
 */


public class dead15 {
  int i00=0, i01=1, i02=2, i03=3, i04=4;
  int i05=5, i06=6, i07=7, i08=8, i09=9;
  int i10=10, i11=11, i12=12, i13=13, i14=14;
  int i15=15, i16=16, i17=17, i18=18, i19=19;

  public static void main(String args[]) {
    dead15 dce = new dead15();

    System.out.println("f()="+dce.f()+"; fopt()="+dce.fopt());
    if (dce.f() == dce.fopt()) {
      System.out.println("Test dead15 Passed.");
    } else {
      throw new Error("Test dead15 Failed: f()=" + dce.f() + " != fopt()=" + dce.fopt());
    }
  }


  int f() {
    i00 = i00;
    i01 = i01;
    i02 = i02;
    i03 = i03;
    i04 = i04;
    i05 = i05;
    i06 = i06;
    i07 = i07;
    i08 = i08;
    i09 = i09;
    i10 = i10;
    i11 = i11;
    i12 = i12;
    i13 = i13;
    i14 = i14;
    i15 = i15;
    i16 = i16;
    i17 = i17;
    i18 = i18;
    i19 = i19;

    return i19;
  }

  // Code fragment after dead code elimination
  int fopt() {
    return i19;
  }
}
