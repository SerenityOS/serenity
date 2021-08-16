/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4888478
 * @summary Test that colors do not distort when buffered image with
 *          premultiplied alpha is encoded to png format
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public class PngPremultAlphaTest {
    protected static int width = 100;
    protected static int height = 100;

    protected static String format = "png";

    protected static int[] iBufferedImageTypes = {
        BufferedImage.TYPE_INT_RGB,
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_4BYTE_ABGR_PRE,
        BufferedImage.TYPE_INT_ARGB_PRE
    };

    protected static String[] strBufferedImageTypes = {
        "TYPE_INT_RGB",
        "TYPE_INT_ARGB",
        "BufferedImage.TYPE_4BYTE_ABGR_PRE",
        "BufferedImage.TYPE_INT_ARGB_PRE"
    };

    public static void main(String[] arg) {
        for(int i=0; i<iBufferedImageTypes.length; i++) {
            System.out.println("Test for " + strBufferedImageTypes[i]);
            doTest(iBufferedImageTypes[i]);
        }
    }

    public static void doTest(int type) {
        try {
            BufferedImage src = new BufferedImage(100, 100,
                                                  type);
            Graphics2D g = src.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
            g.setColor(new Color(0x20, 0x40, 0x60));
            g.fillRect(0,0,100,100);

            int[] samples = new int[src.getData().getNumBands()];
            src.getData().getPixels(0,0,1,1,samples);
            for(int i=0; i<samples.length; i++) {
                System.out.println("sample["+i+"]="+Integer.toHexString(samples[i]));
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(src, format, baos);
            baos.close();


            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            BufferedImage dst = ImageIO.read(bais);

            isSameColors(src, dst);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed.");
        }
    }

    private static boolean isSameColors(BufferedImage src, BufferedImage dst) {
        Object dstPixel = dst.getRaster().getDataElements(width/2, height/2, null);
        Object srcPixel = src.getRaster().getDataElements(width/2, height/2, null);

        // take into account the rounding error
        if ( Math.abs(src.getColorModel().getRed(srcPixel) -  dst.getColorModel().getRed(dstPixel)) > 1
             || Math.abs(src.getColorModel().getGreen(srcPixel) - dst.getColorModel().getGreen(dstPixel)) > 1
             || Math.abs(src.getColorModel().getBlue(srcPixel) - dst.getColorModel().getBlue(dstPixel)) > 1) {
            showPixel(src, width/2, height/2);
            showPixel(dst, width/2, height/2);

            throw new RuntimeException( "Colors are different: "
                                        + Integer.toHexString(src.getColorModel().getRGB(srcPixel))
                                        + " and "
                                        + Integer.toHexString(dst.getColorModel().getRGB(dstPixel)));
        }
        return true;
    }

    private static void showPixel(BufferedImage src, int x, int y) {
        System.out.println("Img is " + src);
        System.out.println("CM is " + src.getColorModel().getClass().getName());
        Object p = src.getRaster().getDataElements(x, y, null);
        System.out.println("RGB:   " +
                           Integer.toHexString(src.getColorModel().getRGB(p)));
        System.out.println("Red:   " +
                           Integer.toHexString(src.getColorModel().getRed(p)));
        System.out.println("Green: " +
                           Integer.toHexString(src.getColorModel().getGreen(p)));
        System.out.println("Blue:  " +
                           Integer.toHexString(src.getColorModel().getBlue(p)));
        System.out.println("Alpha: " +
                           Integer.toHexString(src.getColorModel().getAlpha(p)));
    }
}
