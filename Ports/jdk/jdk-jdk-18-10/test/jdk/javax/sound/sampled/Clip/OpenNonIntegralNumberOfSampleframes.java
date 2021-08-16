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

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

/**
 * @test
 * @bug 8167435
 */
public final class OpenNonIntegralNumberOfSampleframes {

    /**
     * We will try to use all formats, in this case all our providers will be
     * covered by supported/unsupported formats.
     */
    private static final List<AudioFormat> formats = new ArrayList<>(2900);

    private static final Encoding[] encodings = {
            Encoding.ALAW, Encoding.ULAW, Encoding.PCM_SIGNED,
            Encoding.PCM_UNSIGNED, Encoding.PCM_FLOAT
    };

    private static final int[] sampleRates = {
            8000, 11025, 16000, 32000, 44100
    };

    private static final int[] sampleBits = {
            4, 8, 11, 16, 20, 24, 32, 48, 64, 128
    };

    private static final int[] channels = {
            1, 2, 3, 4, 5, 6
    };

    static {
        for (final Boolean end : new boolean[]{false, true}) {
            for (final int sampleSize : sampleBits) {
                for (final int sampleRate : sampleRates) {
                    for (final int channel : channels) {
                        final int frameSize = ((sampleSize + 7) / 8) * channel;
                        if (frameSize == 1) {
                            // frameSize=1 is ok for any buffers, skip it
                            continue;
                        }
                        for (final Encoding enc : encodings) {
                            formats.add(
                                    new AudioFormat(enc, sampleRate, sampleSize,
                                                    channel, frameSize,
                                                    sampleRate, end));
                        }
                    }
                }
            }
        }
    }

    public static void main(final String[] args) {
        for (final AudioFormat af : formats) {
            try (Clip clip = AudioSystem.getClip()) {
                final int bufferSize = af.getFrameSize() + 1;
                try {
                    clip.open(af, new byte[100], 0, bufferSize);
                } catch (final IllegalArgumentException ignored) {
                    // expected exception
                    continue;
                } catch (final LineUnavailableException e) {
                    // should not occur, we passed incorrect bufferSize
                    e.printStackTrace();
                }
                System.err.println("af = " + af);
                System.err.println("bufferSize = " + bufferSize);
                throw new RuntimeException("Expected exception is not thrown");
            } catch (IllegalArgumentException
                    | LineUnavailableException ignored) {
                // the test is not applicable
            }
        }
    }
}
