/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import sun.awt.image.MultiResolutionCachedImage;

/**
 * @test
 * @bug 8132123
 * @author Alexander Scherbatiy
 * @summary MultiResolutionCachedImage unnecessarily creates base image to get
 *          its size
 * @modules java.desktop/sun.awt.image
 * @run main MultiResolutionCachedImageTest
 */
public class MultiResolutionCachedImageTest {

    private static final Color TEST_COLOR = Color.BLUE;

    public static void main(String[] args) {

        Image image = new TestMultiResolutionCachedImage(100);

        image.getWidth(null);
        image.getHeight(null);
        image.getProperty("comment", null);

        int scaledSize = 50;
        Image scaledImage = image.getScaledInstance(scaledSize, scaledSize,
                Image.SCALE_SMOOTH);

        if (!(scaledImage instanceof BufferedImage)) {
            throw new RuntimeException("Wrong scaled image!");
        }

        BufferedImage buffScaledImage = (BufferedImage) scaledImage;

        if (buffScaledImage.getWidth() != scaledSize
                || buffScaledImage.getHeight() != scaledSize) {
            throw new RuntimeException("Wrong scaled image!");
        }

        if (buffScaledImage.getRGB(scaledSize / 2, scaledSize / 2) != TEST_COLOR.getRGB()) {
            throw new RuntimeException("Wrong scaled image!");
        }
    }

    private static Dimension2D getDimension(int size) {
        return new Dimension(size, size);
    }

    private static Dimension2D[] getSizes(int size) {
        return new Dimension2D[]{getDimension(size), getDimension(2 * size)};
    }

    private static Image createImage(int width, int height) {
        BufferedImage buffImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = buffImage.createGraphics();
        g.setColor(TEST_COLOR);
        g.fillRect(0, 0, width, height);
        return buffImage;
    }

    private static class TestMultiResolutionCachedImage
            extends MultiResolutionCachedImage {

        private final int size;

        public TestMultiResolutionCachedImage(int size) {
            super(size, size, getSizes(size), (w, h) -> createImage(w, h));
            this.size = size;
        }

        @Override
        public Image getResolutionVariant(double width, double height) {
            if (width == size || height == size) {
                throw new RuntimeException("Base image is requested!");
            }
            return super.getResolutionVariant(width, height);
        }

        @Override
        protected Image getBaseImage() {
            throw new RuntimeException("Base image is used");
        }
    }
}
