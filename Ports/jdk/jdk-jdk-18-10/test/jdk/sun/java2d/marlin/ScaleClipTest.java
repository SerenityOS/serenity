/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Scaled Line Clipping rendering test
 *
 * @test
 * @summary verify that scaled line is properly rendered
 * @bug 8210335
 */
public class ScaleClipTest {

    static final boolean SAVE_IMAGE = false;
    static final int SIZE = 50;

    enum SCALE_MODE {
        ORTHO,
        NON_ORTHO,
        COMPLEX
    };

    public static void main(String[] args) {

        // First display which renderer is tested:
        // JDK9 only:
        System.setProperty("sun.java2d.renderer.verbose", "true");

        System.out.println("ScaleClipTest: size = " + SIZE);

        final BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

        boolean fail = false;

        // testNegativeScale:
        for (SCALE_MODE mode : SCALE_MODE.values()) {
            try {
                testNegativeScale(image, mode);
            } catch (IllegalStateException ise) {
                System.err.println("testNegativeScale[" + mode + "] failed:");
                ise.printStackTrace();
                fail = true;
            }
        }

        // testMarginScale:
        for (SCALE_MODE mode : SCALE_MODE.values()) {
            try {
                testMarginScale(image, mode);
            } catch (IllegalStateException ise) {
                System.err.println("testMarginScale[" + mode + "] failed:");
                ise.printStackTrace();
                fail = true;
            }
        }

        // Fail at the end:
        if (fail) {
            throw new RuntimeException("ScaleClipTest has failures.");
        }
    }

    private static void testNegativeScale(final BufferedImage image, final SCALE_MODE mode) {

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, SIZE, SIZE);

            g2d.setColor(Color.BLACK);

            // Bug in TransformingPathConsumer2D.adjustClipScale()
            // non ortho scale only
            final double scale = -1.0;

            final AffineTransform at;
            switch (mode) {
                default:
                case ORTHO:
                    at = AffineTransform.getScaleInstance(scale, scale);
                    break;
                case NON_ORTHO:
                    at = AffineTransform.getScaleInstance(scale, scale + 1e-5);
                    break;
                case COMPLEX:
                    at = AffineTransform.getScaleInstance(scale, scale);
                    at.concatenate(AffineTransform.getShearInstance(1e-4, 1e-4));
                    break;
            }
            g2d.setTransform(at);

            // Set cap/join to reduce clip margin:
            g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

            final Path2D p = new Path2D.Double();
            p.moveTo(scale * 10, scale * 10);
            p.lineTo(scale * (SIZE - 10), scale * (SIZE - 10));

            g2d.draw(p);

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("ScaleClipTest-testNegativeScale-" + mode + ".png");

                    System.out.println("Writing file: " + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            // Check image:
            // 25, 25 = black
            checkPixel(image.getData(), 25, 25, Color.BLACK.getRGB());

        } finally {
            g2d.dispose();
        }
    }

    private static void testMarginScale(final BufferedImage image, final SCALE_MODE mode) {

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, SIZE, SIZE);

            g2d.setColor(Color.BLACK);

            // Bug in Stroker.init()
            // ortho scale only: scale used twice !
            final double scale = 1e-2;

            final AffineTransform at;
            switch (mode) {
                default:
                case ORTHO:
                    at = AffineTransform.getScaleInstance(scale, scale);
                    break;
                case NON_ORTHO:
                    at = AffineTransform.getScaleInstance(scale, scale + 1e-5);
                    break;
                case COMPLEX:
                    at = AffineTransform.getScaleInstance(scale, scale);
                    at.concatenate(AffineTransform.getShearInstance(1e-4, 1e-4));
                    break;
            }
            g2d.setTransform(at);

            final double invScale = 1.0 / scale;

            // Set cap/join to reduce clip margin:
            final float w = (float) (3.0 * invScale);
            g2d.setStroke(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));

            final Path2D p = new Path2D.Double();
            p.moveTo(invScale * -0.5, invScale * 10);
            p.lineTo(invScale * -0.5, invScale * (SIZE - 10));

            g2d.draw(p);

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("ScaleClipTest-testMarginScale-" + mode + ".png");

                    System.out.println("Writing file: " + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }

            // Check image:
            // 0, 25 = black
            checkPixel(image.getData(), 0, 25, Color.BLACK.getRGB());
        } finally {
            g2d.dispose();
        }
    }

    private static void checkPixel(final Raster raster,
                                   final int x, final int y,
                                   final int expected) {

        final int[] rgb = (int[]) raster.getDataElements(x, y, null);

        if (rgb[0] != expected) {
            throw new IllegalStateException("bad pixel at (" + x + ", " + y
                    + ") = " + rgb[0] + " expected: " + expected);
        }
    }

}
