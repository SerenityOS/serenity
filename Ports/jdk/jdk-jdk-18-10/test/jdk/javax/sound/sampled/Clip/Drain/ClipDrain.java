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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4732218
 * @summary Clip.drain does not actually block until all I/O is complete as
 *          documented.
 */
public class ClipDrain {
    static int successfulTests = 0;
    static AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
    // create a 10-second file
    static byte[] soundData = new byte[(int) (format.getFrameRate() * format.getFrameSize() * 10)];

    static int TOLERANCE_MS = 2500; // how many milliseconds too short is tolerated...

    private static void doMixerClip(Mixer mixer) throws Exception {
        boolean waitedEnough=false;
        try {
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            Clip clip = (Clip) mixer.getLine(info);
            clip.open(format, soundData, 0, soundData.length);

            // sanity
            if (clip.getMicrosecondLength()/1000 < 9900) {
                throw new Exception("clip's microsecond length should be at least 9900000, but it is "+clip.getMicrosecondLength());
            }
            long start = System.currentTimeMillis();

            System.out.println(" ---------- start --------");
            clip.start();
            // give time to actually start it. ALSA implementation needs that...
            Thread.sleep(300);
            System.out.println("drain ... ");
            clip.drain();
            long elapsedTime = System.currentTimeMillis() - start;
            System.out.println("close ... ");
            clip.close();
            System.out.println("... done");
            System.out.println("Playback duration: "+elapsedTime+" milliseconds.");
            waitedEnough = elapsedTime >= ((clip.getMicrosecondLength() / 1000) - TOLERANCE_MS);
        } catch (Throwable t) {
                System.out.println("  - Caught exception. Not failed.");
                System.out.println("  - "+t.toString());
                return;
        }
        if (!waitedEnough) {
            throw new Exception("Drain did not wait long enough to play entire clip.");
        }
        successfulTests++;
    }


    private static void doAll() throws Exception {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (int i=0; i<mixers.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixers[i]);
            System.out.println("--------------");
            System.out.println("Testing mixer: "+mixers[i]);
            doMixerClip(mixer);
        }
        if (mixers.length==0) {
            System.out.println("No mixers available!");
        }
    }

    public static void main(String[] args) throws Exception {
        if (!isSoundcardInstalled()) {
            return;
        }
        doAll();
        if (successfulTests==0) {
            System.out.println("Could not execute any of the tests. Test NOT failed.");
        } else {
            System.out.println("Test PASSED.");
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
            System.err.println("Exception occured: "+e);
        }
        if (!result) {
            System.err.println("Soundcard does not exist or sound drivers not installed!");
            System.err.println("This test requires sound drivers for execution.");
        }
        return result;
    }
}
