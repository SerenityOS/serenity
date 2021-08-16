/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.imageio.plugins.jpeg;

import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.plugins.jpeg.JPEGHuffmanTable;
import javax.imageio.plugins.jpeg.JPEGQTable;

/**
 * A class containing JPEG-related constants, definitions, and
 * static methods.  This class and its constants must be public so that
 * {@code JPEGImageWriteParam} can see it.
 */
public class JPEG {

    // List of all the JPEG markers (pre-JPEG2000)

    /** For temporary use in arithmetic coding */
    public static final int TEM = 0x01;

    // Codes 0x02 - 0xBF are reserved

    // SOF markers for Nondifferential Huffman coding
    /** Baseline DCT */
    public static final int SOF0 = 0xC0;
    /** Extended Sequential DCT */
    public static final int SOF1 = 0xC1;
    /** Progressive DCT */
    public static final int SOF2 = 0xC2;
    /** Lossless Sequential */
    public static final int SOF3 = 0xC3;

    /** Define Huffman Tables */
    public static final int DHT = 0xC4;

    // SOF markers for Differential Huffman coding
    /** Differential Sequential DCT */
    public static final int SOF5 = 0xC5;
    /** Differential Progressive DCT */
    public static final int SOF6 = 0xC6;
    /** Differential Lossless */
    public static final int SOF7 = 0xC7;

    /** Reserved for JPEG extensions */
    public static final int JPG = 0xC8;

    // SOF markers for Nondifferential arithmetic coding
    /** Extended Sequential DCT, Arithmetic coding */
    public static final int SOF9 = 0xC9;
    /** Progressive DCT, Arithmetic coding */
    public static final int SOF10 = 0xCA;
    /** Lossless Sequential, Arithmetic coding */
    public static final int SOF11 = 0xCB;

    /** Define Arithmetic conditioning tables */
    public static final int DAC = 0xCC;

    // SOF markers for Differential arithmetic coding
    /** Differential Sequential DCT, Arithmetic coding */
    public static final int SOF13 = 0xCD;
    /** Differential Progressive DCT, Arithmetic coding */
    public static final int SOF14 = 0xCE;
    /** Differential Lossless, Arithmetic coding */
    public static final int SOF15 = 0xCF;

    // Restart Markers
    public static final int RST0 = 0xD0;
    public static final int RST1 = 0xD1;
    public static final int RST2 = 0xD2;
    public static final int RST3 = 0xD3;
    public static final int RST4 = 0xD4;
    public static final int RST5 = 0xD5;
    public static final int RST6 = 0xD6;
    public static final int RST7 = 0xD7;
    /** Number of restart markers */
    public static final int RESTART_RANGE = 8;

    /** Start of Image */
    public static final int SOI = 0xD8;
    /** End of Image */
    public static final int EOI = 0xD9;
    /** Start of Scan */
    public static final int SOS = 0xDA;

    /** Define Quantisation Tables */
    public static final int DQT = 0xDB;

    /** Define Number of lines */
    public static final int DNL = 0xDC;

    /** Define Restart Interval */
    public static final int DRI = 0xDD;

    /** Define Heirarchical progression */
    public static final int DHP = 0xDE;

    /** Expand reference image(s) */
    public static final int EXP = 0xDF;

    // Application markers
    /** APP0 used by JFIF */
    public static final int APP0 = 0xE0;
    public static final int APP1 = 0xE1;
    public static final int APP2 = 0xE2;
    public static final int APP3 = 0xE3;
    public static final int APP4 = 0xE4;
    public static final int APP5 = 0xE5;
    public static final int APP6 = 0xE6;
    public static final int APP7 = 0xE7;
    public static final int APP8 = 0xE8;
    public static final int APP9 = 0xE9;
    public static final int APP10 = 0xEA;
    public static final int APP11 = 0xEB;
    public static final int APP12 = 0xEC;
    public static final int APP13 = 0xED;
    /** APP14 used by Adobe */
    public static final int APP14 = 0xEE;
    public static final int APP15 = 0xEF;

    // codes 0xF0 to 0xFD are reserved

    /** Comment marker */
    public static final int COM = 0xFE;

    // JFIF Resolution units
    /** The X and Y units simply indicate the aspect ratio of the pixels. */
    public static final int DENSITY_UNIT_ASPECT_RATIO = 0;
    /** Pixel density is in pixels per inch. */
    public static final int DENSITY_UNIT_DOTS_INCH    = 1;
    /** Pixel density is in pixels per centemeter. */
    public static final int DENSITY_UNIT_DOTS_CM      = 2;
    /** The max known value for DENSITY_UNIT */
    public static final int NUM_DENSITY_UNIT = 3;

    // Adobe transform values
    public static final int ADOBE_IMPOSSIBLE = -1;
    public static final int ADOBE_UNKNOWN = 0;
    public static final int ADOBE_YCC = 1;
    public static final int ADOBE_YCCK = 2;

