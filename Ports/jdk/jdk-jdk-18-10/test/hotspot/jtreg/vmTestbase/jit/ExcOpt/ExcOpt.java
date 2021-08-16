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
// tests exception handler inside optimizable loops and around them


/*
 * @test
 *
 * @summary converted from VM Testbase jit/ExcOpt.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.ExcOpt.ExcOpt
 */

package jit.ExcOpt;

import nsk.share.TestFailure;

public class ExcOpt {
  static int x;

  public static void main (String s[]) {

    x = 0;

    try {
      for (int i = 1; i < 100; i++) {
        x += 1;
      }
    } catch (Exception e) {
      x = 0;
    }

    for (int i=1; i < 100; i++) {
      try {
        x += 1;
      } catch (Exception e) {
        x = 0;
      }
    }

    System.out.println("Done " + x);

    if (x != 198)
        throw new TestFailure("Test failed (x != 198)");
  }
}
