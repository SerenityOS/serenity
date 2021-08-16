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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * @test
 * @bug 4498848
 * @summary Sound causes crashes on Linux (part 2)
 */
public class SDLLinuxCrash implements Runnable {
    SourceDataLine sdl;
    int size;

    SDLLinuxCrash(SourceDataLine sdl, int size) {
        this.sdl = sdl;
        this.size = size - (size % 4);
    }

    public void run() {
        int written=0;
        //byte[] buffer = new byte[4096];
        byte[] buffer = data;
        out("    starting data line feed thread.");
        try {
            while (written<size) {
                int toWrite = buffer.length;
                if (toWrite+written > size) {
                    toWrite = size-written;
                }
                toWrite -= (toWrite % 4);
                //out("    writing "+toWrite+" bytes.");
                int thisWritten = sdl.write(buffer, 0, toWrite);
                if (thisWritten<toWrite) {
                    out("    only wrote "+thisWritten+" bytes instead of "+toWrite);
                }
                if (thisWritten<=0) {
                    break;
                }
                written += thisWritten;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        out("    leaving data line feed thread.");
    }

    public static long bytes2Ms(long bytes, AudioFormat format) {
        return (long) (bytes/format.getFrameRate()*1000/format.getFrameSize());
    }

    static int staticLen=1000;
    static boolean addLen=true;

    public static SourceDataLine start() throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        if (addLen) {
            staticLen+=(int) (staticLen/5)+1000;
        } else {
            staticLen-=(int) (staticLen/5)+1000;
        }
        if (staticLen>8*44100*4) {
            staticLen = 8*44100*4;
            addLen=!addLen;
        }
        if (staticLen<1000) {
            staticLen = 1000;
            addLen=!addLen;
        }
        int len = staticLen;
        len -= (len % 4);
        out("    preparing to play back "+len+" bytes == "+bytes2Ms(len, format)+"ms audio...");

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine sdl = (SourceDataLine) AudioSystem.getLine(info);
        sdl.addLineListener(new LineListener() {
                public void update(LineEvent e) {
                    if (e.getType() == LineEvent.Type.STOP) {
                        out("    calling close() from event dispatcher thread");
                        ((SourceDataLine) e.getSource()).close();
                    }
                    else if (e.getType() == LineEvent.Type.CLOSE) {
                    }
                }
            });

        out("    opening...");
        sdl.open();
        out("    starting...");
        sdl.start();
        (new Thread(new SDLLinuxCrash(sdl, len))).start();
        return sdl;
    }

    public static void main(String[] args) throws Exception {
        if (!isSoundcardInstalled()) {
            return;
        }

        try {
            int COUNT=10;
            out();
            out("4498848 Sound causes crashes on Linux (testing with SourceDataLine)");
            if (args.length>0) {
                COUNT=Integer.parseInt(args[0]);
            }
            for (int i=0; i<COUNT; i++) {
                out("  trial "+(i+1)+"/"+COUNT);
                SourceDataLine sdl = start();
                int waitTime = 500+(1000*(i % 2)); // every 2nd time wait 1500, rather than 500ms.
                out("    waiting for "+waitTime+" ms for audio playback to stop...");
                Thread.sleep(waitTime);
                out("    calling close() from main thread");
                sdl.close();
                // let the subsystem enough time to actually close the soundcard
                out("    waiting for 2 seconds...");
                Thread.sleep(2000);
                out();
            }
            out("  waiting for 1 second...");
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            out("  waiting for 1 second");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {}
            // do not fail if no audio device installed - bug 4742021
            if (!(e instanceof LineUnavailableException)) {
                throw e;
            }
        }
        out("Test passed");
    }

    static void out() {
        out("");
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
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



    static final byte[] data = new byte[] {
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120,
        123, 110, 100, 60, 11, 10, 10, 10, 9, 9,
        9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6,
        7, 7, 7, 7, 8, 8, 8, 8, 9, 9,
        9, 9, 10, 10, 10, 11, 11, 60, 100, 110, 120, 122, 122
    };

}
