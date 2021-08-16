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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8146144
 */
public final class GetTargetIsSupported {

    /**
     * We will try to use all formats, in this case all our providers will be
     * covered by supported/unsupported formats.
     */
    private static final List<AudioFormat> formats = new ArrayList<>(23000);

    private static final Encoding[] encodings = {
            Encoding.ALAW, Encoding.ULAW, Encoding.PCM_SIGNED,
            Encoding.PCM_UNSIGNED, Encoding.PCM_FLOAT, new Encoding("Test")
    };

    private static final int[] sampleRates = {
            AudioSystem.NOT_SPECIFIED, 8000, 11025, 16000, 22050, 32000, 37800,
            44056, 44100, 47250, 48000, 50000, 50400, 88200, 96000, 176400,
            192000, 352800, 2822400, 5644800
    };

    private static final int[] sampleBits = {
            AudioSystem.NOT_SPECIFIED, 4, 8, 11, 16, 20, 24, 32, 48, 64, 128
    };

    private static final int[] channels = {
            AudioSystem.NOT_SPECIFIED, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    };

    static {
        for (final Boolean end : new boolean[]{false, true}) {
            for (final int sampleSize : sampleBits) {
                for (final int sampleRate : sampleRates) {
                    for (final int channel : channels) {
                        for (final Encoding enc : encodings) {
                            formats.add(new AudioFormat(enc, sampleRate,
                                                        sampleSize, channel,
                                                        1, sampleRate, end));
                        }
                    }
                }
            }
        }
    }

    public static void main(final String[] args) {
        for (final AudioFormat format : formats) {
            testAS(format);
            for (final FormatConversionProvider fcp : load
                    (FormatConversionProvider.class)) {
                testFCP(fcp, format);
            }
        }
    }

