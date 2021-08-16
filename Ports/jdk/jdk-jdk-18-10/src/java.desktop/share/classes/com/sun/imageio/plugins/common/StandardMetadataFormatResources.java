/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.common;

import java.util.ListResourceBundle;

public class StandardMetadataFormatResources extends ListResourceBundle {

    public StandardMetadataFormatResources() {}

    protected Object[][] getContents() {
        return new Object[][] {

        // Node name, followed by description, or
        // Node name + "/" + AttributeName, followed by description

        { "Chroma", "Chroma (color) information" },

        { "ColorSpaceType", "The raw color space of the image" },

        { "NumChannels",
          "The number of channels in the raw image, including alpha" },

        { "Gamma", "The image gamma" },

        { "BlackIsZero",
          "True if smaller values represent darker shades"},

        { "Palette", "Palette-color information" },

        { "PaletteEntry", "A palette entry" },
        { "PaletteEntry/index", "The index of the palette entry" },
        { "PaletteEntry/red", "The red value for the palette entry" },
        { "PaletteEntry/green", "The green value for the palette entry" },
        { "PaletteEntry/blue", "The blue value for the palette entry" },
        { "PaletteEntry/alpha", "The alpha value for the palette entry" },

        { "BackgroundIndex", "A palette index to be used as a background" },

        { "BackgroundColor", "An RGB triple to be used as a background" },
        { "BackgroundColor/red", "The red background value" },
        { "BackgroundColor/green", "The green background value" },
        { "BackgroundColor/blue", "The blue background value" },

        { "Compression", "Compression information" },

        { "CompressionTypeName", "The name of the compression scheme in use" },

        { "Lossless",
          "True if the compression scheme is lossless" },

        { "BitRate", "The estimated bit rate of the compression scheme" },

        { "NumProgressiveScans",
          "The number of progressive scans used in the image encoding"},

        { "Data", "Information on the image layout" },

        { "PlanarConfiguration",
          "The organization of image samples in the stream" },

        { "SampleFormat", "The numeric format of image samples" },

        { "BitsPerSample", "The number of bits per sample"},
        { "BitsPerSample/value",
          "A list of integers, one per channel" },

        { "SignificantBitsPerSample",
          "The number of significant bits per sample"},
        { "SignificantBitsPerSample/value",
          "A list of integers, one per channel" },

        { "SampleMSB",
          "The position of the most significant bit of each sample"},
        { "SampleMSB/value",
          "A list of integers, one per channel" },

        { "Dimension", "Dimension information" },

        { "PixelAspectRatio", "The width of a pixel divided by its height" },

        { "ImageOrientation", "The desired orientation of the image in terms of flips and counter-clockwise rotations" },

        { "HorizontalPixelSize",
  "The width of a pixel, in millimeters, as it should be rendered on media" },

        { "VerticalPixelSize",
  "The height of a pixel, in millimeters, as it should be rendered on media" },

        { "HorizontalPhysicalPixelSpacing",
          "The horizontal distance in the subject of the image, in millimeters, represented by one pixel at the center of the image" },

        { "VerticalPhysicalPixelSpacing",
          "The vertical distance in the subject of the image, in millimeters, represented by one pixel at the center of the image" },

        { "HorizontalPosition",
          "The horizontal position, in millimeters, where the image should be rendered on media " },

        { "VerticalPosition",
          "The vertical position, in millimeters, where the image should be rendered on media " },

        { "HorizontalPixelOffset",
          "The horizontal position, in pixels, where the image should be rendered onto a raster display" },

        { "VerticalPixelOffset",
          "The vertical position, in pixels, where the image should be rendered onto a raster display" },

        { "HorizontalScreenSize",
          "The width, in pixels, of the raster display into which the image should be rendered" },

        { "VerticalScreenSize",
          "The height, in pixels, of the raster display into which the image should be rendered" },

        { "Document", "Document information" },

        { "FormatVersion",
          "The version of the format used by the stream" },

        { "SubimageInterpretation",
          "The interpretation of this image in relation to the other images stored in the same stream" },

        { "ImageCreationTime", "The time of image creation" },
        { "ImageCreationTime/year",
          "The full year (e.g., 1967, not 67)" },
        { "ImageCreationTime/month",
          "The month, with January = 1" },
        { "ImageCreationTime/day",
          "The day of the month" },
        { "ImageCreationTime/hour",
          "The hour from 0 to 23" },
        { "ImageCreationTime/minute",
          "The minute from 0 to 59" },
        { "ImageCreationTime/second",
          "The second from 0 to 60 (60 = leap second)" },

        { "ImageModificationTime", "The time of the last image modification" },
        { "ImageModificationTime/year",
          "The full year (e.g., 1967, not 67)" },
        { "ImageModificationTime/month",
          "The month, with January = 1" },
        { "ImageModificationTime/day",
          "The day of the month" },
        { "ImageModificationTime/hour",
          "The hour from 0 to 23" },
        { "ImageModificationTime/minute",
          "The minute from 0 to 59" },
        { "ImageModificationTime/second",
          "The second from 0 to 60 (60 = leap second)" },

        { "Text", "Text information" },

        { "TextEntry", "A text entry"},
        { "TextEntry/keyword", "A keyword associated with the text entry" },
        { "TextEntry/value", "the text entry" },
        { "TextEntry/language", "The language of the text" },
        { "TextEntry/encoding", "The encoding of the text" },
        { "TextEntry/compression", "The method used to compress the text" },

        { "Transparency", "Transparency information" },

        { "Alpha", "The type of alpha information contained in the image" },

        { "TransparentIndex", "A palette index to be treated as transparent" },

        { "TransparentColor", "An RGB color to be treated as transparent" },
        { "TransparentColor/red",
          "The red channel of the transparent color" },
        { "TransparentColor/green",
          "The green channel of the transparent color" },
        { "TransparentColor/blue",
          "The blue channel of the transparent color" },

        { "TileTransparencies", "A list of completely transparent tiles" },

        { "TransparentTile", "The index of a completely transparent tile" },
        { "TransparentTile/x", "The tile's X index" },
        { "TransparentTile/y", "The tile's Y index" },

        { "TileOpacities", "A list of completely opaque tiles" },

        { "OpaqueTile", "The index of a completely opaque tile" },
        { "OpaqueTile/x", "The tile's X index" },
        { "OpaqueTile/y", "The tile's Y index" },

        };
    }
}
