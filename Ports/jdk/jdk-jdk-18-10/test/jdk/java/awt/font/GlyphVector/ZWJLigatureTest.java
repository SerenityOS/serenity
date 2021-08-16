/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
   @bug 7017058 8191130 8195836
   @summary Test handling of ZWJ by layout.
 */

/*
 * A forced mapping of ZWJ (u+200D) to a special invisible glyph ID
 * was breaking many uses of ZWJ to form ligatures in fonts supporting
 * Indic scripts (Malayalam, Bengali, Sinhala at least) and also Emoji.
 * Without knowing the exact properties of a font under test, and also
 * how a layout engine maps chars to glyphs, it is difficult to write
 * a complete robust automated test.
 * So whilst it tries to show rendering for any fonts that claims to
 * support the target alphabet, it will fail only when specific known
 * fonts fail.
 * The test automatically passes or fails only if these fonts do
 * not ligature away the ZWJ.
 * Besides this the test renders the specific text from these fonts
 * and any others that claim to fully support the text, so it can be
 * manually examined if so desired.
 */

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class ZWJLigatureTest {

   // These are fonts and scripts on Windows that should support
   // the behaviours enough to make reliable tests";

   static final String malayalamName = "Malayalam";
   static final String malayalamFont = "Kartika";
   static final String malayalamText = "\u0D2C\u0D3E\u0D32\u0D28\u0D4D\u200D";

   static final String bengaliName = "Bengali";
   static final String bengaliFont = "Vrinda";
   static final String bengaliText =
       "\u09CE \u09A4\u09CD\u200D " +
       "\u09A4\u09BE\u09CE \u09A4\u09BE\u09A4\u09CD\u200D";

   static final String sinhalaName = "Sinhala";
   static final String sinhalaFont = "Iskoola Pota";
   static final String sinhalaText =
       "\u0DC1\u0DCA\u200D\u0DBB\u0DD3" +
       "\u0D9A\u0DCA\u200D\u0DBB\u0DD2" +
       "\u0D9A\u0DCA\u200D\u0DBB\u0DD3" +
       "\u0DA7\u0DCA\u200D\u0DBB\u0DDA" +
       "\u0DB6\u0DCA\u200D\u0DBB\u0DD0" +
       "\u0D9B\u0DCA\u200D\u0DBA\u0DCF";


   static String[] scripts = { malayalamName, bengaliName, sinhalaName };
   static String[] fontNames = { malayalamFont, bengaliFont, sinhalaFont };
   static String[] text = { malayalamText, bengaliText, sinhalaText };


   static void doTest() {
       boolean testFailed = false;

       BufferedImage bi = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
       Graphics2D g2d = (Graphics2D)bi.getGraphics();
       FontRenderContext frc = g2d.getFontRenderContext();
       for (int f=0; f < fontNames.length; f++) {
           Font font = new Font(fontNames[f], Font.PLAIN, 30);
           String family = font.getFamily(Locale.ENGLISH).toLowerCase();
           if (!fontNames[f].toLowerCase().equals(family)) {
               System.out.println(fontNames[f] + " not found, skipping.");
               continue;
           } else {
               System.out.println("Testing " + fontNames[f] +
                                  " for " + scripts[f]);
           }
           char[] chs = text[f].toCharArray();
           GlyphVector gv = font.layoutGlyphVector(frc, chs, 0, chs.length, 0);
           for (int g=0; g<gv.getNumGlyphs(); g++) {
               int glyph = gv.getGlyphCode(g);
               int charIdx = gv.getGlyphCharIndex(g);
               int codePoint = text[f].codePointAt(charIdx);
               Point2D pos = gv.getGlyphPosition(g);

               if (codePoint == 0x200D) {
                  testFailed = true;
                  System.out.println("FAIL: GOT ZWJ\n");
               }
               System.out.println("["+g+"]: gid="+Integer.toHexString(glyph)
                   +", charIdx="+Integer.toHexString(charIdx)
                   +", codePoint="+Integer.toHexString(codePoint)
                   +", pos=["+pos.getX()+","+pos.getY()+"]");
               }
           }
           if (testFailed) {
               throw new RuntimeException("TEST FAILED");
           }
    }

    public static void main(String[] args) {
        doTest();
    }
}
