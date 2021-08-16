/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429934 4429950 4430991 4430993
 * @summary Checks various aspects of ImageTypeSpecifier functionality
 */

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

import javax.imageio.ImageTypeSpecifier;

public class ImageTypeSpecifierTest {

    private static void fail(String message) {
        throw new RuntimeException(message);
    }

    private static void test4429934() {
        try {
            ImageTypeSpecifier itspecifier =
                new ImageTypeSpecifier(null, null);
            fail("Failed to get IAE!");
        } catch( IllegalArgumentException e ) {
        }

        try {
            ImageTypeSpecifier itspecifier = new ImageTypeSpecifier(null);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e ) {
        }
    }

    private static void test4429950() {
        createPackedTest();
        createInterleavedTest();
        createBandedTest();
        createIndexedTest();
    }

    public static void createPackedTest() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int rmask = 0x00ff0000;
        int gmask = 0x0000ff00;
        int bmask = 0x000000ff;
        int amask = 0xff000000;
        try {
            ImageTypeSpecifier.createPacked(null, rmask, gmask, bmask, amask, 0,
false);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        ColorSpace cs1 = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        try {
            ImageTypeSpecifier.createPacked
                (cs1, rmask, gmask, bmask, amask, 0, false);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createPacked(cs, 0, 0, 0, 0, 0, false);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createPacked(cs, rmask, gmask, bmask, amask, -1,
false);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static void createInterleavedTest() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] bandOffsets = {0,0,0,0};
        int dataType = 0;
        boolean hasAlpha = true;
        boolean isAlphaPremultiplied = true;

        try {
            ImageTypeSpecifier.createInterleaved
                (null, bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createInterleaved
                (cs, null, dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        int[] bad_bandOffsets = {0,100,1000};
        try {
            ImageTypeSpecifier.createInterleaved
                (cs, bad_bandOffsets, dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        int[] bad_bandOffsets_1 = {};
        try {
            ImageTypeSpecifier.createInterleaved
                (cs, bad_bandOffsets_1, dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static void createBandedTest() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        int[] bankIndices = {0, 100, 1000, 10000};
        int[] bandOffsets = {0, 100, 1000, 10000};
        int dataType = 0;
        boolean hasAlpha = true;
        boolean isAlphaPremultiplied = true;

        try {
            ImageTypeSpecifier.createBanded(null, bankIndices, bandOffsets,
                dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        int[] bad_bankIndices = {};
        int[] bad_bandOffsets = {};
        try {
            ImageTypeSpecifier.createBanded(cs, bad_bankIndices, bad_bandOffsets,
                dataType, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createBanded(cs, bankIndices, bandOffsets,
                99999, hasAlpha, isAlphaPremultiplied);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static void createGrayscaleTest() {
        int bits = 8;
        int dataType = DataBuffer.TYPE_BYTE;
        boolean isSigned = true;
        // testcase 1
        try {
            ImageTypeSpecifier.createGrayscale(100, dataType, isSigned);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createGrayscale(10, dataType, isSigned);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createGrayscale(bits, 100, isSigned);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
   }

   public static void createIndexedTest() {
        byte[] redLUT = {0};
        byte[] greenLUT = {0};
        byte[] blueLUT = {0};
        byte[] alphaLUT = {0};
        int bits = 8;
        int dataType = DataBuffer.TYPE_BYTE;

        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT,
                blueLUT, alphaLUT, 0, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT,
                blueLUT, alphaLUT, 17, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT,
                blueLUT, alphaLUT, 10, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        byte[] greenLUT_4 = {};
        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT_4,
                blueLUT, alphaLUT, bits, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        byte[] redLUT_5 = {};
        try {
            ImageTypeSpecifier.createIndexed(redLUT_5, greenLUT,
                blueLUT, alphaLUT, bits, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        byte[] alphaLUT_6 = {};
        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT,
                blueLUT, alphaLUT_6 , bits, dataType);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            ImageTypeSpecifier.createIndexed(redLUT, greenLUT,
                blueLUT, alphaLUT , bits, 100);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    private static void test4430991() {
        ImageTypeSpecifier itspecifier;

        itspecifier = ImageTypeSpecifier.createFromBufferedImageType
            (BufferedImage.TYPE_INT_RGB);

        try {
            itspecifier.createBufferedImage(Integer.MAX_VALUE,
                                            Integer.MAX_VALUE);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }

        try {
            itspecifier.getSampleModel(Integer.MAX_VALUE, Integer.MAX_VALUE);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    private static void test4430993() {
        ImageTypeSpecifier itspecifier;

        int bits = 32;
        int rmask = 0x00ff0000;
        int gmask = 0x0000ff00;
        int bmask = 0x000000ff;
        ColorModel dcm = new java.awt.image.DirectColorModel
            (bits, rmask, gmask, bmask);
        int[] bandOffsets = new int[2];
        bandOffsets[1] = 1;
        SampleModel sm = new java.awt.image.ComponentSampleModel
            (DataBuffer.TYPE_SHORT, 1, 1, 2, 2, bandOffsets);

        try {
            itspecifier = new ImageTypeSpecifier(dcm, sm);
            fail("Failed to get IAE!");
        } catch (IllegalArgumentException e) {
        }
    }

    public static void main(String[] args) {
        try {
            test4429934();
            test4429950();
            test4430991();
            test4430993();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unexpected exception: " + e);
        }
    }
}
