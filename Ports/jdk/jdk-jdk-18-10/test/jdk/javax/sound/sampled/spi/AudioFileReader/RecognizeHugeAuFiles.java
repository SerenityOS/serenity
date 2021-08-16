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
 * @bug 6729836
 */
public final class RecognizeHugeAuFiles {

    /**
     * The size of the header's data.
     */
    private static final byte AU_HEADER = 44;

    /**
     * This value should be used if the size in bytes is unknown.
     */
    private static final /* unsigned int */ long MAX_UNSIGNED_INT = 0xffffffffL;

    /**
     * The list of supported au formats and sample size in bits per format.
     */
    private static final byte[][] auTypeBits = {
            {1, 8}, {2, 8}, {3, 16}, {4, 24}, {5, 32}, {6, 32}, {27, 8}
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
     * The {@code MAX_UNSIGNED_INT} used if the size in bytes is unknown.
     */
    private static final long[] dataSizes = {
            0, 1, 2, 3, Integer.MAX_VALUE - AU_HEADER, Integer.MAX_VALUE - 1,
            Integer.MAX_VALUE, (long) Integer.MAX_VALUE + 1,
            (long) Integer.MAX_VALUE + AU_HEADER, MAX_UNSIGNED_INT - AU_HEADER,
            MAX_UNSIGNED_INT - 1, MAX_UNSIGNED_INT
    };

    public static void main(final String[] args) throws Exception {
        for (final byte[] type : auTypeBits) {
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
     * that {@code AudioFileFormat} will store the data above {@code  MAX_INT}
     * as NOT_SPECIFIED.
     */
    private static void testAFF(final byte[] type, final int rate,
                                final int channel, final long size)
            throws Exception {
        final byte[] header = createHeader(type, rate, channel, size);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioFileFormat aff = AudioSystem.getAudioFileFormat(fake);
        final AudioFormat format = aff.getFormat();

        if (aff.getType() != AudioFileFormat.Type.AU) {
            throw new RuntimeException("Error");
        }

        final long frameLength = size / format.getFrameSize();
        if (size != MAX_UNSIGNED_INT && frameLength <= Integer.MAX_VALUE) {
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

        final long byteLength = size + AU_HEADER;
        if (byteLength <= Integer.MAX_VALUE) {
            if (aff.getByteLength() != byteLength) {
                System.err.println("Expected: " + byteLength);
                System.err.println("Actual: " + aff.getByteLength());
                throw new RuntimeException();
            }
        } else {
            if (aff.getByteLength() != AudioSystem.NOT_SPECIFIED) {
                System.err.println("Expected: " + AudioSystem.NOT_SPECIFIED);
                System.err.println("Actual: " + aff.getByteLength());
                throw new RuntimeException();
            }
        }
        validateFormat(type[1], rate, channel, aff.getFormat());
    }

    /**
     * Tests the {@code AudioInputStream} fetched from the fake header.
     * <p>
     * Note that the frameLength is stored as long which means
     * that {@code AudioInputStream} must store all possible data from au file.
     */
    private static void testAIS(final byte[] type, final int rate,
                                final int channel, final long size)
            throws Exception {
        final byte[] header = createHeader(type, rate, channel, size);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioInputStream ais = AudioSystem.getAudioInputStream(fake);
        final AudioFormat format = ais.getFormat();
        final long frameLength = size / format.getFrameSize();
        if (size != MAX_UNSIGNED_INT) {
            if (frameLength != ais.getFrameLength()) {
                System.err.println("Expected: " + frameLength);
                System.err.println("Actual: " + ais.getFrameLength());
                throw new RuntimeException();
            }
        } else {
            if (ais.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
                System.err.println("Expected: " + AudioSystem.NOT_SPECIFIED);
                System.err.println("Actual: " + ais.getFrameLength());
                throw new RuntimeException();
            }
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
    private static void validateFormat(final byte bits, final int rate,
                                       final int channel,
                                       final AudioFormat format) {

        if (Float.compare(format.getSampleRate(), rate) != 0) {
            System.out.println("Expected: " + rate);
            System.out.println("Actual: " + format.getSampleRate());
            throw new RuntimeException();
        }
        if (format.getChannels() != channel) {
            System.out.println("Expected: " + channel);
            System.out.println("Actual: " + format.getChannels());
            throw new RuntimeException();
        }
        int frameSize = ((bits + 7) / 8) * channel;
        if (format.getFrameSize() != frameSize) {
            System.out.println("Expected: " + frameSize);
            System.out.println("Actual: " + format.getFrameSize());
            throw new RuntimeException();
        }
    }

    /**
     * Creates the custom header of the AU file. It is expected that all passed
     * data are supported.
     */
    private static byte[] createHeader(final byte[] type, final int rate,
                                       final int channel, final long size) {
        return new byte[]{
                // AU_SUN_MAGIC
                0x2e, 0x73, 0x6e, 0x64,
                // headerSize
                0, 0, 0, AU_HEADER,
                // dataSize
                (byte) (size >> 24), (byte) (size >> 16), (byte) (size >> 8),
                (byte) size,
                // encoding
                0, 0, 0, type[0],
                // sampleRate
                (byte) (rate >> 24), (byte) (rate >> 16), (byte) (rate >> 8),
                (byte) (rate),
                // channels
                (byte) (channel >> 24), (byte) (channel >> 16),
                (byte) (channel >> 8), (byte) (channel),
                // data
                0, 0, 0, 0, 0, 0
        };
    }
}
