/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8266159
 * @summary Test to detect regression in pixel drawing.
 *          A small circle is drawn and boundary pixels are compared to expected pixels.
 *          Note : this test is specifically written for uiScale=1.0
 * @run main/othervm -Dsun.java2d.uiScale=1.0 DrawOvalTest
 */

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

public class DrawOvalTest {
    public static void main(String[] args) throws IOException {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(10, 10, Transparency.TRANSLUCENT);

        // Draw test rendering sequence
        BufferedImage snapshot = null;
        Graphics2D g2 = vi.createGraphics();

        do {
            vi.validate(gc);
            render(g2);
            snapshot = vi.getSnapshot();
        } while (vi.contentsLost());

        // Pixel color sequence expected after test rendering is complete
        // Blue color = -16776961
        // Red color  = -65536
        int sequence[] = {
            -16776961,
            -16776961,
            -16776961,
            -65536,
            -65536,
            -65536,
            -65536,
            -16776961,
            -16776961,
            -16776961
        };

        // Test the color of pixels at the image boundary
        for (int i = 0; i < snapshot.getWidth(); i++) {

            // Test first row, last row, first column and last column
            if ( snapshot.getRGB(i, 0) != sequence[i] ||
                 snapshot.getRGB(i, 9) != sequence[i] ||
                 snapshot.getRGB(0, i) != sequence[i] ||
                 snapshot.getRGB(9, i) != sequence[i] ) {
                ImageIO.write(snapshot, "png", new File("DrawOvalTest_snapshot.png"));
                throw new RuntimeException("Test failed.");
            }
        }
    }

    private static void render(Graphics2D g2) {
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 10, 10);
        g2.setColor(Color.RED);
        g2.drawOval(0, 0, 9, 9);
    }
}
