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

/**
 * @test
 * @bug     6557713
 * @summary Test verifies that PNG image writer correctly handles indexed images with
 *          various types of transparency.
 *
 * Test for 4bpp OPAQUE image
 * @run     main GrayPngTest 4 1 3
 *
 * Test for 4bpp BITMASK image with transparent pixel 3
 * @run     main GrayPngTest 4 2 3
 *
 * Test for 4bpp TRANSLUCENT image
 * @run     main GrayPngTest 4 3 3
 *
 * Test for 8bpp OPAQUE image
 * @run     main GrayPngTest 8 1 127
 *
 * Test for 8bpp BITMASK image with transparent pixel 127
 * @run     main GrayPngTest 8 2 127
 *
 * Test for 8bpp TRANSLUCENT image
 * @run     main GrayPngTest 8 3 127
 *
 */

import java.awt.Color;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class GrayPngTest {

    public static void main(String[] args) throws IOException {
        /*
         * Expected argiments:
         * args[0] - bits per pixel. Supported range: [1, 8]
         * args[1] - transparency type. Should be one form
         *           java.awt.Transparency type constants.
         * args[2] - transparent pixel for BITMASK transparency type,
         *           otherwise is ignored.
         */
        int bpp = 4;
        int trans_type = Transparency.BITMASK;
        int trans_pixel = 3;
        try {
            bpp = Integer.parseInt(args[0]);
            trans_type = Integer.parseInt(args[1]);
            trans_pixel = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("Ignore ncorrect bpp value: " + args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Default test argumens.");
        }

        new GrayPngTest(bpp).doTest(trans_type, trans_pixel);
    }



    private BufferedImage getTestImage(int trans_type, int trans_pixel) {

        IndexColorModel icm = null;
        switch(trans_type) {
            case Transparency.OPAQUE:
                icm = new IndexColorModel(bpp, numColors, r, g, b);
                break;
            case Transparency.BITMASK:
                icm = new IndexColorModel(bpp, numColors, r, g, b, trans_pixel);
                break;
            case Transparency.TRANSLUCENT:
                a = Arrays.copyOf(r, r.length);
                icm = new IndexColorModel(bpp, numColors, r, g, b, a);
                break;
            default:
                throw new RuntimeException("Invalid transparency: " + trans_type);
        }

        int w = 256 * 2;
        int h = 200;

        dx = w / (numColors);

        WritableRaster wr = icm.createCompatibleWritableRaster(w, h);
        for (int i = 0; i < numColors; i ++) {
            int rx = i * dx;

            int[] samples = new int[h * dx];
            Arrays.fill(samples, i);
            wr.setPixels(rx, 0, dx, h, samples);
        }

        // horizontal line with transparent color
        int[] samples = new int[w * 10];
        Arrays.fill(samples, trans_pixel);
        wr.setPixels(0, h / 2 - 5, w, 10, samples);

        // create index color model
        return new BufferedImage(icm, wr, false, null);
    }

    static File pwd = new File(".");

    private BufferedImage src;
    private BufferedImage dst;
    private int bpp;
    private int numColors;

    private int dx;

    private byte[] r;
    private byte[] g;
    private byte[] b;

    private byte[] a;

    protected GrayPngTest(int bpp) {
        if (0 > bpp || bpp > 8) {
            throw new RuntimeException("Invalid bpp: " + bpp);
        }
        this.bpp = bpp;
        numColors = (1 << bpp);
        System.out.println("Num colors: " + numColors);

        // create palette
        r = new byte[numColors];
        g = new byte[numColors];
        b = new byte[numColors];

        int dc = 0xff / (numColors - 1);
        System.out.println("dc = " + dc);

        for (int i = 0; i < numColors; i ++) {
            byte l = (byte)(i * dc);
            r[i] = l; g[i] = l; b[i] = l;
        }
    }

    public void doTest() throws IOException {
        for (int i = 0; i < numColors; i++) {
            doTest(Transparency.BITMASK, i);
        }
    }

    public void doTest(int trans_type, int trans_index) throws IOException {
        src = getTestImage(trans_type, trans_index);

        System.out.println("src: " + src);

        File f = File.createTempFile("gray_png_" + bpp + "bpp_" +
              trans_type + "tt_" +
                    trans_index + "tp_", ".png", pwd);
        System.out.println("File: " + f.getAbsolutePath());
        if (!ImageIO.write(src, "png", f)) {
            throw new RuntimeException("Writing failed!");
        };

        try {
            dst = ImageIO.read(f);
            System.out.println("dst: " + dst);
        } catch (Exception e) {
            throw new RuntimeException("Test FAILED.", e);
        }

        checkImages();
    }

    private void checkImages() {
        for (int i = 0; i < numColors; i++) {
            int src_rgb = src.getRGB(i * dx, 5);
            int dst_rgb = dst.getRGB(i * dx, 5);

            // here we check transparency only due to possible colors space
            // differences (sRGB in indexed source and Gray in gray+alpha destination)
            if ((0xff000000 & src_rgb) != (0xff000000 & dst_rgb)) {
                throw new RuntimeException("Test FAILED. Color difference detected: " +
                        Integer.toHexString(dst_rgb) + " instead of " +
                        Integer.toHexString(src_rgb) + " for index " + i);

            }
        }
    }
}
