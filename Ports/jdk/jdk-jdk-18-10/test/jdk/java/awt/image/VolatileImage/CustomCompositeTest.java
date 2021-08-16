/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key headful
 * @bug     7124347 8198613
 * @summary Verifies that rendering with XOR composite, and arbitraty
 *          custom composite doesn not cause internal errors.
 *
 * @run     main/othervm CustomCompositeTest
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class CustomCompositeTest {

    private static JFrame frame;
    private static CountDownLatch paintLatch;
    private static Throwable paintError;

    public static void main(String[] args) {

        paintLatch = new CountDownLatch(1);
        paintError = null;

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                initGUI();
            }
        });

        try {
            paintLatch.await();
        } catch (InterruptedException e) {
        };
        System.out.println("Paint is done!");
        if (paintError != null) {
            frame.dispose();
            throw new RuntimeException("Test FAILED.", paintError);
        }

        System.out.println("Phase 1: PASSED.");

        // now resise the frame in order to cause re-paint with accelerated
        // source images.
        paintError = null;
        paintLatch = new CountDownLatch(1);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Dimension size = frame.getSize();
                size.width += 50;
                size.height += 50;

                frame.setSize(size);
            }
        });

        try {
            paintLatch.await();
        } catch (InterruptedException e) {
        };
        if (paintError != null) {
            frame.dispose();
            throw new RuntimeException("Resize test FAILED.", paintError);
        }
        frame.dispose();
        System.out.println("Phase 2: PASSED.");

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsConfiguration cfg = env.getDefaultScreenDevice().getDefaultConfiguration();
        // test rendering to accelerated volatile image
        testVolatileImage(cfg, true);
        System.out.println("Phase 3: PASSED.");

        // test rendering to unaccelerated volatile image
        testVolatileImage(cfg, false);
        System.out.println("Phase 4: PASSED.");
    }

    private static void testVolatileImage(GraphicsConfiguration cfg,
            boolean accelerated)
    {
        VolatileImage dst = null;
        try {
            dst = cfg.createCompatibleVolatileImage(640, 480,
                new ImageCapabilities(accelerated));
        } catch (AWTException e) {
            System.out.println("Unable to create volatile image, skip the test.");
            return;
        }
        renderToVolatileImage(dst);
    }

    private static void renderToVolatileImage(VolatileImage dst) {
        Graphics2D g = dst.createGraphics();
        do {
            System.out.println("Render to volatile image..");
            try {
                MyComp.renderTest(g, dst.getHeight(), dst.getHeight());
            } catch (Throwable e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        } while (dst.contentsLost());
        System.out.println("Done.");
    }

    private static void initGUI() {
        frame = new JFrame("Silly composite");
        frame.getContentPane().add(new MyComp());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static class MyComp extends JComponent {

        private static BufferedImage theImage;

        public MyComp() {
        }

        private static BufferedImage getTestImage() {
            if (theImage == null) {
                theImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = theImage.createGraphics();
                g2d.setColor(Color.red);
                g2d.fillRect(0, 0, 256, 256);

                g2d.setPaint(new GradientPaint(0, 0, Color.red, 256, 256, Color.blue));
                g2d.fillRect(0, 100, 256, 256);
                g2d.dispose();
            }
            return theImage;
        }

        public Dimension getPreferredSize() {
            return new Dimension(640, 375);
        }

        public void paintComponent(Graphics g) {


            Graphics2D g2d = (Graphics2D) g;
            try {
                renderTest(g2d, getWidth(), getHeight());
            } catch (Throwable e) {
                paintError = e;
            }
            if (paintLatch != null) {
                paintLatch.countDown();
            }
        }

        public static void renderTest(Graphics2D g2d, int w, int h) {
            g2d.setColor(Color.yellow);
            g2d.fillRect(0, 0, w, h);

            BufferedImage image = getTestImage();
            // draw original image
            g2d.drawRenderedImage(image, null);

            // draw image with custom composite
            g2d.translate(175, 25);
            Composite currentComposite = g2d.getComposite();
            g2d.setComposite(new TestComposite());
            g2d.drawRenderedImage(image, null);
            g2d.setComposite(currentComposite);

            // draw image with XOR
            g2d.translate(175, 25);
            g2d.setXORMode(Color.red);
            g2d.drawRenderedImage(image, null);


            System.out.println("Painting is done...");
        }
    }

    // A silly custom Composite to demonstrate the problem - just inverts the RGB
    private static class TestComposite implements Composite {

        public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
            return new TestCompositeContext();
        }
    }

    private static class TestCompositeContext implements CompositeContext {

        public void dispose() {
        }

        public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
            int w = src.getWidth();
            int h = src.getHeight();

            DataBufferInt srcDB = (DataBufferInt) src.getDataBuffer();
            DataBufferInt dstOutDB = (DataBufferInt) dstOut.getDataBuffer();
            int srcRGB[] = srcDB.getBankData()[0];
            int dstOutRGB[] = dstOutDB.getBankData()[0];
            int srcOffset = srcDB.getOffset();
            int dstOutOffset = dstOutDB.getOffset();
            int srcScanStride = ((SinglePixelPackedSampleModel) src.getSampleModel()).getScanlineStride();
            int dstOutScanStride = ((SinglePixelPackedSampleModel) dstOut.getSampleModel()).getScanlineStride();
            int srcAdjust = srcScanStride - w;
            int dstOutAdjust = dstOutScanStride - w;

            int si = srcOffset;
            int doi = dstOutOffset;

            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    dstOutRGB[doi] = srcRGB[si] ^ 0x00ffffff;
                    si++;
                    doi++;
                }

                si += srcAdjust;
                doi += dstOutAdjust;
            }
        }
    }
}
