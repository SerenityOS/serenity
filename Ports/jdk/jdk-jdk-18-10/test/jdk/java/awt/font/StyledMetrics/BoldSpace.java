/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6686365 7017637
  @summary Confirm that styling does not affect metrics of zero advance glyphs
*/

import java.awt.*;
import java.awt.image.*;

public class BoldSpace {
    public static void main(String[] s) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

        String errMsg = "ZWJ Space char should have 0 advance\n";

        Graphics g = bi.getGraphics();

        // It turns out that some fonts inexplicably treat this as
        // a standard character. In this 14 pt font, if we see an advance
        // that's clearly bigger than we'd have introduced in bolding we'll
        // not error out this test, presuming that its a consequence of
        // the actual font data. A Linux font 'TLwg Type Bold' is the case
        // in point.
        int errorMargin = 4;
        g.setFont(new Font("monospaced", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        System.out.println("Bold: " + fm.charWidth('\u200b'));
        int cwid = fm.charWidth('\u200b');
        if (cwid > 0 && cwid < errorMargin) {
            throw new RuntimeException(errMsg);
        }

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        fm = g.getFontMetrics();
        System.out.println("Bold + LCD: "+fm.charWidth('\u200b'));
        cwid = fm.charWidth('\u200b');
        if (cwid > 0 && cwid < errorMargin) {
            throw new RuntimeException(errMsg);
        }


        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        fm = g.getFontMetrics();
        System.out.println("Bold FM OFF + AA: " + fm.charWidth('\u200b'));
        cwid = fm.charWidth('\u200b');
        if (cwid > 0 && cwid < errorMargin) {
            throw new RuntimeException(errMsg);
        }

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        fm = g.getFontMetrics();
        System.out.println("Bold FM ON + AA: " + fm.charWidth('\u200b'));
        cwid = fm.charWidth('\u200b');
        if (cwid > 0 && cwid < errorMargin) {
            throw new RuntimeException(errMsg);
        }

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        fm = g.getFontMetrics();
        System.out.println("Bold FM ON + nonAA: " + fm.charWidth('\u200b'));
        cwid = fm.charWidth('\u200b');
        if (cwid > 0 && cwid < errorMargin) {
            throw new RuntimeException(errMsg);
        }

        System.out.println("All printed values should be 0 to PASS");
    }
}
