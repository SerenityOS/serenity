/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8265325 8265940
 * @summary test the optimization of pow(x, 0.5)
 * @run main/othervm TestPow0Dot5Opt
 * @run main/othervm -Xint TestPow0Dot5Opt
 * @run main/othervm -Xbatch -XX:TieredStopAtLevel=1 TestPow0Dot5Opt
 * @run main/othervm -Xbatch -XX:-TieredCompilation  TestPow0Dot5Opt
 */

public class TestPow0Dot5Opt {

  static void test(double a) throws Exception {
    // pow(x, 0.5) isn't replaced with sqrt(x) for x < 0.0
    if (a < 0.0) return;

    double r1 = Math.sqrt(a);
    double r2 = Math.pow(a, 0.5);
    if (r1 != r2) {
      throw new RuntimeException("pow(" + a + ", 0.5), expected: " + r1 + ", actual: " + r2);
    }

    // Special case: pow(+0.0, 0.5) = 0.0
    double r = Math.pow(+0.0, 0.5);
    if (Double.doubleToRawLongBits(r) != Double.doubleToRawLongBits(0.0)) {
      throw new RuntimeException("pow(+0.0, 0.5), expected: 0.0, actual: " + r);
    }

    // Special case: pow(-0.0, 0.5) = 0.0
    r = Math.pow(-0.0, 0.5);
    if (Double.doubleToRawLongBits(r) != Double.doubleToRawLongBits(0.0)) {
      throw new RuntimeException("pow(-0.0, 0.5), expected: 0.0, actual: " + r);
    }

    // Special case: pow(Double.POSITIVE_INFINITY, 0.5) = Infinity
    r = Math.pow(Double.POSITIVE_INFINITY, 0.5);
    if (!(r > 0 && Double.isInfinite(r))) {
      throw new RuntimeException("pow(+Infinity, 0.5), expected: Infinity, actual: " + r);
    }

    // Special case: pow(Double.NEGATIVE_INFINITY, 0.5) = Infinity
    r = Math.pow(Double.NEGATIVE_INFINITY, 0.5);
    if (!(r > 0 && Double.isInfinite(r))) {
      throw new RuntimeException("pow(-Infinity, 0.5), expected: Infinity, actual: " + r);
    }

    // Special case: pow(Double.NaN, 0.5) = NaN
    r = Math.pow(Double.NaN, 0.5);
    if (!Double.isNaN(r)) {
      throw new RuntimeException("pow(NaN, 0.5), expected: NaN, actual: " + r);
    }
  }

  public static void main(String[] args) throws Exception {
    for (int i = 0; i < 10; i++) {
      for (int j = 1; j < 100000; j++) {
        test(j * 1.0);
        test(1.0 / j);
      }
    }
  }

}
