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

package com.sun.imageio.plugins.gif;

import java.util.ListResourceBundle;

public class GIFStreamMetadataFormatResources extends ListResourceBundle {

    public GIFStreamMetadataFormatResources() {}

    protected Object[][] getContents() {
        return new Object[][] {

        // Node name, followed by description
        { "Version", "The file version, either 87a or 89a" },
        { "LogicalScreenDescriptor",
          "The logical screen descriptor, except for the global color table" },
        { "GlobalColorTable", "The global color table" },
        { "ColorTableEntry", "A global color table entry" },

        // Node name + "/" + AttributeName, followed by description
        { "Version/value",
          "The version string" },
        { "LogicalScreenDescriptor/logicalScreenWidth",
          "The width in pixels of the whole picture" },
        { "LogicalScreenDescriptor/logicalScreenHeight",
          "The height in pixels of the whole picture" },
        { "LogicalScreenDescriptor/colorResolution",
          "The number of bits of color resolution, beteen 1 and 8" },
        { "LogicalScreenDescriptor/pixelAspectRatio",
          "If 0, indicates square pixels, else W/H = (value + 15)/64" },
        { "GlobalColorTable/sizeOfGlobalColorTable",
          "The number of entries in the global color table" },
        { "GlobalColorTable/backgroundColorIndex",
          "The index of the color table entry to be used as a background" },
        { "GlobalColorTable/sortFlag",
          "True if the global color table is sorted by frequency" },
        { "ColorTableEntry/index", "The index of the color table entry" },
        { "ColorTableEntry/red",
          "The red value for the color table entry" },
        { "ColorTableEntry/green",
          "The green value for the color table entry" },
        { "ColorTableEntry/blue",
          "The blue value for the color table entry" },

        };
    }
}
