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
package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * A class containing the TIFF tags used to reference the Exif and GPS IFDs.
 * This tag set should be added to the root tag set by means of the
 * {@link TIFFImageReadParam#addAllowedTagSet(TIFFTagSet)
 * TIFFImageReadParam.addAllowedTagSet} method if Exif
 * support is desired.
 *
 * @since 9
 */
public final class ExifParentTIFFTagSet extends TIFFTagSet {

    private static ExifParentTIFFTagSet theInstance = null;

    // 34665 - Exif IFD Pointer                   (LONG/1)
    /** Tag pointing to the Exif IFD (type LONG). */
    public static final int TAG_EXIF_IFD_POINTER = 34665;

    /** Tag pointing to a GPS info IFD (type LONG). */
    public static final int TAG_GPS_INFO_IFD_POINTER = 34853;

    // To be inserted into parent (root) TIFFTagSet
    static class ExifIFDPointer extends TIFFTag {

        public ExifIFDPointer() {
            super("ExifIFDPointer",
                  TAG_EXIF_IFD_POINTER,
                  ExifTIFFTagSet.getInstance());
        }
    }

    // To be inserted into parent (root) TIFFTagSet
    static class GPSInfoIFDPointer extends TIFFTag {

        public GPSInfoIFDPointer() {
            super("GPSInfoIFDPointer",
                  TAG_GPS_INFO_IFD_POINTER,
                  ExifGPSTagSet.getInstance());
        }
    }

    private static List<TIFFTag> tags;

    private static void initTags() {
        tags = new ArrayList<TIFFTag>(1);
        tags.add(new ExifParentTIFFTagSet.ExifIFDPointer());
        tags.add(new ExifParentTIFFTagSet.GPSInfoIFDPointer());
    }

    private ExifParentTIFFTagSet() {
        super(tags);
    }

    /**
     * Returns a shared instance of an {@code ExifParentTIFFTagSet}.
     *
     * @return an {@code ExifParentTIFFTagSet} instance.
     */
    public synchronized static ExifParentTIFFTagSet getInstance() {
        if (theInstance == null) {
            initTags();
            theInstance = new ExifParentTIFFTagSet();
            tags = null;
        }
        return theInstance;
    }
}
