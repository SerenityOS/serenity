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
 * @bug 6679308
 * @summary test drawing to Alpha surfaces
 */

import java.awt.*;
import java.awt.image.*;

public class AlphaSurfaceText {

    int wid=400, hgt=200;

    public AlphaSurfaceText(int biType, Color c) {
        BufferedImage opaquebi0 =
           new BufferedImage(wid, hgt, BufferedImage.TYPE_INT_RGB);
        drawText(opaquebi0, c);

        BufferedImage alphabi = new BufferedImage(wid, hgt, biType);
        drawText(alphabi, c);
        BufferedImage opaquebi1 =
           new BufferedImage(wid, hgt, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = opaquebi1.createGraphics();
        g2d.drawImage(alphabi, 0, 0, null);
        compare(opaquebi0, opaquebi1, biType, c);
    }

    private void drawText(BufferedImage bi, Color c) {
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(c);
        g.setFont(new Font("sansserif", Font.PLAIN, 70));
        g.drawString("Hello!", 20, 100);
        g.setFont(new Font("sansserif", Font.PLAIN, 12));
        g.drawString("Hello!", 20, 130);
        g.setFont(new Font("sansserif", Font.PLAIN, 10));
        g.drawString("Hello!", 20, 150);
    }

    // Need to allow for minimal rounding error, so allow each component
    // to differ by 1.
    void compare(BufferedImage bi0, BufferedImage bi1, int biType, Color c) {
        for (int x=0; x<wid; x++) {
            for (int y=0; y<hgt; y++) {
                int rgb0 = bi0.getRGB(x, y);
                int rgb1 = bi1.getRGB(x, y);
                if (rgb0 == rgb1) continue;
                int r0 = (rgb0 & 0xff0000) >> 16;
                int r1 = (rgb1 & 0xff0000) >> 16;
                int rdiff = r0-r1; if (rdiff<0) rdiff = -rdiff;
                int g0 = (rgb0 & 0x00ff00) >> 8;
                int g1 = (rgb1 & 0x00ff00) >> 8;
                int gdiff = g0-g1; if (gdiff<0) gdiff = -gdiff;
                int b0 = (rgb0 & 0x0000ff);
                int b1 = (rgb1 & 0x0000ff);
                int bdiff = b0-b1; if (bdiff<0) bdiff = -bdiff;
                if (rdiff > 1 || gdiff > 1 || bdiff > 1) {
                    throw new RuntimeException(
                      "Images differ for type "+biType + " col="+c +
                      " at x=" + x + " y="+ y + " " +
                      Integer.toHexString(rgb0) + " vs " +
                      Integer.toHexString(rgb1));
                }
            }
        }

    }
    public static void main(String[] args) {
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB, Color.white);
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB, Color.red);
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB, Color.blue);
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB_PRE, Color.white);
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB_PRE, Color.red);
        new AlphaSurfaceText(BufferedImage.TYPE_INT_ARGB_PRE, Color.blue);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR, Color.white);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR, Color.red);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR, Color.blue);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR_PRE, Color.white);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR_PRE, Color.red);
        new AlphaSurfaceText(BufferedImage.TYPE_4BYTE_ABGR_PRE, Color.blue);
   }
}
