/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

/*
 * @test
 * @bug 5070081
 * @summary Tests that javax.sound.sampled.Clip does not loses position through
 *          stop/start
 */
public class bug5070081 {

    static AudioFormat format = new AudioFormat(22050, 8, 1, false, false);
    // create a 3-second file
    static byte[] soundData = new byte[(int) (format.getFrameRate() * format.getFrameSize() * 3)];

    static final int LOOP_COUNT = 5;

    static boolean test() throws Exception {
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Clip clip = null;
        boolean bSuccess = true;
        try {
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, soundData, 0, soundData.length);
        } catch (LineUnavailableException | IllegalArgumentException ignored) {
            // the test is not applicable
            return bSuccess;
        }

        long nLengthMS = clip.getMicrosecondLength()/1000;

        System.out.println("  Clip length:");
        System.out.println("    frames: " + clip.getFrameLength());
        System.out.println("    seconds: " + nLengthMS/1000.0);

        clip.start();                               // start playing
        Thread.sleep(1000);                         // wait a sec
        long time1 = currentTimeMillis();
        long pos1 = clip.getFramePosition();        // store the position
        clip.stop();                                // and then stop
        long pos2 = clip.getFramePosition();        // 2nd try
        long time2 = currentTimeMillis();

        System.out.println("  Position before stop: " + pos1);
        System.out.println("  Position after stop: " + pos2);

        long timeDiff = Math.abs(time2 - time1);
        // sample rate is 22050 per second, so 22.05 per ms
        long posDiff = (long) (Math.abs(pos2 - pos1) / 22.05);
        System.out.println("  d(time): " + timeDiff + " ms;"
                + "d(clip pos time): " + posDiff + " ms.");

        long nDerivation = posDiff - timeDiff;
        // add 50 ms for deviation (delay for stopping and errors due timer precision)
        if (nDerivation > 50) {
            System.out.println("  ERROR(1): The deviation is too much: " + nDerivation + " ms");
            bSuccess = false;
        }

        Thread.sleep(1000);
        clip.start();                               // start again
        Thread.sleep(100);
        while(clip.isRunning());                    // wait for the sound to finish

        int nEndPos = clip.getFramePosition();
        System.out.println("  Position at end: " + nEndPos);
        if (nEndPos > clip.getFrameLength()) {
            System.out.println("  ERROR(2): end position if out of range");
            bSuccess = false;
        }

        clip.close();

        return bSuccess;
    }

    public static void main(String[] args) throws Exception {
        for (int count=1; count <= LOOP_COUNT; count++)
        {
            System.out.println("loop " + count + "/" + LOOP_COUNT);
            if (!test())
            {
                System.out.println("Test FAILED");
                throw new RuntimeException("Test FAILED.");
            }
        }

        System.out.println("Test passed sucessfully");
    }

    private static long currentTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
