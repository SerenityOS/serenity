/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.lang.Math.toRadians;

/**
 * @test
 * @bug 8065373
 * @summary Verifies that we get correct direction, when draw rotated string.
 * @author Sergey Bylokhov
 * @run main DrawRotatedStringUsingRotatedFont
 */
public final class DrawRotatedStringUsingRotatedFont {

    private static final int SIZE = 500;
    private static final String STR = "MMMMMMMMMMMMMMMM";

    private static AffineTransform[] txs = {
                            AffineTransform.getRotateInstance(toRadians(00)),
                            AffineTransform.getRotateInstance(toRadians(45)),
                            AffineTransform.getRotateInstance(toRadians(-45)),
                            AffineTransform.getRotateInstance(toRadians(90)),
                            AffineTransform.getRotateInstance(toRadians(-90)),
                            AffineTransform.getRotateInstance(toRadians(135)),
                            AffineTransform.getRotateInstance(toRadians(-135)),
                            AffineTransform.getRotateInstance(toRadians(180)),
                            AffineTransform.getRotateInstance(toRadians(-180)),
                            AffineTransform.getRotateInstance(toRadians(225)),
                            AffineTransform.getRotateInstance(toRadians(-225)),
                            AffineTransform.getRotateInstance(toRadians(270)),
                            AffineTransform.getRotateInstance(toRadians(-270)),
                            AffineTransform.getRotateInstance(toRadians(315)),
                            AffineTransform.getRotateInstance(toRadians(-315)),
                            AffineTransform.getRotateInstance(toRadians(360)),
                            AffineTransform.getRotateInstance(toRadians(-360))
    };

    public static void main(final String[] args) throws IOException {
        for (final AffineTransform tx2 : txs) {
            for (final AffineTransform tx1 : txs) {
                for (final boolean aa : new boolean[]{true, false}) {
                    final BufferedImage bi1 = createImage(aa, tx1, tx2);
                    final BufferedImage bi2 = createImage(aa, tx2, tx1);
                    compareImage(bi1, bi2);
                    fillTextArea(bi1, tx1, tx2);
                    fillTextArea(bi2, tx2, tx1);
                    checkColors(bi1, bi2);
                }
            }
        }
        System.out.println("Passed");
    }

    /**
     * Compares two images.
     */
    private static void compareImage(final BufferedImage bi1,
                                     final BufferedImage bi2)
            throws IOException {
        for (int i = 0; i < SIZE; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                if (bi1.getRGB(i, j) != bi2.getRGB(i, j)) {
                    ImageIO.write(bi1, "png", new File("image1.png"));
                    ImageIO.write(bi2, "png", new File("image2.png"));
                    throw new RuntimeException("Failed: wrong text location");
                }
            }
        }
    }

    /**
     * Checks an image color. RED and GREEN are allowed only.
     */
    private static void checkColors(final BufferedImage bi1,
                                    final BufferedImage bi2)
            throws IOException {
        for (int i = 0; i < SIZE; ++i) {
            for (int j = 0; j < SIZE; ++j) {
                final int rgb1 = bi1.getRGB(i, j);
                final int rgb2 = bi2.getRGB(i, j);
                if (rgb1 != rgb2 || rgb1 != 0xFFFF0000 && rgb1 != 0xFF00FF00) {
                    ImageIO.write(bi1, "png", new File("image1.png"));
                    ImageIO.write(bi2, "png", new File("image2.png"));
                    throw new RuntimeException("Failed: wrong text location");
                }
            }
        }
    }

    /**
     * Creates an BufferedImage and draws a text, using two transformations,
     * one for graphics and one for font.
     */
    private static BufferedImage createImage(final boolean aa,
                                             final AffineTransform gtx,
                                             final AffineTransform ftx) {
        final BufferedImage bi = new BufferedImage(SIZE, SIZE, TYPE_INT_RGB);
        final Graphics2D bg = bi.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            aa ? RenderingHints.VALUE_ANTIALIAS_ON
                               : RenderingHints.VALUE_ANTIALIAS_OFF);
        bg.setColor(Color.RED);
        bg.fillRect(0, 0, SIZE, SIZE);
        bg.translate(100, 100);
        bg.transform(gtx);
        bg.setColor(Color.BLACK);
        bg.setFont(bg.getFont().deriveFont(20.0f).deriveFont(ftx));
        bg.drawString(STR, 0, 0);
        bg.dispose();
        return bi;
    }

    /**
     * Fills the area of text using green solid color.
     */
    private static void fillTextArea(final BufferedImage bi,
                                     final AffineTransform tx1,
                                     final AffineTransform tx2) {
        final Graphics2D bg = bi.createGraphics();
        bg.translate(100, 100);
        bg.transform(tx1);
        bg.transform(tx2);
        bg.setColor(Color.GREEN);
        final Font font = bg.getFont().deriveFont(20.0f);
        bg.setFont(font);
        bg.fill(font.getStringBounds(STR, bg.getFontRenderContext()));
        bg.dispose();
    }
}

