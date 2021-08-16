/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import jdk.internal.jimage.ImageLocation;
import jdk.internal.jimage.ImageStream;
import jdk.internal.jimage.ImageStringsReader;

public final class ImageLocationWriter extends ImageLocation {
    private int locationOffset;

    private ImageLocationWriter(ImageStringsWriter strings) {
        super(new long[ATTRIBUTE_COUNT], strings);
    }

    void writeTo(ImageStream stream) {
        byte[] bytes = ImageLocation.compress(attributes);
        locationOffset = stream.getPosition();
        stream.put(bytes, 0, bytes.length);
    }

    private ImageLocationWriter addAttribute(int kind, long value) {
        assert ATTRIBUTE_END < kind &&
               kind < ATTRIBUTE_COUNT : "Invalid attribute kind";
        attributes[kind] = value;
        return this;
    }

    private ImageLocationWriter addAttribute(int kind, String value) {
        return addAttribute(kind, strings.add(value));
    }

    static ImageLocationWriter newLocation(String fullName,
            ImageStringsWriter strings,
            long contentOffset, long compressedSize, long uncompressedSize) {
        String moduleName = "";
        String parentName = "";
        String baseName;
        String extensionName = "";

        if (fullName.startsWith("/modules/")) {
            moduleName = "modules";
            baseName = fullName.substring("/modules/".length());
        } else if ( fullName.startsWith("/packages/")) {
            moduleName = "packages";
            baseName = fullName.substring("/packages/".length());
        } else {
            int offset = fullName.indexOf('/', 1);
            if (fullName.length() >= 2 && fullName.charAt(0) == '/' && offset != -1) {
                moduleName = fullName.substring(1, offset);
                fullName = fullName.substring(offset + 1);
            }

            offset = fullName.lastIndexOf('/');
            if (1 < offset) {
                parentName = fullName.substring(0, offset);
                fullName = fullName.substring(offset + 1);
            }

            offset = fullName.lastIndexOf('.');
            if (offset != -1) {
                baseName = fullName.substring(0, offset);
                extensionName = fullName.substring(offset + 1);
            } else {
                baseName = fullName;
            }
        }

        return new ImageLocationWriter(strings)
               .addAttribute(ATTRIBUTE_MODULE, moduleName)
               .addAttribute(ATTRIBUTE_PARENT, parentName)
               .addAttribute(ATTRIBUTE_BASE, baseName)
               .addAttribute(ATTRIBUTE_EXTENSION, extensionName)
               .addAttribute(ATTRIBUTE_OFFSET, contentOffset)
               .addAttribute(ATTRIBUTE_COMPRESSED, compressedSize)
               .addAttribute(ATTRIBUTE_UNCOMPRESSED, uncompressedSize);
    }

    @Override
    public int hashCode() {
        return hashCode(ImageStringsReader.HASH_MULTIPLIER);
    }

    int hashCode(int seed) {
        int hash = seed;

        if (getModuleOffset() != 0) {
            hash = ImageStringsReader.unmaskedHashCode("/", hash);
            hash = ImageStringsReader.unmaskedHashCode(getModule(), hash);
            hash = ImageStringsReader.unmaskedHashCode("/", hash);
        }

        if (getParentOffset() != 0) {
            hash = ImageStringsReader.unmaskedHashCode(getParent(), hash);
            hash = ImageStringsReader.unmaskedHashCode("/", hash);
        }

        hash = ImageStringsReader.unmaskedHashCode(getBase(), hash);

        if (getExtensionOffset() != 0) {
            hash = ImageStringsReader.unmaskedHashCode(".", hash);
            hash = ImageStringsReader.unmaskedHashCode(getExtension(), hash);
        }

        return hash & ImageStringsReader.POSITIVE_MASK;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ImageLocationWriter)) {
            return false;
        }

        ImageLocationWriter other = (ImageLocationWriter) obj;

        return getModuleOffset() == other.getModuleOffset() &&
               getParentOffset() == other.getParentOffset() &&
               getBaseOffset() == other.getBaseOffset() &&
               getExtensionOffset() == other.getExtensionOffset();
    }

    int getLocationOffset() {
        return locationOffset;
    }
}
