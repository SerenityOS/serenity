/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8013569
 * @author Sergey Bylokhov
 */
public final class IncorrectTextSize {

    static final int scale = 2;
    static final int width = 1200;
    static final int height = 100;
    static BufferedImage bi = new BufferedImage(width, height,
                                                BufferedImage.TYPE_INT_ARGB);
    static final String TEXT = "The quick brown fox jumps over the lazy dog"
            + "The quick brown fox jumps over the lazy dog";

    public static void main(final String[] args) throws IOException {
        for (int  point = 5; point < 11; ++point) {
            Graphics2D g2d = bi.createGraphics();
            g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, point));
            g2d.scale(scale, scale);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.green);
            g2d.drawString(TEXT, 0, 20);
            int length = g2d.getFontMetrics().stringWidth(TEXT);
            if (length < 0) {
                throw new RuntimeException("Negative length");
            }
            for (int i = (length + 1) * scale; i < width; ++i) {
                for (int j = 0; j < height; ++j) {
                    if (bi.getRGB(i, j) != Color.white.getRGB()) {
                        g2d.drawLine(length, 0, length, height);
                        ImageIO.write(bi, "png", new File("image.png"));
                        System.out.println("length = " + length);
                        System.err.println("Wrong color at x=" + i + ",y=" + j);
                        System.err.println("Color is:" + new Color(bi.getRGB(i,
                                                                             j)));
                        throw new RuntimeException("Test failed.");
                    }
                }
            }
            g2d.dispose();
        }
    }
}
