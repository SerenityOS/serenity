/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ListResourceBundle;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class BMPMetadataFormatResources extends ListResourceBundle {

    public BMPMetadataFormatResources() {}

    protected Object[][] getContents() {
        return new Object[][] {

        // Node name, followed by description
        { "BMPVersion", "BMP version string" },
        { "Width", "The width of the image" },
        { "Height","The height of the image" },
        { "BitsPerPixel", "" },
        { "PixelsPerMeter", "Resolution in pixels per unit distance" },
        { "X", "Pixels Per Meter along X" },
        { "Y", "Pixels Per Meter along Y" },
        { "ColorsUsed",
          "Number of color indexes in the color table actually used" },
        { "ColorsImportant",
          "Number of color indexes considered important for display" },
        { "Mask",
          "Color masks; present for BI_BITFIELDS compression only"},

        { "Intent", "Rendering intent" },
        { "Palette", "The color palette" },

        { "Red", "Red Mask/Color Palette" },
        { "Green", "Green Mask/Color Palette/Gamma" },
        { "Blue", "Blue Mask/Color Palette/Gamma" },
        { "Alpha", "Alpha Mask/Color Palette/Gamma" },

        { "ColorSpaceType", "Color Space Type" },

        { "X", "The X coordinate of a point in XYZ color space" },
        { "Y", "The Y coordinate of a point in XYZ color space" },
        { "Z", "The Z coordinate of a point in XYZ color space" },
        };
    }
}
