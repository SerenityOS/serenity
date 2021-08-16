/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

/**
 * A helper class to get access to package-private members
 */
public class Helper {
    @jdk.internal.vm.annotation.ForceInline
    public static boolean StringCodingHasNegatives(byte[] ba, int off, int len) {
        return StringCoding.hasNegatives(ba, off, len);
    }

    @jdk.internal.vm.annotation.ForceInline
    public static byte[] compressByte(byte[] src, int srcOff, int dstSize, int dstOff, int len) {
        byte[] dst = new byte[dstSize];
        StringUTF16.compress(src, srcOff, dst, dstOff, len);
        return dst;
    }

    @jdk.internal.vm.annotation.ForceInline
    public static byte[] compressChar(char[] src, int srcOff, int dstSize, int dstOff, int len) {
        byte[] dst = new byte[dstSize];
        StringUTF16.compress(src, srcOff, dst, dstOff, len);
        return dst;
    }

    @jdk.internal.vm.annotation.ForceInline
    public static byte[] inflateByte(byte[] src, int srcOff, int dstSize, int dstOff, int len) {
        byte[] dst = new byte[dstSize];
        StringLatin1.inflate(src, srcOff, dst, dstOff, len);
        return dst;
    }

    @jdk.internal.vm.annotation.ForceInline
    public static char[] inflateChar(byte[] src, int srcOff, int dstSize, int dstOff, int len) {
        char[] dst = new char[dstSize];
        StringLatin1.inflate(src, srcOff, dst, dstOff, len);
        return dst;
    }

    @jdk.internal.vm.annotation.ForceInline
    public static byte[] toBytes(char[] value, int off, int len) {
        return StringUTF16.toBytes(value, off, len);
    }

    @jdk.internal.vm.annotation.ForceInline
    public static char[] getChars(byte[] value, int srcBegin, int srcEnd, int dstSize, int dstBegin) {
        char[] dst = new char[dstSize];
        StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        return dst;
    }

    public static void putCharSB(byte[] val, int index, int c) {
        StringUTF16.putCharSB(val, index, c);
    }

    public static void putCharsSB(byte[] val, int index, char[] ca, int off, int end) {
        StringUTF16.putCharsSB(val, index, ca, off, end);
    }

    public static void putCharsSB(byte[] val, int index, CharSequence s, int off, int end) {
        StringUTF16.putCharsSB(val, index, s, off, end);
    }

    public static int codePointAtSB(byte[] val, int index, int end) {
        return StringUTF16.codePointAtSB(val, index, end);
    }

    public static int codePointBeforeSB(byte[] val, int index) {
        return StringUTF16.codePointBeforeSB(val, index);
    }

    public static int codePointCountSB(byte[] val, int beginIndex, int endIndex) {
        return StringUTF16.codePointCountSB(val, beginIndex, endIndex);
    }

    public static int getChars(int i, int begin, int end, byte[] value) {
        return StringUTF16.getChars(i, begin, end, value);
    }

    public static int getChars(long l, int begin, int end, byte[] value) {
        return StringUTF16.getChars(l, begin, end, value);
    }

    public static boolean contentEquals(byte[] v1, byte[] v2, int len) {
        return StringUTF16.contentEquals(v1, v2, len);
    }

    public static boolean contentEquals(byte[] value, CharSequence cs, int len) {
        return StringUTF16.contentEquals(value, cs, len);
    }

    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4) {
        return StringUTF16.putCharsAt(value, i, c1, c2, c3, c4);
    }

    public static int putCharsAt(byte[] value, int i, char c1, char c2, char c3, char c4, char c5) {
        return StringUTF16.putCharsAt(value, i, c1, c2, c3, c4, c5);
    }

    public static char charAt(byte[] value, int index) {
        return StringUTF16.charAt(value, index);
    }

    public static void reverse(byte[] value, int count) {
        StringUTF16.reverse(value, count);
    }

    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        StringUTF16.inflate(src, srcOff, dst, dstOff, len);
    }

    public static int indexOf(byte[] src, int srcCount,
                                    byte[] tgt, int tgtCount, int fromIndex) {
        return StringUTF16.indexOf(src, srcCount, tgt, tgtCount, fromIndex);
    }

    public static int indexOfLatin1(byte[] src, int srcCount,
                                    byte[] tgt, int tgtCount, int fromIndex) {
        return StringUTF16.indexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }
    public static int lastIndexOf(byte[] src, byte[] tgt, int tgtCount, int fromIndex) {
        int srcCount = StringUTF16.length(src); // ignored
        return StringUTF16.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex);
    }

    public static int lastIndexOfLatin1(byte[] src, byte[] tgt, int tgtCount, int fromIndex) {
        int srcCount = StringUTF16.length(src); // ignored
        return StringUTF16.lastIndexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
    }

}
