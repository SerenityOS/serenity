/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jimage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * @implNote This class needs to maintain JDK 8 source compatibility.
 *
 * It is used internally in the JDK to implement jimage/jrtfs access,
 * but also compiled and delivered as part of the jrtfs.jar to support access
 * to the jimage file provided by the shipped JDK by tools running on JDK 8.
 */
public class ImageStream {
    private ByteBuffer buffer;

    public ImageStream() {
        this(1024, ByteOrder.nativeOrder());
    }

    public ImageStream(int size) {
        this(size, ByteOrder.nativeOrder());
    }

    public ImageStream(byte[] bytes) {
       this(bytes, ByteOrder.nativeOrder());
    }

    public ImageStream(ByteOrder byteOrder) {
        this(1024, byteOrder);
    }

    public ImageStream(int size, ByteOrder byteOrder) {
        buffer = ByteBuffer.allocate(size);
        buffer.order(Objects.requireNonNull(byteOrder));
    }

    public ImageStream(byte[] bytes, ByteOrder byteOrder) {
        buffer = ByteBuffer.wrap(Objects.requireNonNull(bytes));
        buffer.order(Objects.requireNonNull(byteOrder));
    }

    public ImageStream(ByteBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer);
    }

    public ImageStream align(int alignment) {
        int padding = (getSize() - 1) & ((1 << alignment) - 1);

        for (int i = 0; i < padding; i++) {
            put((byte)0);
        }

        return this;
    }

    public void ensure(int needs) {
        if (needs < 0) {
            throw new IndexOutOfBoundsException("Bad value: " + needs);
        }

        if (needs > buffer.remaining()) {
            byte[] bytes = buffer.array();
            ByteOrder byteOrder = buffer.order();
            int position = buffer.position();
            int newSize = needs <= bytes.length ? bytes.length << 1 : position + needs;
            buffer = ByteBuffer.allocate(newSize);
            buffer.order(byteOrder);
            buffer.put(bytes, 0, position);
        }
    }

    public boolean hasByte() {
        return buffer.remaining() != 0;
    }

    public boolean hasBytes(int needs) {
        return needs <= buffer.remaining();
    }

    public void skip(int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("skip value = " + n);
        }

        buffer.position(buffer.position() + n);
    }

    public int get() {
        return buffer.get() & 0xFF;
    }

    public void get(byte bytes[], int offset, int size) {
        buffer.get(bytes, offset, size);
    }

    public int getShort() {
        return buffer.getShort();
    }

    public int getInt() {
        return buffer.getInt();
    }

    public long getLong() {
        return buffer.getLong();
    }

    public ImageStream put(byte byt) {
        ensure(1);
        buffer.put(byt);

        return this;
    }

    public ImageStream put(int byt) {
        return put((byte)byt);
    }

    public ImageStream put(byte bytes[], int offset, int size) {
        ensure(size);
        buffer.put(bytes, offset, size);

        return this;
    }

    public ImageStream put(ImageStream stream) {
        put(stream.buffer.array(), 0, stream.buffer.position());

        return this;
    }

    public ImageStream putShort(short value) {
        ensure(2);
        buffer.putShort(value);

        return this;
    }

    public ImageStream putShort(int value) {
        return putShort((short)value);
    }

    public ImageStream putInt(int value) {
        ensure(4);
        buffer.putInt(value);

        return this;
    }

    public ImageStream putLong(long value) {
        ensure(8);
        buffer.putLong(value);

        return this;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getPosition() {
        return buffer.position();
    }

    public int getSize() {
        return buffer.position();
    }

    public byte[] getBytes() {
        return buffer.array();
    }

    public void setPosition(int offset) {
        buffer.position(offset);
    }

    public byte[] toArray() {
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
}
