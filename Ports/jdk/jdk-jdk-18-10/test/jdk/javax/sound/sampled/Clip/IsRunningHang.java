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
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

/**
 * @test
 * @bug 8156169
 * @run main/othervm/timeout=300 IsRunningHang
 */
public final class IsRunningHang {

    private static CountDownLatch go;

    /**
     * We will try to use all usually supported formats.
     */
    private static final List<AudioFormat> formats = new ArrayList<>();

    private static final AudioFormat.Encoding[] encodings = {
            AudioFormat.Encoding.ALAW, AudioFormat.Encoding.ULAW,
            AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED,
            AudioFormat.Encoding.PCM_FLOAT
    };

    private static final int[] sampleRates = {8000, 16000, 48000};

    private static final int[] sampleBits = {8, 16, 24, 32, 64};

    private static final int[] channels = {1, 2, 3, 5};

    static {
        for (final Boolean end : new boolean[]{false, true}) {
            for (final int sampleSize : sampleBits) {
                for (final int sampleRate : sampleRates) {
                    for (final int channel : channels) {
                        for (final AudioFormat.Encoding enc : encodings) {
                            int s = ((sampleSize + 7) / 8) * channel;
                            formats.add(new AudioFormat(enc, sampleRate,
                                                        sampleSize, channel,
                                                        s, sampleRate, end));
                        }
                    }
                }
            }
        }
    }

    public static void main(final String[] args) throws Exception {
        for (final AudioFormat format : formats) {
            System.out.println("format = " + format);
            // create a 0.5-second data
            byte[] soundData = new byte[(int) (format.getFrameRate()
                                                       * format.getFrameSize()
                                                       / 2)];
            try {
                test(format, soundData);
            } catch (LineUnavailableException | IllegalArgumentException ignored) {
                // the test is not applicable
            }
        }
    }

    private static void test(final AudioFormat format, final byte[] data)
            throws Exception {
        final Line.Info info = new DataLine.Info(Clip.class, format);
        final Clip clip = (Clip) AudioSystem.getLine(info);

        go = new CountDownLatch(1);
        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
                go.countDown();
            }
        });

        clip.open(format, data, 0, data.length);
        clip.start();
        go.await();
        while (clip.isRunning()) {
            // This loop should not hang
        }
        while (clip.isActive()) {
            // This loop should not hang
        }
        clip.close();
    }
}
