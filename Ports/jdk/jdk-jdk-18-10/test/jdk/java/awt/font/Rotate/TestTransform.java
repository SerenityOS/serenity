/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestTransform
 * @bug 6586545
 * @summary This test verifies that transforms do not cause crash
 * @run main TestTransform
 */

import java.awt.geom.AffineTransform;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class TestTransform {
    public static void testTransformedFont(AffineTransform a, Object textHint) {
        BufferedImage bi = new BufferedImage(200, 200,
                                   BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setFont(g2.getFont().deriveFont(12.0f));
        g2.setTransform(a);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textHint);
        g2.drawString("test", 100, 100);
    }

    public static void testFontOfSize(float sz, Object textHint) {
        BufferedImage bi = new BufferedImage(200, 200,
                                   BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) bi.getGraphics();
        g2.setFont(g2.getFont().deriveFont(sz));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textHint);
        g2.drawString("test", 100, 100);
    }

    public static void main(String[] args) {
        Object aahints[] = {RenderingHints.VALUE_TEXT_ANTIALIAS_OFF,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB};
        int i, j, k;
        AffineTransform a = new AffineTransform();

        for (i=0; i<aahints.length; i++) {
            for(j=0; j<8; j++) {
                System.out.println("Testing hint "+i+" angle="+j);
                a.setToRotation(j*Math.PI/4);
                testTransformedFont(a, aahints[i]);
            }
            testFontOfSize(0.0f, aahints[i]);
            testFontOfSize(-10.0f, aahints[i]);
        }
    }
}
