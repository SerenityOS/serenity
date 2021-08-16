/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4385654
 * @summary Check that the MASTER_GAIN control has a valid precision
 */
//public class test047 extends TRTest
public class FloatControlBug {

    private Clip theClip;

    boolean testPassed = true;

    private AudioFormat.Encoding theEncoding = AudioFormat.Encoding.PCM_SIGNED;

    private float theSampleRate = 44100;

    private int theSampleSize = 16;

    private int theNumberOfChannels = 1;

    private int theFrameSize = 2;

    private float theFrameRate = 44100;

    private boolean isBigEndian = false;

    //_______________________________________________
    //      Method: runTest
    //_______________________________________________
    public boolean runTest() {
        AudioInputStream theAudioInputStream = new AudioInputStream(
                new ByteArrayInputStream(new byte[0]),
                new AudioFormat(44100.0f, 16, 2, true, false), 441000);

        AudioFormat theAudioFormat = theAudioInputStream.getFormat();

        DataLine.Info info = new DataLine.Info(Clip.class, theAudioFormat,
                                               AudioSystem.NOT_SPECIFIED);
        try {
            theClip = (Clip) AudioSystem.getLine(info);
            theClip.open(theAudioInputStream);
            FloatControl theFloatControl = (FloatControl) (theClip.getControl(
                    FloatControl.Type.MASTER_GAIN));
            float theFloatControlPrecision = theFloatControl.getPrecision();
            System.out.println(
                    "theFloatControlPrecision: " + theFloatControlPrecision);
            System.out.println("Minimum: " + theFloatControl.getMinimum());
            System.out.println("Maximum: " + theFloatControl.getMaximum());
            System.out.println("Value  : " + theFloatControl.getValue());
            testPassed = theFloatControlPrecision > 0;
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
        //test047 thisTest = new test047();
        if (isSoundcardInstalled()) {
            FloatControlBug thisTest = new FloatControlBug();
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
