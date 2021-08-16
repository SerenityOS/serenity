/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4185854
  @summary Checks that constructors do not accept nulls and throw NPE
  @run main ConstructorsNullTest
*/

import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorConvertOp;

public class ConstructorsNullTest {

    public static void main(final String[] args) {
      ColorConvertOp gp;
      boolean passed = false;
      try {
          gp = new ColorConvertOp((ColorSpace)null, (RenderingHints)null);
      } catch (NullPointerException e) {
          try {
              gp = new ColorConvertOp((ColorSpace)null, null, null);
          } catch (NullPointerException e1) {
              try {
                  gp = new ColorConvertOp((ICC_Profile[])null, null);
              } catch (NullPointerException e2) {
                  passed = true;
              }
          }
      }

      if (!passed) {
          System.out.println("Test FAILED: one of constructors didn't throw NullPointerException.");
          throw new RuntimeException("Test FAILED: one of constructors didn't throw NullPointerException.");
      }
      System.out.println("Test PASSED: all constructors threw NullPointerException.");
    }
}// class ConstructorsNullTest
