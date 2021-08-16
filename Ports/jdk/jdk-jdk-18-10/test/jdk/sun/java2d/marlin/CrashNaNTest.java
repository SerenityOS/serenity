/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NaN;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8149338 8144938
 * @summary Verifies that Marlin supports NaN coordinates (no JVM crash)
 * but also it skips properly point coordinates with NaN / Infinity values
 * @run main CrashNaNTest
 */
public class CrashNaNTest {

    static final boolean SAVE_IMAGE = false;

    public static void main(String argv[]) {
        Locale.setDefault(Locale.US);

        // initialize j.u.l Looger:
        final Logger log = Logger.getLogger("sun.java2d.marlin");
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                Throwable th = record.getThrown();
                // detect any Throwable:
                if (th != null) {
                    System.out.println("Test failed:\n" + record.getMessage());
                    th.printStackTrace(System.out);

                    throw new RuntimeException("Test failed: ", th);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        // enable Marlin logging & internal checks:
        System.setProperty("sun.java2d.renderer.log", "true");
        System.setProperty("sun.java2d.renderer.useLogger", "true");
        System.setProperty("sun.java2d.renderer.doChecks", "true");

        testFillDefaultAt();
        testDrawComplexAt();

        testPathClosed();

        testStrokedShapes();
    }

    private static void testFillDefaultAt() {
        final int width = 400;
        final int height = 400;

        final BufferedImage image = new BufferedImage(width, height,
                                            BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);

            final Path2D.Double path = new Path2D.Double();
            path.moveTo(100, 100);

            for (int i = 0; i < 20000; i++) {
                path.lineTo(110 + 0.01 * i, 110);
                path.lineTo(111 + 0.01 * i, 100);
            }

            path.lineTo(NaN, 200);
            path.lineTo(200, 200);
            path.lineTo(200, NaN);
            path.lineTo(300, 300);
            path.lineTo(NaN, NaN);
            path.lineTo(100, 200);
            path.closePath();

            final Path2D.Double path2 = new Path2D.Double();
            path2.moveTo(0, 0);
            path2.lineTo(100, height);
            path2.lineTo(0, 200);
            path2.closePath();

            g2d.setColor(Color.BLUE);
            g2d.fill(path);
            g2d.setColor(Color.GREEN);
            g2d.fill(path2);

            g2d.setColor(Color.BLACK);
            g2d.draw(path);
            g2d.draw(path2);

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashNaNTest-fill.png");
                    System.out.println("Writing file: "
                                       + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ex) {
                    System.out.println("Writing file failure:");
                    ex.printStackTrace();
                }
            }

            // Check image on few pixels:
            final Raster raster = image.getData();

            checkPixel(raster, 200, 195, Color.BLUE.getRGB());
            checkPixel(raster, 105, 195, Color.BLUE.getRGB());
            checkPixel(raster, 286, 290, Color.BLUE.getRGB());

            checkPixel(raster, 108, 105, Color.WHITE.getRGB());
            checkPixel(raster, 205, 200, Color.WHITE.getRGB());

            checkPixel(raster, 5, 200, Color.GREEN.getRGB());

        } finally {
            g2d.dispose();
        }
    }

    private static void testDrawComplexAt() {
        final int width = 400;
        final int height = 400;

        final BufferedImage image = new BufferedImage(width, height,
                                            BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                 RenderingHints.VALUE_STROKE_PURE);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);

            final Path2D.Double path = new Path2D.Double();
            path.moveTo(100, 100);

            for (int i = 0; i < 20000; i++) {
                path.lineTo(110 + 0.01 * i, 110);
                path.lineTo(111 + 0.01 * i, 100);
            }

            path.lineTo(NaN, 200);
            path.lineTo(200, 200);
            path.lineTo(200, NaN);
            path.lineTo(300, 300);
            path.lineTo(NaN, NaN);
            path.lineTo(100, 200);
            path.closePath();

            final Path2D.Double path2 = new Path2D.Double();
            path2.moveTo(0, 0);
            path2.lineTo(100, height);
            path2.lineTo(0, 200);
            path2.closePath();

            // Define an non-uniform transform:
            g2d.scale(0.5, 1.0);
            g2d.rotate(Math.PI / 31);

            g2d.setColor(Color.BLACK);
            g2d.draw(path);
            g2d.draw(path2);

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashNaNTest-draw.png");
                    System.out.println("Writing file: "
                                       + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ex) {
                    System.out.println("Writing file failure:");
                    ex.printStackTrace();
                }
            }

            // Check image on few pixels:
            final Raster raster = image.getData();

            checkPixelNotWhite(raster, 40, 210);
            checkPixelNotWhite(raster, 44, 110);
            checkPixelNotWhite(raster, 60, 120);
            checkPixelNotWhite(raster, 89, 219);
            checkPixelNotWhite(raster, 28, 399);
            checkPixelNotWhite(raster, 134, 329);

        } finally {
            g2d.dispose();
        }
    }
    private static void testPathClosed() {
        final int width = 100;
        final int height = 100;

        final BufferedImage image = new BufferedImage(width, height,
                                            BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);

            final Path2D.Double path = new Path2D.Double();
            path.moveTo(40, 40);
            path.lineTo(0,   0);
            path.lineTo(80, 0);
            path.closePath();
            path.lineTo(80, 80);
            path.lineTo(0, 80);
            path.closePath();

            g2d.setColor(Color.BLUE);
            g2d.fill(path);

            g2d.setColor(Color.BLACK);
            g2d.draw(path);

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashNaNTest-path-closed.png");
                    System.out.println("Writing file: "
                                       + file.getAbsolutePath());
                    ImageIO.write(image, "PNG", file);
                } catch (IOException ex) {
                    System.out.println("Writing file failure:");
                    ex.printStackTrace();
                }
            }

            // Check image on few pixels:
            final Raster raster = image.getData();

            checkPixel(raster, 10, 05, Color.BLUE.getRGB());
            checkPixel(raster, 70, 05, Color.BLUE.getRGB());
            checkPixel(raster, 40, 35, Color.BLUE.getRGB());

            checkPixel(raster, 10, 75, Color.BLUE.getRGB());
            checkPixel(raster, 70, 75, Color.BLUE.getRGB());
            checkPixel(raster, 40, 45, Color.BLUE.getRGB());

        } finally {
            g2d.dispose();
        }
    }

    private static void testStrokedShapes() {
        final Stroke stroke = new BasicStroke();

        final Path2D.Double path = new Path2D.Double();
        Shape s;

        // Check filtering NaN values:
        path.reset();
        path.moveTo(100, NaN);
        path.lineTo(NaN, 100);
        path.lineTo(NaN, NaN);

        path.quadTo(NaN, 100, NaN, 100);
        path.quadTo(100, NaN, 100, NaN);
        path.quadTo(NaN, NaN, NaN, NaN);

        path.curveTo(NaN, 100, NaN, 100, NaN, 100);
        path.curveTo(100, NaN, 100, NaN, 100, NaN);
        path.curveTo(NaN, NaN, NaN, NaN, NaN, NaN);
        path.closePath();

        s = stroke.createStrokedShape(path);
        checkEmptyPath(s);

        // Check filtering +Infinity values:
        path.reset();
        path.moveTo(100, POSITIVE_INFINITY);
        path.lineTo(POSITIVE_INFINITY, 100);
        path.lineTo(POSITIVE_INFINITY, POSITIVE_INFINITY);

        path.quadTo(POSITIVE_INFINITY, 100,
                    POSITIVE_INFINITY, 100);
        path.quadTo(100, POSITIVE_INFINITY,
                    100, POSITIVE_INFINITY);
        path.quadTo(POSITIVE_INFINITY, POSITIVE_INFINITY,
                    POSITIVE_INFINITY, POSITIVE_INFINITY);

        path.curveTo(POSITIVE_INFINITY, 100,
                     POSITIVE_INFINITY, 100,
                     POSITIVE_INFINITY, 100);
        path.curveTo(100, POSITIVE_INFINITY,
                     100, POSITIVE_INFINITY,
                     100, POSITIVE_INFINITY);
        path.curveTo(POSITIVE_INFINITY, POSITIVE_INFINITY,
                     POSITIVE_INFINITY, POSITIVE_INFINITY,
                     POSITIVE_INFINITY, POSITIVE_INFINITY);
        path.closePath();

        s = stroke.createStrokedShape(path);
        checkEmptyPath(s);

        // Check filtering -Infinity values:
        path.reset();
        path.moveTo(100, NEGATIVE_INFINITY);
        path.lineTo(NEGATIVE_INFINITY, 100);
        path.lineTo(NEGATIVE_INFINITY, NEGATIVE_INFINITY);

        path.quadTo(NEGATIVE_INFINITY, 100,
                    NEGATIVE_INFINITY, 100);
        path.quadTo(100, NEGATIVE_INFINITY,
                    100, NEGATIVE_INFINITY);
        path.quadTo(NEGATIVE_INFINITY, NEGATIVE_INFINITY,
                    NEGATIVE_INFINITY, NEGATIVE_INFINITY);

        path.curveTo(NEGATIVE_INFINITY, 100,
                     NEGATIVE_INFINITY, 100,
                     NEGATIVE_INFINITY, 100);
        path.curveTo(100, NEGATIVE_INFINITY,
                     100, NEGATIVE_INFINITY,
                     100, NEGATIVE_INFINITY);
        path.curveTo(NEGATIVE_INFINITY, NEGATIVE_INFINITY,
                     NEGATIVE_INFINITY, NEGATIVE_INFINITY,
                     NEGATIVE_INFINITY, NEGATIVE_INFINITY);
        path.closePath();

        s = stroke.createStrokedShape(path);
        checkEmptyPath(s);
    }

    private static void checkEmptyPath(final Shape s) {
        final float[] coords = new float[6];
        final PathIterator it = s.getPathIterator(null);

        int n = 0;
        for (; !it.isDone(); it.next()) {
            int type = it.currentSegment(coords);
            System.out.println("unexpected segment type= " + type
                 + " with coords: " + Arrays.toString(coords));
            n++;
        }
        if (n != 0) {
            System.out.println("path elements = " + n);
            throw new IllegalStateException("Not empty path: "
                          + n + " path elements !");
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

    private static void checkPixelNotWhite(final Raster raster,
                                           final int x, final int y) {

        final int[] rgb = (int[]) raster.getDataElements(x, y, null);

        if (rgb[0] == Color.WHITE.getRGB()) {
            throw new IllegalStateException("bad pixel at (" + x + ", " + y
                          + ") is white (empty)");
        }
    }
}
