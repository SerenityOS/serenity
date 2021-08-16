/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4994702
 * @key headful
 * @summary verifies that no regression were introduced with the fix for this
 *          bug
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageOutputStream;

public class JPEGsNotAcceleratedTest {

    public static final int testRGB = Color.red.getRGB();
    public static final int TEST_W = 100;
    public static final int TEST_H = 100;
    public static final Rectangle roi =
        new Rectangle(TEST_W/4, TEST_H/4, TEST_W/2, TEST_H/2);

    static Frame f;
    static boolean showRes = false;
    static int testsFinished = 0;
    static int testsStarted = 0;
    static boolean lowCompression = false;

    static boolean failed = false;

    public JPEGsNotAcceleratedTest(String name) {
        BufferedImage bi = readTestImage(name, null, null);
        runTestOnImage("no dest image, no region of interest", bi, null);

        BufferedImage destBI = getDestImage();
        bi = readTestImage(name, destBI, null);
        runTestOnImage("w/ dest image, no region of interest", bi, null);

        // steal the raster, see if the destination image
        // gets accelerated
        destBI = getDestImage();
        DataBuffer db =
            ((WritableRaster)destBI.getRaster()).getDataBuffer();
        bi = readTestImage(name, destBI, null);
        runTestOnImage("w/ dest image (with stolen raster),"+
                       " no region of interest", bi, null);

        bi = readTestImage(name, null, roi);
        runTestOnImage("no dest image, region of interest", bi, roi);

        destBI = getDestImage();
        bi = readTestImage(name, destBI, roi);
        runTestOnImage("w/ dest image, region of interest", bi, roi);

        // accelerate the destination image first, then load
        // an image into it. Check that the accelerated copy gets
        // updated
        destBI = getDestImage();
        accelerateImage(destBI);
        bi = readTestImage(name, destBI, roi);
        runTestOnImage("w/ accelerated dest image,"+
                       " region of interest", bi, roi);

        synchronized (JPEGsNotAcceleratedTest.class) {
            testsFinished++;
            JPEGsNotAcceleratedTest.class.notify();
        }

    }

    public static BufferedImage readTestImage(String fileName,
                                   BufferedImage dest,
                                   Rectangle srcROI)
    {
        BufferedImage bi = null;

        try {
            FileImageInputStream is =
                new FileImageInputStream(new File(fileName));
            ImageReader reader =
                (ImageReader)ImageIO.getImageReaders(is).next();
            ImageReadParam param = reader.getDefaultReadParam();
            if (dest != null) {
                param.setDestination(dest);
            }
            if (srcROI != null) {
                param.setSourceRegion(srcROI);
            }
            reader.setInput(is);
            bi = reader.read(0, param);
        } catch (IOException e) {
            System.err.println("Error " + e +
                               " when reading file: " + fileName);
            throw new RuntimeException(e);
        }

        return bi;
    }

    public static void writeTestImage(String fileName) {
        BufferedImage bi =
            new BufferedImage(TEST_W, TEST_H, BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.setColor(new Color(testRGB));
        g.fillRect(0, 0, TEST_W, TEST_H);
        try {
            System.err.printf("Writing %s\n", fileName);
            if (lowCompression) {
                ImageWriter iw = (ImageWriter)ImageIO.getImageWritersBySuffix("jpeg").next();
                if(iw == null) {
                    throw new RuntimeException("No available image writer for "
                                               + "jpeg "
                                               + " Test failed.");
                }

                File file = new File(fileName);
                ImageOutputStream ios = ImageIO.createImageOutputStream(file);
                iw.setOutput(ios);

                ImageWriteParam param = iw.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(1);

                IIOImage iioImg = new IIOImage(bi, null, null);
                iw.write(null, iioImg, param);

            } else {
                ImageIO.write(bi, "jpeg", new File(fileName));
            }
        } catch (IOException e) {
            System.err.println("Error " + e +
                               " when writing file: " + fileName);
            throw new RuntimeException(e);
        }
    }

    public VolatileImage accelerateImage(BufferedImage bi) {
        VolatileImage testVI = f.createVolatileImage(TEST_W, TEST_H);
        do {
            if (testVI.validate(f.getGraphicsConfiguration()) ==
                VolatileImage.IMAGE_INCOMPATIBLE)
            {
                testVI = f.createVolatileImage(TEST_W, TEST_H);
            }
            Graphics2D g = testVI.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setColor(Color.green);
            g.fillRect(0, 0, TEST_W, TEST_H);

            g.drawImage(bi, 0, 0, null);
            g.drawImage(bi, 0, 0, null);
            g.drawImage(bi, 0, 0, null);
            g.dispose();
        } while (testVI.contentsLost());

        return testVI;
    }

    public BufferedImage getDestImage() {
        BufferedImage destBI =
            new BufferedImage(TEST_W, TEST_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)destBI.getGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.blue);
        g.fillRect(0, 0, TEST_W, TEST_H);
        return destBI;
    }

