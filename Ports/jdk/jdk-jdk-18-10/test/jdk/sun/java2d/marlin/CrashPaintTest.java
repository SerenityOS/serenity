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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * @test
 * @bug 8148886
 * @summary Verifies that Marlin supports reentrant operations (ThreadLocal)
 * like in custom Paint or custom Composite
 * @run main CrashPaintTest
 */
public class CrashPaintTest {

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

        // Force using thread-local storage:
        System.setProperty("sun.java2d.renderer.useThreadLocal", "true");
        // Force smaller pixelsize to force using array caches:
        System.setProperty("sun.java2d.renderer.pixelsize", "256");

        final int width = 300;
        final int height = 300;

        final BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = (Graphics2D) image.getGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setBackground(Color.WHITE);
            g2d.clearRect(0, 0, width, height);

            final Ellipse2D.Double ellipse
                = new Ellipse2D.Double(0, 0, width, height);

            final Paint paint = new CustomPaint(100);

            for (int i = 0; i < 20; i++) {
                final long start = System.nanoTime();
                g2d.setPaint(paint);
                g2d.fill(ellipse);

                g2d.setColor(Color.GREEN);
                g2d.draw(ellipse);

                final long time = System.nanoTime() - start;
                System.out.println("paint: duration= " + (1e-6 * time) + " ms.");
            }

            if (SAVE_IMAGE) {
                try {
                    final File file = new File("CrashPaintTest.png");
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

            // 170, 175 = blue
            checkPixel(raster, 170, 175, Color.BLUE.getRGB());
            // 50, 50 = blue
            checkPixel(raster, 50, 50, Color.BLUE.getRGB());

            // 190, 110 = pink
            checkPixel(raster, 190, 110, Color.PINK.getRGB());
            // 280, 210 = pink
            checkPixel(raster, 280, 210, Color.PINK.getRGB());

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

    private static class CustomPaint extends TexturePaint {
        private int size;

        CustomPaint(final int size) {
            super(new BufferedImage(size, size,
                    BufferedImage.TYPE_INT_ARGB),
                    new Rectangle2D.Double(0, 0, size, size)
            );
            this.size = size;
        }

        @Override
        public PaintContext createContext(ColorModel cm,
                                          Rectangle deviceBounds,
                                          Rectangle2D userBounds,
                                          AffineTransform at,
                                          RenderingHints hints) {

            // Fill bufferedImage using
            final Graphics2D g2d = (Graphics2D) getImage().getGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setBackground(Color.PINK);
                g2d.clearRect(0, 0, size, size);

                g2d.setColor(Color.BLUE);
                g2d.drawRect(0, 0, size, size);

                g2d.fillOval(size / 10, size / 10,
                             size * 8 / 10, size * 8 / 10);

            } finally {
                g2d.dispose();
            }

            return super.createContext(cm, deviceBounds, userBounds, at, hints);
        }
    }
}
