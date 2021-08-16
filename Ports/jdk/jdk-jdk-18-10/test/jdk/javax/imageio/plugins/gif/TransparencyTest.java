/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4339415
 * @summary Tests that GIF writer plugin is able to write images with BITMASK
 *          transparency
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

public class TransparencyTest {

    protected static final String fname = "ttest.gif";
    protected BufferedImage src;
    protected BufferedImage dst;

    public static void main(String[] args) {
        System.out.println("Test indexed image...");
        IndexColorModel icm = createIndexedBitmaskColorModel();
        BufferedImage img = createIndexedImage(200, 200, icm);
        TransparencyTest t = new TransparencyTest(img);

        try {
            t.doTest();
        } catch (Exception e) {
            throw new RuntimeException("Test failed!", e);
        }
        System.out.println("Test passed.");
    }

    protected TransparencyTest(BufferedImage src) {
        this.src = src;
    }

    protected void doTest() throws IOException {
        int w = src.getWidth();
        int h = src.getHeight();

        System.out.println("Write image...");
        try {
            ImageWriter writer =
                ImageIO.getImageWritersByFormatName("GIF").next();
            writer.setOutput(ImageIO.createImageOutputStream(new File(fname)));
            writer.write(src);
        } catch (Exception e) {
            throw new RuntimeException("Test failed.", e);
        }
        System.out.println("Read image....");
        dst = ImageIO.read(new File(fname));

        BufferedImage tmp = new BufferedImage(w, 2 * h,
                                              BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tmp.createGraphics();
        g.setColor(Color.pink);
        g.fillRect(0, 0, tmp.getWidth(), tmp.getHeight());

        g.drawImage(src, 0, 0, null);
        g.drawImage(dst, 0, h, null);

        int width = w / 8;
        int x = 5 * width + width / 2;
        for (int y = 0; y < h; y++) {
            int argb = tmp.getRGB(x, y);
            if (Color.pink.getRGB() != argb) {
                throw new RuntimeException("Bad color at " + x + "," + y +
                                           " - " + Integer.toHexString(argb));
            }
        }
    }

    protected static BufferedImage createIndexedImage(int w, int h,
                                                      IndexColorModel icm)
    {
        BufferedImage img = new BufferedImage(w, h,
                                              BufferedImage.TYPE_BYTE_INDEXED,
                                              icm);

        int mapSize = icm.getMapSize();
        int width = w / mapSize;

        WritableRaster wr = img.getRaster();
        for (int i = 0; i < mapSize; i++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < width; x++) {
                    wr.setSample(i * width + x, y, 0, i);
                }
            }
        }
        return img;
    }

    protected  static IndexColorModel createIndexedBitmaskColorModel() {
        int paletteSize = 8;
        byte[] red = new byte[paletteSize];
        byte[] green = new byte[paletteSize];
        byte[] blue = new byte[paletteSize];

        red[0] = (byte)0xff; green[0] = (byte)0x00; blue[0] = (byte)0x00;
        red[1] = (byte)0x00; green[1] = (byte)0xff; blue[1] = (byte)0x00;
        red[2] = (byte)0x00; green[2] = (byte)0x00; blue[2] = (byte)0xff;
        red[3] = (byte)0xff; green[3] = (byte)0xff; blue[3] = (byte)0xff;
        red[4] = (byte)0x00; green[4] = (byte)0x00; blue[4] = (byte)0x00;
        red[5] = (byte)0x80; green[5] = (byte)0x80; blue[5] = (byte)0x80;
        red[6] = (byte)0xff; green[6] = (byte)0xff; blue[6] = (byte)0x00;
        red[7] = (byte)0x00; green[7] = (byte)0xff; blue[7] = (byte)0xff;

        int numBits = 3;

        IndexColorModel icm = new IndexColorModel(numBits, paletteSize,
                                                  red, green, blue, 5);

        return icm;
    }
}
