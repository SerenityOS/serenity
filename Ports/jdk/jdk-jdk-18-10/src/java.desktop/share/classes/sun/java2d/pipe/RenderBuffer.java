/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import jdk.internal.misc.Unsafe;


/**
 * The RenderBuffer class is a simplified, high-performance, Unsafe wrapper
 * used for buffering rendering operations in a single-threaded rendering
 * environment.  It's functionality is similar to the ByteBuffer and related
 * NIO classes.  However, the methods in this class perform little to no
 * alignment or bounds checks for performance reasons.  Therefore, it is
 * the caller's responsibility to ensure that all put() calls are properly
 * aligned and within bounds:
 *   - int and float values must be aligned on 4-byte boundaries
 *   - long and double values must be aligned on 8-byte boundaries
 *
 * This class only includes the bare minimum of methods to support
 * single-threaded rendering.  For example, there is no put(double[]) method
 * because we currently have no need for such a method in the STR classes.
 */
public class RenderBuffer {

    /**
     * These constants represent the size of various data types (in bytes).
     */
    protected static final long SIZEOF_BYTE   = 1L;
    protected static final long SIZEOF_SHORT  = 2L;
    protected static final long SIZEOF_INT    = 4L;
    protected static final long SIZEOF_FLOAT  = 4L;
    protected static final long SIZEOF_LONG   = 8L;
    protected static final long SIZEOF_DOUBLE = 8L;

    /**
     * Represents the number of elements at which we have empirically
     * determined that the average cost of a JNI call exceeds the expense
     * of an element by element copy.  In other words, if the number of
     * elements in an array to be copied exceeds this value, then we should
     * use the copyFromArray() method to complete the bulk put operation.
     * (This value can be adjusted if the cost of JNI downcalls is reduced
     * in a future release.)
     */
    private static final int COPY_FROM_ARRAY_THRESHOLD = 6;

    protected final Unsafe unsafe;
    protected final long baseAddress;
    protected final long endAddress;
    protected long curAddress;
    protected final int capacity;

    protected RenderBuffer(int numBytes) {
        unsafe = Unsafe.getUnsafe();
        curAddress = baseAddress = unsafe.allocateMemory(numBytes);
        endAddress = baseAddress + numBytes;
        capacity = numBytes;
    }

    /**
     * Allocates a fresh buffer using the machine endianness.
     */
    public static RenderBuffer allocate(int numBytes) {
        return new RenderBuffer(numBytes);
    }

    /**
     * Returns the base address of the underlying memory buffer.
     */
    public final long getAddress() {
        return baseAddress;
    }

    /**
     * The behavior (and names) of the following methods are nearly
     * identical to their counterparts in the various NIO Buffer classes.
     */

    public final int capacity() {
        return capacity;
    }

    public final int remaining() {
        return (int)(endAddress - curAddress);
    }

    public final int position() {
        return (int)(curAddress - baseAddress);
    }

    public final void position(long numBytes) {
        curAddress = baseAddress + numBytes;
    }

    public final void clear() {
        curAddress = baseAddress;
    }

    public final RenderBuffer skip(long numBytes) {
        curAddress += numBytes;
        return this;
    }

    /**
     * putByte() methods...
     */

    public final RenderBuffer putByte(byte x) {
        unsafe.putByte(curAddress, x);
        curAddress += SIZEOF_BYTE;
        return this;
    }

    public RenderBuffer put(byte[] x) {
        return put(x, 0, x.length);
    }

