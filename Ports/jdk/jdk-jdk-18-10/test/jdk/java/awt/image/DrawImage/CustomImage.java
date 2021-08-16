/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;

/**
 * @test
 * @bug 7183828
 * @summary Tests that no exceptions are thrown when we draw the custom image
 */
public final class CustomImage extends Image {

    public static void main(String[] args) {
        Image ci = new CustomImage();
        VolatileImage cvi = new CustomVolatileImage();

        BufferedImage bi = generateImage();
        Graphics2D g2d = bi.createGraphics();
        // Custom Image
        test(g2d.drawImage(ci, 0, 0, null), bi);
        test(g2d.drawImage(ci, 0, 0, 5, 5, 0, 0, 5, 5, Color.BLUE, null), bi);
        test(g2d.drawImage(ci, 0, 0, 9, 9, Color.BLUE, null), bi);
        test(g2d.drawImage(ci, 0, 0, 5, 5, 0, 0, 9, 9, Color.BLUE, null), bi);
        test(g2d.drawImage(ci, AffineTransform.getRotateInstance(30), null), bi);
        // Custom VolatileImage
        test(g2d.drawImage(cvi, 0, 0, null), bi);
        test(g2d.drawImage(cvi, 0, 0, 5, 5, 0, 0, 5, 5, Color.BLUE, null), bi);
        test(g2d.drawImage(cvi, 0, 0, 9, 9, Color.BLUE, null), bi);
        test(g2d.drawImage(cvi, 0, 0, 5, 5, 0, 0, 9, 9, Color.BLUE, null), bi);
        test(g2d.drawImage(cvi, AffineTransform.getRotateInstance(30), null), bi);

        g2d.dispose();
    }

    private static BufferedImage generateImage() {
        BufferedImage bi = new BufferedImage(100, 100, TYPE_INT_ARGB_PRE);
        Graphics g = bi.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return bi;
    }

    private static void test(boolean complete, BufferedImage bi) {
        if (complete) {
            throw new RuntimeException("Custom image successfully drawn");
        }
        for (int y = 0; y < bi.getHeight(); ++y) {
            for (int x = 0; x < bi.getWidth(); ++x) {
                if (bi.getRGB(x, y) != Color.GREEN.getRGB()) {
                    throw new RuntimeException("The image was changed");
                }
            }
        }
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return 100;
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return 100;
    }

    @Override
    public ImageProducer getSource() {
        return null;
    }

    @Override
    public Graphics getGraphics() {
        return null;
    }

    @Override
    public Object getProperty(String name, ImageObserver observer) {
        return null;
    }

    private static final class CustomVolatileImage extends VolatileImage {

        @Override
        public BufferedImage getSnapshot() {
            return null;
        }

        @Override
        public int getWidth() {
            return 100;
        }

        @Override
        public int getHeight() {
            return 100;
        }

        @Override
        public Graphics2D createGraphics() {
            return null;
        }

        @Override
        public int validate(GraphicsConfiguration gc) {
            return 0;
        }

        @Override
        public boolean contentsLost() {
            return false;
        }

        @Override
        public ImageCapabilities getCapabilities() {
            return null;
        }

        @Override
        public int getWidth(ImageObserver observer) {
            return 100;
        }

        @Override
        public int getHeight(ImageObserver observer) {
            return 100;
        }

        @Override
        public Object getProperty(String name, ImageObserver observer) {
            return null;
        }
    }
}
