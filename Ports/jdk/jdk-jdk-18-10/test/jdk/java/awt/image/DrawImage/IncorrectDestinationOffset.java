/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @test
 * @key headful
 * @bug 8041129
 * @summary Destination offset should be correct in case of Surface->SW blit.
 *          Destination outside of the drawing area should be untouched.
 * @author Sergey Bylokhov
 */
public final class IncorrectDestinationOffset {

    private static final int SIZE = 128;
    private static final double[] SCALES = {0.25, 0.5, 1, 1.5, 2.0, 4};

    public static void main(final String[] args) throws IOException {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice()
                                     .getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(SIZE, SIZE);
        BufferedImage bi = new BufferedImage(SIZE, SIZE,
                                             BufferedImage.TYPE_INT_ARGB);
        for (double scale : SCALES) {
            while (true) {
                // initialize Volatile Image
                vi.validate(gc);
                Graphics2D g2d = vi.createGraphics();
                g2d.setColor(Color.green);
                g2d.fillRect(0, 0, SIZE, SIZE);
                g2d.dispose();

                if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                // Draw the VolatileImage to BI with scale and offsets
                Graphics2D g = bi.createGraphics();
                g.setComposite(AlphaComposite.Src);
                g.setColor(Color.RED);
                g.fillRect(0, 0, SIZE / 2, SIZE / 2);
                g.setColor(Color.BLUE);
                g.fillRect(SIZE / 2, 0, SIZE / 2, SIZE / 2);
                g.setColor(Color.ORANGE);
                g.fillRect(0, SIZE / 2, SIZE / 2, SIZE / 2);
                g.setColor(Color.MAGENTA);
                g.fillRect(SIZE / 2, SIZE / 2, SIZE / 2, SIZE / 2);

                int point2draw = (int) (100 * scale);
                int size2draw = (int) (SIZE * scale);
                g.drawImage(vi, point2draw, point2draw, size2draw, size2draw,
                            null);
                g.dispose();

                if (vi.contentsLost()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }
                validate(bi, point2draw, size2draw);
                break;
            }
        }
    }

    private static void validate(BufferedImage bi, int point2draw,
                                 int size2draw)
            throws IOException {
        for (int x = 0; x < SIZE; ++x) {
            for (int y = 0; y < SIZE; ++y) {
                if (isInsideGreenArea(point2draw, size2draw, x, y)) {
                    if (bi.getRGB(x, y) != Color.green.getRGB()) {
                        ImageIO.write(bi, "png", new File("image.png"));
                        throw new RuntimeException("Test failed.");
                    }
                } else {
                    if (isRedArea(x, y)) {
                        if (bi.getRGB(x, y) != Color.red.getRGB()) {
                            ImageIO.write(bi, "png", new File("image.png"));
                            throw new RuntimeException("Test failed.");
                        }
                    }
                    if (isBlueArea(x, y)) {
                        if (bi.getRGB(x, y) != Color.blue.getRGB()) {
                            ImageIO.write(bi, "png", new File("image.png"));
                            throw new RuntimeException("Test failed.");
                        }
                    }
                    if (isOrangeArea(x, y)) {
                        if (bi.getRGB(x, y) != Color.orange.getRGB()) {
                            ImageIO.write(bi, "png", new File("image.png"));
                            throw new RuntimeException("Test failed.");
                        }
                    }
                    if (isMagentaArea(x, y)) {
                        if (bi.getRGB(x, y) != Color.magenta.getRGB()) {
                            ImageIO.write(bi, "png", new File("image.png"));
                            throw new RuntimeException("Test failed.");
                        }
                    }
                }
            }
        }
    }

    private static boolean isRedArea(int x, int y) {
        return x < SIZE / 2 && y < SIZE / 2;
    }

    private static boolean isBlueArea(int x, int y) {
        return x >= SIZE / 2 && y < SIZE / 2;
    }

    private static boolean isOrangeArea(int x, int y) {
        return x < SIZE / 2 && y >= SIZE / 2;
    }

    private static boolean isMagentaArea(int x, int y) {
        return x >= SIZE / 2 && y >= SIZE / 2;
    }

    private static boolean isInsideGreenArea(int point2draw, int size2draw,
                                             int x, int y) {
        return x >= point2draw && x < point2draw + size2draw && y >=
                point2draw && y < point2draw + size2draw;
    }
}
