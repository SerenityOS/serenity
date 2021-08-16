/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4895934
 * @summary AudioInputStream.getFrameLength returns wrong value for 12-bit AIFF
 *          file
 */
public class Aiff12bit {

    public static void test(byte[] file) throws Exception {
        InputStream inputStream = new ByteArrayInputStream(file);
        AudioFileFormat aff = AudioSystem.getAudioFileFormat(inputStream);

        if (aff.getFormat().getSampleSizeInBits() != 12) {
            throw new Exception("Wrong sample size. test FAILED");
        }
        if (aff.getFormat().getFrameSize() != 2) {
            throw new Exception("Wrong frame size. test FAILED");
        }
        if (aff.getFrameLength() != 100) {
            throw new Exception("Wrong file length. test FAILED");
        }
    }

    public static void main(String[] args) throws Exception {
        test(AIFF_12BIT);

        System.out.println("Test passed.");
    }

    public static byte[] AIFF_12BIT = {
        70,   79,   82,   77,    0,    0,    0,  -10,   65,   73,   70,   70,   67,   79,   77,   77,
        0,    0,    0,   18,    0,    1,    0,    0,    0,  100,    0,   12,   64,    8,   -6,    0,
        0,    0,    0,    0,    0,    0,   83,   83,   78,   68,    0,    0,    0,  -48,    0,    0,
        0,    0,    0,    0,    0,    0,    0,    0,    0,   16,    0,   32,    0,   48,    0,   64,
        0,   80,    0,   96,    0,  112,    0, -128,    0, -112,    0,  -96,    0,  -80,    0,  -64,
        0,  -48,    0,  -32,    0,  -16,    1,    0,    1,   16,    1,   32,    1,   48,    1,   64,
        1,   80,    1,   96,    1,  112,    1, -128,    1, -112,    1,  -96,    1,  -80,    1,  -64,
        1,  -48,    1,  -32,    1,  -16,    2,    0,    2,   16,    2,   32,    2,   48,    2,   64,
        2,   80,    2,   96,    2,  112,    2, -128,    2, -112,    2,  -96,    2,  -80,    2,  -64,
        2,  -48,    2,  -32,    2,  -16,    3,    0,    3,   16,    3,   32,    3,   48,    3,   64,
        3,   80,    3,   96,    3,  112,    3, -128,    3, -112,    3,  -96,    3,  -80,    3,  -64,
        3,  -48,    3,  -32,    3,  -16,    4,    0,    4,   16,    4,   32,    4,   48,    4,   64,
        4,   80,    4,   96,    4,  112,    4, -128,    4, -112,    4,  -96,    4,  -80,    4,  -64,
        4,  -48,    4,  -32,    4,  -16,    5,    0,    5,   16,    5,   32,    5,   48,    5,   64,
        5,   80,    5,   96,    5,  112,    5, -128,    5, -112,    5,  -96,    5,  -80,    5,  -64,
        5,  -48,    5,  -32,    5,  -16,    6,    0,    6,   16,    6,   32,    6,   48,
    };

}
