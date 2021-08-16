/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.HashMap;
import jdk.internal.jimage.ImageStream;
import jdk.internal.jimage.ImageStrings;
import jdk.internal.jimage.ImageStringsReader;

class ImageStringsWriter implements ImageStrings {
    private static final int NOT_FOUND = -1;
    static final int EMPTY_OFFSET = 0;

    private final HashMap<String, Integer> stringToOffsetMap;
    private final ImageStream stream;

    ImageStringsWriter() {
        this.stringToOffsetMap = new HashMap<>();
        this.stream = new ImageStream();

        // Reserve 0 offset for empty string.
        int offset = addString("");
        if (offset != 0) {
            throw new InternalError("Empty string not offset zero");
        }

        // Reserve 1 offset for frequently used ".class".
        offset = addString("class");
        if (offset != 1) {
            throw new InternalError("'class' string not offset one");
        }
    }

    private int addString(final String string) {
        int offset = stream.getPosition();
        byte[] bytes = ImageStringsReader.mutf8FromString(string);
        stream.put(bytes, 0, bytes.length);
        stream.put('\0');
        stringToOffsetMap.put(string, offset);

        return offset;
    }

    @Override
    public int add(final String string) {
        int offset = find(string);

        return offset == NOT_FOUND ? addString(string) : offset;
    }

    int find(final String string) {
        Integer offset = stringToOffsetMap.get(string);

        return offset != null ? offset : NOT_FOUND;
    }

    @Override
    public String get(int offset) {
        ByteBuffer buffer = stream.getBuffer();
        int capacity = buffer.capacity();
        if (offset < 0 || offset >= capacity) {
            throw new InternalError("String buffer offset out of range");
        }
        int zero = NOT_FOUND;
        for (int i = offset; i < capacity; i++) {
            if (buffer.get(i) == '\0') {
                zero = i;
                break;
            }
        }
        if (zero == NOT_FOUND) {
            throw new InternalError("String zero terminator not found");
        }
        int length = zero - offset;
        byte[] bytes = new byte[length];
        int mark = buffer.position();
        buffer.position(offset);
        buffer.get(bytes);
        buffer.position(mark);

        return ImageStringsReader.stringFromMUTF8(bytes);
    }

    ImageStream getStream() {
        return stream;
    }

    int getSize() {
        return stream.getSize();
    }

    int getCount() {
        return stringToOffsetMap.size();
    }
}
