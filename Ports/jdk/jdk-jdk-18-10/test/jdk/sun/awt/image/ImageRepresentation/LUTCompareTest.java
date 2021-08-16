/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6570475
 * @summary Test verifies that palette comparison procedure of
 *          ImageRepresentation class does not produce extra transparent
 *          pixels.
 *
 * @run     main LUTCompareTest
 */


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.ImageObserver;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;
import javax.swing.JFrame;

public class LUTCompareTest implements ImageObserver {

    public static void main(String[] args) throws IOException {
        Image img = createTestImage();

        Toolkit tk = Toolkit.getDefaultToolkit();

        LUTCompareTest o = new LUTCompareTest(img);

        tk.prepareImage(img, -1, -1, o);

        while(!o.isImageReady()) {
            synchronized(lock) {
                try {
                    lock.wait(200);
                } catch (InterruptedException e) {
                }
            }
        }

        checkResults(img);
    }

    private static Object lock = new Object();

    Image image;

    boolean isReady = false;

    public LUTCompareTest(Image img) {
        this.image = img;
    }

    public boolean imageUpdate(Image image, int info,
                               int x, int y, int w, int h) {
        if (image == this.image) {
            System.out.println("Image status: " + dump(info));
            synchronized(this) {
                isReady = (info & ImageObserver.ALLBITS) != 0;
                    if (isReady) {
                        synchronized(lock) {
                            lock.notifyAll();
                        }
                    }
            }
            return !isReady;
        } else {
            return true;
        }
    }

    public synchronized boolean isImageReady() {
        return isReady;
    }

    private static void checkResults(Image image) {
        BufferedImage buf = new BufferedImage(w, h,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buf.createGraphics();
        g.setColor(Color.pink);
        g.fillRect(0, 0, w, h);

        g.drawImage(image, 0, 0, null);

        g.dispose();

        int rgb = buf.getRGB(w/2, h/2);

        System.out.printf("Result color: %x\n", rgb);

        /* Buffered image should be the same as the last frame
         * of animated sequence (which is filled with blue).
         * Any other color indicates the problem.
         */
        if (rgb != 0xff0000ff) {
            throw new RuntimeException("Test FAILED!");
        }

        System.out.println("Test PASSED.");
    }

    private static int w = 100;
    private static int h = 100;

    /* Create test image with two frames:
     *  1) with {red, red} palette
     *  2) with {blue, red } palette
     */
    private static Image createTestImage() throws IOException  {
        BufferedImage frame1 = createFrame(new int[] { 0xffff0000, 0xffff0000 });
        BufferedImage frame2 = createFrame(new int[] { 0xff0000ff, 0xffff0000 });

        ImageWriter writer = ImageIO.getImageWritersByFormatName("GIF").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(new File("lut_test.gif"));
        ImageWriteParam param = writer.getDefaultWriteParam();
        writer.setOutput(ios);
        writer.prepareWriteSequence(null);
        writer.writeToSequence(new IIOImage(frame1, null, null), param);
        writer.writeToSequence(new IIOImage(frame2, null, null), param);
        writer.endWriteSequence();
        writer.reset();
        writer.dispose();

        ios.flush();
        ios.close();

        return Toolkit.getDefaultToolkit().createImage("lut_test.gif");
    }

    private static BufferedImage createFrame(int[] palette) {
        IndexColorModel icm = new IndexColorModel(getNumBits(palette.length),
            palette.length, palette, 0, false, -1, DataBuffer.TYPE_BYTE);
        WritableRaster wr = icm.createCompatibleWritableRaster(w, h);
        int[] samples = new int[w * h];
        Arrays.fill(samples, 0);
        wr.setSamples(0, 0, w, h, 0, samples);

        BufferedImage img = new BufferedImage(icm, wr, false, null);
        return img;
    }

    private static int getNumBits(int size) {
        if (size < 0) {
            throw new RuntimeException("Invalid palette size: " + size);
        } else if (size < 3) {
            return 1;
        } else if (size < 5) {
            return 2;
        } else {
            throw new RuntimeException("Palette size is not supported: " + size);
        }
    }

     private static String[] name = new String[] {
        "WIDTH", "HEIGHT", "PROPERTIES", "SOMEBITS",
        "FRAMEBITS", "ALLBITS", "ERROR", "ABORT"
    };

    private static String dump(int info) {
        String res = "";
        int count = 0;
        while (info != 0) {
            //System.out.println("info = " + info);
            if ((info & 0x1) == 1) {
                res += name[count];
                if ((info >> 1) != 0) {
                    res += " ";
                }

            }
            count ++;
            info = (info >> 1);
        }
        return res;
    }
}
