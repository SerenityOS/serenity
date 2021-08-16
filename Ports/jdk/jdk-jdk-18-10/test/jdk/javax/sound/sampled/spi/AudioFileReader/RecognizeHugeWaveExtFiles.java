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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 8132782
 */
public final class RecognizeHugeWaveExtFiles {

    /**
     * The maximum size in bytes per WAVE specification.
     */
    private static final /*unsigned int */ long MAX_UNSIGNED_INT = 0xffffffffL;

    /**
     * The supported wave ext format and sample size in bits.
     */
    private static final int[][] waveTypeBits = {
            {0xFFFE/*WAVE_FORMAT_EXTENSIBLE*/, 8}
    };

    /**
     * The list of supported sample rates(stored as unsigned int).
     */
    private static final int[] sampleRates = {
            8000, 11025, 16000, 22050, 32000, 37800, 44056, 44100, 47250, 48000,
            50000, 50400, 88200, 96000, 176400, 192000, 352800, 2822400,
            5644800, Integer.MAX_VALUE
    };

    /**
     * The list of supported channels (stored as unsigned int).
     */
    private static final int[] channels = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    };

    /**
     * The list of supported size of data (stored as unsigned int).
     * <p>
     * The {@code MAX_UNSIGNED_INT} is a maximum size.
     */
    private static final long[] dataSizes = {
            0, 1, 2, 3, Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
            (long) Integer.MAX_VALUE + 1, MAX_UNSIGNED_INT - 1, MAX_UNSIGNED_INT
    };

    public static void main(final String[] args) throws Exception {
        for (final int[] type : waveTypeBits) {
            for (final int sampleRate : sampleRates) {
                for (final int channel : channels) {
                    for (final long dataSize : dataSizes) {
                        testAFF(type, sampleRate, channel, dataSize);
                        testAIS(type, sampleRate, channel, dataSize);
                    }
                }
            }
        }
    }

    /**
     * Tests the {@code AudioFileFormat} fetched from the fake header.
     * <p>
     * Note that the frameLength and byteLength are stored as int which means
     * that {@code AudioFileFormat} will store the data above {@code MAX_INT} as
     * NOT_SPECIFIED.
     */
    private static void testAFF(final int[] type, final int rate,
                                final int channel, final long size)
            throws Exception {
        final byte[] header = createHeader(type, rate, channel, size);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioFileFormat aff = AudioSystem.getAudioFileFormat(fake);
        final AudioFormat format = aff.getFormat();

        if (aff.getType() != AudioFileFormat.Type.WAVE) {
            throw new RuntimeException("Error");
        }

        final long frameLength = size / format.getFrameSize();
        if (frameLength <= Integer.MAX_VALUE) {
            if (aff.getFrameLength() != frameLength) {
                System.err.println("Expected: " + frameLength);
                System.err.println("Actual: " + aff.getFrameLength());
                throw new RuntimeException();
            }
        } else {
            if (aff.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
                System.err.println("Expected: " + AudioSystem.NOT_SPECIFIED);
                System.err.println("Actual: " + aff.getFrameLength());
                throw new RuntimeException();
            }
        }
        validateFormat(type[1], rate, channel, aff.getFormat());
    }

    /**
     * Tests the {@code AudioInputStream} fetched from the fake header.
     * <p>
     * Note that the frameLength is stored as long which means that {@code
     * AudioInputStream} must store all possible data from wave file.
     */
    private static void testAIS(final int[] type, final int rate,
                                final int channel, final long size)
            throws Exception {
        final byte[] header = createHeader(type, rate, channel, size);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioInputStream ais = AudioSystem.getAudioInputStream(fake);
        final AudioFormat format = ais.getFormat();
        final long frameLength = size / format.getFrameSize();
        if (frameLength != ais.getFrameLength()) {
            System.err.println("Expected: " + frameLength);
            System.err.println("Actual: " + ais.getFrameLength());
            throw new RuntimeException();
        }
        if (ais.available() < 0) {
            System.err.println("available should be >=0: " + ais.available());
            throw new RuntimeException();
        }

        validateFormat(type[1], rate, channel, format);
    }

    /**
     * Tests that format contains the same data as were provided to the fake
     * stream.
     */
    private static void validateFormat(final int bits, final int rate,
                                       final int channel,
                                       final AudioFormat format) {

        if (Float.compare(format.getSampleRate(), rate) != 0) {
            System.err.println("Expected: " + rate);
            System.err.println("Actual: " + format.getSampleRate());
            throw new RuntimeException();
        }
        if (format.getChannels() != channel) {
            System.err.println("Expected: " + channel);
            System.err.println("Actual: " + format.getChannels());
            throw new RuntimeException();
        }
        int frameSize = ((bits + 7) / 8) * channel;
        if (format.getFrameSize() != frameSize) {
            System.err.println("Expected: " + frameSize);
            System.err.println("Actual: " + format.getFrameSize());
            throw new RuntimeException();
        }
    }

    /**
     * Creates the custom header of the WAVE file. It is expected that all
     * passed data are supported.
     */
    private static byte[] createHeader(final int[] type, final int rate,
                                       final int channel, final long size) {
        final int frameSize = ((type[1] + 7) / 8) * channel;
        return new byte[]{
                // RIFF_MAGIC
                0x52, 0x49, 0x46, 0x46,
                // fileLength
                -1, -1, -1, -1,
                //  waveMagic
                0x57, 0x41, 0x56, 0x45,
                // FMT_MAGIC
                0x66, 0x6d, 0x74, 0x20,
                // size
                40, 0, 0, 0,
                // wav_type  WAVE_FORMAT_EXTENSIBLE
                (byte) (type[0]), (byte) (type[0] >> 8),
                // channels
                (byte) (channel), (byte) (channel >> 8),
                // samplerate
                (byte) (rate), (byte) (rate >> 8), (byte) (rate >> 16),
                (byte) (rate >> 24),
                // framerate
                1, 0, 0, 0,
                // framesize
                (byte) (frameSize), (byte) (frameSize >> 8),
                // bits
                (byte) type[1], 0,
                // cbsize
                22, 0,
                // validBitsPerSample
                8, 0,
                // channelMask
                0, 0, 0, 0,
                // SUBTYPE_IEEE_FLOAT
                // i1
                0x3, 0x0, 0x0, 0x0,
                //s1
                0x0, 0x0,
                //s2
                0x10, 0,
                //x1
                (byte) 0x80,
                //x2
                0x0,
                //x3
                0x0,
                //x4
                (byte) 0xaa,
                //x5
                0x0,
                //x6
                0x38,
                //x7
                (byte) 0x9b,
                //x8
                0x71,
                // DATA_MAGIC
                0x64, 0x61, 0x74, 0x61,
                // data size
                (byte) (size), (byte) (size >> 8), (byte) (size >> 16),
                (byte) (size >> 24)
                // data
                , 0, 0, 0, 0, 0
        };
    }
}
