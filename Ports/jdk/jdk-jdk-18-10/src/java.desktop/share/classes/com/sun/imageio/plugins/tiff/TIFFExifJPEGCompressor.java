/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import javax.imageio.ImageWriteParam;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;

/**
 * A {@code TIFFCompressor} for the JPEG variant of Exif.
 */
public class TIFFExifJPEGCompressor extends TIFFBaseJPEGCompressor {
    public TIFFExifJPEGCompressor(ImageWriteParam param) {
        super(TIFFImageWriter.EXIF_JPEG_COMPRESSION_TYPE,
              BaselineTIFFTagSet.COMPRESSION_OLD_JPEG,
              false,
              param);
    }

    public void setMetadata(IIOMetadata metadata) {
        // Set the metadata.
        super.setMetadata(metadata);

        // Initialize the JPEG writer and writeparam.
        initJPEGWriter(false, // No stream metadata (not writing abbreviated)
                       true); // Yes image metadata (remove APPn markers)
    }
}
