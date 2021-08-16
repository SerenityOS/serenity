/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224957
 * @summary Test splitting a boxed field load through a loop Phi.
 * @run main/othervm -XX:-TieredCompilation -XX:-ProfileInterpreter
 *                   -XX:CompileOnly=compiler.eliminateAutobox.TestSplitThroughPhi::test
 *                   compiler.eliminateAutobox.TestSplitThroughPhi
 */

package compiler.eliminateAutobox;

public class TestSplitThroughPhi {

  static volatile boolean loop = true;

  public static void test(Object obj) {
      for (int i = 0; i == 0; ) {
          // OSR compilation generates Region with 3 input paths
          while (true) {
              // Load from boxed int
              int val = (Integer)obj;
              if (val == i++) break;
              if (!loop) break;
          }
      }
  }

  public static void main(String[] args) {
      test(100_000);
  }
}
