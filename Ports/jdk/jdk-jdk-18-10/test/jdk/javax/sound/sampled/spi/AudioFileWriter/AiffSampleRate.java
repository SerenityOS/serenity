/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4914639
 * @summary JavaSound writes wrong sample rates to AIFF files
 */
public class AiffSampleRate {

    private static final float[] testSampleRates =
    {8000.0F, 8000.0F + 0.011F, 8193.975F, 10000.0F, 11025.0F, 12000.0F,
     16000.0F, 22050.0F, 24000.0F, 32000.0F, 44100.0F - 1.22222F, 44100.0F,
     47888.888F, 48000.0F, 96000.0F, 192000.0F};

    public static void main(String[] args) throws Exception {
        boolean isTestPassed = true;

        out("#4914639: JavaSound writes wrong sample rates to AIFF files");
        for (int i = 0; i < testSampleRates.length; i++) {
            isTestPassed &= testSampleRate(testSampleRates[i]);
        }
        if (isTestPassed) {
            out("Test PASSED.");
        } else {
            throw new Exception("Test FAILED.");
        }
    }

    private static boolean testSampleRate(float sampleRate) {
        boolean result = true;

        try {
            // create AudioInputStream with sample rate of 10000 Hz
            ByteArrayInputStream data = new ByteArrayInputStream(new byte[1]);
            AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true);
            AudioInputStream stream = new AudioInputStream(data, format, 1);

            // write to AIFF file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            AudioSystem.write(stream, AudioFileFormat.Type.AIFF, outputStream);
            byte[] fileData = outputStream.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(fileData);
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(inputStream);
            if (! equals(sampleRate, aff.getFormat().getFrameRate())) {
                out("error for sample rate " + sampleRate);
                result = false;
            }
        } catch (Exception e) {
            out(e);
            out("Test NOT FAILED");
        }
        return result;
    }

    private static boolean equals(float f1, float f2) {
        return Math.abs(f2 - f1) < 1.0E-9;
    }

    private static void out(Throwable t) {
        t.printStackTrace(System.out);
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
