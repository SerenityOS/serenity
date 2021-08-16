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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 8147407
 */
public final class RecognizeWaveExtensible {

    private static byte[] data = {
            82, 73, 70, 70, 72, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32, 40,
            0, 0, 0, -2, -1, 1, 0, 64, 31, 0, 0, 0, 125, 0, 0, 4, 0, 32, 0, 22,
            0, 32, 0, 4, 0, 0, 0, 1, 0, 0, 0, 0, 0, 16, 0, -128, 0, 0, -86, 0,
            56, -101, 113, 102, 97, 99, 116, 4, 0, 0, 0, 0, 0, 0, 0, 100, 97,
            116, 97, 0, 0, 0, 0
    };

    public static void main(final String[] args) throws Exception {
        final InputStream is = new ByteArrayInputStream(data);
        final AudioFileFormat aff = AudioSystem.getAudioFileFormat(is);
        System.out.println("AudioFileFormat: " + aff);
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(is)) {
            System.out.println("AudioFormat: " + ais.getFormat());
        }
        System.out.println("new String(data) = " + new String(data));
    }
}
