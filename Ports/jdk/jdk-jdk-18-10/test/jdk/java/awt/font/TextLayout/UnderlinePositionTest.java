/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary verify outline and stroking of underline match.
 * @bug 6751616
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

public class UnderlinePositionTest {

    public static void main(String[] args) {
        BufferedImage bi =
           new BufferedImage(600, 150, BufferedImage.TYPE_INT_RGB);
       Graphics2D g2d = bi.createGraphics();
       g2d.setColor(Color.white);
       g2d.fillRect(0, 0, 600, 150);

       float x = 10;
       float y = 90;
       Map map = new HashMap();
       map.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
       map.put(TextAttribute.SIZE, new Float(80));

       FontRenderContext frc = g2d.getFontRenderContext();

       // Use all spaces for the text so we know we are dealing
       // only with pixels from the underline.
       String text = "           ";
       TextLayout tl = new TextLayout(text, map, frc);
       Shape outline = tl.getOutline(null);
       Rectangle2D bounds = outline.getBounds();

       g2d.translate(x, y);
       g2d.setColor(Color.RED);
       tl.draw(g2d, 0, 0);

       /* By getting the outline, then its bounds, then filling
        * according to the same pixelisation rules, this ought to
        * match the position of the original underline. If any
        * red pixels are left, then the test will fail.
        */
       g2d.setColor(Color.BLUE);
       g2d.fill(bounds);
       g2d.dispose();

       checkBI(bi, Color.RED);
   }

   static void checkBI(BufferedImage bi, Color badColor) {
      int badrgb = badColor.getRGB();
      int w = bi.getWidth(null);
      int h = bi.getHeight(null);
      for (int x=0; x<w; x++) {
          for (int y=0; y<h; y++) {
             int col = bi.getRGB(x, y);
             if (col == badrgb) {
                  throw new RuntimeException("Got " + col);
             }
          }
      }
   }
}
