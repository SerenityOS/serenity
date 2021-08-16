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

import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class StandardMetadataFormat extends IIOMetadataFormatImpl {

    // Utility method for nodes with a single attribute named "value"
    private void addSingleAttributeElement(String elementName,
                                           String parentName,
                                           int dataType) {
        addElement(elementName, parentName, CHILD_POLICY_EMPTY);
        addAttribute(elementName, "value", dataType, true, null);
    }

    public StandardMetadataFormat() {
        super(standardMetadataFormatName, CHILD_POLICY_SOME);
        List<String> values;

        // root -> Chroma
        addElement("Chroma", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Chroma -> ColorSpaceType
        addElement("ColorSpaceType", "Chroma",
                   CHILD_POLICY_EMPTY);

        values = new ArrayList<>();
        values.add("XYZ");
        values.add("Lab");
        values.add("Luv");
        values.add("YCbCr");
        values.add("Yxy");
        values.add("YCCK");
        values.add("PhotoYCC");
        values.add("RGB");
        values.add("GRAY");
        values.add("HSV");
        values.add("HLS");
        values.add("CMYK");
        values.add("CMY");
        values.add("2CLR");
        values.add("3CLR");
        values.add("4CLR");
        values.add("5CLR");
        values.add("6CLR");
        values.add("7CLR");
        values.add("8CLR");
        values.add("9CLR");
        values.add("ACLR");
        values.add("BCLR");
        values.add("CCLR");
        values.add("DCLR");
        values.add("ECLR");
        values.add("FCLR");
        addAttribute("ColorSpaceType",
                     "name",
                     DATATYPE_STRING,
                     true,
                     null,
                     values);

        // root -> Chroma -> NumChannels
        addElement("NumChannels", "Chroma",
                   CHILD_POLICY_EMPTY);
        addAttribute("NumChannels", "value",
                     DATATYPE_INTEGER,
                     true,
                     0, Integer.MAX_VALUE);

        // root -> Chroma -> Gamma
        addElement("Gamma", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("Gamma", "value",
                     DATATYPE_FLOAT, true, null);

        // root -> Chroma -> BlackIsZero
        addElement("BlackIsZero", "Chroma", CHILD_POLICY_EMPTY);
        addBooleanAttribute("BlackIsZero", "value", true, true);

        // root -> Chroma -> Palette
        addElement("Palette", "Chroma", 0, Integer.MAX_VALUE);

        // root -> Chroma -> PaletteEntry
        addElement("PaletteEntry", "Palette", CHILD_POLICY_EMPTY);
        addAttribute("PaletteEntry", "index", DATATYPE_INTEGER,
                     true, null);
        addAttribute("PaletteEntry", "red", DATATYPE_INTEGER,
                     true, null);
        addAttribute("PaletteEntry", "green", DATATYPE_INTEGER,
                     true, null);
        addAttribute("PaletteEntry", "blue", DATATYPE_INTEGER,
                     true, null);
        addAttribute("PaletteEntry", "alpha", DATATYPE_INTEGER,
                     false, "255");

        // root -> Chroma -> BackgroundIndex
        addElement("BackgroundIndex", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("BackgroundIndex", "value", DATATYPE_INTEGER,
                     true, null);

        // root -> Chroma -> BackgroundColor
        addElement("BackgroundColor", "Chroma", CHILD_POLICY_EMPTY);
        addAttribute("BackgroundColor", "red", DATATYPE_INTEGER,
                     true, null);
        addAttribute("BackgroundColor", "green", DATATYPE_INTEGER,
                     true, null);
        addAttribute("BackgroundColor", "blue", DATATYPE_INTEGER,
                     true, null);

        // root -> Compression
        addElement("Compression", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Compression -> CompressionTypeName
        addSingleAttributeElement("CompressionTypeName",
                                  "Compression",
                                  DATATYPE_STRING);

        // root -> Compression -> Lossless
        addElement("Lossless", "Compression", CHILD_POLICY_EMPTY);
        addBooleanAttribute("Lossless", "value", true, true);

        // root -> Compression -> NumProgressiveScans
        addSingleAttributeElement("NumProgressiveScans",
                                  "Compression",
                                  DATATYPE_INTEGER);

        // root -> Compression -> BitRate
        addSingleAttributeElement("BitRate",
                                  "Compression",
                                  DATATYPE_FLOAT);

        // root -> Data
        addElement("Data", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Data -> PlanarConfiguration
        addElement("PlanarConfiguration", "Data", CHILD_POLICY_EMPTY);

        values = new ArrayList<>();
        values.add("PixelInterleaved");
        values.add("PlaneInterleaved");
        values.add("LineInterleaved");
        values.add("TileInterleaved");
        addAttribute("PlanarConfiguration", "value",
                     DATATYPE_STRING,
                     true,
                     null,
                     values);

        // root -> Data -> SampleFormat
        addElement("SampleFormat", "Data", CHILD_POLICY_EMPTY);

        values = new ArrayList<>();
        values.add("SignedIntegral");
        values.add("UnsignedIntegral");
        values.add("Real");
        values.add("Index");
        addAttribute("SampleFormat", "value",
                     DATATYPE_STRING,
                     true,
                     null,
                     values);

        // root -> Data -> BitsPerSample
        addElement("BitsPerSample", "Data",
                   CHILD_POLICY_EMPTY);
        addAttribute("BitsPerSample", "value",
                     DATATYPE_INTEGER,
                     true,
                     1, Integer.MAX_VALUE);

        // root -> Data -> SignificantBitsPerSample
        addElement("SignificantBitsPerSample", "Data", CHILD_POLICY_EMPTY);
        addAttribute("SignificantBitsPerSample", "value",
                     DATATYPE_INTEGER,
                     true,
                     1, Integer.MAX_VALUE);

        // root -> Data -> SampleMSB
        addElement("SampleMSB", "Data",
                   CHILD_POLICY_EMPTY);
        addAttribute("SampleMSB", "value",
                     DATATYPE_INTEGER,
                     true,
                     1, Integer.MAX_VALUE);

        // root -> Dimension
        addElement("Dimension", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Dimension -> PixelAspectRatio
        addSingleAttributeElement("PixelAspectRatio",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> ImageOrientation
        addElement("ImageOrientation", "Dimension",
                   CHILD_POLICY_EMPTY);

        values = new ArrayList<>();
        values.add("Normal");
        values.add("Rotate90");
        values.add("Rotate180");
        values.add("Rotate270");
        values.add("FlipH");
        values.add("FlipV");
        values.add("FlipHRotate90");
        values.add("FlipVRotate90");
        addAttribute("ImageOrientation", "value",
                     DATATYPE_STRING,
                     true,
                     null,
                     values);

        // root -> Dimension -> HorizontalPixelSize
        addSingleAttributeElement("HorizontalPixelSize",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> VerticalPixelSize
        addSingleAttributeElement("VerticalPixelSize",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> HorizontalPhysicalPixelSpacing
        addSingleAttributeElement("HorizontalPhysicalPixelSpacing",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> VerticalPhysicalPixelSpacing
        addSingleAttributeElement("VerticalPhysicalPixelSpacing",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> HorizontalPosition
        addSingleAttributeElement("HorizontalPosition",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> VerticalPosition
        addSingleAttributeElement("VerticalPosition",
                                  "Dimension",
                                  DATATYPE_FLOAT);

        // root -> Dimension -> HorizontalPixelOffset
        addSingleAttributeElement("HorizontalPixelOffset",
                                  "Dimension",
                                  DATATYPE_INTEGER);

        // root -> Dimension -> VerticalPixelOffset
        addSingleAttributeElement("VerticalPixelOffset",
                                  "Dimension",
                                  DATATYPE_INTEGER);

        // root -> Dimension -> HorizontalScreenSize
        addSingleAttributeElement("HorizontalScreenSize",
                                  "Dimension",
                                  DATATYPE_INTEGER);

        // root -> Dimension -> VerticalScreenSize
        addSingleAttributeElement("VerticalScreenSize",
                                  "Dimension",
                                  DATATYPE_INTEGER);


        // root -> Document
        addElement("Document", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Document -> FormatVersion
        addElement("FormatVersion", "Document",
                   CHILD_POLICY_EMPTY);
        addAttribute("FormatVersion", "value",
                     DATATYPE_STRING,
                     true,
                     null);

        // root -> Document -> SubimageInterpretation
        addElement("SubimageInterpretation", "Document",
                   CHILD_POLICY_EMPTY);
        values = new ArrayList<>();
        values.add("Standalone");
        values.add("SinglePage");
        values.add("FullResolution");
        values.add("ReducedResolution");
        values.add("PyramidLayer");
        values.add("Preview");
        values.add("VolumeSlice");
        values.add("ObjectView");
        values.add("Panorama");
        values.add("AnimationFrame");
        values.add("TransparencyMask");
        values.add("CompositingLayer");
        values.add("SpectralSlice");
        values.add("Unknown");
        addAttribute("SubimageInterpretation", "value",
                     DATATYPE_STRING,
                     true,
                     null,
                     values);

        // root -> Document -> ImageCreationTime
        addElement("ImageCreationTime", "Document",
                   CHILD_POLICY_EMPTY);
        addAttribute("ImageCreationTime", "year",
                     DATATYPE_INTEGER,
                     true,
                     null);
        addAttribute("ImageCreationTime", "month",
                     DATATYPE_INTEGER,
                     true,
                     null,
                     "1", "12", true, true);
        addAttribute("ImageCreationTime", "day",
                     DATATYPE_INTEGER,
                     true,
                     null,
                     "1", "31", true, true);
        addAttribute("ImageCreationTime", "hour",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "23", true, true);
        addAttribute("ImageCreationTime", "minute",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "59", true, true);
        // second = 60 denotes leap second
        addAttribute("ImageCreationTime", "second",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "60", true, true);

        // root -> Document -> ImageModificationTime
        addElement("ImageModificationTime", "Document",
                   CHILD_POLICY_EMPTY);
        addAttribute("ImageModificationTime", "year",
                     DATATYPE_INTEGER,
                     true,
                     null);
        addAttribute("ImageModificationTime", "month",
                     DATATYPE_INTEGER,
                     true,
                     null,
                     "1", "12", true, true);
        addAttribute("ImageModificationTime", "day",
                     DATATYPE_INTEGER,
                     true,
                     null,
                     "1", "31", true, true);
        addAttribute("ImageModificationTime", "hour",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "23", true, true);
        addAttribute("ImageModificationTime", "minute",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "59", true, true);
        // second = 60 denotes leap second
        addAttribute("ImageModificationTime", "second",
                     DATATYPE_INTEGER,
                     false,
                     "0",
                     "0", "60", true, true);

        // root -> Text
        addElement("Text", standardMetadataFormatName,
                   0, Integer.MAX_VALUE);

        // root -> Text -> TextEntry
        addElement("TextEntry", "Text", CHILD_POLICY_EMPTY);
        addAttribute("TextEntry", "keyword",
                     DATATYPE_STRING,
                     false,
                     null);
        addAttribute("TextEntry", "value",
                     DATATYPE_STRING,
                     true,
                     null);
        addAttribute("TextEntry", "language",
                     DATATYPE_STRING,
                     false,
                     null);
        addAttribute("TextEntry", "encoding",
                     DATATYPE_STRING,
                     false,
                     null);

        values = new ArrayList<>();
        values.add("none");
        values.add("lzw");
        values.add("zip");
        values.add("bzip");
        values.add("other");
        addAttribute("TextEntry", "compression",
                     DATATYPE_STRING,
                     false,
                     "none",
                     values);

        // root -> Transparency
        addElement("Transparency", standardMetadataFormatName,
                   CHILD_POLICY_SOME);

        // root -> Transparency -> Alpha
        addElement("Alpha", "Transparency", CHILD_POLICY_EMPTY);

        values = new ArrayList<>();
        values.add("none");
        values.add("premultiplied");
        values.add("nonpremultiplied");
        addAttribute("Alpha", "value",
                     DATATYPE_STRING,
                     false,
                     "none",
                     values);

        // root -> Transparency -> TransparentIndex
        addSingleAttributeElement("TransparentIndex", "Transparency",
                                  DATATYPE_INTEGER);

        // root -> Transparency -> TransparentColor
        addElement("TransparentColor", "Transparency",
                   CHILD_POLICY_EMPTY);
        addAttribute("TransparentColor", "value",
                     DATATYPE_INTEGER,
                     true,
                     0, Integer.MAX_VALUE);

        // root -> Transparency -> TileTransparencies
        addElement("TileTransparencies", "Transparency",
                   0, Integer.MAX_VALUE);

        // root -> Transparency -> TileTransparencies -> TransparentTile
        addElement("TransparentTile", "TileTransparencies",
                   CHILD_POLICY_EMPTY);
        addAttribute("TransparentTile", "x",
                     DATATYPE_INTEGER,
                     true,
                     null);
        addAttribute("TransparentTile", "y",
                     DATATYPE_INTEGER,
                     true,
                     null);

        // root -> Transparency -> TileOpacities
        addElement("TileOpacities", "Transparency",
                   0, Integer.MAX_VALUE);

        // root -> Transparency -> TileOpacities -> OpaqueTile
        addElement("OpaqueTile", "TileOpacities",
                   CHILD_POLICY_EMPTY);
        addAttribute("OpaqueTile", "x",
                     DATATYPE_INTEGER,
                     true,
                     null);
        addAttribute("OpaqueTile", "y",
                     DATATYPE_INTEGER,
                     true,
                     null);
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
            return true;
    }
}
