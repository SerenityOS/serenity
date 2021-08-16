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
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 4679187
 * @summary Clip.open() throws unexpected Exceptions. verifies that clip,
 *          sourcedataline and targetdataline throw IllegalArgumentExcepotion if
 *          any field in the format is AudioFormat.NOT_SPECIFIED
 */
public class ClipOpenException {
    static boolean failed = false;

    static byte[] audioData = new byte[2048];
    static AudioFormat[] formats = {
        new AudioFormat(AudioSystem.NOT_SPECIFIED,
                        AudioSystem.NOT_SPECIFIED,
                        AudioSystem.NOT_SPECIFIED,
                        true, false),
        new AudioFormat(0, 0, 0, true, false)
    };
    static AudioFormat infoFormat = new AudioFormat(44100.0f,
                                                16,
                                                1,
                                                true, false);
    static DataLine.Info clipInfo = new DataLine.Info(Clip.class, infoFormat);
    static DataLine.Info sdlInfo = new DataLine.Info(SourceDataLine.class, infoFormat);
    static DataLine.Info tdlInfo = new DataLine.Info(TargetDataLine.class, infoFormat);


    public static void print(String s) {
        System.out.print(s);
    }
    public static void println(String s) {
        System.out.println(s);
    }

    public static void test(Line line) {
        for (int format = 0; format < formats.length; format++) {
            try {
                println("  Opening the line with format "+(format+1));
                if (line instanceof Clip) {
                    ((Clip) line).open(formats[format], audioData, 0, audioData.length);
                } else
                if (line instanceof SourceDataLine) {
                    ((SourceDataLine) line).open(formats[format]);
                } else
                if (line instanceof TargetDataLine) {
                    ((TargetDataLine) line).open(formats[format]);
                } else {
                    println("    Unknown type of line: "+line.getClass());
                    return;
                }
                println("    No exception! not OK.");
                failed = true;
            } catch (IllegalArgumentException iae) {
                println("    IllegalArgumentException: "+iae.getMessage());
                println("    OK");
            } catch (LineUnavailableException lue) {
                println("    LineUnavailableException: "+lue.getMessage());
                println("    Probably incorrect, but may happen if the test system is correctly set up.");
            } catch (Exception e) {
                println("    Unexpected Exception: "+e.toString());
                println("    NOT OK!");
                failed = true;
            }
            println("    Closing line.");
            line.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        int succMixers = 0;
        println("Using formats: ");
        for (int i = 0 ; i<formats.length; i++) {
                println(""+(i+1)+". "+formats[i]);
        }
        for (int i=0; i<mixers.length; i++) {
            boolean succ = false;
            try {
                Mixer mixer = AudioSystem.getMixer(mixers[i]);
                println("Mixer "+mixer.getMixerInfo()+":");
                if (mixer.isLineSupported(clipInfo)) {
                    println("Getting clip from mixer...");
                    Clip clip = (Clip) mixer.getLine(clipInfo);
                    succ = true;
                    test(clip);
                }
                if (mixer.isLineSupported(sdlInfo)) {
                    println("Getting source data line from mixer...");
                    SourceDataLine sdl = (SourceDataLine) mixer.getLine(sdlInfo);
                    succ = true;
                    test(sdl);
                }
                if (mixer.isLineSupported(tdlInfo)) {
                    println("Getting target data line from mixer...");
                    TargetDataLine tdl = (TargetDataLine) mixer.getLine(tdlInfo);
                    succ = true;
                    test(tdl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (succ) {
                succMixers++;
            }
        }
        if (succMixers == 0) {
            println("No mixers available! ");
            println("Cannot run test. NOT FAILED");
        }
        else if (failed) {
            throw new Exception("Test FAILED");
        }
        println("Test passed.");
    }
}
