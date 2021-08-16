/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6514990 8198613
 * @summary Verifies that calling
 * Graphics2D.drawImage(BufferedImage, BufferedImageOp, x, y) to an
 * accelerated destination produces the same results when performed
 * in software via BufferedImageOp.filter().
 * @run main/othervm DrawBufImgOp -ignore
 * @author campbelc
 */

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * REMIND: This testcase was originally intended to automatically compare
 * the results of the software BufferedImageOp implementations against
 * the OGL-accelerated codepaths.  However, there are just too many open
 * bugs in the mediaLib-based codepaths (see below), which means that
 * creating the reference image may cause crashes or exceptions,
 * and even if we work around those cases using the "-ignore" flag,
 * the visual results of the reference image are often buggy as well
 * (so the comparison will fail even though the OGL results are correct).
 * Therefore, for now we will run the testcase with the "-ignore" flag
 * but without the "-compare" flag, so at least it will be checking for
 * any exceptions/crashes in the OGL code.  When we fix all of the
 * outstanding bugs with the software codepaths, we can remove the
 * "-ignore" flag and maybe even restore the "-compare" flag.  In the
 * meantime, it stil functions well as a manual testcase (with either
 * the "-show" or "-dump" options).
 */
public class DrawBufImgOp extends Canvas {

    private static final int TESTW = 600;
    private static final int TESTH = 500;
    private static boolean done;

    /*
     * If true, skips tests that are known to trigger bugs (which in
     * turn may cause crashes, exceptions, or other artifacts).
     */
    private static boolean ignore;

    // Test both pow2 and non-pow2 sized images
    private static final int[] srcSizes = { 32, 17 };
    private static final int[] srcTypes = {
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE,
        BufferedImage.TYPE_INT_BGR,
        BufferedImage.TYPE_3BYTE_BGR,
        BufferedImage.TYPE_4BYTE_ABGR,
        BufferedImage.TYPE_USHORT_565_RGB,
        BufferedImage.TYPE_BYTE_GRAY,
        BufferedImage.TYPE_USHORT_GRAY,
    };

    private static final RescaleOp
        rescale1band, rescale3band, rescale4band;
    private static final LookupOp
        lookup1bandbyte, lookup3bandbyte, lookup4bandbyte;
    private static final LookupOp
        lookup1bandshort, lookup3bandshort, lookup4bandshort;
    private static final ConvolveOp
        convolve3x3zero, convolve5x5zero, convolve7x7zero;
    private static final ConvolveOp
        convolve3x3noop, convolve5x5noop, convolve7x7noop;

    static {
        rescale1band = new RescaleOp(0.5f, 10.0f, null);
        rescale3band = new RescaleOp(
            new float[] {  0.6f,  0.4f, 0.6f },
            new float[] { 10.0f, -3.0f, 5.0f },
            null);
        rescale4band = new RescaleOp(
            new float[] {  0.6f, 0.4f, 0.6f, 0.9f },
            new float[] { -1.0f, 5.0f, 3.0f, 1.0f },
            null);

        // REMIND: we should probably test non-zero offsets, but that
        // would require massaging the source image data to avoid going
        // outside the lookup table array bounds
        int offset = 0;
        {
            byte invert[] = new byte[256];
            byte halved[] = new byte[256];
            for (int j = 0; j < 256 ; j++) {
                invert[j] = (byte) (255-j);
                halved[j] = (byte) (j / 2);
            }
            ByteLookupTable lut1 = new ByteLookupTable(offset, invert);
            lookup1bandbyte = new LookupOp(lut1, null);
            ByteLookupTable lut3 =
                new ByteLookupTable(offset,
                                    new byte[][] {invert, halved, invert});
            lookup3bandbyte = new LookupOp(lut3, null);
            ByteLookupTable lut4 =
                new ByteLookupTable(offset,
                               new byte[][] {invert, halved, invert, halved});
            lookup4bandbyte = new LookupOp(lut4, null);
        }

        {
            short invert[] = new short[256];
            short halved[] = new short[256];
            for (int j = 0; j < 256 ; j++) {
                invert[j] = (short) ((255-j) * 255);
                halved[j] = (short) ((j / 2) * 255);
            }
            ShortLookupTable lut1 = new ShortLookupTable(offset, invert);
            lookup1bandshort = new LookupOp(lut1, null);
            ShortLookupTable lut3 =
                new ShortLookupTable(offset,
                                     new short[][] {invert, halved, invert});
            lookup3bandshort = new LookupOp(lut3, null);
            ShortLookupTable lut4 =
                new ShortLookupTable(offset,
                              new short[][] {invert, halved, invert, halved});
            lookup4bandshort = new LookupOp(lut4, null);
        }

        // 3x3 blur
        float[] data3 = {
            0.1f, 0.1f, 0.1f,
            0.1f, 0.2f, 0.1f,
            0.1f, 0.1f, 0.1f,
        };
        Kernel k3 = new Kernel(3, 3, data3);

        // 5x5 edge
        float[] data5 = {
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 24.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,
        };
        Kernel k5 = new Kernel(5, 5, data5);

        // 7x7 blur
        float[] data7 = {
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
            0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f,
        };
        Kernel k7 = new Kernel(7, 7, data7);

        convolve3x3zero = new ConvolveOp(k3, ConvolveOp.EDGE_ZERO_FILL, null);
        convolve5x5zero = new ConvolveOp(k5, ConvolveOp.EDGE_ZERO_FILL, null);
        convolve7x7zero = new ConvolveOp(k7, ConvolveOp.EDGE_ZERO_FILL, null);

        convolve3x3noop = new ConvolveOp(k3, ConvolveOp.EDGE_NO_OP, null);
        convolve5x5noop = new ConvolveOp(k5, ConvolveOp.EDGE_NO_OP, null);
        convolve7x7noop = new ConvolveOp(k7, ConvolveOp.EDGE_NO_OP, null);
    }

