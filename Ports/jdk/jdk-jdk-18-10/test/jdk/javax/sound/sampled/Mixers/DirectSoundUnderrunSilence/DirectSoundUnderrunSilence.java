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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * This is utility class for Test5032020.
 */
public class DirectSoundUnderrunSilence {

    static int sampleRate = 8000;
    static double frequency = 1000.0;
    static double RAD = 2.0 * Math.PI;

    static byte[] audioData = new byte[sampleRate/8];
    static DataLine.Info info;
    static SourceDataLine source;

    //static AudioInputStream ais = null;
    static AudioFormat audioFormat;
    //static String filename;

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
        println("");
    }

    public static void play(Mixer mixer) {
        int res = 0;
        try {
            println("Getting SDL from mixer...");
            source = (SourceDataLine) mixer.getLine(info);
            println("Opening SDL...");
            source.open(audioFormat);
            println("Writing data to SDL...");
            source.write(audioData, 0, audioData.length);
            println("Starting SDL...");
            source.start();
            println("Now open your ears:");
            println("You should have heard a short tone,");
            println("followed by silence (no repeating tones).");
            key();
            source.write(audioData, 0, audioData.length);
            println("Now you should have heard another short tone.");
            println("If you did not hear a second tone, or more than 2 tones,");
            println("the test is FAILED.");
            println("otherwise, if you heard a total of 2 tones, the bug is fixed.");
            key();
        } catch (IllegalArgumentException iae) {
            println("IllegalArgumentException: "+iae.getMessage());
            println("Sound device cannot handle this audio format.");
            println("ERROR: Test environment not correctly set up.");
            if (source!=null) {
                source.close();
                source = null;
            }
            return;
        } catch (LineUnavailableException lue) {
            println("LineUnavailableException: "+lue.getMessage());
            println("This is normal for some mixers.");
        } catch (Exception e) {
            println("Unexpected Exception: "+e.toString());
        }
        if (source != null) {
            println("Stopping...");
            source.stop();
            println("Closing...");
            source.close();
            println("Closed.");
            source = null;
        }
    }

    public static void main(String[] args) throws Exception {
        println("This is an interactive test for DirectAudio.");
        println("If it's impossible to play data after an underun, the test fails.");
        println("");
        println("Make sure that you have speakers connected");
        println("and that the system mixer is not muted.");
        println("Also stop all other programs playing sounds:");
        println("It has been seen that it alters the results.");
        println("");
        println("Press a key to start the test.");
        key();
        Mixer.Info[] mixers=null;

        println("   ...using self-generated sine wave for playback");
        audioFormat = new AudioFormat((float)sampleRate, 8, 1, true, true);
        for (int i=0; i<audioData.length; i++) {
            audioData[i] = (byte)(Math.sin(RAD*frequency/sampleRate*i)*127.0);
        }
        info = new DataLine.Info(SourceDataLine.class, audioFormat);

        mixers = AudioSystem.getMixerInfo();
        int succMixers = 0;
        for (int i=0; i<mixers.length; i++) {
            println(""+mixers[i]+":");
            if ((mixers[i].getName()+mixers[i].getDescription()+mixers[i].getVendor()).indexOf("Direct") < 0) {
                println("  ->not a DirectAudio Mixer!");
            } else {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixers[i]);
                    if (!mixer.isLineSupported(info)) {
                        println("  ->doesn't support SourceDataLine!");
                    } else {
                        succMixers++;
                        println("  -> is getting tested.");
                        play(mixer);
                    }
                } catch (Exception e) {
                    println("  -> Exception occured: "+e);
                    e.printStackTrace();
                }
            }
        }
        if (succMixers == 0) {
            println("No DirectAudio mixers available! ");
            println("Cannot run test.");
        }
    }

}
