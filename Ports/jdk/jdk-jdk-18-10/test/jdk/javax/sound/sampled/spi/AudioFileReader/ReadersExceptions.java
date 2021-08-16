/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.spi.AudioFileReader;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 7058662 7058666 7058672 8130305
 * @author Sergey Bylokhov
 */
public final class ReadersExceptions {

    // empty channels
    static byte[] wrongAIFFCh =
            {0x46, 0x4f, 0x52, 0x4d, // AiffFileFormat.AIFF_MAGIC
             0, 0, 0, 0, // length
             0, 0, 0, 0, // iffType
             0x43, 0x4f, 0x4d, 0x4d, // chunkName
             0, 0, 0, 100, // chunkLen
             0, 0, // channels
             0, 0, 0, 0, //
             0, 10  // sampleSize
                    , 0, 0, 0, 0};
    // empty sampleSize
    static byte[] wrongAIFFSSL =
            {0x46, 0x4f, 0x52, 0x4d, //AiffFileFormat.AIFF_MAGIC
             0, 0, 0, 0, // length
             0, 0, 0, 0, // iffType
             0x43, 0x4f, 0x4d, 0x4d, // chunkName
             0, 0, 0, 100, // chunkLen
             0, 10, // channels
             0, 0, 0, 0, //
             0, 0  // sampleSize
                    , 0, 0, 0, 0};
    // big sampleSize
    static byte[] wrongAIFFSSH =
            {0x46, 0x4f, 0x52, 0x4d, //AiffFileFormat.AIFF_MAGIC
             0, 0, 0, 0, // length
             0, 0, 0, 0, // iffType
             0x43, 0x4f, 0x4d, 0x4d, // chunkName
             0, 0, 0, 100, // chunkLen
             0, 10, // channels
             0, 0, 0, 0, //
             0, 33  // sampleSize
                    , 0, 0, 0, 0};
    // empty channels
    static byte[] wrongAUCh =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 24, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 1, // encoding_local AuFileFormat.AU_ULAW_8
             0, 0, 0, 1, // sampleRate
             0, 0, 0, 0 // channels
            };
    // empty sample rate
    static byte[] wrongAUSR =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 24, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 1, // encoding_local AuFileFormat.AU_ULAW_8
             0, 0, 0, 0, // sampleRate
             0, 0, 0, 1 // channels
            };
    // empty header size
    static byte[] wrongAUEmptyHeader =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 0, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 1, // encoding_local AuFileFormat.AU_ULAW_8
             0, 0, 0, 1, // sampleRate
             0, 0, 0, 1 // channels
            };
    // small header size
    static byte[] wrongAUSmallHeader =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 7, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 1, // encoding_local AuFileFormat.AU_ULAW_8
             0, 0, 0, 1, // sampleRate
             0, 0, 0, 1 // channels
            };
    // frame size overflow, when result negative
    static byte[] wrongAUFrameSizeOverflowNegativeResult =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 24, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 5, // encoding_local AuFileFormat.AU_LINEAR_32
             0, 0, 0, 1, // sampleRate
             0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF // channels
            };
    // frame size overflow, when result positive
    static byte[] wrongAUFrameSizeOverflowPositiveResult =
            {0x2e, 0x73, 0x6e, 0x64,//AiffFileFormat.AU_SUN_MAGIC
             0, 0, 0, 24, // headerSize
             0, 0, 0, 0, // dataSize
             0, 0, 0, 4, // encoding_local AuFileFormat.AU_LINEAR_24
             0, 0, 0, 1, // sampleRate
             0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF // channels
            };
    // empty channels
    static byte[] wrongWAVCh =
            {0x52, 0x49, 0x46, 0x46, // WaveFileFormat.RIFF_MAGIC
             1, 1, 1, 1, // fileLength
             0x57, 0x41, 0x56, 0x45, //  waveMagic
             0x66, 0x6d, 0x74, 0x20, // FMT_MAGIC
             3, 0, 0, 0, // length
             1, 0, // wav_type  WAVE_FORMAT_PCM
             0, 0, // channels
             0, 0, 0, 0, // sampleRate
             0, 0, 0, 0, // avgBytesPerSec
             0, 0, // blockAlign
             1, 0, // sampleSizeInBits
             0x64, 0x61, 0x74, 0x61, // WaveFileFormat.DATA_MAGIC
             0, 0, 0, 0, // dataLength
            };
    // empty sampleSizeInBits
    static byte[] wrongWAVSSB =
            {0x52, 0x49, 0x46, 0x46, // WaveFileFormat.RIFF_MAGIC
             1, 1, 1, 1, // fileLength
             0x57, 0x41, 0x56, 0x45, //  waveMagic
             0x66, 0x6d, 0x74, 0x20, // FMT_MAGIC
             3, 0, 0, 0, // length
             1, 0, // wav_type  WAVE_FORMAT_PCM
             1, 0, // channels
             0, 0, 0, 0, // sampleRate
             0, 0, 0, 0, // avgBytesPerSec
             0, 0, // blockAlign
             0, 0, // sampleSizeInBits
             0x64, 0x61, 0x74, 0x61, // WaveFileFormat.DATA_MAGIC
             0, 0, 0, 0, // dataLength
            };

    static byte[][] data = {
            wrongAIFFCh, wrongAIFFSSL, wrongAIFFSSH, wrongAUCh, wrongAUSR,
            wrongAUEmptyHeader, wrongAUSmallHeader,
            wrongAUFrameSizeOverflowNegativeResult,
            wrongAUFrameSizeOverflowPositiveResult, wrongWAVCh, wrongWAVSSB
    };

    public static void main(final String[] args) throws IOException {
        for (final byte[] bytes : data) {
            testAS(bytes);
            testAFR(bytes);
        }
    }

    private static void testAS(final byte[] buffer) throws IOException {
        // AudioSystem API
        final InputStream is = new ByteArrayInputStream(buffer);
        try {
            AudioSystem.getAudioFileFormat(is);
        } catch (UnsupportedAudioFileException ignored) {
            // Expected.
            return;
        }
        throw new RuntimeException("Test Failed");
    }

    private static void testAFR(final byte[] buffer) throws IOException {
        // AudioFileReader API
        final InputStream is = new ByteArrayInputStream(buffer);
        for (final AudioFileReader afr : load(AudioFileReader.class)) {
            for (int i = 0; i < 10; ++i) {
                try {
                    afr.getAudioFileFormat(is);
                    throw new RuntimeException("UAFE expected");
                } catch (final UnsupportedAudioFileException ignored) {
                    // Expected.
                }
            }
        }
    }
}
