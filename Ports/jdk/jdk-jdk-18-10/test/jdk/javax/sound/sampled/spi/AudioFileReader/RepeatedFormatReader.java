/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * @test
 * @bug 8133677
 * @summary Subsequent read from the same stream should work
 */
public final class RepeatedFormatReader {

    // Stubs

    private static byte[] headerMIDI = {0x4d, 0x54, 0x68, 0x64, // MThd
                                        0, 0, 0, 6, // read header length
                                        0, 0, // type
                                        0, 0, // numtracks
                                        0, 1, // timing
    };

    private static byte[] headerAU = {0x2e, 0x73, 0x6e, 0x64, // AU_SUN_MAGIC
                                      0, 0, 0, 24, // headerSize
                                      0, 0, 0, 0, // dataSize
                                      0, 0, 0, 1, // encoding
                                      0, 0, 0, 1, // sampleRate
                                      0, 0, 0, 1  // channels
    };

    private static byte[] headerWAV = {0x52, 0x49, 0x46, 0x46, // RIFF_MAGIC
                                       1, 1, 1, 1, // fileLength
                                       0x57, 0x41, 0x56, 0x45, //  waveMagic
                                       0x66, 0x6d, 0x74, 0x20, // FMT_MAGIC
                                       3, 0, 0, 0, // length
                                       1, 0, // wav_type  WAVE_FORMAT_PCM
                                       0, 1, // channels
                                       0, 0, 0, 0, // sampleRate
                                       0, 0, 0, 0, // avgBytesPerSec
                                       0, 0, // blockAlign
                                       1, 0, // sampleSizeInBits
                                       0x64, 0x61, 0x74, 0x61, // DATA_MAGIC
                                       0, 0, 0, 0, // dataLength
    };

    private static final byte[][] data = {headerMIDI, headerAU, headerWAV};

    public static void main(final String[] args)
            throws IOException, UnsupportedAudioFileException {
        for (final byte[] bytes : data) {
            test(bytes);
        }
    }

    private static void test(final byte[] buffer)
            throws IOException, UnsupportedAudioFileException {
        final InputStream is = new ByteArrayInputStream(buffer);
        for (int i = 0; i < 10; ++i) {
            AudioSystem.getAudioFileFormat(is);
        }
    }
}
