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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 6459818
 * @summary Audio A-law and law decoder skip() method not implemented
 * @author Klaus Jaensch
 */
public class SkipOnConvertSampleSize {

    private static final int TEST_FRAME_LENGTH = 20000;

    private static void testskipping(final Encoding encoding) throws Exception {

        // create temporary PCM_SIGNED audio file
        int pcmBufSize = TEST_FRAME_LENGTH * 2;
        byte[] tempAudioBuf = new byte[pcmBufSize];
        for (int i = 0; i < TEST_FRAME_LENGTH; i++) {
            // fill with noise
            tempAudioBuf[i * 2] = (byte) ((Math.random() - 1) * Byte.MAX_VALUE);
            tempAudioBuf[i * 2 + 1] = (byte) ((Math.random() - 1)
                    * Byte.MAX_VALUE);
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream(tempAudioBuf);
        AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
        final AudioInputStream testAis = new AudioInputStream(bis, format,
                                                              TEST_FRAME_LENGTH);
        final AudioFormat lawFormat;
        final byte[] alawAudioBuf;
        try (AudioInputStream lawStream = AudioSystem.getAudioInputStream(
                encoding, testAis)) {

            lawFormat = lawStream.getFormat();
            int alawFrameSize = lawFormat.getFrameSize();

            int lawBufSize = TEST_FRAME_LENGTH * alawFrameSize;
            alawAudioBuf = new byte[lawBufSize];
            int r1 = 0;
            int totalRead = 0;
            while ((r1 = lawStream.read(alawAudioBuf, totalRead,
                                        lawBufSize - totalRead)) != -1) {
                totalRead += r1;
            }
        }

        // Convert back to PCM

        ByteArrayInputStream alawBis = new ByteArrayInputStream(alawAudioBuf);
        AudioInputStream lawAis = new AudioInputStream(alawBis, lawFormat,
                                                       TEST_FRAME_LENGTH);
        try (AudioInputStream convPcmStream = AudioSystem.getAudioInputStream(
                Encoding.PCM_SIGNED, lawAis)) {
            final AudioFormat convPcmAudioFormat = convPcmStream.getFormat();
            final int convPcmFrameSize = convPcmAudioFormat.getFrameSize();

            // skip half of the stream
            final long toSkip = (TEST_FRAME_LENGTH / 2) * convPcmFrameSize;
            long skipped = 0;
            do {
                skipped += convPcmStream.skip(toSkip - skipped);
            } while (skipped < toSkip);
            int r2 = convPcmStream.read(new byte[convPcmFrameSize]);
            // if skip is not correctly implemented we are at the end of the
            // stream
            if (r2 == -1) {
                throw new RuntimeException(
                        "Skip method of decoder not correctly implemented!");
            }
            // otherwise we could read the rest ...
            // we don't do it here
        }
    }

    public static void main(final String[] args) throws Exception {
        testskipping(Encoding.ALAW);
        testskipping(Encoding.ULAW);
    }
}
