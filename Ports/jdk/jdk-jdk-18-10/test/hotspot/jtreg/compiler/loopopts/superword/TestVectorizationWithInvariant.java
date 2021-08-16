/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8078497
 * @summary Tests correct alignment of vectors with loop invariant offset.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main compiler.loopopts.superword.TestVectorizationWithInvariant
 */

package compiler.loopopts.superword;

import jdk.internal.misc.Unsafe;

public class TestVectorizationWithInvariant {

    private static Unsafe unsafe;
    private static final long BYTE_ARRAY_OFFSET;
    private static final long CHAR_ARRAY_OFFSET;

    static {
        unsafe = Unsafe.getUnsafe();
        BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
        CHAR_ARRAY_OFFSET = unsafe.arrayBaseOffset(char[].class);
    }

    public static void main(String[] args) throws Exception {
        byte[] byte_array1 = new byte[1000];
        byte[] byte_array2 = new byte[1000];
        char[] char_array = new char[1000];

        for (int i = 0; i < 20_000; ++i) {
            copyByteToChar(byte_array1, byte_array2, char_array, 1);
            copyCharToByte(char_array, byte_array1, 1);
            copyCharToByteAligned(char_array, byte_array1);
            copyCharToByteUnaligned(char_array, byte_array1);
        }
    }

    /*
     * Copy multiple consecutive chars from a byte array to a given offset in a char array
     * to trigger C2's superword optimization. The offset in the byte array is independent
     * of the loop induction variable and can be set to an arbitrary value. It may then not
     * be possible to both align the LoadUS and the StoreC operations. Therefore, vectorization
     * should only be done in this case if unaligned memory accesses are allowed.
     */
    public static void copyByteToChar(byte[] src1, byte[] src2, char[] dst, int off) {
        off = (int) BYTE_ARRAY_OFFSET + (off << 1);
        byte[] src = src1;
        for (int i = (int) CHAR_ARRAY_OFFSET; i < 100; i = i + 8) {
            // Copy 8 chars from src to dst
            unsafe.putChar(dst, i + 0, unsafe.getChar(src, off + 0));
            unsafe.putChar(dst, i + 2, unsafe.getChar(src, off + 2));
            unsafe.putChar(dst, i + 4, unsafe.getChar(src, off + 4));
            unsafe.putChar(dst, i + 6, unsafe.getChar(src, off + 6));
            unsafe.putChar(dst, i + 8, unsafe.getChar(src, off + 8));
            unsafe.putChar(dst, i + 10, unsafe.getChar(src, off + 10));
            unsafe.putChar(dst, i + 12, unsafe.getChar(src, off + 12));
            unsafe.putChar(dst, i + 14, unsafe.getChar(src, off + 14));

            // Prevent loop invariant code motion of char read.
            src = (src == src1) ? src2 : src1;
        }
    }

    /*
     * Copy multiple consecutive chars from a char array to a given offset in a byte array
     * to trigger C2's superword optimization. Checks for similar problems as 'copyByteToChar'.
     */
    public static void copyCharToByte(char[] src, byte[] dst, int off) {
        off = (int) BYTE_ARRAY_OFFSET + (off << 1);
        for (int i = 0; i < 100; i = i + 8) {
            // Copy 8 chars from src to dst
            unsafe.putChar(dst, off + 0, src[i + 0]);
            unsafe.putChar(dst, off + 2, src[i + 1]);
            unsafe.putChar(dst, off + 4, src[i + 2]);
            unsafe.putChar(dst, off + 6, src[i + 3]);
            unsafe.putChar(dst, off + 8, src[i + 4]);
            unsafe.putChar(dst, off + 10, src[i + 5]);
            unsafe.putChar(dst, off + 12, src[i + 6]);
            unsafe.putChar(dst, off + 14, src[i + 7]);
        }
    }

    /*
     * Variant of copyCharToByte with a constant destination array offset.
     * The loop should always be vectorized because both the LoadUS and StoreC
     * operations can be aligned.
     */
    public static void copyCharToByteAligned(char[] src, byte[] dst) {
        final int off = (int) BYTE_ARRAY_OFFSET;
        for (int i = 8; i < 100; i = i + 8) {
            // Copy 8 chars from src to dst
            unsafe.putChar(dst, off + 0, src[i + 0]);
            unsafe.putChar(dst, off + 2, src[i + 1]);
            unsafe.putChar(dst, off + 4, src[i + 2]);
            unsafe.putChar(dst, off + 6, src[i + 3]);
            unsafe.putChar(dst, off + 8, src[i + 4]);
            unsafe.putChar(dst, off + 10, src[i + 5]);
            unsafe.putChar(dst, off + 12, src[i + 6]);
            unsafe.putChar(dst, off + 14, src[i + 7]);
        }
    }

    /*
     * Variant of copyCharToByte with a constant destination array offset. The
     * loop should only be vectorized if unaligned memory operations are allowed
     * because not both the LoadUS and the StoreC can be aligned.
     */
    public static void copyCharToByteUnaligned(char[] src, byte[] dst) {
        final int off = (int) BYTE_ARRAY_OFFSET + 2;
        for (int i = 0; i < 100; i = i + 8) {
            // Copy 8 chars from src to dst
            unsafe.putChar(dst, off + 0, src[i + 0]);
            unsafe.putChar(dst, off + 2, src[i + 1]);
            unsafe.putChar(dst, off + 4, src[i + 2]);
            unsafe.putChar(dst, off + 6, src[i + 3]);
            unsafe.putChar(dst, off + 8, src[i + 4]);
            unsafe.putChar(dst, off + 10, src[i + 5]);
            unsafe.putChar(dst, off + 12, src[i + 6]);
            unsafe.putChar(dst, off + 14, src[i + 7]);
        }
    }
}
