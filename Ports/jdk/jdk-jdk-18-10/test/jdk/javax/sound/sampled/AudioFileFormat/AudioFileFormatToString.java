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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

/**
 * @test
 * @bug 4672864
 * @summary AudioFileFormat.toString() throws unexpected NullPointerException
 */
public class AudioFileFormatToString {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    public static void main(String argv[]) throws Exception {
        int testExitStatus = run(argv, System.out);
        if (testExitStatus != STATUS_PASSED) {
            throw new Exception("Test FAILED " + testExitStatus);
        }
        System.out.println("Test passed.");
    }

    public static int run(String argv[], java.io.PrintStream out) {
        int testResult = STATUS_PASSED;

        out.println("\n==> Test for AudioFileFormat class:");

        AudioFormat testAudioFormat =
         new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,  // AudioFormat.Encoding
                         (float) 44100.0, // float SampleRate
                         (int) 8, // int sampleSizeInBits
                         (int) 2, // int channels
                         (int) 2,    // int frameSize
                         (float) 110.0,    // float frameRate
                         true    // boolean bigEndian
                         );
        AudioFormat nullAudioFormat = null;

        AudioFileFormat.Type testAudioFileFormatType = AudioFileFormat.Type.WAVE;
        AudioFileFormat.Type nullAudioFileFormatType = null;

        AudioFileFormat testedAudioFileFormat = null;
        out.println("\n>> public AudioFileFormat constructor for AudioFileFormat.Type = null: ");
        try {
         testedAudioFileFormat =
             new AudioFileFormat(nullAudioFileFormatType,  // AudioFileFormat.Type
                                 testAudioFormat, // AudioFormat
                                 (int) 1024 // int frameLength
                                 );
         out.println(">  No any Exception was thrown!");
         out.println(">  testedAudioFileFormat.getType():");
         try {
             AudioFileFormat.Type producedType = testedAudioFileFormat.getType();
             out.println(">   PASSED: producedType = " + producedType);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
         out.println(">  testedAudioFileFormat.toString():");
         try {
             String producedString = testedAudioFileFormat.toString();
             out.println(">   PASSED: producedString = " + producedString);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
        } catch (IllegalArgumentException illegArgExcept) {
         out.println(">   PASSED: expected IllegalArgumentException was thrown:");
         illegArgExcept.printStackTrace(out);
        } catch (NullPointerException nullPE) {
         out.println(">   PASSED: expected NullPointerException was thrown:");
         nullPE.printStackTrace(out);
        } catch (Throwable thrown) {
         out.println("##  FAILED: unexpected Exception was thrown:");
         thrown.printStackTrace(out);
         testResult = STATUS_FAILED;
        }

        out.println("\n>> public AudioFileFormat constructor for AudioFormat = null: ");
        try {
         testedAudioFileFormat =
             new AudioFileFormat(testAudioFileFormatType,  // AudioFileFormat.Type
                                 nullAudioFormat, // AudioFormat
                                 (int) 1024 // int frameLength
                                 );
         out.println(">  No any Exception was thrown!");
         out.println(">  testedAudioFileFormat.getFormat():");
         try {
             AudioFormat producedFormat = testedAudioFileFormat.getFormat();
             out.println(">   PASSED: producedFormat = " + producedFormat);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
         out.println(">  testedAudioFileFormat.toString():");
         try {
             String producedString = testedAudioFileFormat.toString();
             out.println(">   PASSED: producedString = " + producedString);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
        } catch (IllegalArgumentException illegArgExcept) {
            out.println(">   PASSED: expected IllegalArgumentException was thrown:");
            illegArgExcept.printStackTrace(out);
        } catch (NullPointerException nullPE) {
            out.println(">   PASSED: expected NullPointerException was thrown:");
            nullPE.printStackTrace(out);
        } catch (Throwable thrown) {
            out.println("##  FAILED: unexpected Exception was thrown:");
            thrown.printStackTrace(out);
            testResult = STATUS_FAILED;
        }

        out.println("\n>> protected AudioFileFormat constructor for AudioFileFormat.Type = null: ");
        try {
         testedAudioFileFormat =
             new TestAudioFileFormat(nullAudioFileFormatType,  // AudioFileFormat.Type
                                     (int) 1024, // byteLength
                                     testAudioFormat, // AudioFormat
                                     (int) 1024 // int frameLength
                                     );
         out.println(">  No any Exception was thrown!");
         out.println(">  testedAudioFileFormat.getType():");
         try {
             AudioFileFormat.Type producedType = testedAudioFileFormat.getType();
             out.println(">   PASSED: producedType = " + producedType);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
         out.println(">  testedAudioFileFormat.toString():");
         try {
             String producedString = testedAudioFileFormat.toString();
             out.println(">   PASSED: producedString = " + producedString);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
        } catch (IllegalArgumentException illegArgExcept) {
         out.println(">   PASSED: expected IllegalArgumentException was thrown:");
         illegArgExcept.printStackTrace(out);
        } catch (NullPointerException nullPE) {
         out.println(">   PASSED: expected NullPointerException was thrown:");
         nullPE.printStackTrace(out);
        } catch (Throwable thrown) {
         out.println("##  FAILED: unexpected Exception was thrown:");
         thrown.printStackTrace(out);
         testResult = STATUS_FAILED;
        }

        out.println("\n>> protected AudioFileFormat constructor for AudioFormat = null: ");
        try {
         testedAudioFileFormat =
             new TestAudioFileFormat(testAudioFileFormatType,  // AudioFileFormat.Type
                                     (int) 1024, // byteLength
                                     nullAudioFormat, // AudioFormat
                                     (int) 1024 // int frameLength
                                     );
         out.println(">  No any Exception was thrown!");
         out.println(">  testedAudioFileFormat.getFormat():");
         try {
             AudioFormat producedFormat = testedAudioFileFormat.getFormat();
             out.println(">   PASSED: producedFormat = " + producedFormat);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
         out.println(">  testedAudioFileFormat.toString():");
         try {
             String producedString = testedAudioFileFormat.toString();
             out.println(">   PASSED: producedString = " + producedString);
         } catch (Throwable thrown) {
             out.println("##  FAILED: unexpected Exception was thrown:");
             thrown.printStackTrace(out);
             testResult = STATUS_FAILED;
         }
        } catch (IllegalArgumentException illegArgExcept) {
         out.println(">   PASSED: expected IllegalArgumentException was thrown:");
         illegArgExcept.printStackTrace(out);
        } catch (NullPointerException nullPE) {
         out.println(">   PASSED: expected NullPointerException was thrown:");
         nullPE.printStackTrace(out);
        } catch (Throwable thrown) {
         out.println("##  FAILED: unexpected Exception was thrown:");
         thrown.printStackTrace(out);
         testResult = STATUS_FAILED;
        }

        if (testResult == STATUS_FAILED) {
            out.println("\n==> test FAILED!");
        } else {
            out.println("\n==> test PASSED!");
        }
        return testResult;
    }
}

class TestAudioFileFormat extends AudioFileFormat {

    TestAudioFileFormat(AudioFileFormat.Type type, int byteLength,
                        AudioFormat format, int frameLength) {
        super(type, byteLength, format, frameLength);
    }
}
