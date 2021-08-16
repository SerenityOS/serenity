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

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * @test
 * @bug 4834461
 * @summary Applet hang when you load it during sound card is in use
 * @run main/manual PlaySine
 */
public class PlaySine {

    static int sampleRate = 8000;
    static double frequency = 2000.0;
    static double RAD = 2.0 * Math.PI;

    static byte[] audioData = new byte[sampleRate/2];
    static SourceDataLine source;
    static Mixer mixer = null;

    static AudioInputStream ais = null;
    static AudioFormat audioFormat;
    static String filename;

    public static void constructAIS() {
        try {
            ais = AudioSystem.getAudioInputStream(new File(filename));
        } catch (Exception e) {
            println("ERROR: could not open "+filename+": "+e.getMessage());
        }
    }

    public static void print(String s) {
        System.out.print(s);
    }
    public static void println(String s) {
        System.out.println(s);
    }

    public static void key() {
        println("");
        print("Press ENTER to continue...");
        try {
            System.in.read();
        } catch (IOException ioe) {
        }
    }

    static int audioLen = -1;
    static int audioOffset = -1;

    public static void writeData() {
        if (audioLen == -1) {
            audioLen = audioData.length;
        }
        if (audioOffset < 0) {
            audioOffset = audioLen;
        }
        try {
            if (audioOffset >= audioLen) {
                audioOffset = 0;
                if (ais!=null) {
                    do {
                        audioLen = ais.read(audioData, 0, audioData.length);
                        if (audioLen < 0) {
                            constructAIS();
                        }
                    } while (audioLen < 0);
                }
            }
            int toWrite = audioLen - audioOffset;
            int written = source.write(audioData, audioOffset, toWrite);
            audioOffset+=written;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static int play(boolean shouldPlay) {
        int res = 0;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            println("Getting line from mixer...");
            source = (SourceDataLine) mixer.getLine(info);
            println("Opening line...");
            println("  -- if the program is hanging here, kill the process that has blocks the audio device now.");
            source.open(audioFormat);
            println("Starting line...");
            source.start();
            println("Writing audio data for 1 second...");
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 1000) {
                writeData();
                Thread.sleep(100);
            }
            res = 1;
        } catch (IllegalArgumentException iae) {
            println("IllegalArgumentException: "+iae.getMessage());
            println("Sound device cannot handle this audio format.");
            println("ERROR: Test environment not correctly set up.");
            if (source!=null) {
                source.close();
            }
            return 3;
        } catch (LineUnavailableException lue) {
            println("LineUnavailableException: "+lue.getMessage());
            if (shouldPlay) {
                println("ERROR: the line should be available now!.");
                println("       Verify that you killed the other audio process.");
            } else {
                println("Correct behavior! the bug is fixed.");
            }
            res = 2;
        } catch (Exception e) {
            println("Unexpected Exception: "+e.toString());
        }
        if (source != null) {
            println("Draining...");
            try {
                source.drain();
            } catch (NullPointerException npe) {
                println("(NullPointerException: bug fixed in J2SE 1.4.2");
            }
            println("Stopping...");
            source.stop();
            println("Closing...");
            source.close();
            source = null;
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        println("This is an interactive test. You can run it with a filename as argument");
        println("It is only meant to be run on linux, with the (old) OSS kernel drivers (/dev/dsp)");
        println("This test should not be run on systems with ALSA installed, or kernel 2.6 or higher.");
        println("");
        println("The test verifies that Java Sound fails correctly if another process is blocking");
        println("the audio device.");
        println("");
        println("Checking sanity...");
        Mixer.Info[] mixers=null;

        mixers = AudioSystem.getMixerInfo();
        for (int i=0; i<mixers.length; i++) {
            try {
                Mixer thisMixer = AudioSystem.getMixer(mixers[i]);
                String mixerName = thisMixer.getMixerInfo().getName();
                if (mixerName.indexOf("Java Sound")>=0
                    && mixerName.indexOf("Engine")>=0) {
                    mixer = thisMixer;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mixer == null) {
            if (mixers.length==0) {
                System.out.println("ERROR: No mixers available!");
            } else {
                println("ERROR: Java Sound Engine could not be found.");
            }
            println("Cannot run this test.");
            return;
        }
        println("  ...using mixer "+mixer.getMixerInfo());

        String osname = System.getProperty("os.name");
        if ((osname == null) || (osname.toLowerCase().indexOf("linux")<0)) {
            println("ERROR: not running on linux (you are running on "+osname+")");
            return;
        }
        println("  ...running on "+osname);
        println("  ...sanity test OK.");

        filename = null;
        if (args.length>0) {
            File f = new File(args[0]);
            if (f.exists()) {
                filename = args[0];
                println("Opening "+filename);
                constructAIS();
                if (ais!=null) {
                    audioFormat = ais.getFormat();
                }
            }
        }
        if (ais == null) {
            println("Using self-generated sine wave for playback");
            audioFormat = new AudioFormat((float)sampleRate, 8, 1, true, true);
            for (int i=0; i<audioData.length; i++) {
                audioData[i] = (byte)(Math.sin(RAD*frequency/sampleRate*i)*127.0);
            }
        }

        println("");
        println("Now, on a second console, run the following command:");
        println("    cat - < /dev/zero > /dev/dsp");
        key();
        println("After you press ENTER now, the mixer will be opened.");
        println("There are 3 possible cases that can occur:");
        println("1) you'll hear a sine wave");
        println("   -> you are running with mixing OSS drivers. ");
        println("      Some soundcards only provide mixing OSS drivers.");
        println("      Test environment not valid. ");
        println("      Repeat on another machine where you can reproduce the bug first.");
        println("2) this program stops doing anything after 'Opening line...'");
        println("   -> this is the bug.");
        println("      Kill the command on the other console with Ctrl-C, this program");
        println("      should continue working then.");
        println("3) this program reports a LineUnavailableException");
        println("   -> bug is fixed.");
        println("      OR you run with non-blocking OSS drivers.");
        println("      make sure that you can reproduce this bug first with e.g. J2SE 1.4.1");
        key();
        int playedFirst = play(false);
        int playedSecond = 0;

        if (playedFirst == 2) {
            println("");
            println("Now kill the other process with Ctrl-C.");
            println("After that, this program should be able to play ");
            println("the sine wave without problems.");
            key();
            playedSecond = play(true);
        }
        println("");
        if (playedFirst == 1) {
            println("Test FAILED.");
        }
        else if (playedFirst == 2 && playedSecond == 1) {
            println("Test SUCCESSFUL");
        } else {
            println("Test not failed (but not successful either...).");
        }
    }
}
