/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.imageio.plugins.bmp;

import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class BMPMetadataFormat extends IIOMetadataFormatImpl {

    private static IIOMetadataFormat instance = null;

    private BMPMetadataFormat() {
        super(BMPMetadata.nativeMetadataFormatName,
              CHILD_POLICY_SOME);

        // root -> ImageDescriptor
        addElement("ImageDescriptor",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("ImageDescriptor", "bmpVersion",
                     DATATYPE_STRING, true, null);
        addAttribute("ImageDescriptor", "width",
                     DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);
        addAttribute("ImageDescriptor", "height",
                     DATATYPE_INTEGER, true, null,
                     "1", "65535", true, true);
        addAttribute("ImageDescriptor", "bitsPerPixel",
                     DATATYPE_INTEGER, true, null,
                     "1", "65535", true, true);
        addAttribute("ImageDescriptor", "compression",
                      DATATYPE_INTEGER, false, null);
        addAttribute("ImageDescriptor", "imageSize",
                     DATATYPE_INTEGER, true, null,
                     "1", "65535", true, true);

        addElement("PixelsPerMeter",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("PixelsPerMeter", "X",
                     DATATYPE_INTEGER, false, null,
                     "1", "65535", true, true);
        addAttribute("PixelsPerMeter", "Y",
                     DATATYPE_INTEGER, false, null,
                     "1", "65535", true, true);

        addElement("ColorsUsed",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("ColorsUsed", "value",
                     DATATYPE_INTEGER, true, null,
                     "0", "65535", true, true);

        addElement("ColorsImportant",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("ColorsImportant", "value",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);

        addElement("BI_BITFIELDS_Mask",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("BI_BITFIELDS_Mask", "red",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);
        addAttribute("BI_BITFIELDS_Mask", "green",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);
        addAttribute("BI_BITFIELDS_Mask", "blue",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);

        addElement("ColorSpace",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("ColorSpace", "value",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);

        addElement("LCS_CALIBRATED_RGB",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);

        /// Should the max value be 1.7976931348623157e+308 ?
        addAttribute("LCS_CALIBRATED_RGB", "redX",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "redY",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "redZ",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "greenX",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "greenY",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "greenZ",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "blueX",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "blueY",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB", "blueZ",
                     DATATYPE_DOUBLE, false, null,
                     "0", "65535", true, true);

        addElement("LCS_CALIBRATED_RGB_GAMMA",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("LCS_CALIBRATED_RGB_GAMMA","red",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB_GAMMA","green",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);
        addAttribute("LCS_CALIBRATED_RGB_GAMMA","blue",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);

        addElement("Intent",
                   BMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("Intent", "value",
                     DATATYPE_INTEGER, false, null,
                     "0", "65535", true, true);

        // root -> Palette
        addElement("Palette",
                   BMPMetadata.nativeMetadataFormatName,
                   2, 256);
        addAttribute("Palette", "sizeOfPalette",
                     DATATYPE_INTEGER, true, null);
        addBooleanAttribute("Palette", "sortFlag",
                            false, false);

        // root -> Palette -> PaletteEntry
        addElement("PaletteEntry", "Palette",
                   CHILD_POLICY_EMPTY);
        addAttribute("PaletteEntry", "index",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("PaletteEntry", "red",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("PaletteEntry", "green",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("PaletteEntry", "blue",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);


        // root -> CommentExtensions
        addElement("CommentExtensions",
                   BMPMetadata.nativeMetadataFormatName,
                   1, Integer.MAX_VALUE);

        // root -> CommentExtensions -> CommentExtension
        addElement("CommentExtension", "CommentExtensions",
                   CHILD_POLICY_EMPTY);
        addAttribute("CommentExtension", "value",
                     DATATYPE_STRING, true, null);
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
        return true;
    }

    public static synchronized IIOMetadataFormat getInstance() {
        if (instance == null) {
            instance = new BMPMetadataFormat();
        }
        return instance;
    }
}