    // Spi initialization stuff
    public static final String vendor = "Oracle Corporation";
    public static final String version = "0.5";
    // Names of the formats we can read or write
    static final String [] names = {"JPEG", "jpeg", "JPG", "jpg"};
    static final String [] suffixes = {"jpg", "jpeg"};
    static final String [] MIMETypes = {"image/jpeg"};
    public static final String nativeImageMetadataFormatName =
        "javax_imageio_jpeg_image_1.0";
    public static final String nativeImageMetadataFormatClassName =
        "com.sun.imageio.plugins.jpeg.JPEGImageMetadataFormat";
    public static final String nativeStreamMetadataFormatName =
        "javax_imageio_jpeg_stream_1.0";
    public static final String nativeStreamMetadataFormatClassName =
        "com.sun.imageio.plugins.jpeg.JPEGStreamMetadataFormat";

    // IJG Color codes.
    public static final int JCS_UNKNOWN = 0;       // error/unspecified
    public static final int JCS_GRAYSCALE = 1;     // monochrome
    public static final int JCS_RGB = 2;           // red/green/blue
    public static final int JCS_YCbCr = 3;         // Y/Cb/Cr (also known as YUV)
    public static final int JCS_CMYK = 4;          // C/M/Y/K
    public static final int JCS_YCCK = 5;         // Y/Cb/Cr/K

    public static final int NUM_JCS_CODES = JCS_YCCK+1;

    /** IJG can handle up to 4-channel JPEGs */
    static final int [] [] bandOffsets = {{0},
                                          {0, 1},
                                          {0, 1, 2},
                                          {0, 1, 2, 3}};

    static final int [] bOffsRGB = { 2, 1, 0 };

    static final ColorSpace sRGB = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    // Default value for ImageWriteParam
    public static final float DEFAULT_QUALITY = 0.75F;

    /**
     * Returns {@code true} if the given imageType can be used
     * in a JFIF file.  If {@code input} is true, then the
     * image type is considered before colorspace conversion.
     */
    static boolean isJFIFcompliant(ImageTypeSpecifier imageType,
                                   boolean input) {
        ColorModel cm = imageType.getColorModel();
        // Can't have alpha
        if (cm.hasAlpha()) {
            return false;
        }
        // Gray is OK, always
        int numComponents = imageType.getNumComponents();
        if (numComponents == 1) {
            return true;
        }

        // If it isn't gray, it must have 3 channels
        if (numComponents != 3) {
            return false;
        }

        if (input) {
            // Must be RGB
            if (cm.getColorSpace().getType() == ColorSpace.TYPE_RGB) {
                return true;
            }
        } else {
            // Must be YCbCr
            if (cm.getColorSpace().getType() == ColorSpace.TYPE_YCbCr) {
                return true;
            }
        }

        return false;
    }

    /**
     * Given an image type, return the Adobe transform corresponding to
     * that type, or ADOBE_IMPOSSIBLE if the image type is incompatible
     * with an Adobe marker segment.  If {@code input} is true, then
     * the image type is considered before colorspace conversion.
     */
    static int transformForType(ImageTypeSpecifier imageType, boolean input) {
        int retval = ADOBE_IMPOSSIBLE;
        ColorModel cm = imageType.getColorModel();
        switch (cm.getColorSpace().getType()) {
        case ColorSpace.TYPE_GRAY:
            retval = ADOBE_UNKNOWN;
            break;
        case ColorSpace.TYPE_RGB:
            retval = input ? ADOBE_YCC : ADOBE_UNKNOWN;
            break;
        case ColorSpace.TYPE_YCbCr:
            retval = ADOBE_YCC;
            break;
        case ColorSpace.TYPE_CMYK:
            retval = input ? ADOBE_YCCK : ADOBE_IMPOSSIBLE;
        }
        return retval;
    }

    /**
     * Converts an ImageWriteParam (i.e. IJG) non-linear quality value
     * to a float suitable for passing to JPEGQTable.getScaledInstance().
     */
    static float convertToLinearQuality(float quality) {
        // The following is converted from the IJG code.
        if (quality <= 0.0F) {
            quality = 0.01F;
        }

        if (quality > 1.00F) {
            quality = 1.00F;
        }

        if (quality < 0.5F) {
            quality = 0.5F / quality;
        } else {
            quality = 2.0F - (quality * 2.0F);
        }

        return quality;
    }

    /**
     * Return an array of default, visually lossless quantization tables.
     */
    static JPEGQTable [] getDefaultQTables() {
        JPEGQTable [] qTables = new JPEGQTable[2];
        qTables[0] = JPEGQTable.K1Div2Luminance;
        qTables[1] = JPEGQTable.K2Div2Chrominance;
        return qTables;
    }

    /**
     * Return an array of default Huffman tables.
     */
    static JPEGHuffmanTable [] getDefaultHuffmanTables(boolean wantDC) {
        JPEGHuffmanTable [] tables = new JPEGHuffmanTable[2];
        if (wantDC) {
            tables[0] = JPEGHuffmanTable.StdDCLuminance;
            tables[1] = JPEGHuffmanTable.StdDCChrominance;
        } else {
            tables[0] = JPEGHuffmanTable.StdACLuminance;
            tables[1] = JPEGHuffmanTable.StdACChrominance;
        }
        return tables;
    }
}
