/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 5053380
 * @summary Verify that getting a line initializes it with the format in
 *          DataLine.Info
 */
public class LineDefFormat {

    final static int samplerate = 22050;
    static int passed = 0;
    static int failed = 0;

    private static void doLine1(DataLine line, AudioFormat format) {
        try {
            System.out.println("  - got line: "+line);
            System.out.println("  - line has format: "+line.getFormat());
            if (!line.getFormat().matches(format)) {
                System.out.println("  ## Error: expected this format: "+format);
                failed++;
            } else {
                passed++;
            }
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - "+t.toString());
        }
    }

    private static void doLine2(DataLine line, AudioFormat format) {
        try {
            System.out.println("  - call to open()");
            line.open();
            try {
                System.out.println("  - line has format: "+line.getFormat());
                if (!line.getFormat().matches(format)) {
                    System.out.println("## Error: expected this format: "+format);
                    failed++;
                } else {
                    passed++;
                }
            } finally {
                line.close();
                System.out.println("  - closed");
            }
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - "+t.toString());
        }
    }

    private static void doMixerClip(Mixer mixer, AudioFormat format) {
        if (mixer==null) return;
        try {
            System.out.println("Clip from mixer "+mixer+":");
            System.out.println("   "+mixer.getMixerInfo());
                DataLine.Info info = new DataLine.Info(
                                          Clip.class,
                                          format);

            if (mixer.isLineSupported(info)) {
                Clip clip = (Clip) mixer.getLine(info);
                doLine1(clip, format);
            } else {
                System.out.println("  - Line not supported");
            }
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - "+t.toString());
        }
    }

    private static void doMixerSDL(Mixer mixer, AudioFormat format) {
        if (mixer==null) return;
        try {
            System.out.println("SDL from mixer "+mixer+":");
                DataLine.Info info = new DataLine.Info(
                                          SourceDataLine.class,
                                          format);

            if (mixer.isLineSupported(info)) {
                SourceDataLine sdl = (SourceDataLine) mixer.getLine(info);
                doLine1(sdl, format);
                doLine2(sdl, format);
            } else {
                System.out.println("  - Line not supported");
            }
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - "+t.toString());
        }
    }

    private static void doMixerTDL(Mixer mixer, AudioFormat format) {
        if (mixer==null) return;
        try {
            System.out.println("TDL from mixer "+mixer+":");
                DataLine.Info info = new DataLine.Info(
                                          TargetDataLine.class,
                                          format);
            if (mixer.isLineSupported(info)) {
                TargetDataLine tdl = (TargetDataLine) mixer.getLine(info);
                doLine1(tdl, format);
                doLine2(tdl, format);
            } else {
                System.out.println("  - Line not supported");
            }
        } catch (Throwable t) {
            System.out.println("  - Caught exception. Not failed.");
            System.out.println("  - "+t.toString());
        }
    }

    private static void doAll() throws Exception {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
               AudioFormat pcm;
        for (int i=0; i<mixers.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixers[i]);
            pcm = new AudioFormat(samplerate, 16, 1, true, false);
            doMixerClip(mixer, pcm);
            pcm = new AudioFormat(samplerate, 8, 1, false, false);
            doMixerSDL(mixer, pcm);
            pcm = new AudioFormat(samplerate, 16, 2, true, true);
            doMixerTDL(mixer, pcm);
        }
        if (mixers.length==0) {
            System.out.println("No mixers available!");
        }

    }

    public static void main(String args[]) throws Exception{
        doAll();
        if (passed==0 && failed==0) {
            System.out.println("Could not execute any of the tests. Test NOT failed.");
        } else if (failed == 0) {
            System.out.println("Test PASSED.");
        } else {
            throw new Exception("Test FAILED!");
        }
    }
}
