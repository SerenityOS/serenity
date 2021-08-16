/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6476665 7033534 6830714 8052162 8196572
 * @summary Verifies color conversion of Component Color Model based images
 * @run main ColConvCCMTest
 */

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
public class ColConvCCMTest extends ColConvTest {

    final static int [] dataTypes = {
        DataBuffer.TYPE_BYTE,
        DataBuffer.TYPE_DOUBLE,
        DataBuffer.TYPE_FLOAT,
        DataBuffer.TYPE_INT,
        DataBuffer.TYPE_SHORT,
        DataBuffer.TYPE_USHORT
    };

    final static int [] cSpaces = {
        ColorSpace.CS_sRGB,
        ColorSpace.CS_LINEAR_RGB,
        ColorSpace.CS_GRAY,
        ColorSpace.CS_PYCC,
        ColorSpace.CS_CIEXYZ
    };

    final static double [] ACCURACY = {
    // Accuracy for color conversions
        2.5,        // sRGB
        (isOpenProfile() ? 45.0 : 10.1), // LINEAR_RGB
        10.5,       // GRAY
        (isOpenProfile() ? 215.0 : 45.5), // PYCC
        (isOpenProfile() ? 56.0 : 47.5) // CIEXYZ
    };

    final static String [] gldImgNames = {
        "SRGB.png", "LRGB.png", "GRAY.png", "PYCC.png",  "CIEXYZ.png"
    };

    static BufferedImage [] gldImages = null;

    static boolean testImage(int dataType, int rBits, int gBits, int bBits,
                              int cs, BufferedImage gldImage,
                              double accuracy)
     {
        BufferedImage src = ImageFactory.createCCMImage(cs, dataType);
        BufferedImage dst = ImageFactory.createDstImage(
            BufferedImage.TYPE_INT_RGB);
        ColorConvertOp op = new ColorConvertOp(null);
        op.filter(src, dst);

        ImageComparator cmp = new ImageComparator(accuracy, rBits, gBits,
                                                  bBits);
        boolean result = cmp.compare(gldImage, dst);
        if (!result) {
            System.err.println(cmp.getStat());
        }
        return result;
    }

     static boolean testSubImage(int x0, int y0, int dx, int dy,
                                 int dataType, int rBits, int gBits,
                                 int bBits, int cs, BufferedImage gldImage,
                                 double accuracy)
     {
        BufferedImage src = ImageFactory.createCCMImage(cs, dataType);
        BufferedImage subSrc = src.getSubimage(x0, y0, dx, dy);
        BufferedImage dst = ImageFactory.createDstImage(
            BufferedImage.TYPE_INT_RGB);
        BufferedImage subDst = dst.getSubimage(x0, y0, dx, dy);
        ColorConvertOp op = new ColorConvertOp(null);

        op.filter(subSrc, subDst);
        ImageComparator cmp = new ImageComparator(accuracy, rBits, gBits,
                                                  bBits);
        boolean result = cmp.compare(subDst, gldImage, x0, y0, dx, dy);
        if (!result) {
            System.err.println(cmp.getStat());
        }
        return result;
    }
     synchronized public static void initGoldenImages() {
        if (gldImages == null) {
            gldImages = new BufferedImage[gldImgNames.length];
            for (int i = 0; i < gldImgNames.length; i++) {
                try {
                    File gldFile = new File(System.getProperty("test.src", "."),
                                            gldImgNames[i]);

                    gldImages[i] = ImageIO.read(gldFile);
                } catch (IOException e) {
                    throw new RuntimeException("Cannot initialize golden " +
                                               "image: " + gldImgNames[i]);
                }
            }
        }
     }

     public void init() {
        initGoldenImages();
     }

     public void runTest() {
        for (int i = 0; i < cSpaces.length; i++) {
            BufferedImage gldImage = gldImages[i];
            for (int j = 0; j < dataTypes.length; j++) {
                if (!testImage(dataTypes[j], 8, 8, 8, cSpaces[i], gldImage,
                               ACCURACY[i]))
                {
                     throw new RuntimeException(
                        "Invalid result of the ColorConvertOp for " +
                        "ColorSpace:" + getCSName(cSpaces[i]) +
                        " Data type:" +
                        getDTName(dataTypes[j]) + ". Golden image:" +
                        gldImages[i]);
                 }

                 if (!testSubImage(SI_X, SI_Y, SI_W, SI_H, dataTypes[j],
                                   8, 8, 8, cSpaces[i], gldImage, ACCURACY[i]))
                 {
                    throw new RuntimeException(
                        "Invalid result of the ColorConvertOp for " +
                        "ColorSpace:" + getCSName(cSpaces[i]) +
                        " Data type:" +
                        getDTName(dataTypes[j]) + ". Golden image:" +
                        gldImages[i]);
                 }
            }
        }
     }

     public static void main(String [] args) throws Exception {
         ColConvCCMTest test = new ColConvCCMTest();
         test.init();
         test.run();
     }
}
