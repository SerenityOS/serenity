/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8256372
 */

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

public class NLGlyphTest {

   public static void main(String[] args) {
      char[] chs = { '\n' };
      FontRenderContext frc = new FontRenderContext(null, true, true);
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      Font[] fonts = ge.getAllFonts();
      for (Font font : fonts) {
          GlyphVector cgv = font.createGlyphVector(frc, "\n");
          GlyphVector lgv = font.layoutGlyphVector(frc, chs, 0, 1, 0);
          int c_code = cgv.getGlyphCode(0);
          int l_code = lgv.getGlyphCode(0);
          if ((c_code != l_code) || (l_code == 0)) {
              System.out.println(font);
              System.out.println("create code="+c_code + " layout code="+l_code);
              Rectangle r_l = lgv.getPixelBounds(frc, 0f, 0f);
              Rectangle r_c = cgv.getPixelBounds(frc, 0f, 0f);
              System.out.println(r_l.isEmpty()+" "+ r_c.isEmpty());
              if (r_l.isEmpty() != r_c.isEmpty()) {
                 throw new RuntimeException("One glyph renders");
              }
          }
      }
   }
}