    public void paint(Graphics g) {
        synchronized (this) {
            if (done) {
                return;
            }
        }

        VolatileImage vimg = createVolatileImage(TESTW, TESTH);
        vimg.validate(getGraphicsConfiguration());

        Graphics2D g2d = vimg.createGraphics();
        renderTest(g2d);
        g2d.dispose();

        g.drawImage(vimg, 0, 0, null);

        Toolkit.getDefaultToolkit().sync();

        synchronized (this) {
            done = true;
            notifyAll();
        }
    }

    /*
     * foreach source image size (once with pow2, once with non-pow2)
     *
     *   foreach BufferedImage type
     *
     *     RescaleOp (1 band)
     *     RescaleOp (3 bands, if src has 3 bands)
     *     RescaleOp (4 bands, if src has 4 bands)
     *
     *     foreach LookupTable type (once with ByteLUT, once with ShortLUT)
     *       LookupOp (1 band)
     *       LookupOp (3 bands, if src has 3 bands)
     *       LookupOp (4 bands, if src has 4 bands)
     *
     *     foreach edge condition (once with ZERO_FILL, once with EDGE_NO_OP)
     *       ConvolveOp (3x3)
     *       ConvolveOp (5x5)
     *       ConvolveOp (7x7)
     */
    private void renderTest(Graphics2D g2d) {
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, TESTW, TESTH);

        int yorig = 2;
        int xinc = 34;
        int yinc = srcSizes[0] + srcSizes[1] + 2 + 2;