    public RenderBuffer put(byte[] x, int offset, int length) {
        if (length > COPY_FROM_ARRAY_THRESHOLD) {
            long offsetInBytes = offset * SIZEOF_BYTE + Unsafe.ARRAY_BYTE_BASE_OFFSET;
            long lengthInBytes = length * SIZEOF_BYTE;
            unsafe.copyMemory(x, offsetInBytes, null, curAddress, lengthInBytes);
            position(position() + lengthInBytes);
        } else {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                putByte(x[i]);
            }
        }
        return this;
    }

    /**
     * putShort() methods...
     */

    public final RenderBuffer putShort(short x) {
        // assert (position() % SIZEOF_SHORT == 0);
        unsafe.putShort(curAddress, x);
        curAddress += SIZEOF_SHORT;
        return this;
    }

    public RenderBuffer put(short[] x) {
        return put(x, 0, x.length);
    }

    public RenderBuffer put(short[] x, int offset, int length) {
        // assert (position() % SIZEOF_SHORT == 0);
        if (length > COPY_FROM_ARRAY_THRESHOLD) {
            long offsetInBytes = offset * SIZEOF_SHORT + Unsafe.ARRAY_SHORT_BASE_OFFSET;
            long lengthInBytes = length * SIZEOF_SHORT;
            unsafe.copyMemory(x, offsetInBytes, null, curAddress, lengthInBytes);
            position(position() + lengthInBytes);
        } else {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                putShort(x[i]);
            }
        }
        return this;
    }

    /**
     * putInt() methods...
     */

    public final RenderBuffer putInt(int pos, int x) {
        // assert (baseAddress + pos % SIZEOF_INT == 0);
        unsafe.putInt(baseAddress + pos, x);
        return this;
    }

    public final RenderBuffer putInt(int x) {
        // assert (position() % SIZEOF_INT == 0);
        unsafe.putInt(curAddress, x);
        curAddress += SIZEOF_INT;
        return this;
    }

    public RenderBuffer put(int[] x) {
        return put(x, 0, x.length);
    }

    public RenderBuffer put(int[] x, int offset, int length) {
        // assert (position() % SIZEOF_INT == 0);
        if (length > COPY_FROM_ARRAY_THRESHOLD) {
            long offsetInBytes = offset * SIZEOF_INT + Unsafe.ARRAY_INT_BASE_OFFSET;
            long lengthInBytes = length * SIZEOF_INT;
            unsafe.copyMemory(x, offsetInBytes, null, curAddress, lengthInBytes);
            position(position() + lengthInBytes);
        } else {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                putInt(x[i]);
            }
        }
        return this;
    }

    /**
     * putFloat() methods...
     */

    public final RenderBuffer putFloat(float x) {
        // assert (position() % SIZEOF_FLOAT == 0);
        unsafe.putFloat(curAddress, x);
        curAddress += SIZEOF_FLOAT;
        return this;
    }

    public RenderBuffer put(float[] x) {
        return put(x, 0, x.length);
    }

    public RenderBuffer put(float[] x, int offset, int length) {
        // assert (position() % SIZEOF_FLOAT == 0);
        if (length > COPY_FROM_ARRAY_THRESHOLD) {
            long offsetInBytes = offset * SIZEOF_FLOAT + Unsafe.ARRAY_FLOAT_BASE_OFFSET;
            long lengthInBytes = length * SIZEOF_FLOAT;
            unsafe.copyMemory(x, offsetInBytes, null, curAddress, lengthInBytes);
            position(position() + lengthInBytes);
        } else {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                putFloat(x[i]);
            }
        }
        return this;
    }

    /**
     * putLong() methods...
     */

    public final RenderBuffer putLong(long x) {
        // assert (position() % SIZEOF_LONG == 0);
        unsafe.putLong(curAddress, x);
        curAddress += SIZEOF_LONG;
        return this;
    }

    public RenderBuffer put(long[] x) {
        return put(x, 0, x.length);
    }

    public RenderBuffer put(long[] x, int offset, int length) {
        // assert (position() % SIZEOF_LONG == 0);
        if (length > COPY_FROM_ARRAY_THRESHOLD) {
            long offsetInBytes = offset * SIZEOF_LONG + Unsafe.ARRAY_LONG_BASE_OFFSET;
            long lengthInBytes = length * SIZEOF_LONG;
            unsafe.copyMemory(x, offsetInBytes, null, curAddress, lengthInBytes);
            position(position() + lengthInBytes);
        } else {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                putLong(x[i]);
            }
        }
        return this;
    }

    /**
     * putDouble() method(s)...
     */

    public final RenderBuffer putDouble(double x) {
        // assert (position() % SIZEOF_DOUBLE == 0);
        unsafe.putDouble(curAddress, x);
        curAddress += SIZEOF_DOUBLE;
        return this;
    }
}
