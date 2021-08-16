/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
// DivTest.java
// bug-12


/*
 * @test
 *
 * @summary converted from VM Testbase jit/DivTest.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.DivTest.DivTest
 */

package jit.DivTest;

import nsk.share.TestFailure;

public class DivTest{
  static int n;
  static boolean test1 (int n1, int n2) {
    try {
      n = n1 / n2;
      System.out.println(n);
      return true;
    } catch (Exception e) {
      System.out.println(e);
      return false;
    }
  }
  public static void main(String s[]) {
    boolean failed;
    failed = test1 (-1, 0);
    failed |= !test1 (-1, 0x80000000);
    failed |= !test1 (0, -1);
    failed |= !test1 (0x80000000, -1);
    if (failed)
        throw new TestFailure("Test failed");
  }
}
