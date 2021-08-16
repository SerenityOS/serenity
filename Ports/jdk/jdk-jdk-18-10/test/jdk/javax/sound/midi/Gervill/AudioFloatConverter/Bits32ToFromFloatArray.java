/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import com.sun.media.sound.AudioFloatConverter;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;

/**
 * @test
 * @bug 8152501
 * @modules java.desktop/com.sun.media.sound
 */
public final class Bits32ToFromFloatArray {

    private static final int SIZE = 32;

    private static final float[] FLOATS = {-1.0f, 0, 1.0f};

    private static int MID_U = (int) (Integer.MAX_VALUE + 1);
    private static int MAX_U = -1;

    // BIG ENDIAN
    private static final byte[] SIGNED_BIG = {
            (byte) ((Integer.MIN_VALUE >> 24) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 16) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 8) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 0) & 0xff),
            0, 0, 0, 0,
            (byte) ((Integer.MAX_VALUE >> 24) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 16) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 8) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 0) & 0xff),
    };

    private static final byte[] UNSIGNED_BIG = {
            0, 0, 0, 0,
            (byte) ((MID_U >> 24) & 0xff),
            (byte) ((MID_U >> 16) & 0xff),
            (byte) ((MID_U >> 8) & 0xff),
            (byte) ((MID_U >> 0) & 0xff),
            (byte) ((MAX_U >> 24) & 0xff),
            (byte) ((MAX_U >> 16) & 0xff),
            (byte) ((MAX_U >> 8) & 0xff),
            (byte) ((MAX_U >> 0) & 0xff),

    };

    // LITTLE ENDIAN
    private static final byte[] SIGNED_LITTLE = {
            (byte) ((Integer.MIN_VALUE >> 0) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 8) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 16) & 0xff),
            (byte) ((Integer.MIN_VALUE >> 24) & 0xff),
            0, 0, 0, 0,
            (byte) ((Integer.MAX_VALUE >> 0) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 8) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 16) & 0xff),
            (byte) ((Integer.MAX_VALUE >> 24) & 0xff),
            };

    private static final byte[] UNSIGNED_LITTLE = {
            0, 0, 0, 0,
            (byte) ((MID_U >> 0) & 0xff),
            (byte) ((MID_U >> 8) & 0xff),
            (byte) ((MID_U >> 16) & 0xff),
            (byte) ((MID_U >> 24) & 0xff),
            (byte) ((MAX_U >> 0) & 0xff),
            (byte) ((MAX_U >> 8) & 0xff),
            (byte) ((MAX_U >> 16) & 0xff),
            (byte) ((MAX_U >> 24) & 0xff),
            };

    public static void main(final String[] args) {
        test(PCM_UNSIGNED, UNSIGNED_BIG, true);
        test(PCM_UNSIGNED, UNSIGNED_LITTLE, false);
        test(PCM_SIGNED, SIGNED_LITTLE, false);
        test(PCM_SIGNED, SIGNED_BIG, true);
    }

    private static void test(final Encoding enc, final byte[] expected,
                             boolean end) {
        AudioFormat af = new AudioFormat(enc, 44100, SIZE, 1, SIZE / 8, 44100,
                                         end);
        byte[] bytes = new byte[FLOATS.length * af.getFrameSize()];
        AudioFloatConverter conv = AudioFloatConverter.getConverter(af);

        conv.toByteArray(FLOATS, bytes);

        if (!Arrays.equals(bytes, expected)) {
            System.err.println("Actual: " + Arrays.toString(bytes));
            System.err.println("Expected: " + Arrays.toString(expected));
            throw new RuntimeException();
        }

        float[] floats = new float[bytes.length / af.getFrameSize()];
        conv.toFloatArray(bytes, floats);

        if (!Arrays.equals(floats, FLOATS)) {
            System.err.println("Actual: " + Arrays.toString(floats));
            System.err.println("Expected: " + Arrays.toString(FLOATS));
            throw new RuntimeException();
        }
    }
}
