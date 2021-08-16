/*
 * Copyright 2016 Google, Inc.  All Rights Reserved.
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
 * @bug 8166742
 * @summary C2 IV elimination throws FPE
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation TestImpossibleIV
 * @author Chuck Rasbold rasbold@google.com
 */

/*
 * Use -XX:-TieredCompilation to get C2 only.
 * Use -XX:-BackgroundCompilation to wait for compilation before test exit.
 */

public class TestImpossibleIV {

  static private void testMethod() {
    int sum = 0;
    // A unit count-down loop which has an induction variable with
    // MIN_VALUE stride.
    for (int i = 100000; i >= 0; i--) {
      sum += Integer.MIN_VALUE;
    }
  }

  public static void main(String[] args) {
    testMethod();
  }
}
