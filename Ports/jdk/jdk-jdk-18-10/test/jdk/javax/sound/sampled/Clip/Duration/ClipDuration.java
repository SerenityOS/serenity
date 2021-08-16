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
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4237703
 * @summary Check that Clip.getMicrosecondLength() returns correct value.
 */
public class ClipDuration {

    public static int run(Mixer m) {
        int res=1; // failed
        int frameCount = 441000; // lets say 10 seconds
        AudioFormat f = new AudioFormat(44100.0f, 16, 2, true, false);
        AudioInputStream audioInputStream =
            new AudioInputStream(new ByteArrayInputStream(new byte[frameCount * f.getFrameSize()]),
                                 f, frameCount);
        AudioFormat     format = audioInputStream.getFormat();
        Clip m_clip = null;
        try {
            if (m == null) {
                m_clip = (Clip) AudioSystem.getClip();
            } else {
                DataLine.Info   info = new DataLine.Info(Clip.class, format, AudioSystem.NOT_SPECIFIED);
                m_clip = (Clip) m.getLine(info);
            }
            System.out.println("Got clip: "+m_clip);
            m_clip.open(audioInputStream);
            long microseconds=m_clip.getMicrosecondLength();
            System.out.println("getFrameLength()="+m_clip.getFrameLength()+" frames");
            System.out.println("getMicrosecondLength()="+microseconds+" us");
            if (Math.abs(microseconds-10000000)<50) {
                System.out.println("->Clip OK");
                res=0; // passes if less than 50us error
            }
        } catch (LineUnavailableException luae) {
            System.err.println(luae);
            res = 3; // line not available, test not failed
        } catch (Throwable t) {
            System.out.println("->Exception:"+t);
            t.printStackTrace();
            res=2; // exception
        }
        if (m_clip != null) {
            m_clip.close();
        }
        return res;
    }



    public static void main(String[] args) throws Exception     {
        if (isSoundcardInstalled()) {
            int res=3;
            res = run(null);
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (int i = 0; i<infos.length; i++) {
                try {
                        Mixer m = AudioSystem.getMixer(infos[i]);
                        int r = run(m);
                        if (r == 1) res = 1;
                } catch (Exception e) {
                }
            }
            if (res!=1) {
                System.out.println("Test passed");
            } else {
                if (res==2) {
                    System.err.println("Test could not execute: test threw unexpected Exception.");
                    throw new Exception("Test threw exception");
                }
                else if (res==3) {
                    System.err.println("Test could not execute: please install an audio device");
                    return;
                }
                throw new Exception("Test returned wrong length");
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