    public void runTestOnImage(String desc, BufferedImage bi,
                               Rectangle srcROI)
    {

        if (srcROI == null) {
            srcROI = new Rectangle(0, 0, TEST_W, TEST_H);
        }

        VolatileImage testVI = accelerateImage(bi);

        ImageCapabilities ic =
            bi.getCapabilities(f.getGraphicsConfiguration());
        boolean accelerated = ic.isAccelerated();

        System.err.println("Testing: " + desc +
                           " -- bi.isAccelerated(): " + accelerated );

        BufferedImage snapshot = testVI.getSnapshot();
        if (showRes) {
            showRes(desc, snapshot);
        }

        for (int y = 0; y < srcROI.height; y++) {
            for (int x = 0; x < srcROI.width; x++) {
                int destRGB = snapshot.getRGB(x, y);
                if (destRGB != testRGB && destRGB != 0xfffe0000) {
                    failed = true;
                    System.err.printf("Test failed at %dx%d pixel=%x\n",
                                      x, y, snapshot.getRGB(x, y));
                    if (!showRes) {
                        showRes(desc, snapshot);
                    }
                    break;
                }
            }
        }
    }

    static int startX = 64, startY = 0;
    static int frameX = startX, frameY = startY;
    private static void showRes(String desc, final BufferedImage src) {
        final int w = src.getWidth();
        final int h = src.getHeight();

        Frame f = new Frame(desc+": dbl-click to exit");
        Component c;
        f.add(c = new Component() {
            public Dimension getPreferredSize() {
                return new Dimension(w,h);
            }

            public void paint(Graphics g) {
                g.clearRect(0, 0, getWidth(), getHeight());
                g.drawImage(src, 0,0, null);
            }
        });
        c.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    System.exit(0);
                }
            }
        });
        f.pack();
        synchronized (JPEGsNotAcceleratedTest.class) {
            f.setLocation(frameX, frameY);
            frameX += f.getWidth();
            if ((frameX + f.getWidth()) >
                f.getGraphicsConfiguration().getBounds().width)
            {
                frameY += TEST_H;
                if ((frameY + f.getHeight()) >
                    f.getGraphicsConfiguration().getBounds().height)
                {
                    startY += 30;
                    startX += 30;
                    frameY = startY;
                }
                frameX = startX;
            }
        };
        f.setVisible(true);
    }

    public static void usage() {
        System.err.println("Usage: java Test [file name] [-write][-show][-low]");
        System.exit(0);
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.pmoffscreen", "true");
        System.setProperty("sun.java2d.ddforcevram", "true");
        String name = "red.jpg";

        f = new Frame();
        f.pack();

        if (f.getGraphicsConfiguration().getColorModel().getPixelSize() < 16) {
            System.err.println("8-bit display mode detected, dithering issues possible, "+
                               "considering test passed.");
            f.dispose();
            return;
        }


        for (String arg : args) {
            if (arg.equals("-write")) {
                writeTestImage(name);
                System.exit(0);
            } else if (arg.equals("-show")) {
                showRes = true;
            } else if (arg.equals("-low")) {
                lowCompression = true;
                name ="red_low.jpg";
                System.err.println("Using low jpeg compression");
            } else if (arg.equals("-help")) {
                usage();
            } else {
                final String filename = arg;
                testsStarted++;
                new Thread(new Runnable() {
                    public void run() {
                        new JPEGsNotAcceleratedTest(filename);
                    }
                }).start();
            }
        }

        if (testsStarted == 0) {
            writeTestImage(name);
            testsStarted++;
            new JPEGsNotAcceleratedTest(name);
        }


        synchronized (JPEGsNotAcceleratedTest.class) {
            while (testsFinished < testsStarted) {
                try {
                    JPEGsNotAcceleratedTest.class.wait(100);
                } catch (InterruptedException e) {
                    failed = true; break;
                }
            }
        }

        f.dispose();
        if (failed) {
            throw new RuntimeException("Test failed");
        }

        System.err.println("Passed.");
    }
}
