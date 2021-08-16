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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import static java.util.ServiceLoader.load;
import static javax.sound.sampled.AudioFileFormat.*;

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
        for (final FormatConversionProvider fcp : load(
                FormatConversionProvider.class)) {
            testFCP(fcp);
        }
        testFCP(customFCP);
    }

    /**
     * Tests the part of AudioSystem API, which implemented via
     * FormatConversionProvider.
     */
    private static void testAS() throws Exception {

        // AudioSystem#getAudioInputStream(Encoding, AudioInputStream)
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.getAudioInputStream(encoding, null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final AudioInputStream stream : aiss) {
            try {
                AudioSystem.getAudioInputStream((Encoding) null, stream);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        // AudioSystem#getAudioInputStream(AudioFormat, AudioInputStream)
        for (final AudioInputStream stream : aiss) {
            try {
                AudioSystem.getAudioInputStream((AudioFormat) null, stream);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.getAudioInputStream(getAFF(encoding), null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }

        // AudioSystem#getTargetEncodings(AudioFormat)
        try {
            AudioSystem.getTargetEncodings((AudioFormat) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }

        // AudioSystem#getTargetEncodings(AudioFormat.Encoding)
        try {
            AudioSystem.getTargetEncodings((Encoding) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }

        // AudioSystem#getTargetFormats(AudioFormat.Encoding, AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.getTargetFormats(null, getAFF(encoding));
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.getTargetFormats(encoding, null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }

        // AudioSystem#isConversionSupported(AudioFormat, AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.isConversionSupported((AudioFormat) null,
                                                  getAFF(encoding));
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.isConversionSupported(getAFF(encoding), null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }

        // AudioSystem#isConversionSupported(AudioFormat.Encoding, AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.isConversionSupported((Encoding) null,
                                                  getAFF(encoding));
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                AudioSystem.isConversionSupported(encoding, null);
                throw new RuntimeException("NPE is expected");
            } catch (final NullPointerException ignored) {
            }
        }
    }

    /**
     * Tests the FormatConversionProvider API directly.
     */
    private static void testFCP(FormatConversionProvider fcp) throws Exception {

        // FormatConversionProvider#isSourceEncodingSupported(Encoding)
        try {
            fcp.isSourceEncodingSupported(null);
            throw new RuntimeException("NPE is expected: " + fcp);
        } catch (final NullPointerException ignored) {
        }

        // FormatConversionProvider#isTargetEncodingSupported(Encoding)
        try {
            fcp.isTargetEncodingSupported(null);
            throw new RuntimeException("NPE is expected: " + fcp);
        } catch (final NullPointerException ignored) {
        }

        // FormatConversionProvider#getTargetEncodings()
        try {
            fcp.getTargetEncodings(null);
            throw new RuntimeException("NPE is expected: " + fcp);
        } catch (final NullPointerException ignored) {
        }

        // FormatConversionProvider#isConversionSupported(Encoding, AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                fcp.isConversionSupported((Encoding) null, getAFF(encoding));
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                fcp.isConversionSupported(encoding, null);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }

        // FormatConversionProvider#getTargetFormats(Encoding, AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                fcp.getTargetFormats(null, getAFF(encoding));
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                fcp.getTargetFormats(encoding, null);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }

        // FormatConversionProvider#isConversionSupported(AudioFormat,
        // AudioFormat)
        for (final Encoding encoding : encodings) {
            try {
                fcp.isConversionSupported((AudioFormat) null, getAFF(encoding));
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                fcp.isConversionSupported(getAFF(encoding), null);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }

        // FormatConversionProvider#getAudioInputStream(Encoding,
        // AudioInputStream)
        for (final AudioInputStream stream : aiss) {
            try {
                fcp.getAudioInputStream((Encoding) null, stream);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                fcp.getAudioInputStream(encoding, null);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }

        // FormatConversionProvider#getAudioInputStream(AudioFormat,
        // AudioInputStream)
        for (final AudioInputStream stream : aiss) {
            try {
                fcp.getAudioInputStream((AudioFormat) null, stream);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
        for (final Encoding encoding : encodings) {
            try {
                fcp.getAudioInputStream(getAFF(encoding), null);
                throw new RuntimeException("NPE is expected: " + fcp);
            } catch (final NullPointerException ignored) {
            }
        }
    }

    /**
     * Tests some default implementation of FormatConversionProvider API, using
     * the custom {@code FormatConversionProvider}, which support nothing.
     */
    static FormatConversionProvider customFCP = new FormatConversionProvider() {

        @Override
        public Encoding[] getSourceEncodings() {
            return new Encoding[0];
        }

        @Override
        public Encoding[] getTargetEncodings() {
            return new Encoding[0];
        }

        @Override
        public Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
            Objects.requireNonNull(sourceFormat);
            return new Encoding[0];
        }

        @Override
        public AudioFormat[] getTargetFormats(Encoding enc, AudioFormat frmt) {
            Objects.requireNonNull(enc);
            Objects.requireNonNull(frmt);
            return new AudioFormat[0];
        }

        @Override
        public AudioInputStream getAudioInputStream(Encoding encoding,
                                                    AudioInputStream stream) {
            Objects.requireNonNull(encoding);
            Objects.requireNonNull(stream);
            return null;
        }

        @Override
        public AudioInputStream getAudioInputStream(AudioFormat format,
                                                    AudioInputStream stream) {
            Objects.requireNonNull(format);
            Objects.requireNonNull(stream);
            return null;
        }
    };

    private static AudioFormat getAFF(final Encoding encoding) {
        return new AudioFormat(encoding, 44100.0f, 16, 2, 1, 1, true);
    }

    private static AudioInputStream getAIS(final Type type, Encoding encoding) {
        AudioFormat af = new AudioFormat(encoding, 44100.0f, 16, 2, 1, 1, true);
        AudioFileFormat aif = new AudioFileFormat(type, af, 0);
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[1024]);
        return new AudioInputStream(bais, aif.getFormat(), 0);
    }
}
