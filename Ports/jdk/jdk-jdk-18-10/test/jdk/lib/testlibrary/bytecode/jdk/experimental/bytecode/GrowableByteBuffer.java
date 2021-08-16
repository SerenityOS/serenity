/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.function.Consumer;

public class GrowableByteBuffer {

    public GrowableByteBuffer() {
    }

    byte[] elems = new byte[64];
    int offset = 0;

    public GrowableByteBuffer writeByte(int b) {
        return writeBytes(1, b);
    }

    public GrowableByteBuffer writeChar(int x) {
        return writeBytes(2, x);
    }

    public GrowableByteBuffer writeInt(int x) {
        return writeBytes(4, x);
    }

    public GrowableByteBuffer writeFloat(float x) {
        return writeInt(Float.floatToIntBits(x));
    }

    public GrowableByteBuffer writeLong(long x) {
        return writeBytes(8, x);
    }

    public GrowableByteBuffer writeDouble(double x) {
        writeLong(Double.doubleToLongBits(x));
        return this;
    }

    public GrowableByteBuffer writeBytes(byte[] barr) {
        expandIfNeeded(barr.length);
        System.arraycopy(barr, 0, elems, offset, barr.length);
        offset += barr.length;
        return this;
    }

    public GrowableByteBuffer writeBytes(GrowableByteBuffer bb) {
        expandIfNeeded(bb.offset);
        System.arraycopy(bb.elems, 0, elems, offset, bb.offset);
        offset += bb.offset;
        return this;
    }

    public GrowableByteBuffer withOffset(int offset, Consumer<GrowableByteBuffer> actions) {
        int prevOffset = this.offset;
        this.offset = offset;
        actions.accept(this);
        this.offset = prevOffset;
        return this;
    }

    private GrowableByteBuffer writeBytes(int size, long x) {
        expandIfNeeded(size);
        for (int i = 0; i < size; i++) {
            elems[offset++] = (byte) ((x >> 8 * (size - i - 1)) & 0xFF);
        }
        return this;
    }

    void expandIfNeeded(int increment) {
        if (offset + increment > elems.length) {
            int newsize = elems.length * 2;
            while (offset + increment > newsize) {
                newsize *= 2;
            }
            byte[] newelems = new byte[newsize];
            System.arraycopy(elems, 0, newelems, 0, offset);
            elems = newelems;
        }
    }

    public byte[] bytes() {
        byte[] bytes = new byte[offset];
        System.arraycopy(elems, 0, bytes, 0, offset);
        return bytes;
    }
}
