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
public final class Bits64ToFromFloatArray {

    private static final int SIZE = 64;

    private static final float[] FLOATS = {-1.0f, 0, 1.0f};

    private static long MID_U = (long) (Long.MAX_VALUE + 1);
    private static long MAX_U = -1;

    // BIG ENDIAN
    private static final byte[] SIGNED_BIG = {
            (byte) ((Long.MIN_VALUE >> 56) & 0xff),
            (byte) ((Long.MIN_VALUE >> 48) & 0xff),
            (byte) ((Long.MIN_VALUE >> 40) & 0xff),
            (byte) ((Long.MIN_VALUE >> 32) & 0xff),
            0, 0, 0, 0, // current javasound impl will ignore this
            0, 0, 0, 0,
            0, 0, 0, 0,
            (byte) ((Long.MAX_VALUE >> 56) & 0xff),
            (byte) ((Long.MAX_VALUE >> 48) & 0xff),
            (byte) ((Long.MAX_VALUE >> 40) & 0xff),
            (byte) ((Long.MAX_VALUE >> 32) & 0xff),
            0, 0, 0, 0, // current javasound impl will ignore this
    };

    private static final byte[] UNSIGNED_BIG = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            (byte) ((MID_U >> 56) & 0xff),
            (byte) ((MID_U >> 48) & 0xff),
            (byte) ((MID_U >> 40) & 0xff),
            (byte) ((MID_U >> 32) & 0xff),
            0, 0, 0, 0, // current javasound impl will ignore this
            (byte) ((MAX_U >> 56) & 0xff),
            (byte) ((MAX_U >> 48) & 0xff),
            (byte) ((MAX_U >> 40) & 0xff),
            (byte) ((MAX_U >> 32) & 0xff),
            0, 0, 0, 0, // current javasound impl will ignore this
    };

    // LITTLE ENDIAN
    private static final byte[] SIGNED_LITTLE = {
            0, 0, 0, 0, // current javasound impl will ignore this
            (byte) ((Long.MIN_VALUE >> 32) & 0xff),
            (byte) ((Long.MIN_VALUE >> 40) & 0xff),
            (byte) ((Long.MIN_VALUE >> 48) & 0xff),
            (byte) ((Long.MIN_VALUE >> 56) & 0xff),
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0, // current javasound impl will ignore this
            (byte) ((Long.MAX_VALUE >> 32) & 0xff),
            (byte) ((Long.MAX_VALUE >> 40) & 0xff),
            (byte) ((Long.MAX_VALUE >> 48) & 0xff),
            (byte) ((Long.MAX_VALUE >> 56) & 0xff),
            };

    private static final byte[] UNSIGNED_LITTLE = {
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0, // current javasound impl will ignore this
            (byte) ((MID_U >> 32) & 0xff),
            (byte) ((MID_U >> 40) & 0xff),
            (byte) ((MID_U >> 48) & 0xff),
            (byte) ((MID_U >> 56) & 0xff),
            0, 0, 0, 0, // current javasound impl will ignore this
            (byte) ((MAX_U >> 32) & 0xff),
            (byte) ((MAX_U >> 40) & 0xff),
            (byte) ((MAX_U >> 48) & 0xff),
            (byte) ((MAX_U >> 56) & 0xff),
            };

    public static void main(final String[] args) {
        test(PCM_UNSIGNED, UNSIGNED_BIG, true);
        test(PCM_UNSIGNED, UNSIGNED_LITTLE, false);
        test(PCM_SIGNED, SIGNED_LITTLE, false);
        test(PCM_SIGNED, SIGNED_BIG, true);
    }

    private static void test(final Encoding enc, final byte[] expected,
                             boolean end) {
        System.err.println(enc);
        AudioFormat af = new AudioFormat(enc, 44100, SIZE, 1, SIZE / 8, 44100,
                                         end);
        byte[] bytes = new byte[FLOATS.length * af.getFrameSize()];
        AudioFloatConverter conv = AudioFloatConverter.getConverter(af);

        conv.toByteArray(FLOATS, bytes);

        if (!Arrays.equals(bytes, expected)) {
            System.err.println("Actual:   " + Arrays.toString(bytes));
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
