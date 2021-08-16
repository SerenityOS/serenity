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

import java.awt.Color;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageFactory {
    public static int WIDTH = 256;
    public static int HEIGHT = 256;


    final static int aMask = 0xFF000000;
    final static int rMask = 0x00FF0000;
    final static int gMask = 0x0000FF00;
    final static int bMask = 0x000000FF;

    final static int r555Mask = 0x7c00;
    final static int g555Mask = 0x3e0;
    final static int b555Mask = 0x1f;

    final static int r565Mask = 0xf800;
    final static int g565Mask = 0x7e0;
    final static int b565Mask = 0x1f;

    final static int rShift = 16;
    final static int gShift = 8;
    final static int bShift = 0;

    public static BufferedImage createCCMImage(int cs, int dataType)
    {

        ColorSpace cSpace = ColorSpace.getInstance(cs);
        ComponentColorModel ccm = null;
        if (dataType == DataBuffer.TYPE_INT)
        {
            ccm = new ComponentColorModel(cSpace,
                    ((cs == ColorSpace.CS_GRAY)?
                        new int[] {8}: new int[] {8,8,8}),
                    false, false, Transparency.OPAQUE, dataType);
        } else {
            ccm = new ComponentColorModel(
                cSpace, false, false, Transparency.OPAQUE, dataType);
        }
        SampleModel sm = ccm.createCompatibleSampleModel(WIDTH, HEIGHT);
        WritableRaster raster = ccm.createCompatibleWritableRaster(WIDTH,
                                                                   HEIGHT);
        DataBuffer data = raster.getDataBuffer();
        fillCCM(data, sm, cSpace);
        return new BufferedImage(ccm, raster, false, null);
    }

    public static BufferedImage createDCMImage(int type, int cs) {
        if (type == BufferedImage.TYPE_INT_RGB && cs == ColorSpace.CS_sRGB) {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT,
                                                    BufferedImage.TYPE_INT_RGB);
            DataBuffer data = image.getData().getDataBuffer();
            fill(image);
            return image;
        }

        ColorSpace cSpace = ColorSpace.getInstance(cs);
        DirectColorModel dcm = null;
        switch(type) {
            case BufferedImage.TYPE_INT_ARGB:
                dcm = new DirectColorModel(cSpace, 32, rMask, gMask,
                                           bMask, aMask, false,
                                           DataBuffer.TYPE_INT);
                break;
            case BufferedImage.TYPE_INT_RGB:
                dcm = new DirectColorModel(cSpace, 24, rMask, gMask,
                                           bMask, 0, false,
                                           DataBuffer.TYPE_INT);
                break;
            case BufferedImage.TYPE_INT_BGR:
                dcm = new DirectColorModel(cSpace, 24, rMask, gMask,
                                           bMask, 0, false,
                                           DataBuffer.TYPE_INT);
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                dcm = new DirectColorModel(cSpace, 15, r555Mask, g555Mask,
                                           b555Mask, 0, false,
                                           DataBuffer.TYPE_USHORT);
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                dcm = new DirectColorModel(cSpace, 16, r565Mask, g565Mask,
                                           b565Mask, 0, false,
                                           DataBuffer.TYPE_USHORT);
                break;
        }
        SampleModel sm = dcm.createCompatibleSampleModel(WIDTH, HEIGHT);
        WritableRaster raster = dcm.createCompatibleWritableRaster(WIDTH,
                                                                   HEIGHT);
        switch(type) {
            case BufferedImage.TYPE_INT_ARGB:
                fillDCM(raster.getDataBuffer(), sm, cSpace.getType());
                break;
            case BufferedImage.TYPE_INT_RGB:
                fillDCM(raster.getDataBuffer(), sm, cSpace.getType());
                break;
            case BufferedImage.TYPE_INT_BGR:
                fillDCM(raster.getDataBuffer(), sm, cSpace.getType());
                break;
            case BufferedImage.TYPE_USHORT_555_RGB:
                fillDCM(raster.getDataBuffer(), sm, cSpace.getType(), 5, 5, 5);
                break;
            case BufferedImage.TYPE_USHORT_565_RGB:
                fillDCM(raster.getDataBuffer(), sm, cSpace.getType(), 5, 6, 5);
                break;
        }
        return new BufferedImage(dcm, raster, false, null);
    }

    public static void fill(BufferedImage image) {
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                image.setRGB(i,j,0xFF000000 | (i << 16) | (j << 8) |
                        ((i + j)>>1));
            }
        }
    }

    public static void fillCCM(DataBuffer data, SampleModel sm, ColorSpace cs) {
        switch (cs.getType()) {
            case ColorSpace.TYPE_RGB:
            case ColorSpace.TYPE_XYZ:
            case ColorSpace.TYPE_3CLR:
                if (data.getDataType() == DataBuffer.TYPE_BYTE ||
                    data.getDataType() == DataBuffer.TYPE_INT)
                {
                    int [] pixel = new int[3];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = i;
                            pixel[1] = j;
                            pixel[2] = ((i + j)>>1);
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_SHORT) {
                    int [] pixel = new int[3];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i<<7);
                            pixel[1] = (j<<7);
                            pixel[2] = ((i + j)<<6);
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_USHORT) {
                    int [] pixel = new int[3];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i<<8);
                            pixel[1] = (j<<8);
                            pixel[2] = ((i + j)<<7);
                            sm.setPixel(i, j, pixel, data);
                        }
                    }

                } else if (data.getDataType() == DataBuffer.TYPE_DOUBLE) {
                    double [] pixel = new double [3];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i/255.0)*(cs.getMaxValue(0) -
                                                  cs.getMinValue(0)) +
                                       cs.getMinValue(0);
                            pixel[1] = (j/255.0)*(cs.getMaxValue(1) -
                                                  cs.getMinValue(1)) +
                                       cs.getMinValue(1);
                            pixel[2] = (((i + j)>>1)/255.0)*(cs.getMaxValue(2) -
                                                             cs.getMinValue(2))+
                                       cs.getMinValue(2);
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_FLOAT) {
                    float [] pixel = new float [3];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i/255.0f)*(cs.getMaxValue(0) -
                                                  cs.getMinValue(0)) +
                                       cs.getMinValue(0);
                            pixel[1] = (j/255.0f)*(cs.getMaxValue(1) -
                                                  cs.getMinValue(1)) +
                                       cs.getMinValue(1);
                            pixel[2] = (((i + j)>>1)/255.0f)*(cs.getMaxValue(2)-
                                                              cs.getMinValue(2))
                                       + cs.getMinValue(2);
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else {
                    throw new RuntimeException("Unsupported DataBuffer type");
                }
                break;
            case ColorSpace.TYPE_GRAY:
                if (data.getDataType() == DataBuffer.TYPE_BYTE ||
                    data.getDataType() == DataBuffer.TYPE_INT) {
                    int [] pixel = new int[1];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = i;
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_SHORT) {
                    int [] pixel = new int[1];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = i << 7;
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_USHORT) {
                    int [] pixel = new int[1];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = i << 8;
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else if (data.getDataType() == DataBuffer.TYPE_DOUBLE) {
                    double [] pixel = new double[1];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i/255.0)*(cs.getMaxValue(0) -
                                                  cs.getMinValue(0)) +
                                       cs.getMinValue(0);;
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                 } else if (data.getDataType() == DataBuffer.TYPE_FLOAT) {
                    float [] pixel = new float[1];
                    for (int i = 0; i < WIDTH; i++) {
                        for (int j = 0; j < HEIGHT; j++) {
                            pixel[0] = (i/255.0f)*(cs.getMaxValue(0) -
                                                  cs.getMinValue(0)) +
                                       cs.getMinValue(0);;
                            sm.setPixel(i, j, pixel, data);
                        }
                    }
                } else {
                    throw new RuntimeException("Unsupported DataBuffer type");
                }
                break;
        }
    }

    public static void fillDCM(DataBuffer data, SampleModel sm, int csType,
                               int c1Bits, int c2Bits, int c3Bits)
    {
        int [] pixel;
        pixel = new int[4];
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                pixel[0] = i >> (8 - c1Bits);
                pixel[1] = j >> (8 - c2Bits);
                pixel[2] = ((i + j)>>1) >> (8 - c3Bits);
                pixel[3] = 0xFF;
                sm.setPixel(i, j, pixel, data);
            }
        }
    }

    public static void fillDCM(DataBuffer data, SampleModel sm, int csType) {
        fillDCM(data, sm, csType, 8, 8, 8);
    }

    public static BufferedImage createDstImage(int type) {
        return new BufferedImage(WIDTH, HEIGHT, type);
    }
}
