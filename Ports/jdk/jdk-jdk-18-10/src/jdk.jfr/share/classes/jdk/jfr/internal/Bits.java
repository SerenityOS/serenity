/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import jdk.internal.misc.Unsafe;

final class Bits {                            // package-private

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final boolean unalignedAccess = unsafe.unalignedAccess();
    private static final boolean bigEndian = unsafe.isBigEndian();

    private Bits() { }

    // -- Swapping --

    private static short swap(short x) {
        return Short.reverseBytes(x);
    }

    private static char swap(char x) {
        return Character.reverseBytes(x);
    }

    private static int swap(int x) {
        return Integer.reverseBytes(x);
    }

    private static long swap(long x) {
        return Long.reverseBytes(x);
    }

    private static float swap(float x) {
        return Float.intBitsToFloat(swap(Float.floatToIntBits(x)));
    }

    private static double swap(double x) {
        return Double.longBitsToDouble(swap(Double.doubleToLongBits(x)));
    }

    // -- Alignment --

    private static boolean isAddressAligned(long a, int datumSize) {
        return (a & datumSize - 1) == 0;
    }

    // -- Primitives stored per byte

    private static byte char1(char x) { return (byte)(x >> 8); }
    private static byte char0(char x) { return (byte)(x     ); }

    private static byte short1(short x) { return (byte)(x >> 8); }
    private static byte short0(short x) { return (byte)(x     ); }

    private static byte int3(int x) { return (byte)(x >> 24); }
    private static byte int2(int x) { return (byte)(x >> 16); }
    private static byte int1(int x) { return (byte)(x >>  8); }
    private static byte int0(int x) { return (byte)(x      ); }

    private static byte long7(long x) { return (byte)(x >> 56); }
    private static byte long6(long x) { return (byte)(x >> 48); }
    private static byte long5(long x) { return (byte)(x >> 40); }
    private static byte long4(long x) { return (byte)(x >> 32); }
    private static byte long3(long x) { return (byte)(x >> 24); }
    private static byte long2(long x) { return (byte)(x >> 16); }
    private static byte long1(long x) { return (byte)(x >>  8); }
    private static byte long0(long x) { return (byte)(x      ); }

    private static void putCharBigEndianUnaligned(long a, char x) {
        putByte_(a    , char1(x));
        putByte_(a + 1, char0(x));
    }

    private static void putShortBigEndianUnaligned(long a, short x) {
        putByte_(a    , short1(x));
        putByte_(a + 1, short0(x));
    }

    private static void putIntBigEndianUnaligned(long a, int x) {
        putByte_(a    , int3(x));
        putByte_(a + 1, int2(x));
        putByte_(a + 2, int1(x));
        putByte_(a + 3, int0(x));
    }

    private static void putLongBigEndianUnaligned(long a, long x) {
        putByte_(a    , long7(x));
        putByte_(a + 1, long6(x));
        putByte_(a + 2, long5(x));
        putByte_(a + 3, long4(x));
        putByte_(a + 4, long3(x));
        putByte_(a + 5, long2(x));
        putByte_(a + 6, long1(x));
        putByte_(a + 7, long0(x));
    }

    private static void putFloatBigEndianUnaligned(long a, float x) {
        putIntBigEndianUnaligned(a, Float.floatToRawIntBits(x));
    }

    private static void putDoubleBigEndianUnaligned(long a, double x) {
        putLongBigEndianUnaligned(a, Double.doubleToRawLongBits(x));
    }

    private static void putByte_(long a, byte b) {
        unsafe.putByte(a, b);
    }

    private static void putBoolean_(long a, boolean x) {
        unsafe.putBoolean(null, a, x);
    }

    private static void putChar_(long a, char x) {
        unsafe.putChar(a, bigEndian ? x : swap(x));
    }

    private static void putShort_(long a, short x) {
        unsafe.putShort(a, bigEndian ? x : swap(x));
    }

    private static void putInt_(long a, int x) {
        unsafe.putInt(a, bigEndian ? x : swap(x));
    }

    private static void putLong_(long a, long x) {
        unsafe.putLong(a, bigEndian ? x : swap(x));
    }

    private static void putFloat_(long a, float x) {
        unsafe.putFloat(a, bigEndian ? x : swap(x));
    }

    private static void putDouble_(long a, double x) {
        unsafe.putDouble(a, bigEndian ? x : swap(x));
    }

    // external api
    static int putByte(long a, byte x) {
        putByte_(a, x);
        return Byte.BYTES;
    }

    static int putBoolean(long a, boolean x) {
        putBoolean_(a, x);
        return Byte.BYTES;
    }

    static int putChar(long a, char x) {
        if (unalignedAccess || isAddressAligned(a, Character.BYTES)) {
            putChar_(a, x);
            return Character.BYTES;
        }
        putCharBigEndianUnaligned(a, x);
        return Character.BYTES;
    }

    static int putShort(long a, short x) {
        if (unalignedAccess || isAddressAligned(a, Short.BYTES)) {
            putShort_(a, x);
            return Short.BYTES;
        }
        putShortBigEndianUnaligned(a, x);
        return Short.BYTES;
    }

    static int putInt(long a, int x) {
        if (unalignedAccess || isAddressAligned(a, Integer.BYTES)) {
            putInt_(a, x);
            return Integer.BYTES;
        }
        putIntBigEndianUnaligned(a, x);
        return Integer.BYTES;
    }

    static int putLong(long a, long x) {
         if (unalignedAccess || isAddressAligned(a, Long.BYTES)) {
            putLong_(a, x);
            return Long.BYTES;
        }
        putLongBigEndianUnaligned(a, x);
        return Long.BYTES;
    }

    static int putFloat(long a, float x) {
        if (unalignedAccess || isAddressAligned(a, Float.BYTES)) {
            putFloat_(a, x);
            return Float.BYTES;
        }
        putFloatBigEndianUnaligned(a, x);
        return Float.BYTES;
    }

    static int putDouble(long a, double x) {
        if (unalignedAccess || isAddressAligned(a, Double.BYTES)) {
            putDouble_(a, x);
            return Double.BYTES;
        }
        putDoubleBigEndianUnaligned(a, x);
        return Double.BYTES;
    }
}
