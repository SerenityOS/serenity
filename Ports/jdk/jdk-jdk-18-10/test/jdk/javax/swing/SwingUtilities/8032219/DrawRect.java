/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.SwingUtilities2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8032219
 * @author Sergey Bylokhov
 * @modules java.desktop/sun.swing
 */
public final class DrawRect {

    private static final int size = 50;

    private static final Rectangle[] rects = {
            new Rectangle(0, 0, 1, 1),
            new Rectangle(0, 0, 1, 2),
            new Rectangle(0, 0, 2, 1),
            new Rectangle(10, 10, 10, 10),
            new Rectangle(10, 10, -1, -1),
            new Rectangle(-1, -1, 10, 10),
            new Rectangle(-1, -1, -10, -10),
            new Rectangle(0, 0, size, size),
    };

    private static final Rectangle[] vlines = {new Rectangle(0, 0, 0, 0),
            new Rectangle(0, 0, 0, 1),
            new Rectangle(0, 0, 0, -1),
            new Rectangle(1, 1, 0, 1),
            new Rectangle(1, 1, 0, -1),
            new Rectangle(15, 15, 0, 10),
            new Rectangle(15, 15, 0, -10),
    };
    private static final Rectangle[] hlines = {new Rectangle(0, 0, 0, 0),
            new Rectangle(0, 0, 1, 0),
            new Rectangle(0, 0, -1, 0),
            new Rectangle(1, 1, 1, 0),
            new Rectangle(1, 1, -1, 0),
            new Rectangle(15, 15, 10, 0),
            new Rectangle(15, 15, -10, 0),
    };

    public static void main(final String[] args) throws IOException {
        BufferedImage gold = new BufferedImage(size, size,
                                               BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gold.createGraphics();
        BufferedImage bi = new BufferedImage(size, size,
                                             BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bi.createGraphics();
        g2d.setColor(new Color(0, 250, 0, 100));
        g2d.setBackground(Color.BLACK);
        g.setColor(new Color(0, 250, 0, 100));
        g.setBackground(Color.BLACK);
        // Rectangle
        for (final Rectangle r : rects) {
            g.clearRect(0, 0, size, size);
            g2d.clearRect(0, 0, size, size);
            g.drawRect(r.x, r.y, r.width, r.height);
            SwingUtilities2.drawRect(g2d, r.x, r.y, r.width, r.height);
            test(gold, bi);
        }
        // Vertical Line
        for (final Rectangle l : vlines) {
            g.clearRect(0, 0, size, size);
            g2d.clearRect(0, 0, size, size);
            g.drawLine(l.x, l.y, l.x + l.width, l.y + l.height);
            SwingUtilities2.drawVLine(g2d, l.x, l.y, l.y + l.height);
            test(gold, bi);
        }
        // Horizontal Line
        for (final Rectangle l : hlines) {
            g.clearRect(0, 0, size, size);
            g2d.clearRect(0, 0, size, size);
            g.drawLine(l.x, l.y, l.x + l.width, l.y + l.height);
            SwingUtilities2.drawHLine(g2d, l.x, l.x+l.width, l.y);
            test(gold, bi);
        }

        g.dispose();
        g2d.dispose();
    }

    private static void test(final BufferedImage gold, final BufferedImage bi)
            throws IOException {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (gold.getRGB(x, y) != bi.getRGB(x, y)) {
                    ImageIO.write(gold, "png", new File("gold.png"));
                    ImageIO.write(bi, "png", new File("image.png"));
                    throw new RuntimeException("wrong color");
                }
            }
        }
    }
}
