/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024924
 * @summary Test non constant addExact
 *
 * @run main compiler.intrinsics.mathexact.AddExactICondTest
 */

package compiler.intrinsics.mathexact;

public class AddExactICondTest {
  public static int result = 0;

  public static void main(String[] args) {
    for (int i = 0; i < 50000; ++i) {
      runTest();
    }
  }

  public static void runTest() {
    int i = 7;
    while (java.lang.Math.addExact(i, result) < 89361) {
        if ((java.lang.Math.addExact(i, i) & 1) == 1) {
            i += 3;
        } else if ((i & 5) == 4) {
            i += 7;
        } else if ((i & 0xf) == 6) {
            i += 2;
        } else {
            i += 1;
        }
        result += 2;
    }
  }
}
