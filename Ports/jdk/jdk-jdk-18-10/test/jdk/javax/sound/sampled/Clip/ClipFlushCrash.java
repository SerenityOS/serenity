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
 * @bug 4946945
 * @summary Crash in javasound while running TicTacToe demo applet tiger b26
 */
public class ClipFlushCrash {
    static int frameCount = 441000; // lets say 10 seconds
    static AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
    static ByteArrayInputStream bais =
      new ByteArrayInputStream(new byte[frameCount * format.getFrameSize()]);

    static int success = 0;

    public static void run(Mixer m) {
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

            AT at1 = new AT(clip, "flush thread", 123) {
                public void doAction() throws Exception {
                        log("flush");
                        clip.flush();
                }
            };
            AT at2 = new AT(clip, "setFramePosition thread", 67) {
                public void doAction() throws Exception {
                        int pos = (int) (Math.random() * clip.getFrameLength());
                        log("setPosition to frame "+pos);
                        clip.setFramePosition(pos);
                }
            };
            AT at3 = new AT(clip, "start/stop thread", 300) {
                public void doAction() throws Exception {
                        if (clip.isRunning()) {
                                log("stop");
                                clip.stop();
                        } else {
                                log("start");
                                clip.setFramePosition(0);
                                clip.start();
                        }
                }
            };
            AT at4 = new AT(clip, "open/close thread", 600) {
                public synchronized void doAction() throws Exception {
                        log("close");
                        clip.close();
                        wait(50);
                        if (!terminated) {
                                log("open");
                                bais.reset();
                                clip.open(new AudioInputStream(bais, format, frameCount));
                        }
                }
            };

            out(" clip.start");
            clip.start();
            out(" for 10 seconds, call start/stop, setFramePosition, and flush from other threads");
            at1.start();
            at2.start();
            at3.start();
            at4.start();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {}
            out(" finished.");
                at1.terminate();
                at2.terminate();
                at3.terminate();
                at4.terminate();
            out(" clip.close()");
            clip.close();
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

    public static void main(String[] args) throws Exception     {
        if (isSoundcardInstalled()) {
                bais.mark(0);
            run(null);
            Mixer.Info[] infos = AudioSystem.getMixerInfo();
            for (int i = 0; i<infos.length; i++) {
                try {
                        Mixer m = AudioSystem.getMixer(infos[i]);
                        run(m);
                } catch (Exception e) {
                }
            }
            if (success > 0) {
                out("No crash -> Test passed");
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

    private abstract static class AT extends Thread {
        protected boolean terminated = false;
        protected Clip clip;
        private int waitTime;

        public AT(Clip clip, String name, int waitTime) {
                super(name);
                this.clip = clip;
                this.waitTime = waitTime;
        }

        public abstract void doAction() throws Exception;

                public void run() {
                        log("start");
                        while (!terminated) {
                                try {
                                        synchronized(this) {
                                            wait(waitTime);
                                        }
                                        if (!terminated) {
                                                doAction();
                                        }
                                } catch(Exception e) {
                                        log("exception: "+e);
                                }
                        }
                        log("exit");
                }

                public synchronized void terminate() {
                        log("terminate");
                        terminated = true;
                        notifyAll();
                }

        protected void log(String s) {
            //out("   "+Thread.currentThread().getId()+" "+getName()+": "+s);
            out("   "+getName()+": "+s);
        }
    }
}
