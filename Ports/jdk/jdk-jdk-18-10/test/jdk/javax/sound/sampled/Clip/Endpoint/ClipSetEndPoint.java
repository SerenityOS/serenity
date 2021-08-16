/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4385928
 * @summary Verify that an endpoint -1 in Clip does not throw an exception
 */
//public class test048 extends TRTest
public class ClipSetEndPoint {

    private Clip theClip;

    boolean testPassed = true;

    //_______________________________________________
    //      Method: runTest
    //_______________________________________________
    public boolean runTest() {
        AudioInputStream theAudioInputStream = new AudioInputStream(
                new ByteArrayInputStream(new byte[2000]),
                new AudioFormat(8000.0f, 8, 1, false, false), 2000); //

        AudioFormat theAudioFormat = theAudioInputStream.getFormat();

        DataLine.Info info = new DataLine.Info(Clip.class, theAudioFormat,
                                               AudioSystem.NOT_SPECIFIED);
        try {
            theClip = (Clip) AudioSystem.getLine(info);
            theClip.open(theAudioInputStream);

            int theStartLoopPoint = 0;
            int theEndLoopPoint = -1;       //      -1 signifies the last frame

            theClip.setLoopPoints(theStartLoopPoint, theEndLoopPoint);
            //theClip.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            testPassed = true;
        } catch (Exception e) {
            e.printStackTrace();
            testPassed = false;
        }
        return testPassed;
    }

    //_______________________________________________
    //      Method: main
    //_______________________________________________
    public static void main(String[] args) throws Exception {
        if (isSoundcardInstalled()) {
            ClipSetEndPoint thisTest = new ClipSetEndPoint();
            boolean testResult = thisTest.runTest();
            if (testResult) {
                System.out.println("Test passed");
            } else {
                System.out.println("Test failed");
                throw new Exception("Test failed");
            }
        }
    }

    /**
     * Returns true if at least one soundcard is correctly installed
     * on the system.
     */
    public static boolean isSoundcardInstalled() {
        boolean result = false;
        try {
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            if (mixers.length > 0) {
                result = AudioSystem.getSourceDataLine(null) != null;
            }
        } catch (Exception e) {
            System.err.println("Exception occured: " + e);
        }
        if (!result) {
            System.err.println(
                    "Soundcard does not exist or sound drivers not installed!");
            System.err.println(
                    "This test requires sound drivers for execution.");
        }
        return result;
    }
}
