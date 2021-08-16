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
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileWriter;

import static java.util.ServiceLoader.load;
import static javax.sound.sampled.AudioFileFormat.Type;
import static javax.sound.sampled.AudioFormat.*;

/**
 * @test
 * @bug 8135100
 * @author Sergey Bylokhov
 */
public final class ExpectedNPEOnNull {

    /**
     * We will try to use all encoding, in this case all our providers will be
     * covered by supported/unsupported encoding.
     */
    private static final Encoding[] encodings = {Encoding.PCM_SIGNED,
                                                 Encoding.PCM_UNSIGNED,
                                                 Encoding.PCM_FLOAT,
                                                 Encoding.ULAW, Encoding.ALAW,
                                                 new Encoding("test")};

    /**
     * We will try to use all types, in this case all our providers will be
     * covered by supported/unsupported types.
     */
    private static final Type[] types = {Type.WAVE, Type.AU, Type.AIFF,
                                         Type.AIFC, Type.SND,
                                         new Type("MIDI", "mid"),
                                         new Type("test", "test")};

    /**
     * We will try to use all supported AudioInputStream, in this case all our
     * providers will be covered by supported/unsupported streams.
     */
    private static final List<AudioInputStream> aiss = new ArrayList<>();

    static {
        for (final Encoding encoding : encodings) {
            for (final Type type : types) {
                aiss.add(getAIS(type, encoding));
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        testAS();
        for (final AudioFileWriter afw : load(AudioFileWriter.class)) {
            testAFW(afw);
        }
        testAFW(customAFW);
    }

    /**
     * Tests the part of AudioSystem API, which implemented via AudioFileWriter.
     */
    private static void testAS() throws Exception {

        // AudioSystem#getAudioFileTypes(AudioInputStream)
        try {
            AudioSystem.getAudioFileTypes(null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }

        // AudioSystem#isFileTypeSupported(Type)
        try {
            AudioSystem.isFileTypeSupported(null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }

        // AudioSystem#isFileTypeSupported(Type, AudioInputStream)
        for (final Type type : types) {
            try {
                AudioSystem.isFileTypeSupported(type, null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                AudioSystem.isFileTypeSupported(null, stream);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }

        // AudioSystem#write(AudioInputStream, Type, OutputStream)
        for (final Type type : types) {
            try {
                AudioSystem.write(null, type, new NullOutputStream());
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                AudioSystem.write(stream, null, new NullOutputStream());
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Type type : types) {
            for (final AudioInputStream stream : aiss) {
                try {
                    AudioSystem.write(stream, type, (OutputStream) null);
                    throw new RuntimeException("NPE is expected");
                } catch (final NullPointerException ignored) {
                }
            }
        }

        // AudioSystem#write(AudioInputStream, Type, File)
        for (final Type type : types) {
            try {
                AudioSystem.write(null, type, new File("test.sound"));
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }

        for (final AudioInputStream stream : aiss) {
            try {
                AudioSystem.write(stream, null, new File("test.sound"));
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            for (final Type type : types) {
                try {
                    AudioSystem.write(stream, type, (File) null);
                    throw new RuntimeException("NPE is expected");
                } catch (final NullPointerException ignored) {
                }
            }
        }
    }

    /**
     * Tests the AudioFileWriter API directly.
     */
    private static void testAFW(final AudioFileWriter afw) throws Exception {

        // AudioFileWriter#isFileTypeSupported(Type)
        try {
            afw.isFileTypeSupported(null);
            throw new RuntimeException("NPE is expected: " + afw);
        } catch (final NullPointerException ignored) {
        }

        // AudioFileWriter#getAudioFileTypes(AudioInputStream)
        try {
            afw.getAudioFileTypes(null);
            throw new RuntimeException("NPE is expected: " + afw);
        } catch (final NullPointerException ignored) {
        }

        // AudioFileWriter#isFileTypeSupported(Type, AudioInputStream)
        for (final Type type : types) {
            try {
                afw.isFileTypeSupported(type, null);
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                afw.isFileTypeSupported(null, stream);
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }

        // AudioFileWriter#write(AudioInputStream, Type, OutputStream)
        for (final Type type : types) {
            try {
                afw.write(null, type, new NullOutputStream());
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                afw.write(stream, null, new NullOutputStream());
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            for (final Type type : types) {
                try {
                    afw.write(stream, type, (OutputStream) null);
                    throw new RuntimeException("NPE is expected: " + afw);
                } catch (final NullPointerException ignored) {
                }
            }
        }

        // AudioFileWriter#write(AudioInputStream, Type, File)
        for (final Type type : types) {
            try {
                afw.write(null, type, new File("test.sound"));
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                afw.write(stream, null, new File("test.sound"));
                throw new RuntimeException("NPE is expected: " + afw);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            for (final Type type : types) {
                try {
                    afw.write(stream, type, (File) null);
                    throw new RuntimeException("NPE is expected: " + afw);
                } catch (final NullPointerException ignored) {
                }
            }
        }
    }

    /**
     * Tests some default implementation of AudioFileWriter API, using the
     * custom {@code AudioFileWriter}, which support nothing.
     */
    static final AudioFileWriter customAFW = new AudioFileWriter() {
        @Override
        public Type[] getAudioFileTypes() {
            return new Type[0];
        }

        @Override
        public Type[] getAudioFileTypes(final AudioInputStream stream) {
            Objects.requireNonNull(stream);
            return new Type[0];
        }

        @Override
        public int write(AudioInputStream stream, Type fileType,
                         OutputStream out) {
            Objects.requireNonNull(stream);
            Objects.requireNonNull(fileType);
            Objects.requireNonNull(out);
            return 0;
        }

        @Override
        public int write(AudioInputStream stream, Type fileType, File out) {
            Objects.requireNonNull(stream);
            Objects.requireNonNull(fileType);
            Objects.requireNonNull(out);
            return 0;
        }
    };

    private static AudioInputStream getAIS(final Type type, Encoding encoding) {
        AudioFormat af = new AudioFormat(encoding, 44100.0f, 16, 2, 1, 1, true);
        AudioFileFormat aif = new AudioFileFormat(type, af, 0);
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[1024]);
        return new AudioInputStream(bais, aif.getFormat(), 0);
    }

    private static final class NullOutputStream extends OutputStream {

        @Override
        public void write(final int b) {
            //do nothing
        }
    }
}
