/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package simp;

import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class SIMPMetadataFormat extends IIOMetadataFormatImpl {

    private static IIOMetadataFormat instance = null;

    public static synchronized IIOMetadataFormat getInstance() {
        if (instance == null) {
            instance = new SIMPMetadataFormat();
        }
        return instance;
    }

    public boolean canNodeAppear(String elementName,
                                 ImageTypeSpecifier imageType) {
        return true;
    }

    private SIMPMetadataFormat() {
        super(SIMPMetadata.nativeMetadataFormatName,
              CHILD_POLICY_SOME);

        addElement("ImageDescriptor",
                   SIMPMetadata.nativeMetadataFormatName,
                   CHILD_POLICY_EMPTY);

        addAttribute("ImageDescriptor", "width",
                     DATATYPE_INTEGER, true, null,
                     "1", "127", true, true);
        addAttribute("ImageDescriptor", "height",
                     DATATYPE_INTEGER, true, null,
                     "1", "127", true, true);
    }
}
