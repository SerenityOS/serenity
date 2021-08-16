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
public final class RecognizeHugeAiffFiles {

    /**
     * The maximum number of sample frames per AIFF specification.
     */
    private static final /* unsigned int */ long MAX_UNSIGNED_INT = 0xffffffffL;

    /**
     * The supported aiff sample size in bits.
     */
    private static final byte[] aiffBits = {
            1, 2, 4, 8, 11, 16, 20, 24, 27, 32
    };

    /**
     * The list of supported sample rates.
     */
    private static final int[] sampleRates = {
            8000, 11025, 16000, 22050, 32000, 37800, 44056, 44100, 47250, 48000,
            50000, 50400, 88200, 96000, 176400, 192000, 352800, 2822400,
            5644800, Integer.MAX_VALUE
    };

    /**
     * The list of supported channels.
     */
    private static final int[] channels = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    };

    /**
     * The list of supported number of frames.
     * <p>
     * The {@code MAX_UNSIGNED_INT} is a maximum.
     */
    private static final long[] numberOfFrames = {
            0, 1, 2, 3, Integer.MAX_VALUE - 1, Integer.MAX_VALUE,
            (long) Integer.MAX_VALUE + 1, MAX_UNSIGNED_INT - 1, MAX_UNSIGNED_INT
    };

    public static void main(final String[] args) throws Exception {
        for (final byte bits : aiffBits) {
            for (final int sampleRate : sampleRates) {
                for (final int channel : channels) {
                    for (final long dataSize : numberOfFrames) {
                        testAFF(bits, sampleRate, channel, dataSize);
                        testAIS(bits, sampleRate, channel, dataSize);
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
    private static void testAFF(final byte bits, final int rate,
                                final int channel, final long frameLength)
            throws Exception {
        final byte[] header = createHeader(bits, rate, channel, frameLength);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioFileFormat aff = AudioSystem.getAudioFileFormat(fake);

        if (aff.getType() != AudioFileFormat.Type.AIFF) {
            throw new RuntimeException("Error");
        }

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
        validateFormat(bits, rate, channel, aff.getFormat());
    }

    /**
     * Tests the {@code AudioInputStream} fetched from the fake header.
     * <p>
     * Note that the frameLength is stored as long which means that {@code
     * AudioInputStream} must store all possible data from aiff file.
     */
    private static void testAIS(final byte bits, final int rate,
                                final int channel, final long frameLength)
            throws Exception {
        final byte[] header = createHeader(bits, rate, channel, frameLength);
        final ByteArrayInputStream fake = new ByteArrayInputStream(header);
        final AudioInputStream ais = AudioSystem.getAudioInputStream(fake);
        final AudioFormat format = ais.getFormat();

        if (frameLength != ais.getFrameLength()) {
            System.err.println("Expected: " + frameLength);
            System.err.println("Actual: " + ais.getFrameLength());
            throw new RuntimeException();
        }
        if (ais.available() < 0) {
            System.err.println("available should be >=0: " + ais.available());
            throw new RuntimeException();
        }

        validateFormat(bits, rate, channel, format);
    }

    /**
     * Tests that format contains the same data as were provided to the fake
     * stream.
     */
    private static void validateFormat(final byte bits, final int rate,
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
            System.out.println("Expected: " + frameSize);
            System.err.println("Actual: " + format.getFrameSize());
            throw new RuntimeException();
        }
    }

    private static final int DOUBLE_MANTISSA_LENGTH = 52;
    private static final int DOUBLE_EXPONENT_LENGTH = 11;
    private static final long DOUBLE_SIGN_MASK     = 0x8000000000000000L;
    private static final long DOUBLE_EXPONENT_MASK = 0x7FF0000000000000L;
    private static final long DOUBLE_MANTISSA_MASK = 0x000FFFFFFFFFFFFFL;
    private static final int DOUBLE_EXPONENT_OFFSET = 1023;

    private static final int EXTENDED_EXPONENT_OFFSET = 16383;
    private static final int EXTENDED_MANTISSA_LENGTH = 63;
    private static final int EXTENDED_EXPONENT_LENGTH = 15;
    private static final long EXTENDED_INTEGER_MASK = 0x8000000000000000L;

    /**
     * Creates the custom header of the AIFF file. It is expected that all
     * passed data are supported.
     */
    private static byte[] createHeader(final byte bits, final int rate,
                                       final int channel, final long frameLength) {
        long doubleBits = Double.doubleToLongBits(rate);

        long sign = (doubleBits & DOUBLE_SIGN_MASK)
                >> (DOUBLE_EXPONENT_LENGTH + DOUBLE_MANTISSA_LENGTH);
        long doubleExponent = (doubleBits & DOUBLE_EXPONENT_MASK)
                >> DOUBLE_MANTISSA_LENGTH;
        long doubleMantissa = doubleBits & DOUBLE_MANTISSA_MASK;

        long extendedExponent = doubleExponent - DOUBLE_EXPONENT_OFFSET
                + EXTENDED_EXPONENT_OFFSET;
        long extendedMantissa = doubleMantissa
                << (EXTENDED_MANTISSA_LENGTH - DOUBLE_MANTISSA_LENGTH);
        long extendedSign = sign << EXTENDED_EXPONENT_LENGTH;
        short extendedBits79To64 = (short) (extendedSign | extendedExponent);
        long extendedBits63To0 = EXTENDED_INTEGER_MASK | extendedMantissa;

        return new byte[]{
                // AIFF_MAGIC
                0x46, 0x4f, 0x52, 0x4d,
                // fileLength (will use the number of frames for testing)
                (byte) (frameLength >> 24), (byte) (frameLength >> 16),
                (byte) (frameLength >> 8), (byte) frameLength,
                //  form aiff
                0x41, 0x49, 0x46, 0x46,
                // COMM_MAGIC
                0x43, 0x4f, 0x4d, 0x4d,
                // comm chunk size
                0, 0, 0, 18,
                // channels
                (byte) (channel >> 8),(byte) channel,
                // numSampleFrames
                (byte) (frameLength >> 24), (byte) (frameLength >> 16),
                (byte) (frameLength >> 8), (byte) (frameLength),
                // samplesize
                (byte) (bits >> 8),(byte) (bits),
                // samplerate
                (byte) (extendedBits79To64 >> 8),
                (byte) extendedBits79To64,
                (byte) (extendedBits63To0 >> 56),
                (byte) (extendedBits63To0 >> 48),
                (byte) (extendedBits63To0 >> 40),
                (byte) (extendedBits63To0 >> 32), (byte) (extendedBits63To0 >> 24),
                (byte) (extendedBits63To0 >> 16), (byte) (extendedBits63To0 >> 8),
                (byte) extendedBits63To0,
                // SND_MAGIC
                0x53, 0x53, 0x4e, 0x44,
                // data chunk size
                0, 0, 0, 0,
                // dataOffset
                0, 0, 0, 0,
                // blocksize
                0, 0, 0, 0,
        };
    }
}
