/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GradientPaint;
import java.awt.geom.Point2D;

import java.awt.Font;

import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;


/*
 * @test
 * @key headful
 * @bug 7189452 8024767
 * @summary Check if source offset for text rendering is handled correctly
 *          (shouldn't see the text on a similarly colored background).
 * @author a.stepanov
 * @run main TextRenderingTest
 */

public class TextRenderingTest {

    private static final int width  = 450;
    private static final int height = 150;

    public static void main(final String[] args) {

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration gc =
            ge.getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(width, height);

        while (true) {
            vi.validate(gc);
            Graphics2D g2d = vi.createGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, width, height);

            g2d.setPaint(new GradientPaint(
                             new Point2D.Float(0, height / 2), Color.white,
                             new Point2D.Float(width, height / 2), Color.black));
            g2d.fillRect(0, 0, width, height);

            String fnt = g2d.getFont().getFamily();
            g2d.setFont(new Font(fnt, Font.PLAIN, 100));
            g2d.drawString("IIIIIIIIII", 100, 100); // draw text with offset

            g2d.dispose();

            if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
                continue;
            }

            if (vi.contentsLost()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
                continue;
            }

            break;
        }

        BufferedImage bi = vi.getSnapshot();

        // the text shifted shouldn't be visible onto a painted rectangle!
        // so the check: color component (blue) must decrease monotonously

        int prev = Integer.MAX_VALUE;
        for (int x = 0; x < width; ++x) {
            int color = bi.getRGB(x, height / 2);
            int b = color & 0xFF;

            if (b > prev) {
                throw new RuntimeException("test failed: can see the text rendered!");
            }

            prev = b;
        }
    }
}
