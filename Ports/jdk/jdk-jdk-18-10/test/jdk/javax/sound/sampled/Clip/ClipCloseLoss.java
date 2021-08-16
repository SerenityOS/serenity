/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4946913 8178403
 * @summary DirectClip doesn't kill the thread correctly, sometimes
 * @run main/othervm ClipCloseLoss
 */
public class ClipCloseLoss {
    static int frameCount = 441000; // lets say 10 seconds
    static AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
    static ByteArrayInputStream bais =
    new ByteArrayInputStream(new byte[frameCount * format.getFrameSize()]);

    static int success = 0;
    static boolean failed = false;

    public static void run(Mixer m, long sleep) {
        Clip clip = null;
        try {
            if (m == null) {
                out("Using default mixer");
                clip = (Clip) AudioSystem.getClip();
            } else {
                out("Using mixer: "+m);
                DataLine.Info info = new DataLine.Info(Clip.class, format, AudioSystem.NOT_SPECIFIED);
                clip = (Clip) m.getLine(info);
            }
            out(" got clip: "+clip);
            if (!clip.getClass().toString().contains("Direct")) {
                out(" no direct audio clip -> do not test.");
                return;
            }

            out(" open");
            bais.reset();
            clip.open(new AudioInputStream(bais, format, frameCount));

            out(" clip.close()");
            // emulates a different delay between open() and close()
            Thread.sleep(sleep);
            //long t = System.currentTimeMillis();
            clip.close();
            //if (System.currentTimeMillis() - t > 1950) {
            //  out(" clip.close needed more than 2 seconds! Causes failure of this test.");
            //  failed = true;
            //}
            out(" clip closed");
            success++;
        } catch (LineUnavailableException luae) {
            // line not available, test not failed
            System.err.println(luae);
        } catch (IllegalArgumentException iae) {
            // line not available, test not failed
            System.err.println(iae);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static int getClipThreadCount() {
        int ret = 0;
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) { tg = tg.getParent(); }
        Thread[] threads = new Thread[500];
        int count = tg.enumerate(threads, true);
        for (int i = 0; i < count; i++) {
                if (threads[i].getName().contains("Direct")
                    && threads[i].getName().contains("Clip")) {
                        out("Found Direct Clip thread object: "+threads[i]);
                        ret++;
                }
        }
        return ret;
    }

    public static void main(String[] args) throws Exception    {
        if (isSoundcardInstalled()) {
            bais.mark(0);
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (int sleep = 0; sleep < 100; ++sleep) {
                run(null, sleep);
                for (int i = 0; i < infos.length; i++) {
                    try {
                        Mixer m = AudioSystem.getMixer(infos[i]);
                        run(m, sleep);
                    } catch (Exception e) {
                    }
                }
            }
            out("Waiting 1 second to dispose of all threads");
            Thread.sleep(1000);
            if (getClipThreadCount() > 0) {
                out("Unused clip threads exist! Causes test failure");
                failed = true;
            }
            if (failed) throw new Exception("Test FAILED!");
            if (success > 0) {
                out("Test passed.");
            } else {
                System.err.println("Test could not execute: please install an audio device");
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

    public static void out(String s) {
        /*long t = System.nanoTime() / 1000000l;
        String ts = ""+(t % 1000);
        while (ts.length() < 3) ts = "0"+ts;
        System.out.println(""+(t/1000)+":"+ts+" "+s);
        System.out.flush();*/
        System.out.println(s);
    }
}
