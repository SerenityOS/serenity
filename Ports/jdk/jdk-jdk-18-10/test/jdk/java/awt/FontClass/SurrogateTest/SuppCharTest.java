/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8015556
 * @summary Surrogate pairs do not render properly on MacOS X.
 */

import java.util.Locale;
import java.awt.*;
import java.awt.font.*;
import javax.swing.*;

public class SuppCharTest {

   static String str = "ABC\uD840\uDC01\uD840\uDC00AB";
   static String EXTB_FONT = "MingLiU-ExtB";

   public static void main(String args[]) throws Exception {

      final Font font = new Font(EXTB_FONT, Font.PLAIN, 36);
      if (!EXTB_FONT.equalsIgnoreCase(font.getFamily(Locale.ENGLISH))) {
         return;
      }

      SwingUtilities.invokeLater(new Runnable(){
        @Override
        public void run(){
            JFrame f = new JFrame("Test Supplementary Char Support");
            Component c = new SuppCharComp(font, str);
            f.add("Center", c);
            JButton b = new JButton(str);
            b.setFont(font);
            f.add("South", b);
            f.pack();
            f.setVisible(true);
        }
      });

      /* If a supplementary character was found, 'invisible glyphs'
       * with value 65535 will be inserted in the place of the 2nd (low)
       * char index. So we are looking here to make sure such substitutions
       * took place.
       */
      FontRenderContext frc = new FontRenderContext(null, false, false);
      GlyphVector gv = font.createGlyphVector(frc, str);
      int numGlyphs = gv.getNumGlyphs();
      int[] codes = gv.getGlyphCodes(0, numGlyphs, null);
      boolean foundInvisibleGlyph = false;
      for (int i=0; i<numGlyphs;i++) {
           if (codes[i] == 65535) {
               foundInvisibleGlyph = true;
               break;
           }
      }

      if (!foundInvisibleGlyph) {
           throw new RuntimeException("No invisible glyphs");
      }

      if (font.canDisplayUpTo(str) != -1) {
          throw new RuntimeException("Font can't display all chars");
      }

   }
}

class SuppCharComp extends Component {

  static final int w=400, h=250;
  public Dimension preferredSize() {
     return new Dimension(w,h);
  }

  String str = null;
  Font font = null;
  public SuppCharComp(Font font, String str) {
    this.font = font;
    this.str = str;
  }
  public void paint(Graphics g) {
     Graphics2D g2d = (Graphics2D)g.create();
     g2d.setColor(Color.white);
     g2d.fillRect(0,0,w,h);
     g2d.setColor(Color.black);
     int y = 0;
     g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                          RenderingHints.VALUE_FRACTIONALMETRICS_ON);
     g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

     g2d.setFont(font);
     g2d.drawString(str, 10, 50);

     FontRenderContext frc = g2d.getFontRenderContext();
     GlyphVector gv = font.createGlyphVector(frc, str);
     g2d.drawGlyphVector(gv, 10, 100);
     TextLayout tl = new TextLayout(str, font, frc);
     tl.draw(g2d, 10, 150);
     char[] ca = str.toCharArray();
     g2d.drawChars(ca, 0, ca.length, 10, 200);

  }

}

