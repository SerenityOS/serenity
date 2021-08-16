/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import jdk.internal.misc.Unsafe;
import java.util.Vector;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This class contains the collection of utility functions to help work with
 * native data types on different platforms similarly.
 */

class Native {

    private static Unsafe unsafe = XlibWrapper.unsafe;

    static int longSize;

    static int dataModel;
    static {
        @SuppressWarnings("removal")
        String dataModelProp = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("sun.arch.data.model");
                }
            });
        try {
            dataModel = Integer.parseInt(dataModelProp);
        } catch (Exception e) {
            dataModel = 32;
        }
        if (dataModel == 32) {
            longSize = 4;
        } else {
            longSize = 8;
        }
    }

    /**
     * Set of helper function to read data of different PLATFORM types
     * from memory pointer by {@code ptr}
     * Note, names of types in function are NATIVE PLATFORM types
     * and they have the same size as they would have in C compiler
     * on the same platform.
     */

    static boolean getBool(long ptr) { return getInt(ptr) != 0; }
    static boolean getBool(long ptr, int index) { return getInt(ptr, index) != 0; }
    static void putBool(long ptr, boolean data) { putInt(ptr, (data)?(1):(0)); }
    static void putBool(long ptr, int index, boolean data) { putInt(ptr, index, (data)?(1):(0)); }


    /**
     * Access to C byte data(one byte)
     */
    static int getByteSize() { return 1; }
    static byte getByte(long ptr) { return unsafe.getByte(ptr); }

    static byte getByte(long ptr, int index) {
        return getByte(ptr+index);
    }
    /**
     * Stores to C byte data(one byte)
     */
    static void putByte(long ptr, byte data) { unsafe.putByte(ptr, data); }

    static void putByte(long ptr, int index, byte data) {
        putByte(ptr+index, data);
    }
    /**
     * Converts length bytes of data pointed by {@code data} into byte array
     * Returns null if data is zero
     * @param data native pointer to native memory
     * @param length size in bytes of native memory
     */
    static byte[] toBytes(long data, int length) {
        if (data == 0) {
            return null;
        }
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++, data++) {
            res[i] = getByte(data);
        }
        return res;
    }
    /**
     * Stores byte array into native memory and returns pointer to this memory
     * Returns 0 if bytes is null
     */
    static long toData(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            putByte(res+i, bytes[i]);
        }
        return res;
    }

    /**
     * Access to C unsigned byte data(one byte)
     */
    static int getUByteSize() { return 1; }
    static short getUByte(long ptr) { return (short)(0xFF & unsafe.getByte(ptr));  }

    static short getUByte(long ptr, int index) {
        return getUByte(ptr+index);
    }

    /**
     * Stores to C unsigned byte data(one byte)
     */
    static void putUByte(long ptr, short data) { unsafe.putByte(ptr, (byte)data); }

    static void putUByte(long ptr, int index, short data) {
        putUByte(ptr+index, data);
    }

    /**
     * Converts length usnigned bytes of data pointed by {@code data} into
     * short array
     * Returns null if data is zero
     * @param data native pointer to native memory
     * @param length size in bytes of native memory
     */
    static short[] toUBytes(long data, int length) {
        if (data == 0) {
            return null;
        }
        short[] res = new short[length];
        for (int i = 0; i < length; i++, data++) {
            res[i] = getUByte(data);
        }
        return res;
    }
    /**
     * Stores short array as unsigned bytes into native memory and returns pointer
     * to this memory
     * Returns 0 if bytes is null
     */
    static long toUData(short[] bytes) {
        if (bytes == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            putUByte(res+i, bytes[i]);
        }
        return res;
    }

    /**
     * Access to C short data(two bytes)
     */
    static int getShortSize() { return 2; }
    static short getShort(long ptr) { return unsafe.getShort(ptr); }
    /**
     * Stores to C short data(two bytes)
     */
    static void putShort(long ptr, short data) { unsafe.putShort(ptr, data); }
    static void putShort(long ptr, int index, short data) {
        putShort(ptr + index*getShortSize(), data);
    }
    static long toData(short[] shorts) {
        if (shorts == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(shorts.length*getShortSize());
        for (int i = 0; i < shorts.length; i++) {
            putShort(res, i, shorts[i]);
        }
        return res;
    }

    /**
     * Access to C unsigned short data(two bytes)
     */
    static int getUShortSize() { return 2; }

    static int getUShort(long ptr) { return 0xFFFF & unsafe.getShort(ptr); }
    /**
     * Stores to C unsigned short data(two bytes)
     */
    static void putUShort(long ptr, int data) { unsafe.putShort(ptr, (short)data); }
    static void putUShort(long ptr, int index, int data) {
        putUShort(ptr + index*getShortSize(), data);
    }

    /**
     * Stores int array as unsigned shorts into native memory and returns pointer
     * to this memory
     * Returns 0 if bytes is null
     */
    static long toUData(int[] shorts) {
        if (shorts == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(shorts.length*getShortSize());
        for (int i = 0; i < shorts.length; i++) {
            putUShort(res, i, shorts[i]);
        }
        return res;
    }

    /**
     * Access to C int data(four bytes)
     */
    static int getIntSize() { return 4; }
    static int getInt(long ptr) { return unsafe.getInt(ptr); }
    static int getInt(long ptr, int index) { return getInt(ptr +getIntSize()*index); }
    /**
     * Stores to C int data(four bytes)
     */
    static void putInt(long ptr, int data) { unsafe.putInt(ptr, data); }
    static void putInt(long ptr, int index, int data) {
        putInt(ptr + index*getIntSize(), data);
    }
    static long toData(int[] ints) {
        if (ints == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(ints.length*getIntSize());
        for (int i = 0; i < ints.length; i++) {
            putInt(res, i, ints[i]);
        }
        return res;
    }

    /**
     * Access to C unsigned int data(four bytes)
     */
    static int getUIntSize() { return 4; }
    static long getUInt(long ptr) { return 0xFFFFFFFFL & unsafe.getInt(ptr); }
    static long getUInt(long ptr, int index) { return getUInt(ptr +getIntSize()*index); }
    /**
     * Stores to C unsigned int data(four bytes)
     */
    static void putUInt(long ptr, long data) { unsafe.putInt(ptr, (int)data); }
    static void putUInt(long ptr, int index, long data) {
        putUInt(ptr + index*getIntSize(), data);
    }

    /**
     * Stores long array as unsigned intss into native memory and returns pointer
     * to this memory
     * Returns 0 if bytes is null
     */
    static long toUData(long[] ints) {
        if (ints == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(ints.length*getIntSize());
        for (int i = 0; i < ints.length; i++) {
            putUInt(res, i, ints[i]);
        }
        return res;
    }

    /**
     * Access to C long data(size depends on platform)
     */
    static int getLongSize() {
        return longSize;
    }
    static long getLong(long ptr) {
        if (XlibWrapper.dataModel == 32) {
            return unsafe.getInt(ptr);
        } else {
            return unsafe.getLong(ptr);
        }
    }
    /**
     * Stores to C long data(four bytes)
     * Note: {@code data} has {@code long} type
     * to be able to keep 64-bit C {@code long} data
     */
    static void putLong(long ptr, long data) {
        if (XlibWrapper.dataModel == 32) {
            unsafe.putInt(ptr, (int)data);
        } else {
            unsafe.putLong(ptr, data);
        }
    }

    static void putLong(long ptr, int index, long data) {
        putLong(ptr+index*getLongSize(), data);
    }

    /**
     * Returns index's element of the array of native long pointed by ptr
     */
    static long getLong(long ptr, int index) {
        return getLong(ptr + index*getLongSize());
    }
    /**
     * Stores Java long[] array into memory. Memory location is treated as array
     * of native {@code long}s
     */
    static void put(long ptr, long[] arr) {
        for (int i = 0; i < arr.length; i ++, ptr += getLongSize()) {
            putLong(ptr, arr[i]);
        }
    }

    /**
     * Stores Java Vector of Longs into memory. Memory location is treated as array
     * of native {@code long}s
     */
    static void putLong(long ptr, Vector<Long> arr) {
        for (int i = 0; i < arr.size(); i ++, ptr += getLongSize()) {
            putLong(ptr, arr.elementAt(i).longValue());
        }
    }

    /**
     * Stores Java Vector of Longs into memory. Memory location is treated as array
     * of native {@code long}s. Array is stored in reverse order
     */
    static void putLongReverse(long ptr, Vector<Long> arr) {
        for (int i = arr.size()-1; i >= 0; i--, ptr += getLongSize()) {
            putLong(ptr, arr.elementAt(i).longValue());
        }
    }
    /**
     * Converts length bytes of data pointed by {@code data} into byte array
     * Returns null if data is zero
     * @param data native pointer to native memory
     * @param length size in longs(platform dependent) of native memory
     */
    static long[] toLongs(long data, int length) {
        if (data == 0) {
            return null;
        }
        long[] res = new long[length];
        for (int i = 0; i < length; i++, data += getLongSize()) {
            res[i] = getLong(data);
        }
        return res;
    }
    static long toData(long[] longs) {
        if (longs == null) {
            return 0;
        }
        long res = XlibWrapper.unsafe.allocateMemory(longs.length*getLongSize());
        for (int i = 0; i < longs.length; i++) {
            putLong(res, i, longs[i]);
        }
        return res;
    }


    /**
     * Access to C "unsigned long" date type, which is XID in X
     */
    static long getULong(long ptr) {
        if (XlibWrapper.dataModel == 32) {
            // Compensate sign-expansion
            return ((long)unsafe.getInt(ptr)) & 0xFFFFFFFFL;
        } else {
            // Can't do anything!!!
            return unsafe.getLong(ptr);
        }
    }

    static void putULong(long ptr, long value) {
        putLong(ptr, value);
    }

    /**
     * Allocates memory for array of native {@code long}s of the size {@code length}
     */
    static long allocateLongArray(int length) {
        return unsafe.allocateMemory(getLongSize() * length);
    }


    static long getWindow(long ptr) {
        return getLong(ptr);
    }
    static long getWindow(long ptr, int index) {
        return getLong(ptr + getWindowSize()*index);
    }

    static void putWindow(long ptr, long window) {
        putLong(ptr, window);
    }

    static void putWindow(long ptr, int index, long window) {
        putLong(ptr, index, window);
    }

    /**
     * Set of function to return sizes of C data of the appropriate
     * type.
     */
    static int getWindowSize() {
        return getLongSize();
    }


    /**
     * Set of function to access CARD32 type. All data which types are derived
     * from CARD32 should be accessed using this accessors.
     * These types are: XID(Window, Drawable, Font, Pixmap, Cursor, Colormap, GContext, KeySym),
     *                  Atom, Mask, VisualID, Time
     */
    static long getCard32(long ptr) {
        return getLong(ptr);
    }
    static void putCard32(long ptr, long value) {
        putLong(ptr, value);
    }
    static long getCard32(long ptr, int index) {
        return getLong(ptr, index);
    }
    static void putCard32(long ptr, int index, long value) {
        putLong(ptr, index, value);
    }
    static int getCard32Size() {
        return getLongSize();
    }
    static long[] card32ToArray(long ptr, int length) {
        return toLongs(ptr, length);
    }
    static long card32ToData(long[] arr) {
        return toData(arr);
    }
}
