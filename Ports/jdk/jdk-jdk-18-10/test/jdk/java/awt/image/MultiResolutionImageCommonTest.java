/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import sun.awt.SunHints;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;
import java.awt.image.MultiResolutionImage;

/**
 * @test
 * @bug 8011059
 * @author Alexander Scherbatiy
 * @summary Test MultiResolution image loading and painting with various scaling
 *          combinations
 * @modules java.desktop/sun.awt
 *          java.desktop/sun.awt.image
 */
public class MultiResolutionImageCommonTest {

    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 200;
    private static final Color COLOR_1X = Color.GREEN;
    private static final Color COLOR_2X = Color.BLUE;

    public static void main(String[] args) throws Exception {
        testCustomMultiResolutionImage();
        System.out.println("Test passed.");
    }

    public static void testCustomMultiResolutionImage() {
        testCustomMultiResolutionImage(false);
        testCustomMultiResolutionImage(true);
    }

    public static void testCustomMultiResolutionImage(
            boolean enableImageScaling) {

        Image image = new MultiResolutionBufferedImage();

        // Same image size
        BufferedImage bufferedImage = new BufferedImage(
                IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.drawImage(image, 0, 0, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 4, 3 * IMAGE_HEIGHT / 4), false);

        // Twice image size
        bufferedImage = new BufferedImage(2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.drawImage(image, 0, 0, 2 * IMAGE_WIDTH,
                2 * IMAGE_HEIGHT, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        checkColor(bufferedImage.getRGB(3 * IMAGE_WIDTH / 2,
                3 * IMAGE_HEIGHT / 2), enableImageScaling);

        // Scale 2x
        bufferedImage = new BufferedImage(
                2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.scale(2, 2);
        g2d.drawImage(image, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 2, 3 * IMAGE_HEIGHT / 2), enableImageScaling);

        // Rotate
        bufferedImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.drawImage(image, 0, 0, null);
        g2d.rotate(Math.PI / 4);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 4, 3 * IMAGE_HEIGHT / 4), false);

        // Scale 2x and Rotate
        bufferedImage = new BufferedImage(
                2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.scale(-2, 2);
        g2d.rotate(-Math.PI / 10);
        g2d.drawImage(image, -IMAGE_WIDTH, 0, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 2, 3 * IMAGE_HEIGHT / 2), enableImageScaling);

        // General Transform
        bufferedImage = new BufferedImage(
                2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        float delta = 0.05f;
        float cos = 1 - delta * delta / 2;
        float sin = 1 + delta;
        AffineTransform transform
                = new AffineTransform(2 * cos, 0.1, 0.3, -2 * sin, 10, -5);
        g2d.setTransform(transform);
        g2d.drawImage(image, 0, -IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 2, 3 * IMAGE_HEIGHT / 2), enableImageScaling);

        int D = 10;
        // From Source to small Destination region
        bufferedImage = new BufferedImage(
                IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.drawImage(image, IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2,
                IMAGE_WIDTH - D, IMAGE_HEIGHT - D,
                D, D, IMAGE_WIDTH - D, IMAGE_HEIGHT - D, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 4, 3 * IMAGE_HEIGHT / 4), false);

        // From Source to large Destination region
        bufferedImage = new BufferedImage(
                2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g2d = (Graphics2D) bufferedImage.getGraphics();
        setImageScalingHint(g2d, enableImageScaling);
        g2d.drawImage(image, D, D, 2 * IMAGE_WIDTH - D, 2 * IMAGE_HEIGHT - D,
                IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2,
                IMAGE_WIDTH - D, IMAGE_HEIGHT - D, null);
        checkColor(bufferedImage.getRGB(
                3 * IMAGE_WIDTH / 2, 3 * IMAGE_HEIGHT / 2), enableImageScaling);
    }

    static class MultiResolutionBufferedImage extends BufferedImage
            implements MultiResolutionImage {

        Image highResolutionImage;

        public MultiResolutionBufferedImage() {
            super(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            highResolutionImage = new BufferedImage(
                    2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT,
                    BufferedImage.TYPE_INT_RGB);
            draw(getGraphics(), 1);
            draw(highResolutionImage.getGraphics(), 2);
        }

        final void draw(Graphics graphics, float resolution) {
            Graphics2D g2 = (Graphics2D) graphics;
            g2.scale(resolution, resolution);
            g2.setColor((resolution == 1) ? COLOR_1X : COLOR_2X);
            g2.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        }

        @Override
        public Image getResolutionVariant(
                double destImageWidth, double destImageHeight) {
            return ((destImageWidth <= getWidth() && destImageHeight <= getHeight()))
                    ? this : highResolutionImage;
        }

        @Override
        public List<Image> getResolutionVariants() {
            return Arrays.asList(this, highResolutionImage);
        }
    }

    static void setImageScalingHint(
            Graphics2D g2d, boolean enableImageScaling) {
        g2d.setRenderingHint(SunHints.KEY_RESOLUTION_VARIANT, enableImageScaling
                ? RenderingHints.VALUE_RESOLUTION_VARIANT_DEFAULT
                : RenderingHints.VALUE_RESOLUTION_VARIANT_BASE);
    }

    static void checkColor(int rgb, boolean isImageScaled) {

        if (!isImageScaled && COLOR_1X.getRGB() != rgb) {
            throw new RuntimeException("Wrong 1x color: " + new Color(rgb));
        }

        if (isImageScaled && COLOR_2X.getRGB() != rgb) {
            throw new RuntimeException("Wrong 2x color" + new Color(rgb));
        }
    }

}
