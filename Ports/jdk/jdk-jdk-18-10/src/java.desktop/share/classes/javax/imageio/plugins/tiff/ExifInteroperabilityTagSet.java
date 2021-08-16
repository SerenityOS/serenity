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
 * A class representing the tags found in an Exif Interoperability IFD.
 *
 * @since 9
 * @see   ExifTIFFTagSet
 */
public final class ExifInteroperabilityTagSet extends TIFFTagSet {
    /**
     * A tag indicating the identification of the Interoperability rule
     * (type ASCII).
     *
     * @see #INTEROPERABILITY_INDEX_R98
     * @see #INTEROPERABILITY_INDEX_THM
     */
    public static final int TAG_INTEROPERABILITY_INDEX = 1;

    /**
     * A value to be used with the "InteroperabilityIndex" tag. Indicates
     * a file conforming to the R98 file specification of Recommended Exif
     * Interoperability Rules (ExifR98) or to the DCF basic file stipulated
     * by the Design Rule for Camera File System (type ASCII).
     *
     * @see #TAG_INTEROPERABILITY_INDEX
     */
    public static final String INTEROPERABILITY_INDEX_R98 = "R98";

    /**
     * A value to be used with the "InteroperabilityIndex" tag. Indicates
     * a file conforming to the DCF thumbnail file stipulated by the Design
     * rule for Camera File System (type ASCII).
     *
     * @see #TAG_INTEROPERABILITY_INDEX
     */
    public static final String INTEROPERABILITY_INDEX_THM = "THM";

    private static ExifInteroperabilityTagSet theInstance = null;

    static class InteroperabilityIndex extends TIFFTag {

        public InteroperabilityIndex() {
            super("InteroperabilityIndex",
                  TAG_INTEROPERABILITY_INDEX,
                  1 << TIFFTag.TIFF_ASCII);
        }
    }

    private static List<TIFFTag> tags;

    private static void initTags() {
        tags = new ArrayList<TIFFTag>(42);

        tags.add(new ExifInteroperabilityTagSet.InteroperabilityIndex());
    }

    private ExifInteroperabilityTagSet() {
        super(tags);
    }

    /**
     * Returns the shared instance of
     * {@code ExifInteroperabilityTagSet}.
     *
     * @return the {@code ExifInteroperabilityTagSet} instance.
     */
    public synchronized static ExifInteroperabilityTagSet getInstance() {
        if (theInstance == null) {
            initTags();
            theInstance = new ExifInteroperabilityTagSet();
            tags = null;
        }
        return theInstance;
    }
}
