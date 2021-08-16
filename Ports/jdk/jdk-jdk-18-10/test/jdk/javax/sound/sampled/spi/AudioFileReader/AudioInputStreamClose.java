/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.AudioFileWriter;

import static java.util.ServiceLoader.load;
import static javax.sound.sampled.AudioFileFormat.Type.AIFC;
import static javax.sound.sampled.AudioFileFormat.Type.AIFF;
import static javax.sound.sampled.AudioFileFormat.Type.AU;
import static javax.sound.sampled.AudioFileFormat.Type.SND;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

/**
 * @test
 * @bug 8191384
 * @summary the stream returned by AudioFileReader should close its data stream
 */
public final class AudioInputStreamClose {

    static final class StreamWrapper extends BufferedInputStream {

        private boolean open = true;

        StreamWrapper(final InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            super.close();
            open = false;
        }

        boolean isOpen() {
            return open;
        }
    }

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

    private static final int[] sampleBits = {1, 4, 8, 11, 16, 20, 24, 32};

    private static final int[] channels = {1, 2, 3, 4, 5};

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
        for (final AudioFileWriter afw : load(AudioFileWriter.class)) {
            for (final AudioFileReader afr : load(AudioFileReader.class)) {
                for (final AudioFileFormat.Type type : types) {
                    for (final AudioFormat from : formats) {
                        test(afw, afr, type, getStream(from, true));
                        test(afw, afr, type, getStream(from, false));
                    }
                }
            }
        }
    }

    /**
     * Writes and reads the data to/from the stream.
     */
    private static void test(final AudioFileWriter afw,
                             final AudioFileReader afr,
                             final AudioFileFormat.Type type,
                             final AudioInputStream ais)
            throws IOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            afw.write(ais, type, out);
            final InputStream input = new ByteArrayInputStream(out.toByteArray());
            final StreamWrapper wrapper = new StreamWrapper(input);

            // the wrapper should be closed as well
            afr.getAudioInputStream(wrapper).close();

            if (wrapper.isOpen()) {
                System.err.println("Writer = " + afw);
                System.err.println("Reader = " + afr);
                throw new RuntimeException("Stream was not closed");
            }
        } catch (IOException | IllegalArgumentException |
                UnsupportedAudioFileException ignored) {
        }
    }

    private static AudioInputStream getStream(final AudioFormat format,
                                              final boolean frameLength) {
        final int dataSize = FRAME_LENGTH * format.getFrameSize();
        byte[] buf = new byte[dataSize];
        final InputStream in = new ByteArrayInputStream(buf);
        if (frameLength) {
            return new AudioInputStream(in, format, FRAME_LENGTH);
        } else {
            return new AudioInputStream(in, format, NOT_SPECIFIED);
        }
    }
}
