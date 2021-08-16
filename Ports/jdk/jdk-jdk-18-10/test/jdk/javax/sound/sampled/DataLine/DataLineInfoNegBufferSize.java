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
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 5021234
 * @summary Using -2 for buffer size will fail retrieval of lines
 */
public class DataLineInfoNegBufferSize {

    /**
     * returns:
     * 0: OK
     * 1: IAE
     * 2: other exception
     * 3: line not available
     */
    public static int run(Mixer m, int bufferSize) {
        int res;
        int frameCount = 441000; // lets say 10 seconds
        AudioFormat f = new AudioFormat(44100.0f, 16, 2, true, false);
        Clip clip = null;
        try {
            System.out.println("Requesting clip from Mixer "
                               +(m==null?"default":m.toString())
                               +" with bufferSize"+bufferSize);
            DataLine.Info info = new DataLine.Info(Clip.class, f, bufferSize);
            if (m==null) {
                clip = (Clip) AudioSystem.getLine(info);
            } else {
                clip = (Clip) m.getLine(info);
            }
            System.out.println("Got clip: "+clip+" with Buffer size "+clip.getBufferSize());

            res = 0;
        } catch (LineUnavailableException luae) {
            System.out.println(luae);
            res = 3; // line not available
        } catch (IllegalArgumentException iae) {
            System.out.println(iae);
            res = 1;
        } catch (Throwable t) {
            System.out.println("->Exception:"+t);
            t.printStackTrace();
            res=2; // other exception
        }
        return res;
    }

    public static void main(String[] args) throws Exception     {
        if (isSoundcardInstalled()) {
            int res=0;
            int count = 0;
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (int i = -1; i<infos.length; i++) {
                try {
                    Mixer m;
                    if (i == -1) {
                        m = null;
                    } else {
                        m = AudioSystem.getMixer(infos[i]);
                    }
                    int r = run(m, AudioSystem.NOT_SPECIFIED);
                    // only continue if successful
                    if (r == 0) {
                        count++;
                        r = run(m, -2);
                        if (r == 1) {
                            // only fail if IAE was thrown
                            System.out.println("#FAILED: using -2 for buffer size does not work!");
                            res = 1;
                        }
                    }
                } catch (Exception e) {
                }
            }
            if (res!=1) {
                System.out.println("Test passed");
            } else {
                if (count == 0) {
                    System.err.println("Test could not execute -- no suitable mixers installed. NOT failed");
                }
                throw new Exception("Test FAILED!");
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
            System.err.println("Exception occured: "+e);
        }
        if (!result) {
            System.err.println("Soundcard does not exist or sound drivers not installed!");
            System.err.println("This test requires sound drivers for execution.");
        }
        return result;
    }
}
