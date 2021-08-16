/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7183458
 * @summary Verify advance of space is not overly widened by bold styling.
 * @run main StyledSpaceAdvance
 */
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

public class StyledSpaceAdvance {

    static String name = "Gulim";

    public static void main(String args[]) {
         for (int sz=9;sz<18;sz++) {
             test(sz);
         }
    }

    static void test(int sz) {
         Font reg = new Font(name, Font.PLAIN, sz);
         Font bold = new Font(name, Font.BOLD, sz);
         //System.out.println("reg="+reg);
         //System.out.println("bold="+bold);
         FontRenderContext frc = new FontRenderContext(null, false, false);
         if (reg.getFontName(Locale.ENGLISH).equals(name) &&
             bold.getFontName(Locale.ENGLISH).equals(name)) {
             Rectangle2D rb = reg.getStringBounds(" ", frc);
             Rectangle2D bb = bold.getStringBounds(" ", frc);
             if (bb.getWidth() > rb.getWidth() + 1.01f) {
                 System.err.println("reg="+reg+" bds = " + rb);
                 System.err.println("bold="+bold+" bds = " + bb);
                 throw new RuntimeException("Advance difference too great.");
             }
         } else {
             System.out.println("Skipping test because fonts aren't as expected");
         }
    }
}
