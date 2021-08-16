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

/*
 * @test
 * @bug 8204929
 * @summary test rotation of font with embedded bitmaps
 * @run main GlyphRotationTest
 */

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class GlyphRotationTest {

    public static final String fontName = "MS UI Gothic";
    public static Font font;
    public static final int SZ = 50;

    public static void main(String[] args) {
        font = new Font(fontName, Font.PLAIN, 15);
        if (!font.getFamily(java.util.Locale.ENGLISH).equals(fontName)) {
            return;
        }
        BufferedImage bi = new BufferedImage(SZ,SZ,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, SZ, SZ);
        g2d.setColor(Color.black);
        g2d.setFont(font);
        g2d.drawString("1", SZ/2, SZ/2);
        int pixCnt1 = countPixels(bi);
        AffineTransform at = AffineTransform.getRotateInstance(Math.PI/2);
        font = font.deriveFont(Font.PLAIN, at);
        g2d.setFont(font);
        g2d.drawString("1", SZ/2, SZ/2);
        int pixCnt2 = countPixels(bi);
        if (args.length > 0) {
            try {
               ImageIO.write(bi, "png", new java.io.File("im.png"));
            }  catch (Exception e) {}
        }
        if (pixCnt1 == pixCnt2) {
            String msg = "cnt 1 = " + pixCnt1 + " cnt 2 = " + pixCnt2;
            throw new RuntimeException(msg);
        }
    }

    static int countPixels(BufferedImage bi) {
        int cnt = 0;
        int w = bi.getWidth(null);
        int h = bi.getHeight(null);
        for (int i=0; i<w; i++) {
            for (int j=0; j<w; j++) {
                int rgb = bi.getRGB(i, j) & 0xFFFFFF;
                if (rgb == 0) {
                    cnt++;
                }
            }
        }
        return cnt;
    }
}