        for (int srcType : srcTypes) {
            int y = yorig;

            for (int srcSize : srcSizes) {
                int x = 2;
                System.out.printf("type=%d size=%d\n", srcType, srcSize);

                BufferedImage srcImg = makeSourceImage(srcSize, srcType);
                ColorModel srcCM = srcImg.getColorModel();

                // RescaleOp
                g2d.drawImage(srcImg, rescale1band, x, y);
                x += xinc;
                // REMIND: 3-band RescaleOp.filter() throws IAE for images
                //         that contain an alpha channel (bug to be filed)
                if (srcCM.getNumColorComponents() == 3 &&
                    !(ignore && srcCM.hasAlpha()))
                {
                    g2d.drawImage(srcImg, rescale3band, x, y);
                }
                x += xinc;
                if (srcCM.getNumComponents() == 4) {
                    g2d.drawImage(srcImg, rescale4band, x, y);
                }
                x += xinc;

                // LookupOp
                // REMIND: Our LUTs are only 256 elements long, so won't
                //         currently work with USHORT_GRAY data
                if (srcType != BufferedImage.TYPE_USHORT_GRAY) {
                    g2d.drawImage(srcImg, lookup1bandbyte, x, y);
                    x += xinc;
                    if (srcCM.getNumColorComponents() == 3) {
                        g2d.drawImage(srcImg, lookup3bandbyte, x, y);
                    }
                    x += xinc;
                    if (srcCM.getNumComponents() == 4) {
                        g2d.drawImage(srcImg, lookup4bandbyte, x, y);
                    }
                    x += xinc;

                    // REMIND: LookupOp.createCompatibleDestImage() throws
                    //         IAE for 3BYTE_BGR/4BYTE_ABGR (bug to be filed)
                    if (!(ignore &&
                          (srcType == BufferedImage.TYPE_3BYTE_BGR ||
                           srcType == BufferedImage.TYPE_4BYTE_ABGR)))
                    {
                        g2d.drawImage(srcImg, lookup1bandshort, x, y);
                        x += xinc;
                        // REMIND: 3-band LookupOp.filter() throws IAE for
                        //         images that contain an alpha channel
                        //         (bug to be filed)
                        if (srcCM.getNumColorComponents() == 3 &&
                            !(ignore && srcCM.hasAlpha()))
                        {
                            g2d.drawImage(srcImg, lookup3bandshort, x, y);
                        }
                        x += xinc;
                        if (srcCM.getNumComponents() == 4) {
                            g2d.drawImage(srcImg, lookup4bandshort, x, y);
                        }
                        x += xinc;
                    } else {
                        x += 3*xinc;
                    }
                } else {
                    x += 6*xinc;
                }

                // ConvolveOp
                // REMIND: ConvolveOp.filter() throws ImagingOpException
                //         for 3BYTE_BGR (see 4957775)
                if (srcType != BufferedImage.TYPE_3BYTE_BGR) {
                    g2d.drawImage(srcImg, convolve3x3zero, x, y);
                    x += xinc;
                    g2d.drawImage(srcImg, convolve5x5zero, x, y);
                    x += xinc;
                    g2d.drawImage(srcImg, convolve7x7zero, x, y);
                    x += xinc;

                    g2d.drawImage(srcImg, convolve3x3noop, x, y);
                    x += xinc;
                    g2d.drawImage(srcImg, convolve5x5noop, x, y);
                    x += xinc;
                    g2d.drawImage(srcImg, convolve7x7noop, x, y);
                    x += xinc;
                } else {
                    x += 6*xinc;
                }

                y += srcSize + 2;
            }

            yorig += yinc;
        }
    }

    private BufferedImage makeSourceImage(int size, int type) {
        int s2 = size/2;
        BufferedImage img = new BufferedImage(size, size, type);
        Graphics2D g2d = img.createGraphics();
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(Color.orange);
        g2d.fillRect(0, 0, size, size);
        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, s2, s2);
        g2d.setColor(Color.green);
        g2d.fillRect(s2, 0, s2, s2);
        g2d.setColor(Color.blue);
        g2d.fillRect(0, s2, s2, s2);
        g2d.setColor(new Color(255, 255, 0, 128));
        g2d.fillRect(s2, s2, s2, s2);
        g2d.setColor(Color.pink);
        g2d.fillOval(s2-3, s2-3, 6, 6);
        g2d.dispose();
        return img;
    }

    public BufferedImage makeReferenceImage() {
        BufferedImage img = new BufferedImage(TESTW, TESTH,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        renderTest(g2d);
        g2d.dispose();
        return img;
    }

    public Dimension getPreferredSize() {
        return new Dimension(TESTW, TESTH);
    }

    private static void compareImages(BufferedImage refImg,
                                      BufferedImage testImg,
                                      int tolerance)
    {
        int x1 = 0;
        int y1 = 0;
        int x2 = refImg.getWidth();
        int y2 = refImg.getHeight();

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                Color expected = new Color(refImg.getRGB(x, y));
                Color actual   = new Color(testImg.getRGB(x, y));
                if (!isSameColor(expected, actual, tolerance)) {
                    throw new RuntimeException("Test failed at x="+x+" y="+y+
                                               " (expected="+expected+
                                               " actual="+actual+
                                               ")");
                }
            }
        }
    }

    private static boolean isSameColor(Color c1, Color c2, int e) {
        int r1 = c1.getRed();
        int g1 = c1.getGreen();
        int b1 = c1.getBlue();
        int r2 = c2.getRed();
        int g2 = c2.getGreen();
        int b2 = c2.getBlue();
        int rmin = Math.max(r2-e, 0);
        int gmin = Math.max(g2-e, 0);
        int bmin = Math.max(b2-e, 0);
        int rmax = Math.min(r2+e, 255);
        int gmax = Math.min(g2+e, 255);
        int bmax = Math.min(b2+e, 255);
        if (r1 >= rmin && r1 <= rmax &&
            g1 >= gmin && g1 <= gmax &&
            b1 >= bmin && b1 <= bmax)
        {
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        boolean show = false;
        boolean dump = false;
        boolean compare = false;

        for (String arg : args) {
            if (arg.equals("-show")) {
                show = true;
            } else if (arg.equals("-dump")) {
                dump = true;
            } else if (arg.equals("-compare")) {
                compare = true;
            } else if (arg.equals("-ignore")) {
                ignore = true;
            }
        }

        DrawBufImgOp test = new DrawBufImgOp();
        Frame frame = new Frame();
        frame.add(test);
        frame.pack();
        frame.setVisible(true);

        // Wait until the component's been painted
        synchronized (test) {
            while (!done) {
                try {
                    test.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Failed: Interrupted");
                }
            }
        }

        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        if (gc.getColorModel() instanceof IndexColorModel) {
            System.out.println("IndexColorModel detected: " +
                               "test considered PASSED");
            frame.dispose();
            return;
        }

        // Grab the screen region
        BufferedImage capture = null;
        try {
            Robot robot = new Robot();
            Point pt1 = test.getLocationOnScreen();
            Rectangle rect = new Rectangle(pt1.x, pt1.y, TESTW, TESTH);
            capture = robot.createScreenCapture(rect);
        } catch (Exception e) {
            throw new RuntimeException("Problems creating Robot");
        } finally {
            if (!show) {
                frame.dispose();
            }
        }

        // Compare the images (allow for +/- 1 bit differences in color comps)
        if (dump || compare) {
            BufferedImage ref = test.makeReferenceImage();
            if (dump) {
                ImageIO.write(ref,     "png",
                              new File("DrawBufImgOp.ref.png"));
                ImageIO.write(capture, "png",
                              new File("DrawBufImgOp.cap.png"));
            }
            if (compare) {
                test.compareImages(ref, capture, 1);
            }
        }
    }
}
