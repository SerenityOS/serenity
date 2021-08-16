/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.png;

import java.util.ListResourceBundle;

public class PNGMetadataFormatResources extends ListResourceBundle {

    public PNGMetadataFormatResources() {}

    protected Object[][] getContents() {
        return new Object[][] {

        // Node name, followed by description
        { "IHDR", "The IHDR chunk, containing the header" },
        { "PLTE", "The PLTE chunk, containing the palette" },
        { "PLTEEntry", "A palette entry" },
        { "bKGD", "The bKGD chunk, containing the background color" },
        { "bKGD_RGB", "An RGB background color, for RGB and RGBAlpha images" },
        { "bKGD_Grayscale",
          "A grayscale background color, for Gray and GrayAlpha images" },
        { "bKGD_Palette", "A background palette index" },
        { "cHRM", "The cHRM chunk, containing color calibration" },
        { "gAMA", "The gAMA chunk, containing the image gamma" },
        { "hIST", "The hIST chunk, containing histogram information " },
        { "hISTEntry", "A histogram entry" },
        { "iCCP", "The iCCP chunk, containing an ICC color profile" },
        { "iTXt", "The iTXt chunk, containing internationalized text" },
        { "iTXtEntry", "A localized text entry" },
        { "pHYS",
          "The pHYS chunk, containing the pixel size and aspect ratio" },
        { "sBIT", "The sBIT chunk, containing significant bit information" },
        { "sBIT_Grayscale", "Significant bit information for gray samples" },
        { "sBIT_GrayAlpha",
          "Significant bit information for gray and alpha samples" },
        { "sBIT_RGB", "Significant bit information for RGB samples" },
        { "sBIT_RGBAlpha", "Significant bit information for RGBA samples" },
        { "sBIT_Palette",
          "Significant bit information for RGB palette entries" },
        { "sPLT", "The sPLT chunk, containing a suggested palette" },
        { "sPLTEntry", "A suggested palette entry" },
        { "sRGB", "The sRGB chunk, containing rendering intent information" },
        { "tEXt", "The tEXt chunk, containing text" },
        { "tEXtEntry", "A text entry" },
        { "tIME", "The tIME chunk, containing the image modification time" },
        { "tRNS", "The tRNS chunk, containing transparency information" },
        { "tRNS_Grayscale",
          "A grayscale value that should be considered transparent" },
        { "tRNS_RGB",
          "An RGB value that should be considered transparent" },
        { "tRNS_Palette",
          "A palette index that should be considered transparent" },
        { "zTXt", "The zTXt chunk, containing compressed text" },
        { "zTXtEntry", "A compressed text entry" },
        { "UnknownChunks", "A set of unknown chunks" },
        { "UnknownChunk", "Unknown chunk data stored as a byte array" },

        // Node name + "/" + AttributeName, followed by description
        { "IHDR/width", "The width of the image in pixels" },
        { "IHDR/height", "The height of the image in pixels" },
        { "IHDR/bitDepth", "The bit depth of the image samples" },
        { "IHDR/colorType", "The color type of the image" },
        { "IHDR/compressionMethod",
"The compression used for image data, always \"deflate\"" },
        { "IHDR/filterMethod",
"The filtering method used for compression, always \"adaptive\"" },
        { "IHDR/interlaceMethod",
          "The interlacing method, \"none\" or \"adam7\"" },

        { "PLTEEntry/index", "The index of a palette entry" },
        { "PLTEEntry/red", "The red value of a palette entry" },
        { "PLTEEntry/green", "The green value of a palette entry" },
        { "PLTEEntry/blue", "The blue value of a palette entry" },

        { "bKGD_Grayscale/gray", "A gray value to be used as a background" },
        { "bKGD_RGB/red", "A red value to be used as a background" },
        { "bKGD_RGB/green", "A green value to be used as a background" },
        { "bKGD_RGB/blue", "A blue value to be used as a background" },
        { "bKGD_Palette/index", "A palette index to be used as a background" },

        { "cHRM/whitePointX",
              "The CIE x coordinate of the white point, multiplied by 1e5" },
        { "cHRM/whitePointY",
              "The CIE y coordinate of the white point, multiplied by 1e5" },
        { "cHRM/redX",
              "The CIE x coordinate of the red primary, multiplied by 1e5" },
        { "cHRM/redY",
              "The CIE y coordinate of the red primary, multiplied by 1e5" },
        { "cHRM/greenX",
              "The CIE x coordinate of the green primary, multiplied by 1e5" },
        { "cHRM/greenY",
              "The CIE y coordinate of the green primary, multiplied by 1e5" },
        { "cHRM/blueX",
              "The CIE x coordinate of the blue primary, multiplied by 1e5" },
        { "cHRM/blueY",
              "The CIE y coordinate of the blue primary, multiplied by 1e5" },

        { "gAMA/value",
              "The image gamma, multiplied by 1e5" },

        { "hISTEntry/index", "The palette index of this histogram entry" },
        { "hISTEntry/value", "The frequency of this histogram entry" },

        { "iCCP/profileName", "The name of this ICC profile" },
        { "iCCP/compressionMethod",
              "The compression method used to store this ICC profile" },

        { "iTXtEntry/keyword", "The keyword" },
        { "iTXtEntry/compressionMethod",
              "The compression method used to store this iTXt entry" },
        { "iTXtEntry/languageTag",
              "The ISO tag describing the language this iTXt entry" },
        { "iTXtEntry/translatedKeyword",
              "The translated keyword for iTXt entry" },
        { "iTXtEntry/text",
              "The localized text" },

        { "pHYS/pixelsPerUnitXAxis",
            "The number of horizontal pixels per unit, multiplied by 1e5" },
        { "pHYS/pixelsPerUnitYAxis",
            "The number of vertical pixels per unit, multiplied by 1e5" },
        { "pHYS/unitSpecifier",
            "The unit specifier for this chunk (i.e., meters)" },

        { "sBIT_Grayscale/gray",
            "The number of significant bits of the gray samples" },
        { "sBIT_GrayAlpha/gray",
            "The number of significant bits of the gray samples" },
        { "sBIT_GrayAlpha/alpha",
            "The number of significant bits of the alpha samples" },
        { "sBIT_RGB/red",
            "The number of significant bits of the red samples" },
        { "sBIT_RGB/green",
            "The number of significant bits of the green samples" },
        { "sBIT_RGB/blue",
            "The number of significant bits of the blue samples" },
        { "sBIT_RGBAlpha/red",
            "The number of significant bits of the red samples" },
        { "sBIT_RGBAlpha/green",
            "The number of significant bits of the green samples" },
        { "sBIT_RGBAlpha/blue",
            "The number of significant bits of the blue samples" },
        { "sBIT_RGBAlpha/alpha",
            "The number of significant bits of the alpha samples" },
        { "sBIT_Palette/red",
            "The number of significant bits of the red palette entries" },
        { "sBIT_Palette/green",
            "The number of significant bits of the green palette entries" },
        { "sBIT_Palette/blue",
            "The number of significant bits of the blue palette entries" },

        { "sPLTEntry/index", "The index of a suggested palette entry" },
        { "sPLTEntry/red", "The red value of a suggested palette entry" },
        { "sPLTEntry/green", "The green value of a suggested palette entry" },
        { "sPLTEntry/blue", "The blue value of a suggested palette entry" },
        { "sPLTEntry/alpha", "The blue value of a suggested palette entry" },

        { "sRGB/renderingIntent", "The rendering intent" },

        { "tEXtEntry/keyword", "The keyword" },
        { "tEXtEntry/value", "The text" },

        { "tIME/year", "The year when the image was last modified" },
        { "tIME/month",
          "The month when the image was last modified, 1 = January" },
        { "tIME/day",
          "The day of the month when the image was last modified" },
        { "tIME/hour",
          "The hour when the image was last modified" },
        { "tIME/minute",
          "The minute when the image was last modified" },
        { "tIME/second",
          "The second when the image was last modified, 60 = leap second" },

        { "tRNS_Grayscale/gray",
          "The gray value to be considered transparent" },
        { "tRNS_RGB/red",
          "The red value to be considered transparent" },
        { "tRNS_RGB/green",
          "The green value to be considered transparent" },
        { "tRNS_RGB/blue",
          "The blure value to be considered transparent" },
        { "tRNS_Palette/index",
          "A palette index to be considered transparent" },
        { "tRNS_Palette/alpha",
          "The transparency associated with the palette entry" },

        { "zTXtEntry/keyword", "The keyword" },
        { "zTXtEntry/compressionMethod", "The compression method" },
        { "zTXtEntry/text", "The compressed text" },

        { "UnknownChunk/type", "The 4-character type of the unknown chunk" }

        };
    }
}
