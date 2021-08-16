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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import static java.awt.Transparency.TRANSLUCENT;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR_PRE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * @test
 * @key headful
 * @bug 8017626
 * @summary Tests drawing transparent volatile image to transparent BI.
 *          Results of the blit compatibleImage to transparent BI used for
 *          comparison.
 * @run main/othervm -Dsun.java2d.uiScale=1 IncorrectAlphaSurface2SW
 * @run main/othervm -Dsun.java2d.uiScale=2 IncorrectAlphaSurface2SW
 * @run main/othervm -Dsun.java2d.uiScale=3 IncorrectAlphaSurface2SW
 * @run main/othervm -Dsun.java2d.uiScale=4 IncorrectAlphaSurface2SW
 */
public final class IncorrectAlphaSurface2SW {

    private static final int[] SCALES = {1, 2, 4, 8};
    private static final int[] SIZES = {1, 2, 3, 127, 128, 254, 255, 256};
    private static final int[] dstTypes = {TYPE_INT_ARGB, TYPE_INT_ARGB_PRE,
            TYPE_4BYTE_ABGR, TYPE_4BYTE_ABGR_PRE};
    private static final int[] srcTypes = {TRANSLUCENT};


    public static void main(final String[] args) throws IOException {
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice()
                                     .getDefaultConfiguration();
        BufferedImage destVI;
        BufferedImage destBI;
        BufferedImage sourceBI;
        VolatileImage sourceVI;

        for (final int s : SIZES) {
            for (final int srcType : srcTypes) {
                for (final int dstType : dstTypes) {
                    for (final int scale : SCALES) {
                        int sw = s * scale;
                        destVI = new BufferedImage(sw, sw, dstType);
                        destBI = new BufferedImage(sw, sw, dstType);
                        sourceBI = gc.createCompatibleImage(sw, sw, srcType);
                        sourceVI = gc.createCompatibleVolatileImage(s, s, srcType);

                        // draw to dest BI using compatible image
                        fill(sourceBI, s);
                        Graphics2D big = destBI.createGraphics();
                        big.setComposite(AlphaComposite.Src);
                        big.drawImage(sourceBI, 0, 0, sw, sw, null);
                        big.dispose();

                        // draw to dest BI using compatible image
                        fill(sourceVI, s);
                        drawVItoBI(gc, destVI, sourceVI);

                        validate(destVI, destBI);
                        sourceVI.flush();
                    }
                }
            }
        }
        System.out.println("Test PASSED");
    }

    private static void drawVItoBI(GraphicsConfiguration gc,
                                   BufferedImage bi, VolatileImage vi) {
        while (true) {
            vi.validate(gc);
            fill(vi, vi.getHeight());
            if (vi.validate(gc) != VolatileImage.IMAGE_OK) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ignored) {
                }
                continue;
            }

            Graphics2D big = bi.createGraphics();
            big.setComposite(AlphaComposite.Src);
            big.drawImage(vi, 0, 0, bi.getWidth(), bi.getHeight(), null);
            big.dispose();

            if (vi.contentsLost()) {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException ignored) {
                }
                continue;
            }
            break;
        }
    }

    private static void validate(BufferedImage bi, BufferedImage gold)
            throws IOException {
        for (int x = 0; x < bi.getWidth(); ++x) {
            for (int y = 0; y < bi.getHeight(); ++y) {
                if (gold.getRGB(x, y) != bi.getRGB(x, y)) {
                    System.err.println("Expected color = " + gold.getRGB(x, y));
                    System.err.println("Actual color = " + bi.getRGB(x, y));
                    ImageIO.write(gold, "png", new File("gold.png"));
                    ImageIO.write(bi, "png", new File("bi.png"));
                    throw new RuntimeException("Test failed.");
                }
            }
        }
    }

    /**
     * Fills the whole image using different alpha for each row.
     *
     * @param image to fill
     */
    private static void fill(final Image image, final int size) {
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        int row = image.getHeight(null) / size;
        for (int i = 0; i < size; ++i) {
            graphics.setColor(new Color(23, 127, 189, i));
            graphics.fillRect(0, i * row, image.getWidth(null), row);
        }
        graphics.dispose();
    }
}
