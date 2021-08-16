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

/*
 * @test TranslatedOutlineTest
 * @bug 6703377
 * @summary This test verifies that outline is translated in a correct direction
 * @run main TranslatedOutlineTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

public class TranslatedOutlineTest {
   public static void main(String a[]) {
        /* prepare blank image */
        BufferedImage bi = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 50, 50);

        /* draw outline somethere in the middle of the image */
        FontRenderContext frc = new FontRenderContext(null, false, false);
        g2.setColor(Color.RED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        GlyphVector gv = g2.getFont().createGlyphVector(frc, "test");
        g2.fill(gv.getOutline(20, 20));

        /* Check if anything was drawn.
         * If y direction is not correct then image is still blank and
         * test will fail.
         */
        int bgcolor = Color.WHITE.getRGB();
        for (int i=0; i<bi.getWidth(); i++) {
            for(int j=0; j<bi.getHeight(); j++) {
               if (bi.getRGB(i, j) != bgcolor) {
                   System.out.println("Test passed.");
                   return;
               }
            }
        }
        throw new RuntimeException("Outline was not detected");
    }
}
