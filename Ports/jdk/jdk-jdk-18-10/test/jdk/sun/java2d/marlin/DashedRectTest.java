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
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

/**
 * Simple Dashed Rect rendering test
 *
 * @test
 * @summary verify that dashed rectangle is properly rasterized
 * @bug 8202580
 */
public class DashedRectTest {

    static final boolean SAVE_IMAGE = false;

    private final static int N = 10;

    final static float DASH_LEN = 3.0f;
    final static float DASH_PH = 5000f;

    final static int MAX = 100;

    public static void main(String[] args) {

        final int size = 200;

        // First display which renderer is tested:
        // JDK9 only:
        System.setProperty("sun.java2d.renderer.verbose", "true");

        System.out.println("DashedRectClipTest: size = " + size);

        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.setClip(0, 0, size, size);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, size, size);

            // Corrupt Marlin Dasher.dash cached array:
            g2d.setColor(Color.RED);
            g2d.setStroke(createBadStroke());
            g2d.drawRect(20, 20, 50, 50);

            g2d.setStroke(createStroke());

            g2d.setColor(Color.BLUE);

            for (int i = 0; i < N; i++) {
                final long start = System.nanoTime();

                g2d.drawRect(5, 5, MAX, MAX);

                final long time = System.nanoTime() - start;

                System.out.println("paint: duration= " + (1e-6 * time) + " ms.");
            }

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("DashedRectClipTest-MAX-" + MAX + "-dashed.png");

                    System.out.println("Writing file: " + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            // Check image on few pixels:
            final Raster raster = image.getData();

            // 10, 5 = blue
            checkPixel(raster, 10, 5, Color.BLUE.getRGB());

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

    private static BasicStroke createStroke() {
        return new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
                new float[]{DASH_LEN}, DASH_PH) {

        };
    }

    private static BasicStroke createBadStroke() {
        final float[] dash = new float[100];
        Arrays.fill(dash, 19.333f);

        return new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash, DASH_PH);
    }

}
