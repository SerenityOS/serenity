/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * @test
 * @bug 4681384
 * @summary SourceDataLine.write() causes Unexpected Signal 11 in native code
 *          outside the VM
 */
public class SourceDataLineDefaultBufferSizeCrash {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    public static void main(String argv[]) throws Exception {
        int testExitStatus = run(argv, System.out) + STATUS_TEMP;
    }

    public static int run(String argv[], java.io.PrintStream out) throws Exception {
        int testResult = STATUS_PASSED;

        int framesNumberToExceed = 2;
        if ( argv.length > 0 ) {
            try {
                framesNumberToExceed = Integer.parseInt(argv[0]);
            }
            catch (NumberFormatException e) {
            }
        }

        out.println
            ("\n==> Test for SourceDataLine.write() method:");

        Mixer.Info[] installedMixersInfo = AudioSystem.getMixerInfo();

        if ( installedMixersInfo == null ) {
            out.println("## AudioSystem.getMixerInfo() returned unexpected result:");
            out.println("#  expected: an array of Mixer.Info objects (may be array of length 0);");
            out.println("#  produced: null;");
            return STATUS_FAILED;
        }

        if ( installedMixersInfo.length == 0 ) {
            // there are no mixers installed on the system -
            // so this testcase can not be tested
            out.println("\n>>>  There are no mixers installed on the system!");
            return STATUS_PASSED;
        }

        out.println("\n>>>  Number of mixers installed on the system = "
            + installedMixersInfo.length);
        Mixer installedMixer = null;
        for (int i=0; i < installedMixersInfo.length; i++) {
            try {
                installedMixer = AudioSystem.getMixer(installedMixersInfo[i]);
            } catch (SecurityException securityException) {
                // installed Mixer is unavailable because of security restrictions
                out.println("\n>>>  installedMixer[" + i
                    + "] is unavailable because of security restrictions");
                continue;
            } catch (Throwable thrown) {
                out.println("\n##  installedMixer[" + i + "] is unavailable because of");
                out.println("#  AudioSystem.getMixer() threw unexpected exception:");
                thrown.printStackTrace(out);
                testResult = STATUS_FAILED;
                continue;
            }

            out.println("\n>>>  installedMixer["+i+"] = " + installedMixer);
            try {
                installedMixer.open();
            } catch (LineUnavailableException lineUnavailableException) {
                // installedMixer is not available due to resource restrictions
                out.println(">>   installedMixer[" + i
                    + "] is not opened because of resource restrictions");
                continue;
            } catch (SecurityException securityException) {
                // installedMixer is not available due to security restrictions
                out.println(">>   installedMixer[" + i
                    + "] is not opened because of security restrictions");
                continue;
            } catch (Throwable thrown) {
                out.println("## installedMixer.open() throws unexpected exception:");
                thrown.printStackTrace(out);
                testResult = STATUS_FAILED;
                continue;
            }
            Line.Info supportedSourceLineInfo[] = null;
            try {
                supportedSourceLineInfo = installedMixer.getSourceLineInfo();
            } catch (Throwable thrown) {
                out.println("## installedMixer.getSourceLineInfo() throws "
                    + "unexpected exception:");
                thrown.printStackTrace(out);
                testResult = STATUS_FAILED;
                installedMixer.close();
                continue;
            }
            if ( supportedSourceLineInfo == null ) {
                out.println("## installedMixer.getSourceLineInfo() returned null array");
                out.println("#  Mixer = " + installedMixer);
                testResult = STATUS_FAILED;
                installedMixer.close();
                continue;
            }
            out.println("\n>>  Number of SourceLineInfo supported by installedMixer ="
                + supportedSourceLineInfo.length);

            for (int j=0; j < supportedSourceLineInfo.length; j++) {
                Line.Info testSourceLineInfo = supportedSourceLineInfo[j];

                out.println("\n>  testSourceLineInfo["+j+"] = " + testSourceLineInfo);
                Line testSourceLine = null;
                try {
                    testSourceLine = installedMixer.getLine(testSourceLineInfo);
                } catch (LineUnavailableException lineUnavailableException) {
                    // line is not available due to resource restrictions
                    out.println(">  Line for this SourceLine Info is not available "
                        + "due to resource restrictions");
                    continue;
                } catch (SecurityException securityException) {
                    // line is not available due to security restrictions
                    out.println(">  Line for this SourceLine Info is not available "
                        + "due to security restrictions");
                    continue;
                } catch (Throwable thrown) {
                    out.println("## installedMixer.getLine(testSourceLineInfo) throws"
                        + "unexpected Exception:");
                    thrown.printStackTrace(out);
                    testResult = STATUS_FAILED;
                    continue;
                }

                out.println(">  testedSourceLine = " + testSourceLine);
                if ( ! (testSourceLine instanceof SourceDataLine) ) {
                    out.println(">  testSourceLine is not SourceDataLine");
                    continue;
                }

                SourceDataLine testedSourceLine = (SourceDataLine)testSourceLine;
                AudioFormat lineAudioFormat = testedSourceLine.getFormat();

                out.println("\n>  opening tested SourceLine:");
                try {
                    //testedSourceLine.open(lineAudioFormat, 2048);
                    testedSourceLine.open(lineAudioFormat);
                    out.println(">  OK - line is opened with "+testedSourceLine.getBufferSize()+" bytes buffer");
                } catch (LineUnavailableException lineUnavailableException) {
                    out.println(">  Line is not available due to resource restrictions:");
                    lineUnavailableException.printStackTrace(out);
                    continue;
                } catch (SecurityException securityException) {
                    out.println("> Line is not available due to security restrictions:");
                    securityException.printStackTrace(out);
                    continue;
                } catch (Throwable thrown) {
                    out.println("## SourceDataLine.open(AudioFormat format) failed:");
                    out.println("#  Unexpected Exception is thrown");
                    out.println("#  Mixer = " + installedMixer);
                    out.println("#  SourceDataLine = " + testedSourceLine);
                    thrown.printStackTrace(out);
                    testResult = STATUS_FAILED;
                    continue;
                }

                testedSourceLine.start();

                int frameSize = 1;
                if ( lineAudioFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED ) {
                    frameSize = lineAudioFormat.getFrameSize();
                } else {
                    if ( lineAudioFormat.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED ) {
                        frameSize = lineAudioFormat.getSampleSizeInBits()/8;
                        if ( lineAudioFormat.getSampleSizeInBits()%8 != 0 ) {
                            frameSize++;
                        }
                    }
                }
                int bufferSizeToWrite = testedSourceLine.available()
                    + (frameSize * framesNumberToExceed);
                byte[] dataToWrite = new byte[bufferSizeToWrite];
                for (int k=0; k < bufferSizeToWrite; k++) {
                    dataToWrite[k] = (byte)1;
                }
                int offsetToWrite = 0;

                out.println("\n>  check SourceDataLine.write() to write more data "
                    + "than can currently be written:");

                out.println(">  testedSourceLine.available() = " + testedSourceLine.available());
                out.println(">  frame size = " + frameSize);
                out.println(">  number of bytes to write = " + bufferSizeToWrite);
                int writtenBytes = -1;
                try {
                    writtenBytes =
                        testedSourceLine.write(dataToWrite, offsetToWrite, bufferSizeToWrite);
                    out.println(">  OK - number of written bytes = " + writtenBytes);
                } catch (Throwable thrown) {
                    out.println("## SourceDataLine.write(byte[] b, int off, int len) failed:");
                    out.println("#  Unexpected Exception is thrown");
                    thrown.printStackTrace(out);
                    testResult = STATUS_FAILED;
                }

                testedSourceLine.close();

            }  // for (int j=0; j < supportedSourceLineInfo.length; j++)
            installedMixer.close();

        }  // for (int i=0; i < installedMixersInfo.length; i++)

        if ( testResult == STATUS_FAILED ) {
            throw new Exception("Test FAILED!");
        } else {
            out.println("\n==> test PASSED!");
        }
        return testResult;
    }

}
