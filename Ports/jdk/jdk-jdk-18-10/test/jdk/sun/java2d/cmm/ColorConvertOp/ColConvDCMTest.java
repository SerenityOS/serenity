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
 * @bug 6476665
 * @summary Verifies color conversion of Direct Color Model based images
 * @run main ColConvDCMTest
 */

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ColConvDCMTest extends ColConvTest {

    /*
     * Test case descriptors: <imgType> <rBits> <gBits> <bBits> <csNum> <gldNum>
     */
    final static int [][] imgTypes = {
        {BufferedImage.TYPE_INT_ARGB, 8, 8, 8, 0, 0},
        {BufferedImage.TYPE_INT_ARGB, 8, 8, 8, 1, 3},

        {BufferedImage.TYPE_INT_RGB, 8, 8, 8, 0, 0},
        {BufferedImage.TYPE_INT_RGB, 8, 8, 8, 1, 3},

        {BufferedImage.TYPE_INT_BGR, 8, 8, 8, 0, 0},
        {BufferedImage.TYPE_INT_BGR, 8, 8, 8, 1, 3},

        {BufferedImage.TYPE_USHORT_555_RGB, 5, 5, 5, 0, 1},
        {BufferedImage.TYPE_USHORT_555_RGB, 5, 5, 5, 1, 4},

        {BufferedImage.TYPE_USHORT_565_RGB, 5, 6, 5, 0, 2},
        {BufferedImage.TYPE_USHORT_565_RGB, 5, 6, 5, 1, 5}
    };

    final static int [] cSpaces = {
        ColorSpace.CS_sRGB,
        ColorSpace.CS_LINEAR_RGB,
    };

    final static double [] ACCURACY = {
        // Accuracy for color conversions
        2.5, // sRGB
        (isOpenProfile() ? 45.0 : 2.5), // LINEAR_RGB
    };

    final static String [] gldImgNames = {
        "SRGB.png", "SRGB555.png", "SRGB565.png", "LRGB.png", "LRGB555.png",
        "LRGB565.png"
    };

    static BufferedImage [] gldImages = null;

    static boolean testImage(int type, int rBits, int gBits, int bBits,
                              int cs, BufferedImage gldImage,
                              double accuracy)
    {
        BufferedImage src = ImageFactory.createDCMImage(type, cs);
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

     static boolean testSubImage(int x0, int y0, int dx, int dy, int type,
                                 int rBits, int gBits, int bBits,
                                 int cs, BufferedImage gldImage,
                                 double accuracy)
     {
        BufferedImage src = ImageFactory.createDCMImage(type, cs);
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
        for (int i = 0; i < imgTypes.length; i++) {
            BufferedImage gldImage = gldImages[imgTypes[i][5]];

            if (!testImage(imgTypes[i][0], imgTypes[i][1], imgTypes[i][2],
                           imgTypes[i][3], cSpaces[imgTypes[i][4]], gldImage,
                           ACCURACY[imgTypes[i][4]]))
            {
                throw new RuntimeException(
                    "Invalid result of the ColorConvertOp for " +
                    "ColorSpace:" + getCSName(cSpaces[imgTypes[i][4]]) +
                    " Image type:" +
                    getImageTypeName(imgTypes[i][0]) + ". Golden image:" +
                    gldImgNames[imgTypes[i][5]]);
            }

            if (!testSubImage(SI_X, SI_Y, SI_W, SI_H, imgTypes[i][0],
                              imgTypes[i][1], imgTypes[i][2], imgTypes[i][3],
                              cSpaces[imgTypes[i][4]], gldImage,
                              ACCURACY[imgTypes[i][4]]))
            {
                throw new RuntimeException(
                    "Invalid result of the ColorConvertOp for " +
                     "ColorSpace:" + getCSName(cSpaces[imgTypes[i][4]]) +
                     " Image type:" +
                     getImageTypeName(imgTypes[i][0]) + ". Golden image:" +
                     gldImgNames[imgTypes[i][5]]);
            }
        }
     }

     public static void main(String [] args) throws Exception {
         ColConvDCMTest test = new ColConvDCMTest();
         test.init();
         test.run();
     }
}
