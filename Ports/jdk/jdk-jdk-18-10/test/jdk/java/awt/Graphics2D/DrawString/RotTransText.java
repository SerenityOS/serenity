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

/**
 * @test
 * @bug 6683472 6687298 8197797
 * @summary Transformed fonts using drawString and TextLayout should be in
 *          the same position.
 */

import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.HashMap;

public class RotTransText  {

    public static void main(String[] args) {

        testIt(false);
        testIt(true);

    }

    public static void testIt(boolean fmOn) {

        int wid=400, hgt=400;
        BufferedImage bi =
            new BufferedImage(wid, hgt, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bi.createGraphics();

        if (fmOn) {
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                 RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }

        int x=130, y=130;
        String s = "Text";

        int xt=90, yt=50;
        for (int angle=0;angle<360;angle+=30) {

            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, wid, hgt);

            AffineTransform aff = AffineTransform.getTranslateInstance(50,90);
            aff.rotate(angle * Math.PI/180.0);

            Font fnt = new Font("SansSerif", Font.PLAIN, 60);
            fnt = fnt.deriveFont(Font.PLAIN, aff);
            g2d.setFont(fnt);
            g2d.setColor(Color.blue);
            g2d.drawString(s, x, y);

            g2d.setColor(Color.red);
            FontRenderContext frc = g2d.getFontRenderContext();
            HashMap attrMap = new HashMap();
            attrMap.put(TextAttribute.STRIKETHROUGH,
                    TextAttribute.STRIKETHROUGH_ON);
            fnt = fnt.deriveFont(attrMap);
            TextLayout tl = new TextLayout(s, fnt, frc);
            tl.draw(g2d, (float)x, (float)y);

            // Test BI: should be minimal blue relative to red.
            int redCount = 0;
            int blueCount = 0;
            int red = Color.red.getRGB();
            int blue = Color.blue.getRGB();
            for (int px=0;px<wid;px++) {
                for (int py=0;py<hgt;py++) {
                    int rgb = bi.getRGB(px, py);
                    if (rgb == red) {
                        redCount++;
                    } else if (rgb == blue) {
                        blueCount++;
                    }
                }
            }
            if (redCount == 0 || (blueCount/(double)redCount) > 0.1) {
                throw new
                    RuntimeException("Ratio of blue to red is too great: " +
                                     (blueCount/(double)redCount));
            }
        }
    }
}
