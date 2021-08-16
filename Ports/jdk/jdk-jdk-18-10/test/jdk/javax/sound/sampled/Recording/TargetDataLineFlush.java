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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 4836433
 * @summary Windows: TargetDataLine.flush() does not work. Since this test has
 *          some real-time variance, I disabled it by making it a manual test.
 * @run main/manual TargetDataLineFlush
 */
public class TargetDataLineFlush {
    TargetDataLine inLine;
    int SAMPLE_RATE = 11025;
    int BUFFER_MILLIS = 1000;
    int WAIT_MILLIS;
    int BITS = 16;
    int CHANNELS = 2;
    int bufferSize;
    AudioFormat format;
    Mixer.Info[] mixers;
    static boolean failed = false;

    public TargetDataLineFlush() {
        mixers = AudioSystem.getMixerInfo();
    }

    private void init() {
        // float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian
        format = new AudioFormat( (float) SAMPLE_RATE, BITS, CHANNELS, true, false);
        bufferSize = SAMPLE_RATE * BUFFER_MILLIS / 1000 * format.getFrameSize();
    }

    boolean openInputLine(int num)  throws LineUnavailableException {
        init();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format is an AudioFormat object
        // Obtain and open a outLine.
        if (num < 0) {
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("TargetDataLine is not supported by default mixer.");
                return false;
            }
            inLine = (TargetDataLine) AudioSystem.getLine(info);
        } else {
            Mixer mixer = AudioSystem.getMixer(mixers[num]);
            if (!mixer.isLineSupported(info)) {
                System.out.println("TargetDataLine is not supported by this mixer.");
                return false;
            }
            inLine = (TargetDataLine) mixer.getLine(info);
        }
        inLine.open(format, bufferSize);
        /*if (Math.abs(inLine.getBufferSize() - bufferSize) > 100) {
                inLine.close();
                System.out.println("TargetDataLine does not support buffer size of "+bufferSize+" bytes!");
                return false;
        }*/
        bufferSize = inLine.getBufferSize();
        /* 3/4 of buffer size ot wait */
        WAIT_MILLIS = (int) (bufferSize / format.getFrameSize() * 750 / format.getFrameRate());
        System.out.println("Buffer size: "+bufferSize+" bytes = "
            +((int) (bufferSize / format.getFrameSize() * 750 / format.getFrameRate()))+" millis");
        return true;
    }

    private String available() {
        int avail = inLine.available();
        int availMillis = (int) (avail / format.getFrameSize() * 1000 / format.getFrameRate());
        return "available "+avail+" bytes = "+availMillis+" millis";
    }

    private boolean recordSound(int num)  throws LineUnavailableException {
        if (!openInputLine(num)) {
            return false;
        }
        byte data[] = new byte[1000];
        try {
            System.out.println("Got line: "+inLine);
            System.out.println("Start recording" );
            inLine.start();
            System.out.print("Warm-up...");
            //System.out.print("Waiting 500 millis...");
            try { Thread.sleep(500); } catch (InterruptedException ie) {}
            //System.out.println("done. "+available());
            //System.out.print("Reading all data...");
            int avail0 = inLine.available();
            if (avail0 == 0) {
                System.out.println("Problem: TargetDataLine did not deliver any data!");
                System.out.println("Not a test failure, but serious failure nonetheless.");
            } else {
                while ((avail0 -= inLine.read(data, 0, Math.min(data.length, avail0))) > 0);
                System.out.println("done.  "+available());
                System.out.print("Waiting "+(WAIT_MILLIS)+" millis...");
                try { Thread.sleep(WAIT_MILLIS); } catch (InterruptedException ie) {}
                int avail1 = inLine.available();
                System.out.println("done. "+available());

                System.out.print("Flushing...");
                inLine.flush();
                System.out.println("done.            "+available());
                System.out.print("Waiting "+(WAIT_MILLIS)+" millis...");
                try { Thread.sleep(WAIT_MILLIS); } catch (InterruptedException ie) {}
                int avail2 = inLine.available();
                System.out.println("done.  "+available());
                if (avail2 > avail1) {
                    failed = true;
                    System.out.println("Failed: Flushing with native flush() should "
                                       +"result in fewer bytes available.");
                }
                if (avail2 == 0) {
                    failed = true;
                    System.out.println("Failed: Recording after flush() did not work at all!");
                }
            }
        } finally {
            System.out.print("Closing line....");
            inLine.close();
            System.out.println("done");
        }
        return true;
    }

    public void runTests(int testRuns) {
        if (mixers.length > 0) {
            for (int num = -1; num < mixers.length; num++) {
                try {
                    if (num<0) {
                        System.out.println("------Using default line...." );
                    } else {
                        System.out.println("------Using line "+num+" from mixer "+mixers[num]+"...");
                    }
                    for (int testRun = 0; testRun < testRuns; testRun++) {
                        if (testRuns>1) {
                            System.out.println("--Run "+(testRun+1)+"/"+testRuns+":");
                        }
                        if (!recordSound(num)) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("Caught " + ex );
                }
                System.out.println("------------------------------------------------------");
                if (failed) {
                    break;
                }
            }
        } else {
            System.out.println("No mixers present. Cannot execute this test.");
        }
    }


    public static void main(String[] args) throws Exception {
        System.out.println("Test TargetDataLineFlush");
        System.out.println("This verifies that TargetDataLine.flush() actually");
        System.out.println("flushes the native buffers. This is done by");
        System.out.println("comparing a manual flush (i.e. just discarding");
        System.out.println("everything that is currently available in the TargetDataLine)");
        System.out.println("to a flushed line");
        TargetDataLineFlush app = new TargetDataLineFlush();
        int testRuns = 1;
        if (args.length > 0) {
            try {
                testRuns = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe) {
                System.out.println("Usage: java TargetDataLineFlush [number of runs]");
                System.out.println("Parameters ignored.");
            }
        }
        app.runTests(testRuns);
        if (failed) {
            throw new Exception("Test FAILED");
        }
        // test always passes if it gets here
        System.out.println("Test PASSED");
    }
}
