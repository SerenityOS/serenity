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
 *
 */

/* @test
 * @bug 8214002
 * @requires (os.family == "windows")
 * @summary verify MS Mincho's Plain & Italic style
 */

import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class FontGlyphCompare {

    static BufferedImage getFontImage(Font font, String text) {
        int x = 1;
        int y = 15;
        int w = 10;
        int h = 18;
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)bi.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.white);
        g.setFont(font);
        g.drawString(text, x, y);
        return bi;
    }

    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        System.out.println("OS is " + osName);
        osName = osName.toLowerCase();
        if (!osName.startsWith("windows")) {
            return;
        }
        Font msMincho = new Font("MS Mincho", Font.PLAIN, 16);
        String family = msMincho.getFamily(java.util.Locale.ENGLISH);
        if (!family.equalsIgnoreCase("MS Mincho")) {
            System.out.println("Japanese fonts not installed");
            return;
        }
        String s = "|";
        BufferedImage bi1 = getFontImage(new Font("MS Mincho", Font.PLAIN, 16), s);
        int h1 = bi1.getHeight();
        int w1 = bi1.getWidth();
        BufferedImage bi2 = getFontImage(new Font("MS Mincho", Font.ITALIC, 16), s);
        int h2 = bi2.getHeight();
        int w2 = bi2.getWidth();
        if ((h1 == h2) && (w1 == w2)) {
            int cnt = 0;
            for(int yy = 0; yy < h1; yy++) {
                for(int xx = 0; xx < w1; xx++) {
                    if (bi1.getRGB(xx, yy) != bi2.getRGB(xx, yy)) {
                        cnt++;
                    }
                }
            }
            if (cnt == 0) {
                throw new Exception("Test failed");
            }
        }
    }
}
