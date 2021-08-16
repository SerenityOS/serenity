/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class GIFStreamMetadataFormat extends IIOMetadataFormatImpl {

    private static IIOMetadataFormat instance = null;

    private GIFStreamMetadataFormat() {
        super(GIFStreamMetadata.nativeMetadataFormatName,
              CHILD_POLICY_SOME);

        // root -> Version
        addElement("Version", GIFStreamMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("Version", "value",
                     DATATYPE_STRING, true, null,
                     Arrays.asList(GIFStreamMetadata.versionStrings));

        // root -> LogicalScreenDescriptor
        addElement("LogicalScreenDescriptor",
                   GIFStreamMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);
        addAttribute("LogicalScreenDescriptor", "logicalScreenWidth",
                     DATATYPE_INTEGER, true, null,
                     "1", "65535", true, true);
        addAttribute("LogicalScreenDescriptor", "logicalScreenHeight",
                     DATATYPE_INTEGER, true, null,
                     "1", "65535", true, true);
        addAttribute("LogicalScreenDescriptor", "colorResolution",
                     DATATYPE_INTEGER, true, null,
                     "1", "8", true, true);
        addAttribute("LogicalScreenDescriptor", "pixelAspectRatio",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);

        // root -> GlobalColorTable
        addElement("GlobalColorTable",
                   GIFStreamMetadata.nativeMetadataFormatName,
                   2, 256);
        addAttribute("GlobalColorTable", "sizeOfGlobalColorTable",
                     DATATYPE_INTEGER, true, null,
                     Arrays.asList(GIFStreamMetadata.colorTableSizes));
        addAttribute("GlobalColorTable", "backgroundColorIndex",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addBooleanAttribute("GlobalColorTable", "sortFlag",
                            false, false);

        // root -> GlobalColorTable -> ColorTableEntry
        addElement("ColorTableEntry", "GlobalColorTable",
                   CHILD_POLICY_EMPTY);
        addAttribute("ColorTableEntry", "index",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("ColorTableEntry", "red",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("ColorTableEntry", "green",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
        addAttribute("ColorTableEntry", "blue",
                     DATATYPE_INTEGER, true, null,
                     "0", "255", true, true);
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
        return true;
    }

    public static synchronized IIOMetadataFormat getInstance() {
        if (instance == null) {
            instance = new GIFStreamMetadataFormat();
        }
        return instance;
    }
}