    /**
     * Tests the part of AudioSystem API, which implemented via
     * FormatConversionProvider.
     *
     * @see AudioSystem#getTargetEncodings(Encoding)
     * @see AudioSystem#getTargetEncodings(AudioFormat)
     * @see AudioSystem#getTargetFormats(Encoding, AudioFormat)
     * @see AudioSystem#isConversionSupported(AudioFormat, AudioFormat)
     * @see AudioSystem#isConversionSupported(Encoding, AudioFormat)
     */
    private static void testAS(final AudioFormat source) {
        Encoding[] all = AudioSystem.getTargetEncodings(source.getEncoding());
        Encoding[] part = AudioSystem.getTargetEncodings(source);

        // Check encodings which are reported as supported
        for (final Encoding enc : part) {
            // If encoding is reported for the source format means that
            // the list of target formats should not be empty
            AudioFormat[] targets = AudioSystem.getTargetFormats(enc, source);
            // all reported formats should be supported
            for (final AudioFormat target : targets) {
                if (!AudioSystem.isConversionSupported(target, source)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!enc.equals(target.getEncoding())) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
            // If encoding is reported for the source format means that
            // conversion source -> encoding is supported
            if (!AudioSystem.isConversionSupported(enc, source)) {
                throw new RuntimeException("Error:" + enc);
            }
            // encoding for a particular source should be included in the
            // list of all encodings for the source's encoding
            if (!Arrays.asList(all).contains(enc)) {
                throw new RuntimeException("Error:" + enc);
            }
            // If conversion source -> encoding is supported then an
            // array of target formats should not be empty
            if (source.getEncoding().equals(enc)) {
                // this is unspecified but we works this way
                if (!isContains(source, targets)) {
                    throw new RuntimeException("Error:" + enc);
                }
            } else {
                if (targets.length == 0) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
        }

        // Check all encodings
        for (final Encoding enc : encodings) {
            AudioFormat[] targets = AudioSystem.getTargetFormats(enc, source);
            // If target format is reported for the source format means that
            // conversion source -> target is supported
            for (final AudioFormat target : targets) {
                if (!AudioSystem.isConversionSupported(target, source)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!enc.equals(target.getEncoding())) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
            if (AudioSystem.isConversionSupported(enc, source)) {
                // encoding for a particular source should be included in the
                // list of all encodings for the source's encoding
                if (!Arrays.asList(all).contains(enc)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!Arrays.asList(part).contains(enc)) {
                    System.out.println("enc = " + enc);
                    System.out.println("part = " + Arrays.toString(part));
                    System.out.println("source = " + source);
                    throw new RuntimeException("Error:" + enc);
                }
                // If conversion source -> encoding is supported then an
                // array of target formats should not be empty
                if (source.getEncoding().equals(enc)) {
                    // this is unspecified but we works this way
                    if (!isContains(source, targets)) {
                        throw new RuntimeException("Error:" + enc);
                    }
                } else {
                    if (targets.length == 0) {
                        throw new RuntimeException("Error:" + enc);
                    }
                }
            } else {
                // If conversion source -> encoding is not supported then an
                // array of target formats should be empty
                if (targets.length != 0) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (Arrays.asList(part).contains(enc)) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
        }
    }

    /**
     * Tests the FormatConversionProvider API directly.
     *
     * @see FormatConversionProvider#getTargetEncodings()
     * @see FormatConversionProvider#getTargetEncodings(AudioFormat)
     * @see FormatConversionProvider#getTargetFormats(Encoding, AudioFormat)
     * @see FormatConversionProvider#isTargetEncodingSupported(Encoding)
     * @see FormatConversionProvider#isConversionSupported(Encoding,
     * AudioFormat)
     * @see FormatConversionProvider#isConversionSupported(AudioFormat,
     * AudioFormat)
     */
    private static void testFCP(final FormatConversionProvider fcp,
                                final AudioFormat source) {
        final Encoding[] all = fcp.getTargetEncodings();
        for (final Encoding enc : all) {
            if (!fcp.isTargetEncodingSupported(enc)) {
                throw new RuntimeException("Error:" + enc);
            }
        }

        // Check encodings which are reported as supported
        final Encoding[] part = fcp.getTargetEncodings(source);
        for (final Encoding enc : part) {
            // If encoding is reported for the source format means that
            // the list of target formats should not be empty for this encoding
            AudioFormat[] targets = fcp.getTargetFormats(enc, source);
            // all reported formats should be supported
            for (final AudioFormat target : targets) {
                if (!fcp.isConversionSupported(target, source)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!enc.equals(target.getEncoding())) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
            // If encoding is reported for the source format means that
            // conversion source -> encoding is supported
            if (!fcp.isConversionSupported(enc, source)) {
                throw new RuntimeException("Error:" + enc);
            }
            // If conversion source -> encoding is supported then an
            // array of target formats should not be empty
            if (targets.length == 0) {
                throw new RuntimeException("Error:" + enc);
            }
            // encoding for a particular source should be included in the
            // list of all encodings for the source's encoding
            if (!Arrays.asList(all).contains(enc)) {
                throw new RuntimeException("Error:" + enc);
            }
        }
        // Check all encodings
        for (final Encoding enc : encodings) {
            AudioFormat[] targets = fcp.getTargetFormats(enc, source);
            // If target format is reported for the source format means that
            // conversion source -> target is supported
            for (final AudioFormat target : targets) {
                if (!fcp.isConversionSupported(target, source)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!enc.equals(target.getEncoding())) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
            if (fcp.isConversionSupported(enc, source)) {
                // If conversion source -> encoding is supported then an
                // array of target formats should not be empty
                if (targets.length == 0) {
                    throw new RuntimeException("Error:" + enc);
                }
                // encoding for a particular source should be included in the
                // list of all encodings for the source's encoding
                if (!Arrays.asList(all).contains(enc)) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (!Arrays.asList(part).contains(enc)) {
                    throw new RuntimeException("Error:" + enc);
                }
            } else {
                // If conversion source -> encoding is not supported then an
                // array of target formats should be empty
                if (targets.length != 0) {
                    throw new RuntimeException("Error:" + enc);
                }
                if (Arrays.asList(part).contains(enc)) {
                    throw new RuntimeException("Error:" + enc);
                }
            }
        }
    }

    private static boolean isContains(AudioFormat obj, AudioFormat[] array) {
        for (final AudioFormat format : array) {
            if (obj.matches(format)) {
                return true;
            }
        }
        return false;
    }
}
