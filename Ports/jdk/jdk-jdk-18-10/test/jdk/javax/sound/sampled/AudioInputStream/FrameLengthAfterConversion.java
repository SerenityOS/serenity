/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileWriter;
import javax.sound.sampled.spi.FormatConversionProvider;

import static java.util.ServiceLoader.load;
import static javax.sound.sampled.AudioFileFormat.Type.AIFC;
import static javax.sound.sampled.AudioFileFormat.Type.AIFF;
import static javax.sound.sampled.AudioFileFormat.Type.AU;
import static javax.sound.sampled.AudioFileFormat.Type.SND;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

/**
 * @test
 * @bug 8038139 8178401
 */
public final class FrameLengthAfterConversion {

    /**
     * We will try to use all formats, in this case all our providers will be
     * covered by supported/unsupported formats.
     */
    private static final List<AudioFormat> formats = new ArrayList<>(23000);

    private static final AudioFormat.Encoding[] encodings = {
            AudioFormat.Encoding.ALAW, AudioFormat.Encoding.ULAW,
            AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED,
            AudioFormat.Encoding.PCM_FLOAT, new AudioFormat.Encoding("Test")
    };

    private static final int[] sampleBits = {
            1, 4, 8, 11, 16, 20, 24, 32
    };

    private static final int[] channels = {
            1, 2, 3, 4, 5
    };

    private static final AudioFileFormat.Type[] types = {
            WAVE, AU, AIFF, AIFC, SND,
            new AudioFileFormat.Type("TestName", "TestExt")
    };

    private static final int FRAME_LENGTH = 10;

    static {
        for (final int sampleSize : sampleBits) {
            for (final int channel : channels) {
                for (final AudioFormat.Encoding enc : encodings) {
                    final int frameSize = ((sampleSize + 7) / 8) * channel;
                    formats.add(new AudioFormat(enc, 44100, sampleSize, channel,
                                                frameSize, 44100, true));
                    formats.add(new AudioFormat(enc, 44100, sampleSize, channel,
                                                frameSize, 44100, false));
                }
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        for (final FormatConversionProvider fcp : load(
                FormatConversionProvider.class)) {
            System.out.println("fcp = " + fcp);
            for (final AudioFormat from : formats) {
                for (final AudioFormat to : formats) {
                    testAfterConversion(fcp, to, getStream(from, true));
                }
            }
        }

        for (final AudioFileWriter afw : load(AudioFileWriter.class)) {
            System.out.println("afw = " + afw);
            for (final AudioFileFormat.Type type : types) {
                for (final AudioFormat from : formats) {
                    testAfterSaveToStream(afw, type, getStream(from, true));
                }
            }
        }

        for (final AudioFileWriter afw : load(AudioFileWriter.class)) {
            System.out.println("afw = " + afw);
            for (final AudioFileFormat.Type type : types) {
                for (final AudioFormat from : formats) {
                    testAfterSaveToFile(afw, type, getStream(from, true));
                }
            }
        }

        for (final AudioFileWriter afw : load(AudioFileWriter.class)) {
            System.out.println("afw = " + afw);
            for (final AudioFileFormat.Type type : types) {
                for (final AudioFormat from : formats) {
                    testAfterSaveToFile(afw, type, getStream(from, false));
                }
            }
        }
    }

    /**
     * Verifies the frame length after the stream was saved/read to/from
     * stream.
     */
    private static void testAfterSaveToStream(final AudioFileWriter afw,
                                              final AudioFileFormat.Type type,
                                              final AudioInputStream ais)
            throws IOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            afw.write(ais, type, out);
            final InputStream input = new ByteArrayInputStream(
                    out.toByteArray());
            validate(AudioSystem.getAudioInputStream(input).getFrameLength());
        } catch (IllegalArgumentException | UnsupportedAudioFileException
                ignored) {
        }
    }

    /**
     * Verifies the frame length after the stream was saved/read to/from file.
     */
    private static void testAfterSaveToFile(final AudioFileWriter afw,
                                            final AudioFileFormat.Type type,
                                            AudioInputStream ais)
            throws IOException {
        final File temp = File.createTempFile("sound", ".tmp");
        try {
            afw.write(ais, type, temp);
            ais = AudioSystem.getAudioInputStream(temp);
            final long frameLength = ais.getFrameLength();
            ais.close();
            validate(frameLength);
        } catch (IllegalArgumentException | UnsupportedAudioFileException
                ignored) {
        } finally {
            Files.delete(Paths.get(temp.getAbsolutePath()));
        }
    }

    /**
     * Verifies the frame length after the stream was converted to other
     * stream.
     *
     * @see FormatConversionProvider#getAudioInputStream(AudioFormat,
     * AudioInputStream)
     */
    private static void testAfterConversion(final FormatConversionProvider fcp,
                                            final AudioFormat to,
                                            final AudioInputStream ais) {
        if (fcp.isConversionSupported(to, ais.getFormat())) {
            validate(fcp.getAudioInputStream(to, ais).getFrameLength());
        }
    }

    /**
     * Throws an exception if the frameLength is specified and is not equal to
     * the gold value.
     */
    private static void validate(final long frameLength) {
        if (frameLength != FRAME_LENGTH) {
            System.err.println("Expected: " + FRAME_LENGTH);
            System.err.println("Actual: " + frameLength);
            throw new RuntimeException();
        }
    }

    private static AudioInputStream getStream(final AudioFormat format,
                                              final boolean frameLength) {
        final int dataSize = FRAME_LENGTH * format.getFrameSize();
        final InputStream in = new ByteArrayInputStream(new byte[dataSize]);
        if (frameLength) {
            return new AudioInputStream(in, format, FRAME_LENGTH);
        } else {
            return new AudioInputStream(in, format, NOT_SPECIFIED);
        }
    }
}
