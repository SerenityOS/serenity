/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233097
 * @summary Test we get non-zero metrics with large sizes.
 * @run main MassiveMetricsTest
 */

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;

public class MassiveMetricsTest {

    public static void main(String [] args) {

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] fonts = ge.getAllFonts();
        BufferedImage bi = new BufferedImage(1,1,1);
        Graphics2D g2d = bi.createGraphics();
        int[] sizes = { 80, 100, 120, 600, 1600, 2400, 3600, 7200, 12000 };
        String s = "m";

        for (Font f : fonts) {
            Font sz12Font = f.deriveFont(Font.PLAIN, 12);
            FontMetrics sz12 = g2d.getFontMetrics(sz12Font);
            if (sz12.stringWidth(s) == 0) {
                continue; // code point not supported or similar.
            }
            boolean fail = false;
            for (int sz : sizes) {
                Font font = f.deriveFont(Font.PLAIN, sz);
                FontMetrics fm = g2d.getFontMetrics(font);
                if (fm.stringWidth(s) == 0) {
                  fail = true;
                  System.err.println("zero for " + font);
                }
            }
            if (fail) {
                throw new RuntimeException("Zero stringwidth");
            }
        }
    }
}
