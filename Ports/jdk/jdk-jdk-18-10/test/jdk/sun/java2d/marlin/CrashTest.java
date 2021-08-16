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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import static java.awt.geom.Path2D.WIND_NON_ZERO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * @test
 * @summary Simple crash rendering test using huge GeneralPaths with the Marlin renderer
 * @run main/othervm -mx512m CrashTest
 * @ignore tests that take a long time and consumes 5Gb memory
 * @run main/othervm -ms4g -mx4g CrashTest -slow
*/
public class CrashTest {

    static final boolean SAVE_IMAGE = false;
    static boolean USE_ROUND_CAPS_AND_JOINS = true;

    public static void main(String[] args) {
        boolean runSlowTests = (args.length != 0 && "-slow".equals(args[0]));

        // First display which renderer is tested:
        System.setProperty("sun.java2d.renderer.verbose", "true");

        // try insane image sizes:

        // subpixel coords may overflow:
        // check MAX_VALUE / (8 * 2); overflow may happen due to orientation flag
        // But as it is impossible to allocate an image larger than 2Gb (byte) then
        // it is also impossible to have rowAAChunk larger than 2Gb !

        // Disabled test as it consumes 4GB heap + offheap (2Gb) ie > 6Gb !
        if (runSlowTests) {
            testHugeImage((Integer.MAX_VALUE >> 4) - 100, 16);
        }

        // larger than 23 bits: (RLE)
        testHugeImage(8388608 + 1, 10);

        if (runSlowTests) {
            test(0.1f, false, 0);
            test(0.1f, true, 7f);
        }

        // Exceed 2Gb OffHeap buffer for edges:
        try {
            USE_ROUND_CAPS_AND_JOINS = true;
            test(0.1f, true, 0.1f);
            System.out.println("Exception MISSING.");
        }
        catch (Throwable th) {
            if (th instanceof ArrayIndexOutOfBoundsException) {
                System.out.println("ArrayIndexOutOfBoundsException expected.");
            } else {
                throw new RuntimeException("Unexpected exception", th);
            }
        }
    }

    private static void test(final float lineStroke,
                             final boolean useDashes,
                             final float dashMinLen)
        throws ArrayIndexOutOfBoundsException
    {
        System.out.println("---\n" + "test: "
            + "lineStroke=" + lineStroke
            + ", useDashes=" + useDashes
            +", dashMinLen=" + dashMinLen
        );

        final BasicStroke stroke = createStroke(lineStroke, useDashes, dashMinLen);

        // TODO: test Dasher.firstSegmentsBuffer resizing ?
// array.dasher.firstSegmentsBuffer.d_float[2] sum: 6 avg: 3.0 [3 | 3]
        /*
         // Marlin growable arrays:
         = new StatLong("array.dasher.firstSegmentsBuffer.d_float");
         = new StatLong("array.stroker.polystack.curves.d_float");
         = new StatLong("array.stroker.polystack.curveTypes.d_byte");
         = new StatLong("array.marlincache.rowAAChunk.d_byte");
         = new StatLong("array.marlincache.touchedTile.int");
         = new StatLong("array.renderer.alphaline.int");
         = new StatLong("array.renderer.crossings.int");
         = new StatLong("array.renderer.aux_crossings.int");
         = new StatLong("array.renderer.edgeBuckets.int");
         = new StatLong("array.renderer.edgeBucketCounts.int");
         = new StatLong("array.renderer.edgePtrs.int");
         = new StatLong("array.renderer.aux_edgePtrs.int");
         */
        // size > 8192 (exceed both tile and buckets arrays)
        final int size = 9000;
        System.out.println("image size = " + size);

        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2d.setClip(0, 0, size, size);
            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, size, size);

            g2d.setStroke(stroke);
            g2d.setColor(Color.BLACK);

            final long start = System.nanoTime();

            paint(g2d, size - 10f);

            final long time = System.nanoTime() - start;

            System.out.println("paint: duration= " + (1e-6 * time) + " ms.");

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashTest-dash-" + useDashes + ".bmp");

                    System.out.println("Writing file: " + file.getAbsolutePath());
                    ImageIO.write(image, "BMP", file);
                } catch (IOException ex) {
                    System.out.println("Writing file failure:");
                    ex.printStackTrace();
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private static void testHugeImage(final int width, final int height)
        throws ArrayIndexOutOfBoundsException
    {
        System.out.println("---\n" + "testHugeImage: "
            + "width=" + width + ", height=" + height);

        final BasicStroke stroke = createStroke(2.5f, false, 0);

        // size > 24bits (exceed both tile and buckets arrays)
        System.out.println("image size = " + width + " x "+height);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);

            g2d.setStroke(stroke);
            g2d.setColor(Color.BLACK);

            final Path2D.Float path = new Path2D.Float(WIND_NON_ZERO, 32);
            path.moveTo(0, 0);
            path.lineTo(width, 0);
            path.lineTo(width, height);
            path.lineTo(0, height);
            path.lineTo(0, 0);

            final long start = System.nanoTime();

            g2d.draw(path);

            final long time = System.nanoTime() - start;

            System.out.println("paint: duration= " + (1e-6 * time) + " ms.");

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashTest-huge-"
                        + width + "x" +height + ".bmp");

                    System.out.println("Writing file: " + file.getAbsolutePath());
                    ImageIO.write(image, "BMP", file);
                } catch (IOException ex) {
                    System.out.println("Writing file failure:");
                    ex.printStackTrace();
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private static void paint(final Graphics2D g2d, final float size) {
        final double halfSize = size / 2.0;

        final Path2D.Float path = new Path2D.Float(WIND_NON_ZERO, 32 * 1024);

        // show cross:
        path.moveTo(0, 0);
        path.lineTo(size, size);

        path.moveTo(size, 0);
        path.lineTo(0, size);

        path.moveTo(0, 0);
        path.lineTo(size, 0);

        path.moveTo(0, 0);
        path.lineTo(0, size);

        path.moveTo(0, 0);

        double r = size;

        final int ratio = 100;
        int repeats = 1;

        int n = 0;

        while (r > 1.0) {
            repeats *= ratio;

            if (repeats > 10000) {
                repeats = 10000;
            }

            for (int i = 0; i < repeats; i++) {
                path.lineTo(halfSize - 0.5 * r + i * r / repeats,
                            halfSize - 0.5 * r);
                n++;
                path.lineTo(halfSize - 0.5 * r + i * r / repeats + 0.1,
                            halfSize + 0.5 * r);
                n++;
            }

            r -= halfSize;
        }
        System.out.println("draw : " + n + " lines.");
        g2d.draw(path);
    }

    private static BasicStroke createStroke(final float width,
                                            final boolean useDashes,
                                            final float dashMinLen) {
        final float[] dashes;

        if (useDashes) {
            // huge dash array (exceed Dasher.INITIAL_ARRAY)
            dashes = new float[512];

            float cur = dashMinLen;
            float step = 0.01f;

            for (int i = 0; i < dashes.length; i += 2) {
                dashes[i] = cur;
                dashes[i + 1] = cur;
                cur += step;
            }
        } else {
            dashes = null;
        }

        if (USE_ROUND_CAPS_AND_JOINS) {
            // Use both round Caps & Joins:
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 100.0f, dashes, 0.0f);
        }
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 100.0f, dashes, 0.0f);
    }
}
